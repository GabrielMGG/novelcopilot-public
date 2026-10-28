package com.novelcopilot.backend.benchmark.controller;

import com.novelcopilot.backend.benchmark.config.AiBenchmarkParticipantProperties;
import com.novelcopilot.backend.benchmark.dto.AiBenchmarkCase;
import com.novelcopilot.backend.benchmark.dto.BlindBenchmarkResponse;
import com.novelcopilot.backend.benchmark.dto.AiBenchmarkRunStatusDTO;
import com.novelcopilot.backend.benchmark.entity.AiBenchmarkEvaluation;
import com.novelcopilot.backend.benchmark.entity.AiBenchmarkResponse;
import com.novelcopilot.backend.benchmark.entity.AiBenchmarkRun;
import com.novelcopilot.backend.benchmark.repository.AiBenchmarkEvaluationRepository;
import com.novelcopilot.backend.benchmark.repository.AiBenchmarkResponseRepository;
import com.novelcopilot.backend.benchmark.repository.AiBenchmarkRunRepository;
import com.novelcopilot.backend.benchmark.service.AiBenchmarkEstimateService;
import com.novelcopilot.backend.benchmark.service.AiBenchmarkEstimateService.AiBenchmarkEstimate;
import com.novelcopilot.backend.benchmark.service.AiBenchmarkEvaluationService;
import com.novelcopilot.backend.benchmark.service.AiBenchmarkEvaluationService.EvaluationRequest;
import com.novelcopilot.backend.benchmark.service.AiBenchmarkRunnerService;
import com.novelcopilot.backend.service.AiCostCalculator;
import com.novelcopilot.backend.service.AiProvider;
import com.novelcopilot.backend.service.AiRateLimitService;
import com.novelcopilot.backend.security.AuthenticatedUserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/internal/benchmark")
@Profile("benchmark")
@PreAuthorize("hasRole('ADMIN')")
public class AiBenchmarkController {

    private static final Logger log = LoggerFactory.getLogger(AiBenchmarkController.class);

    private final AiBenchmarkRunnerService runnerService;
    private final AiBenchmarkEstimateService estimateService;
    private final AiBenchmarkEvaluationService evaluationService;
    private final AiBenchmarkRunRepository runRepository;
    private final AiBenchmarkResponseRepository responseRepository;
    private final AiBenchmarkEvaluationRepository evaluationRepository;
    private final AiBenchmarkParticipantProperties participantProperties;
    private final AiCostCalculator costCalculator;
    private final List<AiProvider> providers;
    private final AiRateLimitService rateLimitService;

    public AiBenchmarkController(
            AiBenchmarkRunnerService runnerService,
            AiBenchmarkEstimateService estimateService,
            AiBenchmarkEvaluationService evaluationService,
            AiBenchmarkRunRepository runRepository,
            AiBenchmarkResponseRepository responseRepository,
            AiBenchmarkEvaluationRepository evaluationRepository,
            AiBenchmarkParticipantProperties participantProperties,
            AiCostCalculator costCalculator,
            List<AiProvider> providers,
            AiRateLimitService rateLimitService) {
        this.runnerService = runnerService;
        this.estimateService = estimateService;
        this.evaluationService = evaluationService;
        this.runRepository = runRepository;
        this.responseRepository = responseRepository;
        this.evaluationRepository = evaluationRepository;
        this.participantProperties = participantProperties;
        this.costCalculator = costCalculator;
        this.providers = providers;
        this.rateLimitService = rateLimitService;
    }

    @GetMapping("/config")
    public Map<String, Object> getConfig() {
        log.info("Auth Flow -> Controller reached: true (GET /config)");
        List<AiBenchmarkCase> cases = runnerService.getAvailableCases();
        Map<String, AiBenchmarkParticipantProperties.Participant> participants =
                participantProperties.getParticipants() != null
                        ? participantProperties.getParticipants()
                        : Map.of();
        return Map.of(
                "cases", cases,
                "participants", participants,
                "totalCases", cases.size(),
                "maxRunCostUsd", runnerService.getMaxRunCostUsd(),
                "maxDailyCostUsd", runnerService.getMaxDailyCostUsd()
        );
    }

