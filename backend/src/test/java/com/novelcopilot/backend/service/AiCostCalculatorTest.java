package com.novelcopilot.backend.service;

import com.novelcopilot.backend.benchmark.config.AiBenchmarkParticipantProperties;
import com.novelcopilot.backend.benchmark.config.AiBenchmarkParticipantProperties.Participant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.novelcopilot.backend.entity.ModelPricing;
import com.novelcopilot.backend.repository.ModelPricingRepository;
import java.time.LocalDateTime;
import java.util.Optional;

public class AiCostCalculatorTest {

    private AiCostCalculator calculator;
    private AiBenchmarkParticipantProperties properties;
    private ModelPricingRepository modelPricingRepository;

    @BeforeEach
    void setUp() {
        properties = new AiBenchmarkParticipantProperties();
        modelPricingRepository = mock(ModelPricingRepository.class);
        
        Participant groq = new Participant();
        groq.setProvider("Groq");
        groq.setModel("qwen/qwen3.6-27b");
        groq.setInputPricePerMillion(new BigDecimal("0.60"));
        groq.setCachedInputPricePerMillion(new BigDecimal("0.60"));
        groq.setOutputPricePerMillion(new BigDecimal("3.00"));

        Participant gptOss120b = new Participant();
        gptOss120b.setProvider("Groq");
        gptOss120b.setModel("openai/gpt-oss-120b");
        gptOss120b.setInputPricePerMillion(new BigDecimal("0.15"));
        gptOss120b.setCachedInputPricePerMillion(new BigDecimal("0.075"));
        gptOss120b.setOutputPricePerMillion(new BigDecimal("0.60"));

        Participant deepSeekFree = new Participant();
        deepSeekFree.setProvider("OpenRouter");
        deepSeekFree.setModel("deepseek/deepseek-v4-flash:free");
        deepSeekFree.setInputPricePerMillion(BigDecimal.ZERO);
        deepSeekFree.setCachedInputPricePerMillion(BigDecimal.ZERO);
        deepSeekFree.setOutputPricePerMillion(BigDecimal.ZERO);

        Participant cloudflareGemma = new Participant();
        cloudflareGemma.setProvider("Cloudflare");
        cloudflareGemma.setModel("@cf/google/gemma-4-26b-a4b-it");
        cloudflareGemma.setInputPricePerMillion(new BigDecimal("0.10"));
        cloudflareGemma.setCachedInputPricePerMillion(new BigDecimal("0.10"));
        cloudflareGemma.setOutputPricePerMillion(new BigDecimal("0.30"));

        Participant cloudflareLlama = new Participant();
        cloudflareLlama.setProvider("Cloudflare");
        cloudflareLlama.setModel("@cf/meta/llama-3.3-70b-instruct-fp8-fast");
        cloudflareLlama.setInputPricePerMillion(new BigDecimal("0.29"));
        cloudflareLlama.setCachedInputPricePerMillion(new BigDecimal("0.29"));
        cloudflareLlama.setOutputPricePerMillion(new BigDecimal("2.25"));

        Participant cloudflareGlm = new Participant();
        cloudflareGlm.setProvider("Cloudflare");
        cloudflareGlm.setModel("@cf/zai-org/glm-4.7-flash");
        cloudflareGlm.setInputPricePerMillion(new BigDecimal("0.06"));
        cloudflareGlm.setCachedInputPricePerMillion(new BigDecimal("0.06"));
        cloudflareGlm.setOutputPricePerMillion(new BigDecimal("0.40"));

        Participant cloudflareQwen = new Participant();
        cloudflareQwen.setProvider("Cloudflare");
        cloudflareQwen.setModel("@cf/qwen/qwen3-30b-a3b-fp8");
        cloudflareQwen.setInputPricePerMillion(new BigDecimal("0.051"));
        cloudflareQwen.setCachedInputPricePerMillion(new BigDecimal("0.051"));
        cloudflareQwen.setOutputPricePerMillion(new BigDecimal("0.335"));

        Participant cloudflareGranite = new Participant();
        cloudflareGranite.setProvider("Cloudflare");
        cloudflareGranite.setModel("@cf/ibm-granite/granite-4.0-h-micro");
        cloudflareGranite.setInputPricePerMillion(new BigDecimal("0.017"));
        cloudflareGranite.setCachedInputPricePerMillion(new BigDecimal("0.017"));
        cloudflareGranite.setOutputPricePerMillion(new BigDecimal("0.112"));

        Participant cloudflareLlama4Scout = new Participant();
        cloudflareLlama4Scout.setProvider("Cloudflare");
        cloudflareLlama4Scout.setModel("@cf/meta/llama-4-scout-17b-16e-instruct");
        cloudflareLlama4Scout.setInputPricePerMillion(new BigDecimal("0.270"));
        cloudflareLlama4Scout.setCachedInputPricePerMillion(new BigDecimal("0.270"));
        cloudflareLlama4Scout.setOutputPricePerMillion(new BigDecimal("0.850"));

        Participant gemini = new Participant();
        gemini.setProvider("Gemini");
        gemini.setModel("gemini-3.5-flash");
        gemini.setInputPricePerMillion(new BigDecimal("1.50"));
        gemini.setCachedInputPricePerMillion(new BigDecimal("0.15"));
        gemini.setOutputPricePerMillion(new BigDecimal("9.00"));
        
        Participant openrouter = new Participant();
        openrouter.setProvider("OpenRouter");
        openrouter.setModel("qwen/qwen3-235b-a22b-2507");
        openrouter.setInputPricePerMillion(new BigDecimal("0.09"));
        openrouter.setOutputPricePerMillion(new BigDecimal("0.10"));

        properties.setParticipants(Map.ofEntries(
            Map.entry("groq-qwen-3-6-27b", groq),
            Map.entry("groq-gpt-oss", gptOss120b),
            Map.entry("openrouter-deepseek-v4-flash-free", deepSeekFree),
            Map.entry("cloudflare-gemma", cloudflareGemma),
            Map.entry("cloudflare-llama", cloudflareLlama),
            Map.entry("cloudflare-glm", cloudflareGlm),
            Map.entry("cloudflare-qwen3-30b", cloudflareQwen),
            Map.entry("cloudflare-granite", cloudflareGranite),
            Map.entry("cloudflare-llama4-scout", cloudflareLlama4Scout),
            Map.entry("gemini-flash", gemini),
            Map.entry("openrouter-qwen3-235b", openrouter)
        ));

        ModelPricing dummyQwen = new ModelPricing();
        dummyQwen.setProvider("groq");
        dummyQwen.setModel("qwen/qwen3.6-27b");
        dummyQwen.setInputPriceUsdPerMillion(new BigDecimal("0.60"));
        dummyQwen.setCachedInputPriceUsdPerMillion(new BigDecimal("0.60"));
        dummyQwen.setOutputPriceUsdPerMillion(new BigDecimal("3.00"));
        when(modelPricingRepository.findActivePricing(anyString(), anyString(), any(LocalDateTime.class)))
            .thenAnswer(inv -> {
                String p = inv.getArgument(0);
                String m = inv.getArgument(1);
                if ("groq".equalsIgnoreCase(p) && "qwen/qwen3.6-27b".equals(m)) {
                    return Optional.of(dummyQwen);
                }
                return Optional.empty();
            });

        calculator = new AiCostCalculator(properties, modelPricingRepository);
    }

