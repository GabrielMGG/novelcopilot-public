package com.novelcopilot.backend;

import com.novelcopilot.backend.config.AiRateLimitProperties;
import com.novelcopilot.backend.config.GlobalExceptionHandler;
import com.novelcopilot.backend.config.NovelAiConfigProperties;
import com.novelcopilot.backend.entity.AiRateLimitCounter;
import com.novelcopilot.backend.repository.AiRateLimitCounterRepository;
import com.novelcopilot.backend.repository.ModelPricingRepository;
import com.novelcopilot.backend.service.AiProviderResolver;
import com.novelcopilot.backend.service.AiRateLimitExceededException;
import com.novelcopilot.backend.service.AiRateLimitService;
import com.novelcopilot.backend.service.ChapterVersionConflictException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "SUPABASE_URL=http://localhost:8000",
    "SUPABASE_ANON_KEY=fake-key",
    "SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/testdb",
    "DB_USER=test",
    "DB_PASSWORD=test",
    "GEMINI_API_KEY=fake-gemini",
    "GROQ_API_KEY=fake-groq"
})
class BackendApplicationTests {

    @Autowired
    private AiRateLimitService rateLimitService;

    @Autowired
    private AiRateLimitCounterRepository rateLimitRepository;

    @Autowired
    private AiRateLimitProperties rateLimitProperties;

    @Autowired
    private NovelAiConfigProperties aiConfig;

    @Autowired
    private AiProviderResolver providerResolver;

    @Autowired
    private ModelPricingRepository modelPricingRepository;

    @BeforeEach
    void configureRateLimits() {
        rateLimitRepository.deleteAll();
        rateLimitProperties.setEnabled(true);
        rateLimitProperties.setGeneralLimit(3);
        rateLimitProperties.setGeneralWindowSeconds(3600);
        rateLimitProperties.setExpensiveLimit(2);
        rateLimitProperties.setExpensiveWindowSeconds(3600);
        rateLimitProperties.setBenchmarkRunLimit(1);
        rateLimitProperties.setBenchmarkRunWindowSeconds(3600);
        rateLimitProperties.setBenchmarkRetryLimit(2);
        rateLimitProperties.setBenchmarkRetryWindowSeconds(3600);
    }

    @AfterEach
    void restoreRateLimits() {
        rateLimitRepository.deleteAll();
        rateLimitProperties.setEnabled(true);
        rateLimitProperties.setGeneralLimit(10);
        rateLimitProperties.setGeneralWindowSeconds(60);
        rateLimitProperties.setExpensiveLimit(3);
        rateLimitProperties.setExpensiveWindowSeconds(300);
        rateLimitProperties.setBenchmarkRunLimit(1);
        rateLimitProperties.setBenchmarkRunWindowSeconds(3600);
        rateLimitProperties.setBenchmarkRetryLimit(2);
        rateLimitProperties.setBenchmarkRetryWindowSeconds(3600);
    }

	@Test
	void contextLoads() {
	}

    @Test
    void activatesCloudflareGemmaForLiteraryGenerationAndIndependentSummaries() {
        assertEquals("cloudflare", aiConfig.getProvider());
        assertEquals("@cf/google/gemma-4-26b-a4b-it", aiConfig.getPrimaryModel());
        assertEquals("cloudflare", aiConfig.getSummary().getProvider());
        assertEquals("@cf/google/gemma-4-26b-a4b-it", aiConfig.getSummary().getModel());
        assertEquals(1400, aiConfig.getSummary().getMaxOutputTokens());
        assertEquals("Cloudflare", providerResolver.resolve("cloudflare").providerName());
        assertEquals("Groq", providerResolver.resolve("groq").providerName());

        var activePricing = modelPricingRepository.findActivePricing(
                "cloudflare",
                "@cf/google/gemma-4-26b-a4b-it",
                LocalDateTime.now()
        ).orElseThrow();

        assertEquals(0, new java.math.BigDecimal("0.1000")
                .compareTo(activePricing.getInputPriceUsdPerMillion()));
        assertEquals(0, new java.math.BigDecimal("0.1000")
                .compareTo(activePricing.getCachedInputPriceUsdPerMillion()));
        assertEquals(0, new java.math.BigDecimal("0.3000")
                .compareTo(activePricing.getOutputPriceUsdPerMillion()));

        var activeSummaryPricing = modelPricingRepository.findActivePricing(
                "Cloudflare",
                "@cf/google/gemma-4-26b-a4b-it",
                LocalDateTime.now()
        ).orElseThrow();

        assertEquals(0, new java.math.BigDecimal("0.1000")
                .compareTo(activeSummaryPricing.getInputPriceUsdPerMillion()));
        assertEquals(0, new java.math.BigDecimal("0.1000")
                .compareTo(activeSummaryPricing.getCachedInputPriceUsdPerMillion()));
        assertEquals(0, new java.math.BigDecimal("0.3000")
                .compareTo(activeSummaryPricing.getOutputPriceUsdPerMillion()));
        assertTrue(modelPricingRepository.findActivePricing(
                "cloudflare",
                "@cf/ibm-granite/granite-4.0-h-micro",
                LocalDateTime.now()
        ).isEmpty());
    }