    @PostMapping("/estimate")
    public AiBenchmarkEstimate estimate(@RequestBody Map<String, Object> payload) {
        log.info("Auth Flow -> Controller reached: true (POST /estimate)");
        @SuppressWarnings("unchecked")
        List<String> caseIds = (List<String>) payload.get("caseIds");
        @SuppressWarnings("unchecked")
        List<String> participantIds = (List<String>) payload.get("participantIds");
        
        List<AiBenchmarkCase> allCases = runnerService.getAvailableCases();
        List<AiBenchmarkCase> selected = allCases.stream()
                .filter(c -> caseIds == null || caseIds.isEmpty() || caseIds.contains(c.getId()))
                .toList();
        return estimateService.calculateEstimate(
                selected,
                participantIds,
                runnerService.getMaxRetries()
        );
    }

    @PostMapping("/run")
    public AiBenchmarkRun runBenchmark(
            @RequestBody Map<String, Object> payload,
            @AuthenticationPrincipal AuthenticatedUserPrincipal user) {
        log.info("Auth Flow -> Controller reached: true (POST /run)");
        String suiteName = (String) payload.getOrDefault("suiteName", "Manual Run");
        @SuppressWarnings("unchecked")
        List<String> caseIds = (List<String>) payload.get("caseIds");
        @SuppressWarnings("unchecked")
        List<String> participantIds = (List<String>) payload.get("participantIds");
        rateLimitService.checkBenchmarkRun(user.getId());
        return runnerService.runBenchmark(suiteName, caseIds, participantIds, user.getId());
    }

