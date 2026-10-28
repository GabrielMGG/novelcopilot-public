package com.novelcopilot.backend.benchmark;

import com.novelcopilot.backend.benchmark.config.AiBenchmarkParticipantProperties;
import com.novelcopilot.backend.benchmark.dto.AiBenchmarkCase;
import com.novelcopilot.backend.benchmark.entity.AiBenchmarkResponse;
import com.novelcopilot.backend.benchmark.entity.AiBenchmarkRun;
import com.novelcopilot.backend.benchmark.repository.AiBenchmarkResponseRepository;
import com.novelcopilot.backend.benchmark.repository.AiBenchmarkRunRepository;
import com.novelcopilot.backend.benchmark.service.AiBenchmarkRunnerService;
import com.novelcopilot.backend.dto.AiGenerationResult;
import com.novelcopilot.backend.service.AiCostCalculator;
import com.novelcopilot.backend.service.AiProvider;
import com.novelcopilot.backend.service.AiUsageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AiBenchmarkRetryTest {

    private AiBenchmarkRunnerService runnerService;

    @Mock
    private AiProvider mockProvider;

    @Mock
    private AiBenchmarkRunRepository runRepository;

    @Mock
    private AiBenchmarkResponseRepository responseRepository;

    @Mock
    private AiCostCalculator costCalculator;

    @Mock
    private AiUsageService aiUsageService;

    private AiBenchmarkParticipantProperties participantProperties;

    @BeforeEach
    void setUp() {
        participantProperties = new AiBenchmarkParticipantProperties();
        Map<String, AiBenchmarkParticipantProperties.Participant> map = new HashMap<>();
        
        AiBenchmarkParticipantProperties.Participant p1 = new AiBenchmarkParticipantProperties.Participant();
        p1.setEnabled(true);
        p1.setProvider("mock");
        p1.setModel("model-a");
        p1.setInputPricePerMillion(new BigDecimal("0.1"));
        p1.setOutputPricePerMillion(new BigDecimal("0.1"));
        map.put("p1", p1);
        
        AiBenchmarkParticipantProperties.Participant p2 = new AiBenchmarkParticipantProperties.Participant();
        p2.setEnabled(true);
        p2.setProvider("mock");
        p2.setModel("model-b");
        p2.setInputPricePerMillion(new BigDecimal("0.1"));
        p2.setOutputPricePerMillion(new BigDecimal("0.1"));
        map.put("p2", p2);
        
        participantProperties.setParticipants(map);

        when(mockProvider.providerName()).thenReturn("mock");

        runnerService = new AiBenchmarkRunnerService(
                List.of(mockProvider),
                runRepository,
                responseRepository,
                costCalculator,
                aiUsageService,
                participantProperties
        );
        
        ReflectionTestUtils.setField(runnerService, "benchmarkEnabled", true);
        ReflectionTestUtils.setField(runnerService, "activeProfile", "benchmark");
        ReflectionTestUtils.setField(runnerService, "maxCasesPerRun", 2);
        ReflectionTestUtils.setField(runnerService, "delayBetweenCallsMs", 10L); // Fast for test
        ReflectionTestUtils.setField(runnerService, "maxRetries", 2);

        org.mockito.Mockito.lenient().when(costCalculator.calculateCost(any(), any(), any(), anyLong(), anyLong(), anyLong()))
                .thenReturn(new com.novelcopilot.backend.service.AiCostCalculator.CalculationResult(java.math.BigDecimal.valueOf(0.01), "CALCULATED", null));
    }

    @Test
    void testSequentialExecutionAndDelay() throws Exception {
        AiBenchmarkRun run = new AiBenchmarkRun();
        run.setId(10L);
        when(runRepository.save(any(AiBenchmarkRun.class))).thenReturn(run);
        
        when(mockProvider.generate(anyString(), anyString(), anyString())).thenReturn(
                new AiGenerationResult("resp1", "mock", "model", 10L, 0L, 10L, 20L, 500L)
        );

        AiBenchmarkRun startedRun = runnerService.runBenchmark("Suite 1", List.of("case-pt-01"), List.of("p1", "p2"), "test-user");
        
        // Wait for async thread to complete
        Thread.sleep(200); 

        verify(mockProvider, times(2)).generate(anyString(), anyString(), anyString());
        verify(responseRepository, times(2)).save(any(AiBenchmarkResponse.class));
        
        ArgumentCaptor<AiBenchmarkRun> runCaptor = ArgumentCaptor.forClass(AiBenchmarkRun.class);
        verify(runRepository, atLeast(2)).save(runCaptor.capture());
        
        AiBenchmarkRun finalRun = runCaptor.getValue();
        assertEquals("COMPLETED", finalRun.getStatus());
    }

    @Test
    void testRetryRespectsRetryAfterHeader() throws Exception {
        AiBenchmarkRun run = new AiBenchmarkRun();
        run.setId(11L);
        when(runRepository.save(any(AiBenchmarkRun.class))).thenReturn(run);

        // Fail once with 429 and Retry-After 1 second, then succeed
        when(mockProvider.generate(anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException("429 Too Many Requests Retry-After: 1"))
                .thenReturn(new AiGenerationResult("Success", "mock", "model", 10L, 0L, 10L, 20L, 500L));

        long start = System.currentTimeMillis();
        runnerService.runBenchmark("Suite 1", List.of("case-pt-01"), List.of("p1", "p2"), "test-user");
        
        Thread.sleep(1500); // 1s retry + buffer
        
        long end = System.currentTimeMillis();
        assertTrue((end - start) >= 1000, "Should have waited at least 1 second due to Retry-After");
        verify(mockProvider, times(3)).generate(anyString(), anyString(), anyString()); // 1 fail, 1 success (for p1), 1 success (for p2)
    }

    @Test
    void testRetryMaxRetriesReached() throws Exception {
        AiBenchmarkRun run = new AiBenchmarkRun();
        run.setId(12L);
        when(runRepository.save(any(AiBenchmarkRun.class))).thenReturn(run);

        // Always fail with 429
        when(mockProvider.generate(anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException("429 Too Many Requests Retry-After: 0")); // 0 to make test fast

        runnerService.runBenchmark("Suite 1", List.of("case-pt-01"), List.of("p1", "p2"), "test-user");
        
        Thread.sleep(500); 
        
        // 1 initial + 2 retries = 3 attempts per participant. Total = 6
        verify(mockProvider, times(6)).generate(anyString(), anyString(), anyString());
        
        ArgumentCaptor<AiBenchmarkRun> runCaptor = ArgumentCaptor.forClass(AiBenchmarkRun.class);
        verify(runRepository, atLeast(2)).save(runCaptor.capture());
        AiBenchmarkRun finalRun = runCaptor.getValue();
        
        assertEquals("COMPLETED_WITH_ERRORS", finalRun.getStatus()); // 3/8 -> Not success total
    }
    
    @Test
    void testRetryFailedPreservesPositionAndIgnoresCompleted() throws Exception {
        AiBenchmarkRun run = new AiBenchmarkRun();
        run.setId(20L);
        run.setStatus("COMPLETED_WITH_ERRORS");
        when(runRepository.findById(20L)).thenReturn(Optional.of(run));
        when(runRepository.findAll()).thenReturn(new ArrayList<>());
        
        AiBenchmarkResponse completed = new AiBenchmarkResponse();
        completed.setId(100L);
        completed.setCaseId("case-pt-01");
        completed.setStatus("COMPLETED");
        completed.setParticipantId("p1");
        completed.setAnonymousLabel("A");

        AiBenchmarkResponse failed = new AiBenchmarkResponse();
        failed.setId(101L);
        failed.setCaseId("case-pt-01");
        failed.setStatus("FAILED");
        failed.setParticipantId("p2");
        failed.setAnonymousLabel("B");
        
        when(responseRepository.findByRunId(20L))
                .thenReturn(List.of(completed, failed)) // Initial status
                .thenReturn(List.of(completed, completed)); // After retry
                
        when(mockProvider.generate(anyString(), anyString(), anyString()))
                .thenReturn(new AiGenerationResult("Fixed", "mock", "model", 10L, 0L, 10L, 20L, 500L));
        
        runnerService.retryFailed(20L);
        
        Thread.sleep(300);
        
        // Verify only 1 generate was called (for the failed one)
        verify(mockProvider, times(1)).generate(anyString(), anyString(), eq("model-b"));
        
        // Ensure failed one was deleted and recreated (or saved)
        verify(responseRepository).delete(failed);
        
        ArgumentCaptor<AiBenchmarkResponse> responseCaptor = ArgumentCaptor.forClass(AiBenchmarkResponse.class);
        verify(responseRepository, atLeastOnce()).save(responseCaptor.capture());
        
        // Get the latest saved response
        AiBenchmarkResponse savedNew = responseCaptor.getValue();
        assertEquals("p2", savedNew.getParticipantId());
        assertEquals("B", savedNew.getAnonymousLabel()); // Position B preserved!
        assertEquals("COMPLETED", savedNew.getStatus());
    }
}