    @Test
    void testGroqCost_BasicCalculation() {
        AiCostCalculator.CalculationResult res = calculator.calculateCost("groq-qwen-3-6-27b", "Groq", "qwen/qwen3.6-27b", 1000, 0, 500);
        assertNotNull(res);
        BigDecimal cost = res.costUsd();
        BigDecimal expected = new BigDecimal("0.00060").add(new BigDecimal("0.00150"));
        assertEquals(0, expected.setScale(10, RoundingMode.HALF_UP).compareTo(cost.setScale(10, RoundingMode.HALF_UP)));
    }

    @Test
    void testGroqCost_WithCachedTokens_NotDoubleCounted() {
        BigDecimal costWithCache = calculator.calculateCost("groq-qwen-3-6-27b", "Groq", "qwen/qwen3.6-27b", 1000, 300, 500).costUsd();
        BigDecimal costWithoutCache = calculator.calculateCost("groq-qwen-3-6-27b", "Groq", "qwen/qwen3.6-27b", 1000, 0, 500).costUsd();

        assertNotNull(costWithCache);
        assertEquals(0, costWithCache.compareTo(costWithoutCache));
    }

    @Test
    void testGptOss120bCost_MatchesOfficialPricingForObservedRequest() {
        BigDecimal cost = calculator.calculateCost(
                "groq-gpt-oss",
                "Groq",
                "openai/gpt-oss-120b",
                3516,
                0,
                1000
        ).costUsd();

        BigDecimal expected = new BigDecimal("0.0011274");
        assertEquals(0, expected.setScale(10, RoundingMode.HALF_UP)
                .compareTo(cost.setScale(10, RoundingMode.HALF_UP)));
    }