    @GetMapping("/runs")
    public List<Map<String, Object>> getRunHistory() {
        log.info("Auth Flow -> Controller reached: true (GET /runs)");
        List<AiBenchmarkRun> runs = new ArrayList<>(runRepository.findAll());
        runs.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
        
        List<Map<String, Object>> history = new ArrayList<>();
        List<AiBenchmarkEvaluation> allEvaluations = evaluationRepository.findAll();
        List<AiBenchmarkResponse> allResponses = responseRepository.findAll();
        
        for (AiBenchmarkRun run : runs) {
            List<AiBenchmarkResponse> runResponses = allResponses.stream()
                    .filter(r -> r.getRunId().equals(run.getId()))
                    .toList();
                    
            long completed = runResponses.stream().filter(r -> "COMPLETED".equals(r.getStatus())).count();
            long failed = runResponses.stream().filter(r -> "FAILED".equals(r.getStatus())).count();
            
            java.math.BigDecimal totalCost = java.math.BigDecimal.ZERO;
            for (AiBenchmarkResponse r : runResponses) {
                if (r.getEstimatedCostUsd() != null) {
                    totalCost = totalCost.add(r.getEstimatedCostUsd());
                }
            }
            
            long evaluatedCount = runResponses.stream()
                    .filter(r -> allEvaluations.stream()
                            .anyMatch(e -> e.getResponseId().equals(r.getId())))
                    .map(AiBenchmarkResponse::getCaseId)
                    .filter(java.util.Objects::nonNull)
                    .distinct()
                    .count();
            
            history.add(Map.of(
                    "id", run.getId(),
                    "suiteName", run.getSuiteName(),
                    "status", run.getStatus(),
                    "createdAt", run.getCreatedAt(),
                    "completedAt", run.getCompletedAt() != null ? run.getCompletedAt() : "",
                    "expectedCalls", runResponses.size() > 0 ? runResponses.size() : 0, // Fallback, not exact if it failed before creating responses, but okay
                    "completedCalls", completed,
                    "failedCalls", failed,
                    "evaluatedCases", evaluatedCount,
                    "totalCostUsd", totalCost
            ));
        }
        return history;
    }
    @GetMapping("/runs/{runId}")
    public AiBenchmarkRunStatusDTO getRunStatus(@PathVariable Long runId) {
        log.info("Auth Flow -> Controller reached: true (GET /runs/{})", runId);
        AiBenchmarkRun run = runRepository.findById(runId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Run not found"));
        List<AiBenchmarkResponse> responses = responseRepository.findByRunId(runId);
        List<AiBenchmarkEvaluation> evaluations = evaluationRepository.findAll();
        
        log.info("Run {}: Found {} responses, {} total evaluations in DB", runId, responses.size(), evaluations.size());
        
        long completed = responses.stream().filter(r -> "COMPLETED".equals(r.getStatus())).count();
        long failed = responses.stream().filter(r -> "FAILED".equals(r.getStatus())).count();
        
        String suiteType = "LEGACY";
        int expectedCases = 8;
        int expectedParticipantCount = 2;
        
        boolean hasLitDecCases = responses.stream().anyMatch(r -> r.getCaseId() != null && r.getCaseId().startsWith("lit-dec-"));
        boolean isCompatName = run.getSuiteName() != null && run.getSuiteName().toLowerCase().contains("compatibilidade");
        boolean isLiteraryName = run.getSuiteName() != null && run.getSuiteName().toLowerCase().contains("liter");
        boolean isFinalName = "FINAL_FACTS_CHARACTERS".equals(run.getSuiteName());
        
        if (AiBenchmarkRunnerService.NC014_RETEST_SUITE.equals(run.getSuiteName())) {
            suiteType = "NC014_RETEST_ARKENDU";
            expectedCases = 1;
            expectedParticipantCount = 3;
        } else if (isFinalName) {
            suiteType = "FINAL_FACTS_CHARACTERS";
            expectedCases = 8;
            expectedParticipantCount = 3;
        } else if (hasLitDecCases || (isLiteraryName && responses.size() >= 36)) {
            suiteType = "LITERARY_DECISION";
            expectedCases = 12;
            expectedParticipantCount = 3;
        } else if (isCompatName) {
            suiteType = "COMPATIBILITY";
            expectedCases = 8;
            expectedParticipantCount = 3;
        } else if (isLiteraryName) {
            suiteType = "LITERARY_LEGACY";
            expectedCases = 8;
            expectedParticipantCount = 2;
        }

        log.info("Run {}: Detected suiteType={}, expectedCases={}, expectedParticipantCount={}", runId, suiteType, expectedCases, expectedParticipantCount);
        log.info("Run {}: Starting metrics calculation", runId);

        long completeCases = 0;
        long incompleteCases = 0;
        java.util.Map<String, List<AiBenchmarkResponse>> caseResponses = responses.stream()
                .filter(r -> r.getCaseId() != null)
                .collect(java.util.stream.Collectors.groupingBy(AiBenchmarkResponse::getCaseId));
        
        for (java.util.Map.Entry<String, List<AiBenchmarkResponse>> entry : caseResponses.entrySet()) {
            List<AiBenchmarkResponse> cr = entry.getValue();
            long comp = cr.stream().filter(r -> "COMPLETED".equals(r.getStatus()) && r.getContent() != null && !r.getContent().trim().isEmpty()).count();
            boolean hasA = cr.stream().anyMatch(r -> "A".equals(r.getAnonymousLabel()));
            boolean hasB = cr.stream().anyMatch(r -> "B".equals(r.getAnonymousLabel()));
            boolean hasC = (expectedParticipantCount < 3) || cr.stream().anyMatch(r -> "C".equals(r.getAnonymousLabel()));
            
            if (comp == expectedParticipantCount && hasA && hasB && hasC) {
                completeCases++;
            } else {
                incompleteCases++;
            }
        }
        
        if (caseResponses.size() < expectedCases) {
            incompleteCases += (expectedCases - caseResponses.size());
        }
        
        long evaluableResponses = completeCases * expectedParticipantCount;
        
        long evaluatedResponses = responses.stream()
                .filter(r -> evaluations.stream().anyMatch(e -> e.getResponseId().equals(r.getId())))
                .count();
                
        boolean evaluationCompleted = evaluableResponses > 0 && evaluatedResponses == (expectedCases * expectedParticipantCount) && completeCases == expectedCases;
        log.info("Run {}: Metrics calculation finished. completeCases={}, evaluatedResponses={}", runId, completeCases, evaluatedResponses);

        log.info("Run {}: Starting cost calculation", runId);
        java.math.BigDecimal totalCostUsd = java.math.BigDecimal.ZERO;
        List<String> warnings = new ArrayList<>();
        
        for (AiBenchmarkResponse r : responses) {
            if ("COMPLETED".equals(r.getStatus())) {
                java.math.BigDecimal currentCost = r.getEffectiveCostUsd() != null ? r.getEffectiveCostUsd() : r.getEstimatedCostUsd();
                if (currentCost != null && currentCost.compareTo(java.math.BigDecimal.ZERO) > 0) {
                    totalCostUsd = totalCostUsd.add(currentCost);
                } else {
                    try {
                        AiCostCalculator.CalculationResult result = costCalculator.calculateCost(
                            r.getParticipantId(),
                            r.getProvider(), r.getModel(), 
                            r.getInputTokens() != null ? r.getInputTokens() : 0L, 
                            r.getCachedInputTokens() != null ? r.getCachedInputTokens() : 0L, 
                            r.getOutputTokens() != null ? r.getOutputTokens() : 0L);
                        java.math.BigDecimal cost = result.costUsd();
                        if (cost != null) {
                            totalCostUsd = totalCostUsd.add(cost);
                        }
                    } catch (Exception e) {
                        warnings.add("Falha ao calcular custo offline para resposta " + r.getId() + ": " + e.getMessage());
                        log.warn("Run {}: Fallback offline cost calculation failed for response {}: {}", runId, r.getId(), e.getMessage());
                    }
                }
            }
        }
        log.info("Run {}: Cost calculation finished. totalCostUsd={}", runId, totalCostUsd);

        List<Object> secureResponses = new ArrayList<>();
        for (AiBenchmarkResponse r : responses) {
            if (evaluationCompleted) {
                secureResponses.add(r);
            } else {
                secureResponses.add(new BlindBenchmarkResponse(r));
            }
        }
        
        AiBenchmarkRunStatusDTO dto = new AiBenchmarkRunStatusDTO();
        dto.setId(run.getId());
        dto.setRun(run);
        dto.setSuiteType(suiteType);
        dto.setExpectedParticipantCount(expectedParticipantCount);
        dto.setExpectedCases(expectedCases);
        dto.setExpectedResponses(expectedCases * expectedParticipantCount);
        dto.setCompletedResponses(completed);
        dto.setFailedResponses(failed);
        dto.setCompleteCases(completeCases);
        dto.setIncompleteCases(incompleteCases);
        dto.setEvaluableResponses(evaluableResponses);
        dto.setEvaluatedResponses(evaluatedResponses);
        dto.setEvaluationCompleted(evaluationCompleted);
        dto.setReportAvailable(evaluationCompleted);
        dto.setTotalCostUsd(totalCostUsd);
        dto.setResponses(secureResponses);
        dto.setWarnings(warnings);
        
        return dto;
    }


    @PostMapping("/runs/{runId}/cases/{caseId}/evaluate")
    public Map<String, String> evaluateCase(
            @PathVariable Long runId,
            @PathVariable String caseId,
            @RequestBody Map<String, Object> payload
    ) {
        String evaluatorId = (String) payload.getOrDefault("evaluatorId", "local_admin");
        String preference = (String) payload.get("preference");
        
        List<EvaluationRequest> requests = new ArrayList<>();
        
        if (payload.containsKey("requests")) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> reqs = (List<Map<String, Object>>) payload.get("requests");
            for (Map<String, Object> req : reqs) {
                requests.add(parseEvaluationRequest(req));
            }
        } else {
            @SuppressWarnings("unchecked")
            Map<String, Object> reqA = (Map<String, Object>) payload.get("requestA");
            @SuppressWarnings("unchecked")
            Map<String, Object> reqB = (Map<String, Object>) payload.get("requestB");
            @SuppressWarnings("unchecked")
            Map<String, Object> reqC = (Map<String, Object>) payload.get("requestC");
            
            if (reqA != null) requests.add(parseEvaluationRequest(reqA));
            if (reqB != null) requests.add(parseEvaluationRequest(reqB));
            if (reqC != null) requests.add(parseEvaluationRequest(reqC));
        }
        
        evaluationService.evaluateCase(runId, caseId, evaluatorId, requests, preference);
        return Map.of("status", "success");
    }

