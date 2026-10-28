package com.novelcopilot.backend.benchmark;

import com.novelcopilot.backend.benchmark.config.AiBenchmarkParticipantProperties;
import com.novelcopilot.backend.benchmark.controller.AiBenchmarkController;
import com.novelcopilot.backend.benchmark.dto.AiBenchmarkCase;
import com.novelcopilot.backend.benchmark.dto.AiBenchmarkRunStatusDTO;
import com.novelcopilot.backend.benchmark.entity.AiBenchmarkResponse;
import com.novelcopilot.backend.benchmark.entity.AiBenchmarkRun;
import com.novelcopilot.backend.benchmark.repository.AiBenchmarkEvaluationRepository;
import com.novelcopilot.backend.benchmark.repository.AiBenchmarkResponseRepository;
import com.novelcopilot.backend.benchmark.repository.AiBenchmarkRunRepository;
import com.novelcopilot.backend.benchmark.service.AiBenchmarkRunnerService;
import com.novelcopilot.backend.service.AiCostCalculator;
import com.novelcopilot.backend.service.AiProvider;
import com.novelcopilot.backend.service.AiRateLimitService;
import com.novelcopilot.backend.service.AiUsageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AiBenchmarkFinalSuiteTest {

    private AiBenchmarkRunnerService runnerService;
    private AiBenchmarkController controller;

    @Mock
    private AiProvider mockProvider;

    @Mock
    private AiBenchmarkRunRepository runRepository;

    @Mock
    private AiBenchmarkResponseRepository responseRepository;

    @Mock
    private AiBenchmarkEvaluationRepository evaluationRepository;

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
        map.put("openrouter-qwen3-235b", p1);

        AiBenchmarkParticipantProperties.Participant reference = new AiBenchmarkParticipantProperties.Participant();
        reference.setEnabled(true);
        reference.setProvider("mock");
        reference.setModel("reference-model");
        map.put("groq-llama", reference);
        
        AiBenchmarkParticipantProperties.Participant p2 = new AiBenchmarkParticipantProperties.Participant();
        p2.setEnabled(true);
        p2.setProvider("mock");
        p2.setModel("model-b");
        map.put("openrouter-mistral-small-4", p2);

        AiBenchmarkParticipantProperties.Participant p3 = new AiBenchmarkParticipantProperties.Participant();
        p3.setEnabled(true);
        p3.setProvider("mock");
        p3.setModel("model-c");
        map.put("openrouter-mistral-small-3-2", p3);
        
        participantProperties.setParticipants(map);

        runnerService = new AiBenchmarkRunnerService(
                List.of(mockProvider),
                runRepository,
                responseRepository,
                costCalculator,
                aiUsageService,
                participantProperties
        );
        
        controller = new AiBenchmarkController(
                runnerService,
                null, // estimateService
                null, // evaluationService
                runRepository,
                responseRepository,
                evaluationRepository,
                participantProperties,
                costCalculator,
                List.of(mockProvider),
                org.mockito.Mockito.mock(AiRateLimitService.class)
        );
        
        org.springframework.test.util.ReflectionTestUtils.setField(runnerService, "benchmarkEnabled", true);
        org.springframework.test.util.ReflectionTestUtils.setField(runnerService, "maxCasesPerRun", 20);
    }

    @Test
    void testFinalSuiteConfiguration() {
        List<AiBenchmarkCase> cases = runnerService.getAvailableCases();
        List<AiBenchmarkCase> finalCases = cases.stream()
                .filter(c -> c.getId() != null && c.getId().startsWith("lit-final-"))
                .collect(Collectors.toList());

        assertEquals(8, finalCases.size(), "Suite should contain exactly 8 cases");

        for (int i = 0; i < 4; i++) {
            assertTrue(finalCases.get(i).getCategory().toLowerCase().contains("fatos"), 
                "Cases 1 to 4 should prioritize facts");
        }

        for (int i = 4; i < 8; i++) {
            assertTrue(finalCases.get(i).getCategory().toLowerCase().contains("personagem"), 
                "Cases 5 to 8 should prioritize character");
        }
    }

    @Test
    void testFinalSuiteStatusLogic() {
        AiBenchmarkRun run = new AiBenchmarkRun();
        run.setId(100L);
        run.setSuiteName("FINAL_FACTS_CHARACTERS");

        AiBenchmarkResponse r1 = new AiBenchmarkResponse();
        r1.setId(1L);
        r1.setRunId(100L);
        r1.setCaseId("lit-final-01");
        r1.setParticipantId("openrouter-qwen3-235b");
        r1.setAnonymousLabel("A");
        r1.setStatus("COMPLETED");
        r1.setContent("test");

        AiBenchmarkResponse r2 = new AiBenchmarkResponse();
        r2.setId(2L);
        r2.setRunId(100L);
        r2.setCaseId("lit-final-01");
        r2.setParticipantId("openrouter-mistral-small-4");
        r2.setAnonymousLabel("B");
        r2.setStatus("COMPLETED");
        r2.setContent("test");

        AiBenchmarkResponse r3 = new AiBenchmarkResponse();
        r3.setId(3L);
        r3.setRunId(100L);
        r3.setCaseId("lit-final-01");
        r3.setParticipantId("groq-llama");
        r3.setAnonymousLabel("C");
        r3.setStatus("COMPLETED");
        r3.setContent("test");

        when(runRepository.findById(100L)).thenReturn(Optional.of(run));
        when(responseRepository.findByRunId(100L)).thenReturn(List.of(r1, r2, r3));

        AiBenchmarkRunStatusDTO status = controller.getRunStatus(100L);

        assertEquals(3, status.getExpectedParticipantCount());
        assertEquals(24, status.getExpectedResponses());
        assertEquals(1, status.getCompleteCases(), "Should have 1 complete case with A, B and C");
        assertTrue(status.getResponses().stream().allMatch(
                response -> response instanceof com.novelcopilot.backend.benchmark.dto.BlindBenchmarkResponse
        ));
        
        assertThrows(IllegalArgumentException.class, () -> {
            runnerService.runBenchmark("FINAL_FACTS_CHARACTERS", null, 
                List.of("openrouter-qwen3-235b", "openrouter-mistral-small-3-2"), "test-user");
        });

        assertThrows(
                org.springframework.web.server.ResponseStatusException.class,
                () -> controller.getReport(100L),
                "O relatório não pode revelar os modelos antes da avaliação completa"
        );
    }

    @Test
    void testCostPriorityAndCalculation() {
        AiBenchmarkRun run = new AiBenchmarkRun();
        run.setId(100L);
        run.setSuiteName("FINAL_FACTS_CHARACTERS");

        AiBenchmarkResponse r1 = new AiBenchmarkResponse();
        r1.setId(1L);
        r1.setRunId(100L);
        r1.setCaseId("lit-final-01");
        r1.setStatus("COMPLETED");
        r1.setEffectiveCostUsd(new BigDecimal("0.05"));
        r1.setCostSource("PROVIDER");

        AiBenchmarkResponse r2 = new AiBenchmarkResponse();
        r2.setId(2L);
        r2.setRunId(100L);
        r2.setCaseId("lit-final-02");
        r2.setStatus("COMPLETED");
        r2.setEffectiveCostUsd(new BigDecimal("0.02"));
        r2.setCostSource("CALCULATED");

        when(runRepository.findById(100L)).thenReturn(Optional.of(run));
        when(responseRepository.findByRunId(100L)).thenReturn(List.of(r1, r2));

        AiBenchmarkRunStatusDTO status = controller.getRunStatus(100L);
        assertEquals(new BigDecimal("0.07"), status.getTotalCostUsd());
    }
}