    @Test
    void testDeepSeekV4FlashFreeCost_RemainsZero() {
        AiCostCalculator.CalculationResult result = calculator.calculateCost(
                "openrouter-deepseek-v4-flash-free",
                "OpenRouter",
                "deepseek/deepseek-v4-flash:free",
                3516,
                0,
                1000
        );

        assertEquals("CALCULATED", result.source());
        assertEquals(0, BigDecimal.ZERO.compareTo(result.costUsd()));
    }

    @Test
    void testCloudflareGemmaCost_MatchesOfficialPricingForObservedRequest() {
        BigDecimal cost = calculator.calculateCost(
                "cloudflare-gemma",
                "Cloudflare",
                "@cf/google/gemma-4-26b-a4b-it",
                3516,
                0,
                1000
        ).costUsd();

        BigDecimal expected = new BigDecimal("0.0006516");
        assertEquals(0, expected.setScale(10, RoundingMode.HALF_UP)
                .compareTo(cost.setScale(10, RoundingMode.HALF_UP)));
    }

    @Test
    void testCloudflareLlamaCost_MatchesOfficialPricingForObservedRequest() {
        BigDecimal cost = calculator.calculateCost(
                "cloudflare-llama",
                "Cloudflare",
                "@cf/meta/llama-3.3-70b-instruct-fp8-fast",
                3092,
                0,
                758
        ).costUsd();

        BigDecimal expected = new BigDecimal("0.00260218");
        assertEquals(0, expected.setScale(10, RoundingMode.HALF_UP)
                .compareTo(cost.setScale(10, RoundingMode.HALF_UP)));
    }

    @Test
    void testCloudflareGlmCost_MatchesOfficialPricingForObservedRequest() {
        BigDecimal cost = calculator.calculateCost(
                "cloudflare-glm",
                "Cloudflare",
                "@cf/zai-org/glm-4.7-flash",
                3598,
                0,
                692
        ).costUsd();

        BigDecimal expected = new BigDecimal("0.00049268");
        assertEquals(0, expected.setScale(10, RoundingMode.HALF_UP)
                .compareTo(cost.setScale(10, RoundingMode.HALF_UP)));
    }

    @Test
    void testCloudflareQwenCost_MatchesOfficialPricingForObservedRequest() {
        BigDecimal cost = calculator.calculateCost(
                "cloudflare-qwen3-30b",
                "Cloudflare",
                "@cf/qwen/qwen3-30b-a3b-fp8",
                3427,
                0,
                1000
        ).costUsd();

        BigDecimal expected = new BigDecimal("0.000509777");
        assertEquals(0, expected.setScale(10, RoundingMode.HALF_UP)
                .compareTo(cost.setScale(10, RoundingMode.HALF_UP)));
    }

    @Test
    void testCloudflareGraniteCost_MatchesOfficialPricingForObservedRequest() {
        BigDecimal cost = calculator.calculateCost(
                "cloudflare-granite",
                "Cloudflare",
                "@cf/ibm-granite/granite-4.0-h-micro",
                3427,
                0,
                1000
        ).costUsd();

        BigDecimal expected = new BigDecimal("0.000170259");
        assertEquals(0, expected.setScale(10, RoundingMode.HALF_UP)
                .compareTo(cost.setScale(10, RoundingMode.HALF_UP)));
    }