    @PostMapping("/runs/{runId}/recalculate-costs")
    public Map<String, Object> recalculateCosts(@PathVariable Long runId) {
        List<AiBenchmarkResponse> responses = responseRepository.findByRunId(runId);
        int updated = 0;
        java.math.BigDecimal previousTotalCost = java.math.BigDecimal.ZERO;
        java.math.BigDecimal newTotalCost = java.math.BigDecimal.ZERO;
        
        for (AiBenchmarkResponse r : responses) {
            if ("COMPLETED".equals(r.getStatus())) {
                java.math.BigDecimal currentCost = r.getEffectiveCostUsd() != null ? r.getEffectiveCostUsd() : r.getEstimatedCostUsd();
                if (currentCost != null) {
                    previousTotalCost = previousTotalCost.add(currentCost);
                }

                if (currentCost == null || currentCost.compareTo(java.math.BigDecimal.ZERO) == 0) {
                    long inputTokens = r.getInputTokens() != null ? r.getInputTokens() : 0;
                    long cachedTokens = r.getCachedInputTokens() != null ? r.getCachedInputTokens() : 0;
                    long outputTokens = r.getOutputTokens() != null ? r.getOutputTokens() : 0;
                    
                    AiCostCalculator.CalculationResult calcResult = costCalculator.calculateCost(
                        r.getParticipantId(),
                        r.getProvider(),
                        r.getModel(),
                        inputTokens,
                        cachedTokens,
                        outputTokens
                    );
                    
                    java.math.BigDecimal calc = calcResult.costUsd();
                    r.setCalculatedCostUsd(calc);
                    r.setEffectiveCostUsd(calc);
                    r.setEstimatedCostUsd(calc); // manter compatibilidade
                    r.setCostSource(calcResult.source());
                    responseRepository.save(r);
                    updated++;
                    currentCost = calc;
                }
                
                if (currentCost != null) {
                    newTotalCost = newTotalCost.add(currentCost);
                }
            }
        }
        
        return Map.of(
            "updatedResponses", updated,
            "previousTotalCostUsd", previousTotalCost,
            "newTotalCostUsd", newTotalCost,
            "status", "success"
        );
    }