    @Test
    void limitsBurstPerUserWithoutAffectingAnotherUser() {
        rateLimitService.checkAiGeneration("user-a", false);
        rateLimitService.checkAiGeneration("user-a", false);
        rateLimitService.checkAiGeneration("user-a", false);

        AiRateLimitExceededException exceeded = assertThrows(
                AiRateLimitExceededException.class,
                () -> rateLimitService.checkAiGeneration("user-a", false)
        );
        assertEquals("AI_GENERAL", exceeded.getScope());
        assertTrue(exceeded.getRetryAfterSeconds() > 0);

        rateLimitService.checkAiGeneration("user-b", false);
    }

    @Test
    void appliesAdditionalLimitToExpensiveOperationsAtomically() {
        rateLimitProperties.setGeneralLimit(10);

        rateLimitService.checkAiGeneration("expensive-user", true);
        rateLimitService.checkAiGeneration("expensive-user", true);

        AiRateLimitExceededException exceeded = assertThrows(
                AiRateLimitExceededException.class,
                () -> rateLimitService.checkAiGeneration("expensive-user", true)
        );
        assertEquals("AI_EXPENSIVE", exceeded.getScope());

        List<AiRateLimitCounter> counters = rateLimitRepository.findAll();
        int generalCount = counters.stream()
                .filter(counter -> "AI_GENERAL".equals(counter.getScope()))
                .mapToInt(AiRateLimitCounter::getRequestCount)
                .sum();
        int expensiveCount = counters.stream()
                .filter(counter -> "AI_EXPENSIVE".equals(counter.getScope()))
                .mapToInt(AiRateLimitCounter::getRequestCount)
                .sum();

        assertEquals(2, generalCount);
        assertEquals(2, expensiveCount);
    }

    @Test
    void enforcesLimitUnderConcurrentBurst() throws Exception {
        rateLimitProperties.setGeneralLimit(5);
        int requests = 20;
        ExecutorService executor = Executors.newFixedThreadPool(requests);
        CountDownLatch ready = new CountDownLatch(requests);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> results = new ArrayList<>();

        try {
            for (int index = 0; index < requests; index++) {
                results.add(executor.submit(() -> {
                    ready.countDown();
                    start.await(5, TimeUnit.SECONDS);
                    try {
                        rateLimitService.checkAiGeneration("concurrent-user", false);
                        return true;
                    } catch (AiRateLimitExceededException exception) {
                        return false;
                    }
                }));
            }

            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();

            long accepted = 0;
            for (Future<Boolean> result : results) {
                if (result.get(10, TimeUnit.SECONDS)) {
                    accepted++;
                }
            }
            assertEquals(5, accepted);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void keepsBenchmarkRunAndRetryLimitsIndependent() {
        rateLimitService.checkBenchmarkRun("admin-user");
        assertThrows(
                AiRateLimitExceededException.class,
                () -> rateLimitService.checkBenchmarkRun("admin-user")
        );

        rateLimitService.checkBenchmarkRetry("admin-user");
        rateLimitService.checkBenchmarkRetry("admin-user");
        AiRateLimitExceededException retryExceeded = assertThrows(
                AiRateLimitExceededException.class,
                () -> rateLimitService.checkBenchmarkRetry("admin-user")
        );
        assertEquals("BENCHMARK_RETRY", retryExceeded.getScope());
    }

    @Test
    void returnsStructured429WithRetryAfter() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        var response = handler.handleAiRateLimitExceeded(
                new AiRateLimitExceededException("AI_GENERAL", 42)
        );

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, response.getStatusCode());
        assertEquals("42", response.getHeaders().getFirst(HttpHeaders.RETRY_AFTER));
        assertEquals("AI_RATE_LIMIT_EXCEEDED", response.getBody().get("code"));
        assertEquals(42L, response.getBody().get("retryAfterSeconds"));
    }

    @Test
    void returnsStructured409ForAStaleChapterVersion() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        var response = handler.handleChapterVersionConflict(
                new ChapterVersionConflictException(7, 3L, 4L)
        );

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("CHAPTER_VERSION_CONFLICT", response.getBody().get("code"));
        assertEquals(7, response.getBody().get("chapterId"));
        assertEquals(3L, response.getBody().get("requestedVersion"));
        assertEquals(4L, response.getBody().get("currentVersion"));
    }

    @Test
    void removesExpiredWindowsForTheSameUserAndScope() {
        AiRateLimitCounter expired = new AiRateLimitCounter();
        expired.setUserId("cleanup-user");
        expired.setScope("AI_GENERAL");
        expired.setWindowStartedAt(LocalDateTime.now().minusHours(2).withNano(0));
        expired.setRequestCount(3);
        expired.setCreatedAt(LocalDateTime.now().minusHours(2));
        expired.setUpdatedAt(LocalDateTime.now().minusHours(2));
        rateLimitRepository.saveAndFlush(expired);

        rateLimitService.checkAiGeneration("cleanup-user", false);

        long remaining = rateLimitRepository.findAll().stream()
                .filter(counter -> "cleanup-user".equals(counter.getUserId()))
                .filter(counter -> "AI_GENERAL".equals(counter.getScope()))
                .count();
        assertEquals(1, remaining);
    }
}
