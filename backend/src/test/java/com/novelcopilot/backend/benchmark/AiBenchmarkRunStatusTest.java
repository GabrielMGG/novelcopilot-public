package com.novelcopilot.backend.benchmark;

import com.novelcopilot.backend.benchmark.controller.AiBenchmarkController;
import com.novelcopilot.backend.benchmark.entity.AiBenchmarkEvaluation;
import com.novelcopilot.backend.benchmark.entity.AiBenchmarkResponse;
import com.novelcopilot.backend.benchmark.entity.AiBenchmarkRun;
import com.novelcopilot.backend.benchmark.repository.AiBenchmarkEvaluationRepository;
import com.novelcopilot.backend.benchmark.repository.AiBenchmarkResponseRepository;
import com.novelcopilot.backend.benchmark.repository.AiBenchmarkRunRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.*;
import com.novelcopilot.backend.benchmark.dto.AiBenchmarkRunStatusDTO;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AiBenchmarkRunStatusTest {

    private AiBenchmarkController controller;
    private AiBenchmarkRunRepository runRepository;
    private AiBenchmarkResponseRepository responseRepository;
    private AiBenchmarkEvaluationRepository evaluationRepository;

    @BeforeEach
    public void setup() {
        runRepository = mock(AiBenchmarkRunRepository.class);
        responseRepository = mock(AiBenchmarkResponseRepository.class);
        evaluationRepository = mock(AiBenchmarkEvaluationRepository.class);

        com.novelcopilot.backend.service.AiCostCalculator costCalculator = mock(com.novelcopilot.backend.service.AiCostCalculator.class);
        when(costCalculator.calculateCost(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyLong()))
                .thenReturn(new com.novelcopilot.backend.service.AiCostCalculator.CalculationResult(java.math.BigDecimal.valueOf(0.01), "CALCULATED", null));
        controller = new AiBenchmarkController(
                null,
                null,
                null,
                runRepository,
                responseRepository,
                evaluationRepository,
                null,
                costCalculator,
                null,
                mock(com.novelcopilot.backend.service.AiRateLimitService.class)
        );
    }

    private AiBenchmarkRun createRun(Long id, String suiteName) {
        AiBenchmarkRun run = new AiBenchmarkRun();
        ReflectionTestUtils.setField(run, "id", id);
        run.setSuiteName(suiteName);
        run.setStatus("COMPLETED");
        return run;
    }

    private AiBenchmarkResponse createResponse(Long id, Long runId, String caseId, String label, String status, BigDecimal cost) {
        AiBenchmarkResponse r = new AiBenchmarkResponse();
        ReflectionTestUtils.setField(r, "id", id);
        r.setRunId(runId);
        r.setCaseId(caseId);
        r.setAnonymousLabel(label);
        r.setStatus(status);
        r.setEstimatedCostUsd(cost);
        r.setContent("Test Content");
        return r;
    }

    @Test
    public void testPaidBenchmarkEndpointsApplyTheirOwnUserLimits() {
        var runner = mock(com.novelcopilot.backend.benchmark.service.AiBenchmarkRunnerService.class);
        var limiter = mock(com.novelcopilot.backend.service.AiRateLimitService.class);
        var limitedController = new AiBenchmarkController(
                runner,
                null,
                null,
                runRepository,
                responseRepository,
                evaluationRepository,
                null,
                mock(com.novelcopilot.backend.service.AiCostCalculator.class),
                null,
                limiter
        );
        var principal = new com.novelcopilot.backend.security.AuthenticatedUserPrincipal(
                "admin-user",
                "admin@example.com",
                "Admin",
                null,
                "email"
        );

        limitedController.runBenchmark(
                Map.of(
                        "suiteName", "Suite",
                        "caseIds", List.of("case-1"),
                        "participantIds", List.of("model-a", "model-b")
                ),
                principal
        );
        limitedController.retryFailed(10L, principal);

        verify(limiter).checkBenchmarkRun("admin-user");
        verify(limiter).checkBenchmarkRetry("admin-user");
        verify(runner).runBenchmark(
                "Suite",
                List.of("case-1"),
                List.of("model-a", "model-b"),
                "admin-user"
        );
        verify(runner).retryFailed(10L);
    }

    private AiBenchmarkEvaluation createEvaluation(Long responseId) {
        AiBenchmarkEvaluation e = new AiBenchmarkEvaluation();
        e.setResponseId(responseId);
        return e;
    }

    @Test
    public void testCompatSuiteWith8CompleteCases() {
        AiBenchmarkRun run = createRun(3L, "Compatibilidade mínima NovelCopilot");
        when(runRepository.findById(3L)).thenReturn(Optional.of(run));

        List<AiBenchmarkResponse> responses = new ArrayList<>();
        long idCounter = 1;
        for (int i = 1; i <= 8; i++) {
            responses.add(createResponse(idCounter++, 3L, "case-" + i, "A", "COMPLETED", new BigDecimal("0.0001")));
            responses.add(createResponse(idCounter++, 3L, "case-" + i, "B", "COMPLETED", new BigDecimal("0.0001")));
            responses.add(createResponse(idCounter++, 3L, "case-" + i, "C", "COMPLETED", new BigDecimal("0.0002")));
        }
        when(responseRepository.findByRunId(3L)).thenReturn(responses);
        when(evaluationRepository.findAll()).thenReturn(Collections.emptyList());

        AiBenchmarkRunStatusDTO status = controller.getRunStatus(3L);

        assertEquals("COMPATIBILITY", status.getSuiteType());
        assertEquals(3, status.getExpectedParticipantCount());
        assertEquals(8L, status.getCompleteCases());
        assertEquals(0L, status.getIncompleteCases());
        assertEquals(24L, status.getEvaluableResponses());
        assertEquals(0L, status.getEvaluatedResponses());
        assertEquals(false, status.isEvaluationCompleted());
        assertEquals(new BigDecimal("0.0032"), status.getTotalCostUsd());
    }

    @Test
    public void testCompatSuiteWithMissingCIsIncomplete() {
        AiBenchmarkRun run = createRun(3L, "Compatibilidade mínima NovelCopilot");
        when(runRepository.findById(3L)).thenReturn(Optional.of(run));

        List<AiBenchmarkResponse> responses = new ArrayList<>();
        responses.add(createResponse(1L, 3L, "case-1", "A", "COMPLETED", new BigDecimal("0.0001")));
        responses.add(createResponse(2L, 3L, "case-1", "B", "COMPLETED", new BigDecimal("0.0001")));
        // C is missing
        
        when(responseRepository.findByRunId(3L)).thenReturn(responses);
        when(evaluationRepository.findAll()).thenReturn(Collections.emptyList());

        AiBenchmarkRunStatusDTO status = controller.getRunStatus(3L);
        assertEquals(0L, status.getCompleteCases());
        assertEquals(8L, status.getIncompleteCases()); // 1 present but incomplete + 7 missing
    }

    @Test
    public void testCompatSuiteEvaluationProgress() {
        AiBenchmarkRun run = createRun(3L, "Compatibilidade");
        when(runRepository.findById(3L)).thenReturn(Optional.of(run));

        List<AiBenchmarkResponse> responses = new ArrayList<>();
        List<AiBenchmarkEvaluation> evaluations = new ArrayList<>();
        long idCounter = 1;
        for (int i = 1; i <= 8; i++) {
            AiBenchmarkResponse ra = createResponse(idCounter++, 3L, "case-" + i, "A", "COMPLETED", BigDecimal.ZERO);
            AiBenchmarkResponse rb = createResponse(idCounter++, 3L, "case-" + i, "B", "COMPLETED", BigDecimal.ZERO);
            AiBenchmarkResponse rc = createResponse(idCounter++, 3L, "case-" + i, "C", "COMPLETED", BigDecimal.ZERO);
            responses.add(ra); responses.add(rb); responses.add(rc);
            
            // Evaluate all except the very last one
            if (i < 8) {
                evaluations.add(createEvaluation(ra.getId()));
                evaluations.add(createEvaluation(rb.getId()));
                evaluations.add(createEvaluation(rc.getId()));
            } else {
                evaluations.add(createEvaluation(ra.getId()));
                evaluations.add(createEvaluation(rb.getId()));
                // rc is NOT evaluated
            }
        }
        
        when(responseRepository.findByRunId(3L)).thenReturn(responses);
        when(evaluationRepository.findAll()).thenReturn(evaluations);

        AiBenchmarkRunStatusDTO status23 = controller.getRunStatus(3L);
        assertEquals(23L, status23.getEvaluatedResponses());
        assertEquals(false, status23.isEvaluationCompleted());
        assertEquals(false, status23.isReportAvailable());
        
        // Now evaluate the last one
        evaluations.add(createEvaluation(idCounter - 1));
        
        AiBenchmarkRunStatusDTO status24 = controller.getRunStatus(3L);
        assertEquals(24L, status24.getEvaluatedResponses());
        assertEquals(true, status24.isEvaluationCompleted());
        assertEquals(true, status24.isReportAvailable());
    }

    @Test
    public void testLiterarySuiteStillUsesTwoParticipants() {
        AiBenchmarkRun run = createRun(4L, "Benchmark literário");
        when(runRepository.findById(4L)).thenReturn(Optional.of(run));

        List<AiBenchmarkResponse> responses = new ArrayList<>();
        long idCounter = 1;
        for (int i = 1; i <= 8; i++) {
            responses.add(createResponse(idCounter++, 4L, "case-" + i, "A", "COMPLETED", BigDecimal.ZERO));
            responses.add(createResponse(idCounter++, 4L, "case-" + i, "B", "COMPLETED", BigDecimal.ZERO));
        }
        when(responseRepository.findByRunId(4L)).thenReturn(responses);
        when(evaluationRepository.findAll()).thenReturn(Collections.emptyList());

        AiBenchmarkRunStatusDTO status = controller.getRunStatus(4L);
        assertEquals("LITERARY_LEGACY", status.getSuiteType());
        assertEquals(2, status.getExpectedParticipantCount());
        assertEquals(8L, status.getCompleteCases());
        assertEquals(16L, status.getEvaluableResponses());
    }

    @Test
    public void testLiteraryDecisionWith12CasesIsRecognized() {
        AiBenchmarkRun run = createRun(4L, "Benchmark literário decisório NovelCopilot");
        when(runRepository.findById(4L)).thenReturn(Optional.of(run));

        List<AiBenchmarkResponse> responses = new ArrayList<>();
        long idCounter = 1;
        for (int i = 1; i <= 12; i++) {
            AiBenchmarkResponse rA = createResponse(idCounter++, 4L, "lit-dec-" + String.format("%02d", i), "A", "COMPLETED", new BigDecimal("0.0001"));
            rA.setContent("Resposta texto...");
            AiBenchmarkResponse rB = createResponse(idCounter++, 4L, "lit-dec-" + String.format("%02d", i), "B", "COMPLETED", new BigDecimal("0.0001"));
            rB.setContent("Resposta texto...");
            AiBenchmarkResponse rC = createResponse(idCounter++, 4L, "lit-dec-" + String.format("%02d", i), "C", "COMPLETED", new BigDecimal("0.0002"));
            rC.setContent("Resposta texto...");
            
            responses.add(rA);
            responses.add(rB);
            responses.add(rC);
        }
        when(responseRepository.findByRunId(4L)).thenReturn(responses);
        when(evaluationRepository.findAll()).thenReturn(Collections.emptyList());

        AiBenchmarkRunStatusDTO status = controller.getRunStatus(4L);

        assertEquals("LITERARY_DECISION", status.getSuiteType());
        assertEquals(3, status.getExpectedParticipantCount());
        assertEquals(12, status.getExpectedCases());
        assertEquals(36, status.getExpectedResponses());
        assertEquals(12L, status.getCompleteCases());
        assertEquals(0L, status.getIncompleteCases());
        assertEquals(36L, status.getEvaluableResponses());
        assertEquals(0L, status.getEvaluatedResponses());
        assertEquals(false, status.isEvaluationCompleted());
        assertEquals(new BigDecimal("0.0048"), status.getTotalCostUsd());
        assertNotNull(status.getResponses());
    }

    @Test
    public void testLiteraryDecisionMissingCLeavesCaseIncomplete() {
        AiBenchmarkRun run = createRun(4L, "Benchmark literário decisório NovelCopilot");
        when(runRepository.findById(4L)).thenReturn(Optional.of(run));

        List<AiBenchmarkResponse> responses = new ArrayList<>();
        long idCounter = 1;
        for (int i = 1; i <= 12; i++) {
            AiBenchmarkResponse rA = createResponse(idCounter++, 4L, "lit-dec-" + String.format("%02d", i), "A", "COMPLETED", new BigDecimal("0.0001"));
            rA.setContent("Texto");
            AiBenchmarkResponse rB = createResponse(idCounter++, 4L, "lit-dec-" + String.format("%02d", i), "B", "COMPLETED", new BigDecimal("0.0001"));
            rB.setContent("Texto");
            responses.add(rA);
            responses.add(rB);
            
            if (i < 12) {
                AiBenchmarkResponse rC = createResponse(idCounter++, 4L, "lit-dec-" + String.format("%02d", i), "C", "COMPLETED", new BigDecimal("0.0002"));
                rC.setContent("Texto");
                responses.add(rC);
            }
        }
        // One case only has A/B
        when(responseRepository.findByRunId(4L)).thenReturn(responses);
        when(evaluationRepository.findAll()).thenReturn(Collections.emptyList());

        AiBenchmarkRunStatusDTO status = controller.getRunStatus(4L);

        assertEquals("LITERARY_DECISION", status.getSuiteType());
        assertEquals(11L, status.getCompleteCases());
        assertEquals(1L, status.getIncompleteCases());
        assertEquals(33L, status.getEvaluableResponses());
        assertEquals(false, status.isEvaluationCompleted());
    }

    @Test
    public void testLiteraryDecisionReportBlockedAt35Evaluations() {
        AiBenchmarkRun run = createRun(4L, "Benchmark literário decisório NovelCopilot");
        when(runRepository.findById(4L)).thenReturn(Optional.of(run));

        List<AiBenchmarkResponse> responses = new ArrayList<>();
        long idCounter = 1;
        for (int i = 1; i <= 12; i++) {
            AiBenchmarkResponse rA = createResponse(idCounter++, 4L, "lit-dec-" + String.format("%02d", i), "A", "COMPLETED", new BigDecimal("0.0001"));
            rA.setContent("T");
            AiBenchmarkResponse rB = createResponse(idCounter++, 4L, "lit-dec-" + String.format("%02d", i), "B", "COMPLETED", new BigDecimal("0.0001"));
            rB.setContent("T");
            AiBenchmarkResponse rC = createResponse(idCounter++, 4L, "lit-dec-" + String.format("%02d", i), "C", "COMPLETED", new BigDecimal("0.0001"));
            rC.setContent("T");
            responses.add(rA);
            responses.add(rB);
            responses.add(rC);
        }
        when(responseRepository.findByRunId(4L)).thenReturn(responses);
        
        List<AiBenchmarkEvaluation> evals = new ArrayList<>();
        for (int i = 0; i < 35; i++) {
            evals.add(createEvaluation(responses.get(i).getId()));
        }
        when(evaluationRepository.findAll()).thenReturn(evals);

        AiBenchmarkRunStatusDTO status = controller.getRunStatus(4L);
        assertEquals(35L, status.getEvaluatedResponses());
        assertEquals(false, status.isEvaluationCompleted());
        assertEquals(false, status.isReportAvailable());
    }
    
    @Test
    public void testLiteraryDecisionReportAvailableAt36Evaluations() {
        AiBenchmarkRun run = createRun(4L, "Benchmark literário decisório NovelCopilot");
        when(runRepository.findById(4L)).thenReturn(Optional.of(run));

        List<AiBenchmarkResponse> responses = new ArrayList<>();
        long idCounter = 1;
        for (int i = 1; i <= 12; i++) {
            AiBenchmarkResponse rA = createResponse(idCounter++, 4L, "lit-dec-" + String.format("%02d", i), "A", "COMPLETED", new BigDecimal("0.0001"));
            rA.setContent("T");
            AiBenchmarkResponse rB = createResponse(idCounter++, 4L, "lit-dec-" + String.format("%02d", i), "B", "COMPLETED", new BigDecimal("0.0001"));
            rB.setContent("T");
            AiBenchmarkResponse rC = createResponse(idCounter++, 4L, "lit-dec-" + String.format("%02d", i), "C", "COMPLETED", new BigDecimal("0.0001"));
            rC.setContent("T");
            responses.add(rA);
            responses.add(rB);
            responses.add(rC);
        }
        when(responseRepository.findByRunId(4L)).thenReturn(responses);
        
        List<AiBenchmarkEvaluation> evals = new ArrayList<>();
        for (int i = 0; i < 36; i++) {
            evals.add(createEvaluation(responses.get(i).getId()));
        }
        when(evaluationRepository.findAll()).thenReturn(evals);

        AiBenchmarkRunStatusDTO status = controller.getRunStatus(4L);
        assertEquals(36L, status.getEvaluatedResponses());
        assertEquals(true, status.isEvaluationCompleted());
        assertEquals(true, status.isReportAvailable());
    }
}
