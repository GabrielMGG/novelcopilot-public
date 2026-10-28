package com.novelcopilot.backend.benchmark.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.novelcopilot.backend.benchmark.dto.AiBenchmarkCase;
import com.novelcopilot.backend.benchmark.entity.AiBenchmarkResponse;
import com.novelcopilot.backend.benchmark.entity.AiBenchmarkRun;
import com.novelcopilot.backend.benchmark.repository.AiBenchmarkResponseRepository;
import com.novelcopilot.backend.benchmark.repository.AiBenchmarkRunRepository;
import com.novelcopilot.backend.benchmark.config.AiBenchmarkParticipantProperties;
import com.novelcopilot.backend.dto.AiGenerationResult;
import com.novelcopilot.backend.service.AiCostCalculator;
import com.novelcopilot.backend.service.AiProvider;
import com.novelcopilot.backend.service.AiUsageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class AiBenchmarkRunnerService {

    private static final Logger log = LoggerFactory.getLogger(AiBenchmarkRunnerService.class);
    public static final String NC014_RETEST_SUITE = "NC014_RETEST_ARKENDU";
    public static final List<String> NC014_RETEST_PARTICIPANTS = List.of(
            "groq-qwen-3-6-27b",
            "cloudflare-gemma",
            "cloudflare-llama4-scout"
    );

    @Value("${ai.benchmark.enabled:false}")
    private boolean benchmarkEnabled;

    @Value("${ai.benchmark.max-cases-per-run:20}")
    private int maxCasesPerRun;

    @Value("${ai.benchmark.delay-between-calls-ms:10000}")
    private long delayBetweenCallsMs;

    @Value("${ai.benchmark.max-retries:3}")
    private int maxRetries;

    @Value("${ai.benchmark.max-run-cost-usd:1.00}")
    private BigDecimal maxRunCostUsd;

    @Value("${ai.benchmark.max-daily-cost-usd:3.00}")
    private BigDecimal maxDailyCostUsd;

    @Value("${spring.profiles.active:default}")
    private String activeProfile;

    private final List<AiProvider> aiProviders;
    private final AiBenchmarkRunRepository runRepository;
    private final AiBenchmarkResponseRepository responseRepository;
    private final AiCostCalculator aiCostCalculator;
    private final AiUsageService aiUsageService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AiBenchmarkParticipantProperties participantProperties;

    public AiBenchmarkRunnerService(
            List<AiProvider> aiProviders,
            AiBenchmarkRunRepository runRepository,
            AiBenchmarkResponseRepository responseRepository,
            AiCostCalculator aiCostCalculator,
            AiUsageService aiUsageService,
            AiBenchmarkParticipantProperties participantProperties
    ) {
        this.aiProviders = aiProviders;
        this.runRepository = runRepository;
        this.responseRepository = responseRepository;
        this.aiCostCalculator = aiCostCalculator;
        this.aiUsageService = aiUsageService;
        this.participantProperties = participantProperties;
    }

    public List<AiBenchmarkCase> getAvailableCases() {
        return loadCases();
    }

    public BigDecimal getMaxRunCostUsd() {
        return maxRunCostUsd != null ? maxRunCostUsd : new BigDecimal("1.00");
    }

    public BigDecimal getMaxDailyCostUsd() {
        return maxDailyCostUsd != null ? maxDailyCostUsd : new BigDecimal("3.00");
    }

    public int getMaxRetries() {
        return Math.max(0, maxRetries);
    }

    public synchronized AiBenchmarkRun runBenchmark(
            String suiteName,
            List<String> caseIds,
            List<String> participantIds,
            String createdBy) {
        if (!benchmarkEnabled) {
            throw new IllegalStateException("Benchmark is disabled.");
        }
        if ("prod".equalsIgnoreCase(activeProfile) || "production".equalsIgnoreCase(activeProfile)) {
            throw new IllegalStateException("Cannot run benchmark in production without explicit authorization.");
        }
        if (createdBy == null || createdBy.isBlank()) {
            throw new IllegalArgumentException("Benchmark author is required.");
        }
        ensureNoActiveRun(null);
        
        if (suiteName == null || suiteName.trim().isEmpty()) {
            throw new IllegalArgumentException("Suite name is required.");
        }
        
        List<String> selectedParticipantIds = participantIds;
        if (selectedParticipantIds == null || selectedParticipantIds.isEmpty()) {
            throw new IllegalStateException("Participants not specified.");
        }
        
        if (selectedParticipantIds.size() < 2) {
            throw new IllegalStateException("Need at least 2 participants to run benchmark.");
        }

        List<AiBenchmarkCase> cases = getAvailableCases();
        if (cases.isEmpty()) {
            throw new IllegalStateException("No cases found to run.");
        }

        List<AiBenchmarkCase> selectedCases = cases.stream()
                .filter(c -> caseIds == null || caseIds.isEmpty() || caseIds.contains(c.getId()))
                .filter(c -> {
                    if (NC014_RETEST_SUITE.equals(suiteName)) {
                        return c.getId().startsWith("nc014-retest-");
                    } else if ("Benchmark literário decisório NovelCopilot".equals(suiteName)) {
                        return c.getId().startsWith("lit-dec-");
                    } else if ("FINAL_FACTS_CHARACTERS".equals(suiteName)) {
                        return c.getId().startsWith("lit-final-");
                    } else if (suiteName.toLowerCase().contains("compatibilidade")) {
                        return c.getCategory().toLowerCase().contains("compatibilidade");
                    } else {
                        return !c.getId().startsWith("lit-dec-") && !c.getId().startsWith("lit-final-");
                    }
                })
                .limit(maxCasesPerRun)
                .toList();
                
        if (selectedCases.isEmpty()) {
            throw new IllegalStateException("No cases selected or found to run.");
        }
        
        boolean isCompat = suiteName.toLowerCase().contains("compatibilidade");
        if (NC014_RETEST_SUITE.equals(suiteName)) {
            if (selectedParticipantIds.size() != NC014_RETEST_PARTICIPANTS.size()
                    || !new java.util.HashSet<>(selectedParticipantIds).equals(new java.util.HashSet<>(NC014_RETEST_PARTICIPANTS))) {
                throw new IllegalArgumentException("A suíte NC-014 exige exatamente Qwen 3.6, Gemma 4 e Llama 4 Scout.");
            }
            if (selectedCases.size() != 1 || !"nc014-retest-arkendu-01".equals(selectedCases.getFirst().getId())) {
                throw new IllegalArgumentException("A suíte NC-014 exige exatamente o caso congelado de Arkendu.");
            }
            AiBenchmarkCase frozenCase = selectedCases.getFirst();
            if (!"APROVADO".equals(frozenCase.getApprovalStatus())
                    || frozenCase.getCharacters() == null || frozenCase.getCharacters().isBlank()
                    || frozenCase.getStoryContext() == null || frozenCase.getStoryContext().isBlank()
                    || frozenCase.getEstablishedFacts() == null || frozenCase.getEstablishedFacts().isBlank()
                    || frozenCase.getChapterContent() == null || frozenCase.getChapterContent().isBlank()) {
                throw new IllegalArgumentException("O caso NC-014 precisa conter capítulo, memórias, card de personagem e card de mundo aprovados.");
            }
        } else if (isCompat) {
            if (selectedParticipantIds.size() != 3) {
                throw new IllegalArgumentException("A suite de compatibilidade exige exatamente 3 participantes.");
            }
            if (selectedCases.size() != 8) {
                throw new IllegalArgumentException("A suite de compatibilidade exige exatamente 8 casos.");
            }
            for (String pid : selectedParticipantIds) {
                if (!List.of("openrouter-qwen3-235b", "openrouter-mistral-small-4", "openrouter-mistral-small-3-2").contains(pid)) {
                    throw new IllegalArgumentException("Participante incompatível com a suíte de compatibilidade: " + pid);
                }
            }
            for (AiBenchmarkCase c : selectedCases) {
                if (!c.getCategory().toLowerCase().contains("compatibilidade")) {
                    throw new IllegalArgumentException("Caso incompatível com a suíte de compatibilidade: " + c.getId());
                }
            }
        } else if ("FINAL_FACTS_CHARACTERS".equals(suiteName)) {
            if (selectedParticipantIds.size() != 3) {
                throw new IllegalArgumentException("A suite FINAL_FACTS_CHARACTERS exige exatamente 3 participantes.");
            }
            if (selectedCases.size() != 8) {
                throw new IllegalArgumentException("A suite FINAL_FACTS_CHARACTERS exige exatamente 8 casos.");
            }
            for (String pid : selectedParticipantIds) {
                if (!List.of(
                        "groq-llama",
                        "openrouter-qwen3-235b",
                        "openrouter-mistral-small-4"
                ).contains(pid)) {
                    throw new IllegalArgumentException("Participante incompatível com a suíte FINAL_FACTS_CHARACTERS: " + pid);
                }
            }
            for (AiBenchmarkCase c : selectedCases) {
                if (!c.getId().startsWith("lit-final-")) {
                    throw new IllegalArgumentException("Caso legado ou inválido na suíte FINAL_FACTS_CHARACTERS: " + c.getId());
                }
            }
        } else if ("Benchmark literário decisório NovelCopilot".equals(suiteName)) {
            if (selectedParticipantIds.size() != 3) {
                throw new IllegalArgumentException("A suite literária decisória exige exatamente 3 participantes.");
            }
            if (selectedCases.size() != 12) {
                throw new IllegalArgumentException("A suite literária decisória exige exatamente 12 casos.");
            }
            for (String pid : selectedParticipantIds) {
                if (!List.of("openrouter-qwen3-235b", "openrouter-mistral-small-4", "openrouter-mistral-small-3-2").contains(pid)) {
                    throw new IllegalArgumentException("Participante incompatível com a suíte literária: " + pid);
                }
            }
            for (AiBenchmarkCase c : selectedCases) {
                if (!c.getId().startsWith("lit-dec-")) {
                    throw new IllegalArgumentException("Caso legado ou inválido na suíte literária: " + c.getId());
                }
                if (c.getCategory() != null && c.getCategory().toLowerCase().contains("continua") && (c.getChapterContent() == null || c.getChapterContent().isEmpty())) {
                    throw new IllegalArgumentException("Tarefa de continuação sem contexto: " + c.getId());
                }
                if (c.getCategory() != null && (c.getCategory().toLowerCase().contains("reescrita") || c.getCategory().toLowerCase().contains("revis")) && (c.getSelectedExcerpt() == null || c.getSelectedExcerpt().isEmpty())) {
                    throw new IllegalArgumentException("Tarefa de reescrita sem trecho original: " + c.getId());
                }
                if (!"APROVADO".equals(c.getApprovalStatus())) {
                    throw new IllegalArgumentException("Todos os casos precisam estar revisados e aprovados. Caso pendente: " + c.getId());
                }
            }
        }

        AiBenchmarkEstimateService.AiBenchmarkEstimate estimate = new AiBenchmarkEstimateService(participantProperties)
                .calculateEstimate(selectedCases, selectedParticipantIds, getMaxRetries());

        BigDecimal reservedCost = estimate.maxEstimatedCost();
        ensureWithinFinancialLimits(reservedCost, null);

        AiBenchmarkRun run = new AiBenchmarkRun();
        run.setSuiteName(suiteName);
        run.setStatus("RUNNING");
        run.setStartedAt(LocalDateTime.now());
        run.setCreatedAt(LocalDateTime.now());
        run.setCreatedBy(createdBy);
        run.setEstimatedCostUsd(reservedCost);
        run.setFinalCostUsd(BigDecimal.ZERO);
        final AiBenchmarkRun savedRun = runRepository.save(run);

        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                boolean hasErrors = false;
                for (AiBenchmarkCase testCase : selectedCases) {
                    List<String> shuffledParticipants = new ArrayList<>(selectedParticipantIds);
                    Collections.shuffle(shuffledParticipants);
                    
                    String instructions = buildInstructions(testCase);
                    String prompt = buildPrompt(testCase);

                    char currentLabel = 'A';
                    for (String participant : shuffledParticipants) {
                        boolean success = runParticipant(savedRun, testCase, String.valueOf(currentLabel), participant, instructions, prompt);
                        if (!success) {
                            hasErrors = true;
                        }
                        currentLabel++;
                        delay();
                    }
                }
                savedRun.setStatus(hasErrors ? "COMPLETED_WITH_ERRORS" : "COMPLETED");
            } catch (Exception e) {
                log.error("Benchmark run failed", e);
                savedRun.setStatus("FAILED");
            } finally {
                savedRun.setCompletedAt(LocalDateTime.now());
                savedRun.setFinalCostUsd(calculateFinalCost(savedRun.getId()));
                runRepository.save(savedRun);
            }
        });

        return savedRun;
    }

    private void delay() {
        try {
            Thread.sleep(delayBetweenCallsMs);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    private boolean runParticipant(AiBenchmarkRun run, AiBenchmarkCase testCase, String label, String participantId, String instructions, String prompt) {
        AiBenchmarkParticipantProperties.Participant participant = participantProperties.getParticipants().get(participantId);
        if (participant == null || !participant.isEnabled()) {
            throw new IllegalStateException("Participant not found or disabled: " + participantId);
        }

        AiProvider provider = aiProviders.stream()
                .filter(p -> p.providerName().equalsIgnoreCase(participant.getProvider()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Provider not found: " + participant.getProvider()));

        AiBenchmarkResponse response = new AiBenchmarkResponse();
        response.setRunId(run.getId());
        response.setCaseId(testCase.getId());
        response.setParticipantId(participantId);
        response.setAnonymousLabel(label);
        response.setProvider(provider.providerName());
        response.setModel(participant.getModel());
        response.setPromptSha256(sha256(instructions + "\n" + prompt));
        response.setCreatedAt(LocalDateTime.now());
        response.setStatus("RUNNING");

        int attempts = 0;
        long backoffMs = 30000;
        Exception lastError = null;
        
        while (attempts <= maxRetries) {
            try {
                if (attempts > 0) {
                    log.info("Retry {}/{} for participant {} case {}", attempts, maxRetries, participantId, testCase.getId());
                }
                long startTime = System.currentTimeMillis();
                AiGenerationResult result = provider.generate(instructions, prompt, participant.getModel());
                long duration = System.currentTimeMillis() - startTime;

                response.setContent(result.content());
                response.setInputTokens(safeZero(result.inputTokens()));
                response.setCachedInputTokens(safeZero(result.cachedInputTokens()));
                response.setOutputTokens(safeZero(result.outputTokens()));
                response.setTotalTokens(safeZero(result.totalTokens()));
                
                BigDecimal providerCost = result.providerReportedCostUsd();
                AiCostCalculator.CalculationResult calcResult = aiCostCalculator.calculateCost(
                        participantId,
                        result.provider(), 
                        result.model(), 
                        response.getInputTokens(), 
                        response.getCachedInputTokens(), 
                        response.getOutputTokens()
                );
                BigDecimal calculatedCost = calcResult.costUsd();

                response.setProviderReportedCostUsd(providerCost);
                response.setCalculatedCostUsd(calculatedCost);

                if (providerCost != null && providerCost.compareTo(BigDecimal.ZERO) > 0) {
                    response.setEffectiveCostUsd(providerCost);
                    response.setCostSource("PROVIDER");
                } else {
                    log.warn("OpenRouter não retornou custo válido (maior que zero) para a chamada. Usando custo CALCULATED.");
                    response.setEffectiveCostUsd(calculatedCost != null ? calculatedCost : BigDecimal.ZERO);
                    response.setCostSource("CALCULATED");
                }

                // Mantém backward compatibility
                response.setEstimatedCostUsd(response.getEffectiveCostUsd());
                response.setDurationMs(duration);
                response.setStatus("COMPLETED");

                // Salva na tabela normal de estatisticas pra auditoria
                aiUsageService.recordSuccess("BENCHMARK", null, null, "BENCHMARK", result);
                responseRepository.save(response);
                return true;
            } catch (Exception e) {
                lastError = e;
                String msg = e.getMessage() != null ? e.getMessage() : "";
                if (msg.contains("429")) {
                    long waitMs = extractRetryAfterMs(msg, backoffMs);
                    log.warn("429 Too Many Requests for participant {} case {}. Waiting {} ms", participantId, testCase.getId(), waitMs);
                    try { Thread.sleep(waitMs); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                    backoffMs *= 2; // exponential for next failure
                    attempts++;
                } else if (msg.contains("400") || msg.contains("401") || msg.contains("403")) {
                    log.error("Fatal error (400/401/403) for participant {} case {}: {}", participantId, testCase.getId(), msg);
                    break;
                } else {
                    log.error("Unknown error for participant {} case {}: {}", participantId, testCase.getId(), msg);
                    try { Thread.sleep(5000); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                    attempts++;
                }
            }
        }
        
        response.setStatus("FAILED");
        String finalMsg = lastError != null && lastError.getMessage() != null ? lastError.getMessage() : "Unknown Error";
        response.setErrorCode("Attempt " + attempts + ": " + finalMsg.substring(0, Math.min(finalMsg.length(), 200)));
        responseRepository.save(response);
        return false;
    }

    private long extractRetryAfterMs(String errorMessage, long defaultBackoffMs) {
        try {
            if (errorMessage.contains("Retry-After:")) {
                String[] parts = errorMessage.split("Retry-After:");
                if (parts.length > 1) {
                    String val = parts[1].trim().split("[^0-9]")[0]; // extract first number
                    if (!val.isEmpty()) {
                        return Long.parseLong(val) * 1000;
                    }
                }
            }
        } catch (Exception ignore) {}
        return defaultBackoffMs;
    }

    public synchronized void retryFailed(Long runId) {
        if (!benchmarkEnabled) {
            throw new IllegalStateException("Benchmark is disabled.");
        }
        AiBenchmarkRun run = runRepository.findById(runId).orElseThrow(() -> new IllegalArgumentException("Run not found"));
        ensureNoActiveRun(null);

        List<AiBenchmarkResponse> responses = responseRepository.findByRunId(runId);
        List<AiBenchmarkResponse> failedResponses = responses.stream()
                .filter(r -> "FAILED".equals(r.getStatus()))
                .toList();

        if (failedResponses.isEmpty()) {
            throw new IllegalStateException("No failed responses to retry.");
        }

        BigDecimal retryBudget = calculateRetryBudget(failedResponses);
        BigDecimal updatedRunBudget = safeCost(run.getEstimatedCostUsd()).add(retryBudget);
        ensureWithinFinancialLimits(updatedRunBudget, runId);
        run.setEstimatedCostUsd(updatedRunBudget);
        run.setStatus("RUNNING");
        run.setCompletedAt(null);
        runRepository.save(run);

        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                for (AiBenchmarkResponse failedResp : failedResponses) {
                    AiBenchmarkCase testCase = getAvailableCases().stream().filter(c -> c.getId().equals(failedResp.getCaseId())).findFirst().orElse(null);
                    if (testCase == null) continue;

                    String instructions = buildInstructions(testCase);
                    String prompt = buildPrompt(testCase);

                    responseRepository.delete(failedResp); // Replace the old failed one
                    runParticipant(run, testCase, failedResp.getAnonymousLabel(), failedResp.getParticipantId(), instructions, prompt);
                    delay();
                }

                // Verify final status of the run
                List<AiBenchmarkResponse> updatedResponses = responseRepository.findByRunId(runId);
                boolean stillHasErrors = updatedResponses.stream().anyMatch(r -> "FAILED".equals(r.getStatus()));
                run.setStatus(stillHasErrors ? "COMPLETED_WITH_ERRORS" : "COMPLETED");
            } catch (Exception e) {
                log.error("Retry run failed", e);
                run.setStatus("FAILED");
            } finally {
                run.setCompletedAt(LocalDateTime.now());
                run.setFinalCostUsd(calculateFinalCost(runId));
                runRepository.save(run);
            }
        });
    }

    private void ensureNoActiveRun(Long ignoredRunId) {
        boolean activeRunExists = runRepository.findAll().stream()
                .anyMatch(run -> "RUNNING".equals(run.getStatus())
                        && (ignoredRunId == null || !ignoredRunId.equals(run.getId())));
        if (activeRunExists) {
            throw new IllegalStateException("Another benchmark execution is already active.");
        }
    }

    private void ensureWithinFinancialLimits(BigDecimal proposedRunBudget, Long replacedRunId) {
        BigDecimal runLimit = maxRunCostUsd != null ? maxRunCostUsd : new BigDecimal("1.00");
        BigDecimal dailyLimit = maxDailyCostUsd != null ? maxDailyCostUsd : new BigDecimal("3.00");

        if (proposedRunBudget.compareTo(runLimit) > 0) {
            throw new IllegalStateException(
                    "O custo máximo estimado (" + proposedRunBudget
                            + " USD) ultrapassa o limite por execução de "
                            + runLimit + " USD.");
        }

        BigDecimal dailyBudget = runRepository
                .findByCreatedAtGreaterThanEqual(LocalDate.now().atStartOfDay())
                .stream()
                .filter(run -> replacedRunId == null || !replacedRunId.equals(run.getId()))
                .map(this::budgetedCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .add(proposedRunBudget);

        if (dailyBudget.compareTo(dailyLimit) > 0) {
            throw new IllegalStateException(
                    "O custo acumulado previsto para hoje (" + dailyBudget
                            + " USD) ultrapassa o limite diário de "
                            + dailyLimit + " USD.");
        }
    }

    private BigDecimal calculateRetryBudget(List<AiBenchmarkResponse> failedResponses) {
        BigDecimal retryEstimate = BigDecimal.ZERO;
        List<AiBenchmarkCase> availableCases = getAvailableCases();

        for (AiBenchmarkResponse failedResponse : failedResponses) {
            AiBenchmarkCase benchmarkCase = availableCases.stream()
                    .filter(candidate -> candidate.getId().equals(failedResponse.getCaseId()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException(
                            "Benchmark case not found for retry: " + failedResponse.getCaseId()));

            BigDecimal responseEstimate = new AiBenchmarkEstimateService(participantProperties)
                    .calculateEstimate(
                            List.of(benchmarkCase),
                            List.of(failedResponse.getParticipantId()),
                            getMaxRetries())
                    .maxEstimatedCost();
            retryEstimate = retryEstimate.add(responseEstimate);
        }

        return retryEstimate;
    }

    private BigDecimal calculateFinalCost(Long runId) {
        return responseRepository.findByRunId(runId).stream()
                .map(response -> {
                    if (response.getEffectiveCostUsd() != null) {
                        return response.getEffectiveCostUsd();
                    }
                    if (response.getCalculatedCostUsd() != null) {
                        return response.getCalculatedCostUsd();
                    }
                    return safeCost(response.getEstimatedCostUsd());
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal budgetedCost(AiBenchmarkRun run) {
        return safeCost(run.getEstimatedCostUsd()).max(safeCost(run.getFinalCostUsd()));
    }

    private BigDecimal safeCost(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private String buildInstructions(AiBenchmarkCase testCase) {
        if (testCase.getExactTask() != null && !testCase.getExactTask().isEmpty()) {
            return """
                    Você é o assistente literário especialista do NovelCopilot.
                    Você deve seguir estritamente as regras de mundo, restrições e o estilo narrativo solicitado.
                    Não adicione introduções, explicações ou notas. Entregue apenas a prosa literária final.
                    """;
        }
        return """
                Voce e o assistente de escrita do NovelCopilot. Trate o material como narrativa ficcional.
                Use o idioma predominante do conteudo narrativo como idioma da resposta: %s.
                Siga a instrucao e gere o texto ficcional correspondente.
                Nao inclua markdown nem explicacoes. Retorne so o texto ficcional.
                """.formatted(testCase.getExpectedLanguage());
    }

    String buildPrompt(AiBenchmarkCase testCase) {
        if (testCase.getExactTask() != null && !testCase.getExactTask().isEmpty()) {
            StringBuilder sb = new StringBuilder();
            if (testCase.getStoryContext() != null && !testCase.getStoryContext().isEmpty()) {
                sb.append("Card de Mundo Relevante:\n").append(testCase.getStoryContext()).append("\n\n");
            }
            if (testCase.getChapterContent() != null && !testCase.getChapterContent().isEmpty()) {
                sb.append("Contexto Anterior:\n").append(testCase.getChapterContent()).append("\n\n");
            }
            if (testCase.getSelectedExcerpt() != null && !testCase.getSelectedExcerpt().isEmpty()) {
                sb.append("Trecho Original:\n").append(testCase.getSelectedExcerpt()).append("\n\n");
            }
            sb.append("Personagens:\n").append(testCase.getCharacters()).append("\n\n");
            sb.append("Fatos Estabelecidos:\n").append(testCase.getEstablishedFacts()).append("\n\n");
            sb.append("Tarefa Exata:\n").append(testCase.getExactTask()).append("\n\n");
            sb.append("Objetivo Narrativo:\n").append(testCase.getNarrativeObjective()).append("\n\n");
            sb.append("Elementos Obrigatórios:\n").append(testCase.getMandatoryElements()).append("\n\n");
            sb.append("Restrições:\n").append(testCase.getRestrictions()).append("\n\n");
            sb.append("Formato e Tom:\n");
            sb.append("Idioma: ").append(testCase.getExpectedLanguage()).append("\n");
            sb.append("Tom: ").append(testCase.getTone()).append("\n");
            sb.append("Ponto de Vista: ").append(testCase.getPov()).append("\n");
            sb.append("Faixa de Palavras: ").append(testCase.getWordRange()).append("\n");
            sb.append("Formato de Saída: ").append(testCase.getOutputFormat()).append("\n");
            return sb.toString();
        }
        return """
                Contexto do Mundo/Historia:
                %s
                
                Conteudo do Capitulo:
                %s
                
                Trecho selecionado (se houver):
                %s
                
                Instrucao do Usuario:
                %s
                """.formatted(
                testCase.getStoryContext() != null ? testCase.getStoryContext() : "Nao informado",
                testCase.getChapterContent() != null ? testCase.getChapterContent() : "Nao informado",
                testCase.getSelectedExcerpt() != null ? testCase.getSelectedExcerpt() : "Nao informado",
                testCase.getUserInstruction()
        );
    }

    String promptSha256(AiBenchmarkCase testCase) {
        return sha256(buildInstructions(testCase) + "\n" + buildPrompt(testCase));
    }

    private String sha256(String value) {
        try {
            byte[] digest = java.security.MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (java.security.NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 indisponível.", ex);
        }
    }

    private List<AiBenchmarkCase> loadCases() {
        List<AiBenchmarkCase> allCases = new ArrayList<>();
        try (InputStream is = new ClassPathResource("benchmark/benchmark-cases.json").getInputStream()) {
            allCases.addAll(objectMapper.readValue(is, new TypeReference<>() {}));
        } catch (Exception e) {
            log.error("Failed to load benchmark-cases.json", e);
        }
        try (InputStream is = new ClassPathResource("benchmark/benchmark-literary-cases.json").getInputStream()) {
            List<AiBenchmarkCase> literaries = objectMapper.readValue(is, new TypeReference<>() {});
            allCases.addAll(literaries);
        } catch (Exception e) {
            log.error("Failed to load benchmark-literary-cases.json", e);
        }
        try (InputStream is = new ClassPathResource("benchmark/benchmark-final-cases.json").getInputStream()) {
            List<AiBenchmarkCase> finalCases = objectMapper.readValue(is, new TypeReference<>() {});
            allCases.addAll(finalCases);
        } catch (Exception e) {
            log.error("Failed to load benchmark-final-cases.json", e);
        }
        return allCases;
    }

    private long safeZero(Long val) {
        return val != null ? val : 0L;
    }
}
