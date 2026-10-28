package com.novelcopilot.backend.benchmark;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.novelcopilot.backend.benchmark.config.AiBenchmarkParticipantProperties;
import com.novelcopilot.backend.benchmark.entity.AiBenchmarkEvaluation;
import com.novelcopilot.backend.benchmark.entity.AiBenchmarkResponse;
import com.novelcopilot.backend.benchmark.entity.AiBenchmarkRun;
import com.novelcopilot.backend.benchmark.repository.AiBenchmarkEvaluationRepository;
import com.novelcopilot.backend.benchmark.repository.AiBenchmarkResponseRepository;
import com.novelcopilot.backend.benchmark.repository.AiBenchmarkRunRepository;
import com.novelcopilot.backend.benchmark.service.AiBenchmarkEvaluationService;
import com.novelcopilot.backend.benchmark.service.AiBenchmarkRunnerService;
import com.novelcopilot.backend.dto.AiGenerationResult;
import com.novelcopilot.backend.service.AiCostCalculator;
import com.novelcopilot.backend.service.AiProvider;
import com.novelcopilot.backend.service.AiUsageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class AiBenchmarkTest {

    private AiProvider mockGroq;
    private AiProvider mockGemini;
    private AiBenchmarkRunRepository runRepository;
    private AiBenchmarkResponseRepository responseRepository;
    private AiBenchmarkEvaluationRepository evaluationRepository;
    private AiCostCalculator aiCostCalculator;
    private AiUsageService aiUsageService;
    private AiBenchmarkParticipantProperties participantProperties;
    
    private AiBenchmarkRunnerService runnerService;
    private AiBenchmarkEvaluationService evaluationService;

    @BeforeEach
    public void setup() {
        mockGroq = mock(AiProvider.class);
        when(mockGroq.providerName()).thenReturn("groq");
        when(mockGroq.modelName()).thenReturn("llama-3.3-70b-versatile");

        mockGemini = mock(AiProvider.class);
        when(mockGemini.providerName()).thenReturn("gemini");
        when(mockGemini.modelName()).thenReturn("gemini-3.5-flash");

        runRepository = mock(AiBenchmarkRunRepository.class);
        responseRepository = mock(AiBenchmarkResponseRepository.class);
        evaluationRepository = mock(AiBenchmarkEvaluationRepository.class);
        aiCostCalculator = mock(AiCostCalculator.class);
        aiUsageService = mock(AiUsageService.class);

        when(aiCostCalculator.calculateCost(any(), any(), any(), anyLong(), anyLong(), anyLong()))
                .thenReturn(new com.novelcopilot.backend.service.AiCostCalculator.CalculationResult(BigDecimal.valueOf(0.01), "CALCULATED", null));

        when(runRepository.save(any(AiBenchmarkRun.class))).thenAnswer(i -> {
            AiBenchmarkRun run = i.getArgument(0);
            if (run.getId() == null) ReflectionTestUtils.setField(run, "id", 1L);
            return run;
        });
        when(runRepository.findAll()).thenReturn(List.of());
        when(runRepository.findByCreatedAtGreaterThanEqual(any(LocalDateTime.class))).thenReturn(List.of());
        when(responseRepository.findByRunId(anyLong())).thenReturn(List.of());

        participantProperties = new AiBenchmarkParticipantProperties();
        AiBenchmarkParticipantProperties.Participant p1 = new AiBenchmarkParticipantProperties.Participant();
        p1.setEnabled(true);
        p1.setProvider("groq");
        p1.setModel("llama-3.3-70b-versatile");
        p1.setInputPricePerMillion(new BigDecimal("0.1"));
        p1.setOutputPricePerMillion(new BigDecimal("0.1"));
        
        AiBenchmarkParticipantProperties.Participant p2 = new AiBenchmarkParticipantProperties.Participant();
        p2.setEnabled(true);
        p2.setProvider("gemini");
        p2.setModel("gemini-3.5-flash");
        p2.setInputPricePerMillion(new BigDecimal("0.1"));
        p2.setOutputPricePerMillion(new BigDecimal("0.1"));
        
        participantProperties.setParticipants(Map.of(
            "groq", p1,
            "gemini", p2
        ));

        runnerService = new AiBenchmarkRunnerService(
                List.of(mockGroq, mockGemini),
                runRepository,
                responseRepository,
                aiCostCalculator,
                aiUsageService,
                participantProperties
        );
        
        ReflectionTestUtils.setField(runnerService, "benchmarkEnabled", true);
        ReflectionTestUtils.setField(runnerService, "maxCasesPerRun", 2);
        ReflectionTestUtils.setField(runnerService, "maxRetries", 0);
        ReflectionTestUtils.setField(runnerService, "maxRunCostUsd", new BigDecimal("1.00"));
        ReflectionTestUtils.setField(runnerService, "maxDailyCostUsd", new BigDecimal("3.00"));
        ReflectionTestUtils.setField(runnerService, "activeProfile", "default");

        evaluationService = new AiBenchmarkEvaluationService(evaluationRepository, responseRepository);
    }

    @Test
    public void testSameRequestSentToBothProvidersAndOrderRandomized() {
        when(mockGroq.generate(any(), any(), any())).thenReturn(
                new AiGenerationResult("Groq Result", "groq", "llama", 10L, 0L, 10L, 20L, 100L)
        );
        when(mockGemini.generate(any(), any(), any())).thenReturn(
                new AiGenerationResult("Gemini Result", "gemini", "flash", 10L, 0L, 10L, 20L, 100L)
        );

        AiBenchmarkRun run = runnerService.runBenchmark("Test Suite", null, List.of("groq", "gemini"), "test-user");

        ArgumentCaptor<String> instructionCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);

        verify(mockGroq, timeout(1000).times(2)).generate(instructionCaptor.capture(), promptCaptor.capture(), any());
        verify(mockGemini, timeout(1000).times(2)).generate(instructionCaptor.capture(), promptCaptor.capture(), any());

        // Same requests were sent
        assertEquals(instructionCaptor.getAllValues().get(0), instructionCaptor.getAllValues().get(2));
        assertEquals(promptCaptor.getAllValues().get(0), promptCaptor.getAllValues().get(2));
    }

    @Test
    public void testBenchmarkDisabledPreventsExecution() {
        ReflectionTestUtils.setField(runnerService, "benchmarkEnabled", false);
        assertThrows(IllegalStateException.class, () -> runnerService.runBenchmark("Test", null, List.of("groq", "gemini"), "test-user"));
    }

    @Test
    public void testServerRejectsRunAboveConfiguredLimit() {
        ReflectionTestUtils.setField(runnerService, "maxRunCostUsd", BigDecimal.ZERO);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> runnerService.runBenchmark(
                        "Test",
                        null,
                        List.of("groq", "gemini"),
                        "test-user"));

        assertTrue(exception.getMessage().contains("limite por execução"));
        verify(runRepository, never()).save(any());
    }

    @Test
    public void testServerRejectsRunAboveDailyAccumulatedLimit() {
        AiBenchmarkRun previousRun = new AiBenchmarkRun();
        previousRun.setId(99L);
        previousRun.setEstimatedCostUsd(new BigDecimal("3.00"));
        previousRun.setFinalCostUsd(new BigDecimal("2.50"));
        when(runRepository.findByCreatedAtGreaterThanEqual(any(LocalDateTime.class)))
                .thenReturn(List.of(previousRun));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> runnerService.runBenchmark(
                        "Test",
                        null,
                        List.of("groq", "gemini"),
                        "test-user"));

        assertTrue(exception.getMessage().contains("limite diário"));
        verify(runRepository, never()).save(any());
    }

    @Test
    public void testRunRecordsAuthenticatedAuthorAndReservedEstimate() {
        when(mockGroq.generate(any(), any(), any())).thenReturn(
                new AiGenerationResult("Groq Result", "groq", "llama", 10L, 0L, 10L, 20L, 100L)
        );
        when(mockGemini.generate(any(), any(), any())).thenReturn(
                new AiGenerationResult("Gemini Result", "gemini", "flash", 10L, 0L, 10L, 20L, 100L)
        );
        AiBenchmarkResponse completedResponse = new AiBenchmarkResponse();
        completedResponse.setEffectiveCostUsd(new BigDecimal("0.02"));
        when(responseRepository.findByRunId(anyLong()))
                .thenReturn(List.of(completedResponse));

        AiBenchmarkRun run = runnerService.runBenchmark(
                "Test",
                null,
                List.of("groq", "gemini"),
                "authenticated-admin-id");

        assertEquals("authenticated-admin-id", run.getCreatedBy());
        assertNotNull(run.getEstimatedCostUsd());
        assertTrue(run.getEstimatedCostUsd().compareTo(BigDecimal.ZERO) > 0);
        verify(runRepository, timeout(2000).atLeast(2)).save(any(AiBenchmarkRun.class));
        assertEquals(new BigDecimal("0.02"), run.getFinalCostUsd());
    }

    @Test
    public void testMaxCasesLimit() {
        ReflectionTestUtils.setField(runnerService, "maxCasesPerRun", 1);
        when(mockGroq.generate(any(), any(), any())).thenReturn(
                new AiGenerationResult("Groq Result", "groq", "llama", 10L, 0L, 10L, 20L, 100L)
        );
        when(mockGemini.generate(any(), any(), any())).thenReturn(
                new AiGenerationResult("Gemini Result", "gemini", "flash", 10L, 0L, 10L, 20L, 100L)
        );

        runnerService.runBenchmark("Test", null, List.of("groq", "gemini"), "test-user");
        verify(mockGroq, timeout(1000).times(1)).generate(any(), any(), any());
    }

    @Test
    public void testProviderFailureDoesNotStopRun() {
        when(mockGroq.generate(any(), any(), any())).thenThrow(new RuntimeException("400 Bad Request - Fatal Error"));
        when(mockGemini.generate(any(), any(), any())).thenReturn(
                new AiGenerationResult("Gemini Result", "gemini", "flash", 10L, 0L, 10L, 20L, 100L)
        );

        AiBenchmarkRun run = runnerService.runBenchmark("Test", null, List.of("groq", "gemini"), "test-user");

        ArgumentCaptor<AiBenchmarkResponse> responseCaptor = ArgumentCaptor.forClass(AiBenchmarkResponse.class);
        verify(responseRepository, timeout(1000).times(4)).save(responseCaptor.capture());
        
        long failedCount = responseCaptor.getAllValues().stream()
                .filter(r -> "FAILED".equals(r.getStatus())).count();
        assertEquals(2, failedCount); // 2 cases run, Groq fails on both
    }

    @Test
    public void testEvaluationScoresValidationAndCalculation() {
        AiBenchmarkResponse fakeResponse = new AiBenchmarkResponse();
        fakeResponse.setId(1L);
        fakeResponse.setAnonymousLabel("A");
        fakeResponse.setProvider("groq");
        when(responseRepository.findById(1L)).thenReturn(Optional.of(fakeResponse));

        // Invalid score
        assertThrows(IllegalArgumentException.class, () -> 
            evaluationService.saveEvaluation(1L, "user1", 6, 5, 5, 5, 5, 5, 5, 5, 5, 5, "Too high", "A")
        );
        assertThrows(IllegalArgumentException.class, () -> 
            evaluationService.saveEvaluation(1L, "user1", 0, 5, 5, 5, 5, 5, 5, 5, 5, 5, "Too low", "A")
        );

        // Valid score
        evaluationService.saveEvaluation(1L, "user1", 5, 4, 3, 4, 5, 4, 4, 4, 4, 4, "Good", "A");

        ArgumentCaptor<AiBenchmarkEvaluation> evalCaptor = ArgumentCaptor.forClass(AiBenchmarkEvaluation.class);
        verify(evaluationRepository).save(evalCaptor.capture());

        AiBenchmarkEvaluation savedEval = evalCaptor.getValue();
        // 5*0.25(1.25) + 4*0.20(0.8) + 4*0.20(0.8) + 3*0.15(0.45) + 5*0.10(0.5) + 4*0.10(0.4) = 4.2
        assertEquals(new BigDecimal("3.99"), savedEval.getOverallScore());
    }
}