    @Test
    void testCloudflareLlama4ScoutCost_MatchesOfficialPricingForObservedRequest() {
        BigDecimal cost = calculator.calculateCost(
                "cloudflare-llama4-scout",
                "Cloudflare",
                "@cf/meta/llama-4-scout-17b-16e-instruct",
                3585,
                0,
                529
        ).costUsd();

        BigDecimal expected = new BigDecimal("0.00141760");
        assertEquals(0, expected.setScale(10, RoundingMode.HALF_UP)
                .compareTo(cost.setScale(10, RoundingMode.HALF_UP)));
    }


    @Test
    void testGeminiCost_BasicCalculation() {
        BigDecimal cost = calculator.calculateCost("gemini-flash", "Gemini", "gemini-3.5-flash", 1000, 0, 500).costUsd();
        assertNotNull(cost);
        BigDecimal expected = new BigDecimal("0.006");
        assertEquals(0, expected.setScale(10, RoundingMode.HALF_UP).compareTo(cost.setScale(10, RoundingMode.HALF_UP)));
    }

    @Test
    void testGeminiCost_WithCachedTokens_CorrectDiscount() {
        BigDecimal cost = calculator.calculateCost("gemini-flash", "Gemini", "gemini-3.5-flash", 1000, 400, 500).costUsd();
        assertNotNull(cost);
        BigDecimal expected = new BigDecimal("0.0009")
                .add(new BigDecimal("0.00006"))
                .add(new BigDecimal("0.0045"));
        assertEquals(0, expected.setScale(10, RoundingMode.HALF_UP).compareTo(cost.setScale(10, RoundingMode.HALF_UP)));
    }

    @Test
    void testUnknownModel_ReturnsNullAndUnavailable() {
        AiCostCalculator.CalculationResult res = calculator.calculateCost("unknown", "OpenAI", "gpt-5", 1000, 0, 500);
        assertNotNull(res);
        assertEquals("UNAVAILABLE", res.source());
        assertNull(res.costUsd());
    }

    @Test
    void testZeroTokens_ReturnsZeroCost() {
        BigDecimal cost = calculator.calculateCost("groq-qwen-3-6-27b", "Groq", "qwen/qwen3.6-27b", 0, 0, 0).costUsd();
        assertNotNull(cost);
        assertEquals(0, BigDecimal.ZERO.compareTo(cost));
    }

    @Test
    void testCostUsesBigDecimal_NotFloat() {
        BigDecimal cost = calculator.calculateCost("gemini-flash", "Gemini", "gemini-3.5-flash", 1000000, 0, 1000000).costUsd();
        assertNotNull(cost);
        BigDecimal expected = new BigDecimal("10.5000000000");
        assertEquals(0, expected.compareTo(cost));
    }

    @Test
    void testCaseInsensitiveLookup_AndNoParticipantIdFallback() {
        AiCostCalculator.CalculationResult res = calculator.calculateCost(null, "gRoQ", "qwen/qwen3.6-27b", 1000, 0, 500);
        assertEquals("CALCULATED", res.source());
        assertNotNull(res.costUsd());
    }

    @Test
    void testCloudflareLlama32Cost_MatchesObservedRequestFromDB() {
        ModelPricing llama32 = new ModelPricing();
        llama32.setProvider("cloudflare");
        llama32.setModel("@cf/meta/llama-3.2-3b-instruct");
        llama32.setInputPriceUsdPerMillion(new BigDecimal("0.051"));
        llama32.setCachedInputPriceUsdPerMillion(new BigDecimal("0.051"));
        llama32.setOutputPriceUsdPerMillion(new BigDecimal("0.335"));

        when(modelPricingRepository.findActivePricing(eq("Cloudflare"), eq("@cf/meta/llama-3.2-3b-instruct"), any()))
            .thenReturn(Optional.of(llama32));

        BigDecimal cost = calculator.calculateCost(null, "Cloudflare", "@cf/meta/llama-3.2-3b-instruct", 807, 0, 262).costUsd();

        assertNotNull(cost);
        BigDecimal expected = new BigDecimal("0.0001289270");
        assertEquals(0, expected.setScale(10, RoundingMode.HALF_UP).compareTo(cost.setScale(10, RoundingMode.HALF_UP)));
    }
}