    private EvaluationRequest parseEvaluationRequest(Map<String, Object> req) {
        return new EvaluationRequest(
                Long.valueOf(req.get("responseId").toString()),
                req.containsKey("literaryQuality") ? (int) req.get("literaryQuality") : 3,
                req.containsKey("contextCoherence") ? (int) req.get("contextCoherence") : 3,
                req.containsKey("instructionFollowing") ? (int) req.get("instructionFollowing") : 3,
                req.containsKey("languageNaturalness") ? (int) req.get("languageNaturalness") : 3,
                req.containsKey("repetitionControl") ? (int) req.get("repetitionControl") : 3,
                req.containsKey("stylePreservation") ? (int) req.get("stylePreservation") : 3,
                req.containsKey("factPreservation") ? (int) req.get("factPreservation") : 3,
                req.containsKey("characterPreservation") ? (int) req.get("characterPreservation") : 3,
                req.containsKey("narrativeProgression") ? (int) req.get("narrativeProgression") : 3,
                req.containsKey("noContradictions") ? (int) req.get("noContradictions") : 3,
                (String) req.get("notes"),
                (String) req.get("compatibilityStatus")
        );
    }
    
    @GetMapping("/runs/{runId}/report")
    public Map<String, Object> getReport(@PathVariable Long runId) {
        AiBenchmarkRunStatusDTO status = getRunStatus(runId);
        if (!status.isReportAvailable()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "O relatório só pode revelar os modelos após a avaliação cega completa."
            );
        }
        AiBenchmarkRun run = runRepository.findById(runId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Run not found"));
        List<AiBenchmarkResponse> responses = responseRepository.findByRunId(runId);
        List<AiBenchmarkEvaluation> evaluations = evaluationRepository.findAll();
        
        List<AiBenchmarkEvaluation> runEvaluations = evaluations.stream()
            .filter(e -> responses.stream().anyMatch(r -> r.getId().equals(e.getResponseId())))
            .toList();
            
        // Calculate aggregations here or in a dedicated service
        // We will just return the raw data and let the frontend do the report layout for now
        // Wait, the prompt says "Ao concluir todos os casos, mostrar: modelo vencedor geral... pontuação média...". The backend can compute this or the frontend can. Let's let the frontend compute the report from raw data.
        // Or actually the requirement says: "gerar um relatório comparativo final". Usually doing it in backend is cleaner, but frontend is fine.
        return Map.of(
            "run", run,
            "responses", responses,
            "evaluations", runEvaluations
        );
    }

    @PostMapping("/runs/{runId}/retry-failed")
    public Map<String, String> retryFailed(
            @PathVariable Long runId,
            @AuthenticationPrincipal AuthenticatedUserPrincipal user
    ) {
        log.info("Auth Flow -> Controller reached: true (POST /retry-failed)");
        rateLimitService.checkBenchmarkRetry(user.getId());
        runnerService.retryFailed(runId);
        return Map.of("status", "success");
    }
}
