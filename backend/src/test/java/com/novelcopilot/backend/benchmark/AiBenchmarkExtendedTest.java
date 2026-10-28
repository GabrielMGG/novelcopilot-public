package com.novelcopilot.backend.benchmark;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.novelcopilot.backend.benchmark.config.AiBenchmarkParticipantProperties;
import com.novelcopilot.backend.benchmark.controller.AiBenchmarkController;
import com.novelcopilot.backend.benchmark.dto.AiBenchmarkCase;
import com.novelcopilot.backend.benchmark.dto.BlindBenchmarkResponse;
import com.novelcopilot.backend.benchmark.entity.AiBenchmarkResponse;
import com.novelcopilot.backend.benchmark.entity.AiBenchmarkRun;
import com.novelcopilot.backend.benchmark.repository.AiBenchmarkEvaluationRepository;
import com.novelcopilot.backend.benchmark.repository.AiBenchmarkResponseRepository;
import com.novelcopilot.backend.benchmark.repository.AiBenchmarkRunRepository;
import com.novelcopilot.backend.benchmark.service.AiBenchmarkEstimateService;
import com.novelcopilot.backend.benchmark.service.AiBenchmarkEvaluationService;
import com.novelcopilot.backend.benchmark.service.AiBenchmarkRunnerService;
import com.novelcopilot.backend.security.AuthenticatedUserPrincipal;
import com.novelcopilot.backend.dto.AiGenerationResult;
import com.novelcopilot.backend.service.AiCostCalculator;
import com.novelcopilot.backend.service.AiProvider;
import com.novelcopilot.backend.service.AiRateLimitService;
import com.novelcopilot.backend.service.AiUsageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class AiBenchmarkExtendedTest {

    private AiProvider mockGroq;
    private AiProvider mockGemini;
    private AiBenchmarkRunnerService runnerService;
    private AiBenchmarkEstimateService estimateService;
    private AiBenchmarkEvaluationService evaluationService;
    private AiBenchmarkController controller;
    private AiBenchmarkRunRepository runRepository;
    private AiBenchmarkResponseRepository responseRepository;
    private AiBenchmarkEvaluationRepository evaluationRepository;
    private AiBenchmarkParticipantProperties participantProperties;

    @BeforeEach
    public void setup() {
        mockGroq = mock(AiProvider.class);
        when(mockGroq.providerName()).thenReturn("groq");
        when(mockGroq.modelName()).thenReturn("llama-3.3-70b-versatile");
        when(mockGroq.generate(anyString(), anyString(), anyString())).thenReturn(
                new AiGenerationResult("mock text", "groq", "model", 10L, 5L, 20L, 30L, 100L)
        );

        mockGemini = mock(AiProvider.class);
        when(mockGemini.providerName()).thenReturn("gemini");
        when(mockGemini.modelName()).thenReturn("gemini-3.5-flash");
        when(mockGemini.generate(anyString(), anyString(), anyString())).thenReturn(
                new AiGenerationResult("mock text gemini", "gemini", "model", 10L, 5L, 20L, 30L, 100L)
        );

        runRepository = mock(AiBenchmarkRunRepository.class);
        responseRepository = mock(AiBenchmarkResponseRepository.class);
        evaluationRepository = mock(AiBenchmarkEvaluationRepository.class);
        AiCostCalculator aiCostCalculator = mock(AiCostCalculator.class);
        when(aiCostCalculator.calculateCost(any(), any(), any(), anyLong(), anyLong(), anyLong()))
                .thenReturn(new com.novelcopilot.backend.service.AiCostCalculator.CalculationResult(BigDecimal.valueOf(0.01), "CALCULATED", null));
        AiUsageService aiUsageService = mock(AiUsageService.class);

        when(runRepository.save(any(AiBenchmarkRun.class))).thenAnswer(i -> {
            AiBenchmarkRun run = i.getArgument(0);
            if (run.getId() == null) ReflectionTestUtils.setField(run, "id", 1L);
            return run;
        });

        participantProperties = new AiBenchmarkParticipantProperties();
        AiBenchmarkParticipantProperties.Participant p1 = new AiBenchmarkParticipantProperties.Participant();
        p1.setEnabled(true);
        p1.setProvider("groq");
        p1.setModel("llama-3.3-70b-versatile");
        p1.setInputPricePerMillion(new BigDecimal("0.59"));
        p1.setOutputPricePerMillion(new BigDecimal("0.79"));

        AiBenchmarkParticipantProperties.Participant p2 = new AiBenchmarkParticipantProperties.Participant();
        p2.setEnabled(true);
        p2.setProvider("groq");
        p2.setModel("openai/gpt-oss-120b");
        p2.setInputPricePerMillion(new BigDecimal("0.15"));
        p2.setCachedInputPricePerMillion(new BigDecimal("0.075"));
        p2.setOutputPricePerMillion(new BigDecimal("0.60"));

        AiBenchmarkParticipantProperties.Participant p3 = new AiBenchmarkParticipantProperties.Participant();
        p3.setEnabled(false);
        p3.setProvider("gemini");
        p3.setModel("gemini-3.5-flash");
        p3.setInputPricePerMillion(new BigDecimal("0.1"));
        p3.setOutputPricePerMillion(new BigDecimal("0.1"));

        participantProperties.setParticipants(Map.of(
                "groq-llama", p1,
                "groq-gpt-oss", p2,
                "gemini", p3
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
        ReflectionTestUtils.setField(runnerService, "maxCasesPerRun", 20);
        ReflectionTestUtils.setField(runnerService, "maxRetries", 3);
        ReflectionTestUtils.setField(runnerService, "activeProfile", "benchmark");

        estimateService = new AiBenchmarkEstimateService(participantProperties);
        evaluationService = new AiBenchmarkEvaluationService(evaluationRepository, responseRepository);

        controller = new AiBenchmarkController(
                runnerService, estimateService, evaluationService,
                runRepository, responseRepository, evaluationRepository,
                participantProperties, aiCostCalculator, List.of(mockGroq, mockGemini),
                mock(AiRateLimitService.class)
        );
    }

    @Test
    public void testSameProviderTwoParticipants() {
        List<AiBenchmarkCase> cases = runnerService.getAvailableCases();
        List<String> selection = List.of(cases.get(0).getId());
        List<String> participants = List.of("groq-llama", "groq-gpt-oss");

        AiBenchmarkRun run = controller.runBenchmark(
                Map.of("caseIds", selection, "participantIds", participants),
                testPrincipal());
        assertNotNull(run);

        ArgumentCaptor<String> modelIdCaptor = ArgumentCaptor.forClass(String.class);
        verify(mockGroq, timeout(2000).times(2)).generate(anyString(), anyString(), modelIdCaptor.capture());

        List<String> passedModels = modelIdCaptor.getAllValues();
        assertTrue(passedModels.contains("llama-3.3-70b-versatile"));
        assertTrue(passedModels.contains("openai/gpt-oss-120b"));
    }

    @Test
    public void testDisabledParticipant() {
        List<AiBenchmarkCase> cases = runnerService.getAvailableCases();
        List<String> selection = List.of(cases.get(0).getId());
        List<String> participants = List.of("gemini", "groq-llama");

        // Runs asynchronously. gemini is disabled, so it will throw inside the async task
        controller.runBenchmark(
                Map.of("caseIds", selection, "participantIds", participants),
                testPrincipal());
        
        // Wait a bit to let the async task fail
        try { Thread.sleep(500); } catch (InterruptedException e) {}
    }

    @Test
    public void testMinimumTwoParticipants() {
        List<AiBenchmarkCase> cases = runnerService.getAvailableCases();
        List<String> selection = List.of(cases.get(0).getId());
        List<String> participants = List.of("groq-llama");

        assertThrows(IllegalStateException.class, () -> {
            controller.runBenchmark(
                    Map.of("caseIds", selection, "participantIds", participants),
                    testPrincipal());
        });
    }

    @Test
    public void testClientCannotBypassServerCostLimit() {
        ReflectionTestUtils.setField(runnerService, "maxRunCostUsd", BigDecimal.ZERO);
        List<AiBenchmarkCase> cases = runnerService.getAvailableCases();
        List<String> selection = List.of(cases.get(0).getId());
        List<String> participants = List.of("groq-llama", "groq-gpt-oss");

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> controller.runBenchmark(
                        Map.of(
                                "caseIds", selection,
                                "participantIds", participants,
                                "bypassCostLimit", true),
                        testPrincipal()));

        assertTrue(exception.getMessage().contains("limite por execução"));
        verify(runRepository, never()).save(any(AiBenchmarkRun.class));
    }

    private AuthenticatedUserPrincipal testPrincipal() {
        return new AuthenticatedUserPrincipal(
                "test-user",
                "test@example.com",
                "Test User",
                null,
                "email");
    }

    @Test
    public void testEstimateUsesSelectedParticipants() {
        List<AiBenchmarkCase> cases = runnerService.getAvailableCases();
        List<String> selection = List.of(cases.get(0).getId());
        List<String> participants = List.of("groq-llama", "groq-gpt-oss");

        AiBenchmarkEstimateService.AiBenchmarkEstimate est = controller.estimate(Map.of("caseIds", selection, "participantIds", participants));
        assertEquals(1, est.caseCount());
        assertEquals(2, est.providerCount());
        assertEquals(2, est.totalCalls());
        assertEquals(8, est.maxProviderCalls());
        assertTrue(est.maxEstimatedCost().compareTo(est.baseEstimatedCost()) > 0);
        assertTrue(est.maxEstimatedCost().compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    public void testRunHistoryAnd404() {
        AiBenchmarkRun run = new AiBenchmarkRun();
        ReflectionTestUtils.setField(run, "id", 1L);
        run.setSuiteName("Test Suite");
        run.setStatus("COMPLETED");
        run.setCreatedAt(java.time.LocalDateTime.now());
        when(runRepository.findAll()).thenReturn(List.of(run));
        
        List<Map<String, Object>> history = controller.getRunHistory();
        assertEquals(1, history.size());
        assertEquals(1L, history.get(0).get("id"));
        
        when(runRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> {
            controller.getRunStatus(99L);
        });
    }
}
