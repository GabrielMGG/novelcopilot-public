package com.novelcopilot.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.novelcopilot.backend.config.AiQuotaProperties;
import com.novelcopilot.backend.config.NovelAiConfigProperties;
import com.novelcopilot.backend.dto.NovelAiRequest;
import com.novelcopilot.backend.dto.NovelAiResponse;
import com.novelcopilot.backend.dto.NovelAiAction;
import com.novelcopilot.backend.dto.AiGenerationHistoryItem;
import com.novelcopilot.backend.dto.PreparedAiGeneration;
import com.novelcopilot.backend.entity.AiGenerationUsage;
import com.novelcopilot.backend.entity.CreditUsageStatus;
import com.novelcopilot.backend.repository.AiGenerationUsageRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.EnumSet;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class NovelAiService {

    private static final Logger logger = LoggerFactory.getLogger(NovelAiService.class);

    private static final EnumSet<NovelAiAction> EXPENSIVE_ACTIONS = EnumSet.of(
            NovelAiAction.REVIEW_CHAPTER,
            NovelAiAction.CREATE_NEXT_CHAPTER,
            NovelAiAction.WRITE_CHAPTER_FROM_BEATS
    );

    private final NovelAiConfigProperties config;
    private final NovelContextService contextService;
    private final NovelPromptBuilder promptBuilder;
    private final AiGenerationUsageRepository usageRepository;
    private final AiProviderResolver aiProviderResolver;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AiCostCalculator costCalculator;
    private final CreditService creditService;
    private final AiCreditCalculator aiCreditCalculator;
    private final AiQuotaProperties quotaConfig;
    private final AiRateLimitService rateLimitService;
    private final DevelopmentCreditService developmentCreditService;
    private final TokenEstimator tokenEstimator;
    private final AiGenerationHistoryService historyService;

    private final ConcurrentHashMap<String, Boolean> userLocks = new ConcurrentHashMap<>();

    public NovelAiService(NovelAiConfigProperties config,
                          NovelContextService contextService,
                          NovelPromptBuilder promptBuilder,
                          AiGenerationUsageRepository usageRepository,
                          AiProviderResolver aiProviderResolver,
                          AiCostCalculator costCalculator,
                          CreditService creditService,
                          AiCreditCalculator aiCreditCalculator,
                          AiQuotaProperties quotaConfig,
                          AiRateLimitService rateLimitService,
                          DevelopmentCreditService developmentCreditService,
                          TokenEstimator tokenEstimator,
                          AiGenerationHistoryService historyService) {
        this.config = config;
        this.contextService = contextService;
        this.promptBuilder = promptBuilder;
        this.usageRepository = usageRepository;
        this.aiProviderResolver = aiProviderResolver;
        this.costCalculator = costCalculator;
        this.creditService = creditService;
        this.aiCreditCalculator = aiCreditCalculator;
        this.quotaConfig = quotaConfig;
        this.rateLimitService = rateLimitService;
        this.developmentCreditService = developmentCreditService;
        this.tokenEstimator = tokenEstimator;
        this.historyService = historyService;
    }
    
    public PreparedAiGeneration prepareGeneration(String userId, NovelAiRequest request) {
        if (!config.isEnabled()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "AI is disabled");
        }

        if (request.action() == NovelAiAction.REWRITE_SELECTION ||
            request.action() == NovelAiAction.EXPAND_SELECTION ||
            request.action() == NovelAiAction.IMPROVE_DIALOGUE ||
            request.action() == NovelAiAction.ADD_TENSION ||
            request.action() == NovelAiAction.SIMPLIFY_SELECTION ||
            request.action() == NovelAiAction.SUMMARIZE_SELECTION) {
            if (request.selectedText() == null || request.selectedText().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecione um trecho antes de usar esta ação.");
            }
        }
        
        com.fasterxml.jackson.databind.node.ObjectNode diagnostics = objectMapper.createObjectNode();
        diagnostics.put("storyId", request.storyId());
        diagnostics.put("chapterId", request.chapterId());
        if (request.characterIds() != null) {
            diagnostics.set("characterIds", objectMapper.valueToTree(request.characterIds()));
        }
        if (request.worldNoteIds() != null) {
            diagnostics.set("worldNoteIds", objectMapper.valueToTree(request.worldNoteIds()));
        }
        
        NovelContextService.ContextResult contextResult = contextService.buildContext(userId, request, diagnostics);
        String context = contextResult.contextText;

        String systemPrompt = promptBuilder.buildSystemPrompt(request);
        String actionPrompt = promptBuilder.buildActionPrompt(request.action(), request, context);
        
        int estimatedContextTokens = diagnostics.path("estimatedInputTokens").asInt(0);
        int estimatedInputTokens = tokenEstimator.estimateTokens(
                systemPrompt + "\n" + actionPrompt
        );
        int maxOutputTokens = diagnostics.path("outputLimit").asInt(4096);
        if (request.action() == NovelAiAction.REWRITE_SELECTION) {
            maxOutputTokens = Math.min(
                    maxOutputTokens,
                    NovelPromptBuilder.rewriteLimits(request.selectedText()).maximumOutputTokens()
            );
            diagnostics.put("rewriteSourceWords", NovelPromptBuilder.countRewriteWords(request.selectedText()));
            diagnostics.put("outputLimit", maxOutputTokens);
        }
        
        AiCostCalculator.CalculationResult calcRes = costCalculator.calculateCost(null, config.getProvider(), config.getPrimaryModel(), estimatedInputTokens, 0L, maxOutputTokens);
        if (calcRes.costUsd() == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Preço do modelo não configurado.");
        }
        BigDecimal estimatedProviderCostUsd = calcRes.costUsd();
        
        BigDecimal estimatedCredits = calculateRequiredCredits(
            request.action(),
            estimatedInputTokens,
            maxOutputTokens
        );
        
        diagnostics.put("contextSizeChars", context != null ? context.length() : 0);
        diagnostics.put("estimatedContextTokens", estimatedContextTokens);
        diagnostics.put("estimatedInputTokens", estimatedInputTokens);
        String exactPrompt = systemPrompt + "\n" + actionPrompt;
        diagnostics.put("promptHash", actionPrompt != null ? actionPrompt.hashCode() : 0);
        diagnostics.put("promptSha256", sha256(exactPrompt));
        
        PreparedAiGeneration prepared = new PreparedAiGeneration();
        prepared.setAction(request.action().name());
        prepared.setProfile(com.novelcopilot.backend.dto.ContextProfile.valueOf(diagnostics.path("contextProfile").asText("SCENE_CONTINUITY")));
        prepared.setFinalContext(context);
        prepared.setSystemPrompt(systemPrompt);
        prepared.setActionPrompt(actionPrompt);
        prepared.setEstimatedInputTokens(estimatedInputTokens);
        prepared.setMaxOutputTokens(maxOutputTokens);
        prepared.setEstimatedProviderCostUsd(estimatedProviderCostUsd);
        prepared.setEstimatedCredits(estimatedCredits);
        prepared.setDiagnostics(objectMapper.convertValue(diagnostics, java.util.Map.class));
        prepared.setPromptHash(sha256(exactPrompt));
        
        return prepared;
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(
                    digest.digest(value.getBytes(StandardCharsets.UTF_8))
            );
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 indisponível", exception);
        }
    }
    
    public java.util.Map<String, Object> estimate(String userId, NovelAiRequest request) {
        PreparedAiGeneration prepared = prepareGeneration(userId, request);
        BigDecimal availableCredits =
                developmentCreditService.prepareAndGetAvailableCredits(userId);
        
        java.util.Map<String, Object> response = new java.util.HashMap<>();
        response.put("action", prepared.getAction());
        response.put("estimatedCredits", prepared.getEstimatedCredits());
        response.put("estimatedInputTokens", prepared.getEstimatedInputTokens());
        response.put("maxOutputTokens", prepared.getMaxOutputTokens());
        response.put(
                "contextTruncated",
                Boolean.TRUE.equals(prepared.getDiagnostics().get("contextTruncated"))
        );
        response.put("profile", prepared.getProfile());
        BigDecimal maximumCredits = quotaConfig.getMaximumCreditsPerGeneration();
        boolean exceedsPerRequestLimit =
                prepared.getEstimatedCredits().compareTo(maximumCredits) > 0;
        response.put("maximumCreditsPerGeneration", maximumCredits);
        response.put("exceedsPerRequestLimit", exceedsPerRequestLimit);
        response.put(
                "canGenerate",
                !exceedsPerRequestLimit
                        && availableCredits.compareTo(prepared.getEstimatedCredits()) >= 0
        );
        response.put("availableCredits", availableCredits);
        
        return response;
    }

    public NovelAiResponse generate(String userId, NovelAiRequest request) {
        if (request.idempotencyKey() != null) {
            Optional<AiGenerationUsage> existing = usageRepository.findByUserIdAndIdempotencyKey(userId, request.idempotencyKey());
            if (existing.isPresent()) {
                return replayOrReject(existing.get(), request);
            }
        }

        if (userLocks.putIfAbsent(userId, true) != null) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "You already have an active generation running.");
        }

        AiGenerationUsage usage = new AiGenerationUsage();
        try {
            rateLimitService.checkAiGeneration(
                    userId,
                    EXPENSIVE_ACTIONS.contains(request.action())
            );
            PreparedAiGeneration prepared = prepareGeneration(userId, request);
            enforcePerRequestCreditLimit(prepared.getEstimatedCredits());
            developmentCreditService.prepareAndGetAvailableCredits(userId);

            usage.setUserId(userId);
            usage.setStoryId(request.storyId());
            usage.setChapterId(request.chapterId());
            usage.setAction(request.action().name());
            usage.setProvider(config.getProvider());
            usage.setModel(config.getPrimaryModel());
            usage.setIdempotencyKey(request.idempotencyKey());
            usage.setStatus("pending");
            usage.setCreatedAt(LocalDateTime.now());
            usage.setEstimatedCredits(prepared.getEstimatedCredits());
            usage.setCreditUsageStatus(CreditUsageStatus.ESTIMATED);
            try {
                usage.setContextDiagnostics(objectMapper.writeValueAsString(prepared.getDiagnostics()));
            } catch (Exception e) {}
            try {
                usage = usageRepository.save(usage);
                usageRepository.flush();
            } catch (DataIntegrityViolationException duplicateKey) {
                Optional<AiGenerationUsage> existing =
                        usageRepository.findByUserIdAndIdempotencyKey(
                                userId,
                                request.idempotencyKey()
                        );
                if (existing.isPresent()) {
                    return replayOrReject(existing.get(), request);
                }
                throw duplicateKey;
            }

            // Reserve Credits
            try {
                creditService.reserveCredits(userId, prepared.getEstimatedCredits(), request.idempotencyKey(), usage.getId());
                usage.setReservedCredits(prepared.getEstimatedCredits());
                usage.setCreditUsageStatus(CreditUsageStatus.RESERVED);
                usageRepository.save(usage);
            } catch (AiInsufficientCreditsException e) {
                usage.setStatus("error");
                usage.setErrorMessage("AI_INSUFFICIENT_CREDITS");
                usage.setCreditUsageStatus(CreditUsageStatus.FAILED);
                usageRepository.save(usage);
                throw e;
            }

            long startTime = System.currentTimeMillis();
            AiProvider aiProvider = aiProviderResolver.resolve(config.getProvider());
            com.novelcopilot.backend.dto.AiGenerationResult providerResponse;
            try {
                providerResponse = aiProvider.generate(prepared.getSystemPrompt(), prepared.getActionPrompt(), config.getPrimaryModel(), prepared.getMaxOutputTokens());
            } catch (Exception e) {
                creditService.releaseCredits(userId, request.idempotencyKey(), usage.getId());
                usage.setReleasedCredits(prepared.getEstimatedCredits());
                usage.setCreditUsageStatus(CreditUsageStatus.RELEASED);
                throw new AiProviderUnavailableException(
                        aiProvider.providerName(),
                        e
                );
            }

            long duration = System.currentTimeMillis() - startTime;
            usage.setDurationMs(duration);

            usage.setProvider(providerResponse.provider());
            usage.setModel(providerResponse.model());

            usage.setInputTokens(providerResponse.inputTokens() != null ? providerResponse.inputTokens().intValue() : null);
            usage.setCachedInputTokens(providerResponse.cachedInputTokens() != null ? providerResponse.cachedInputTokens().intValue() : null);
            usage.setOutputTokens(providerResponse.outputTokens() != null ? providerResponse.outputTokens().intValue() : null);
            usage.setTotalTokens(providerResponse.totalTokens() != null ? providerResponse.totalTokens().intValue() : null);
            
            BigDecimal reportedCost = providerResponse.providerReportedCostUsd();
            if (reportedCost != null && reportedCost.compareTo(BigDecimal.ZERO) <= 0) {
                reportedCost = null;
            }
            AiCostCalculator.CalculationResult calcRes = costCalculator.calculateCost(null, providerResponse.provider(), providerResponse.model(),
                    usage.getInputTokens() != null ? usage.getInputTokens() : 0L, 
                    usage.getCachedInputTokens() != null ? usage.getCachedInputTokens() : 0L, 
                    usage.getOutputTokens() != null ? usage.getOutputTokens() : 0L);
            BigDecimal calcCost = calcRes.costUsd();
            
            if (reportedCost == null && calcCost == null) {
                creditService.releaseCredits(userId, request.idempotencyKey(), usage.getId());
                usage.setReleasedCredits(prepared.getEstimatedCredits());
                usage.setCreditUsageStatus(CreditUsageStatus.RELEASED);
                usage.setStatus("error");
                usage.setErrorMessage("Preço do modelo retornado não configurado.");
                usageRepository.save(usage);
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Preço do modelo retornado não configurado.");
            }

            usage.setProviderReportedCostUsd(reportedCost);
            usage.setCalculatedCostUsd(calcCost);
            
            BigDecimal actualCostUsd = reportedCost != null ? reportedCost : calcCost;
            usage.setEffectiveCostUsd(actualCostUsd);
            usage.setCostSource(reportedCost != null ? "PROVIDER" : "CALCULATED");

            BigDecimal actualCredits = calculateRequiredCredits(
                request.action(),
                usage.getInputTokens() != null ? usage.getInputTokens() : 0L,
                usage.getOutputTokens() != null ? usage.getOutputTokens() : 0L
            );

            JsonNode structuredResult = null;
            String textContent = providerResponse.content();
            
            try {
                switch (request.action()) {
                    case PLAN_SCENE, SUGGEST_CONTINUATION, SUMMARIZE_SELECTION, GENERATE_CHAPTER_TITLE, REVIEW_CHAPTER -> {
                        String jsonStr = textContent;
                        if (jsonStr.contains("```json")) {
                            jsonStr = jsonStr.substring(jsonStr.indexOf("```json") + 7, jsonStr.lastIndexOf("```"));
                        } else if (jsonStr.contains("```")) {
                            jsonStr = jsonStr.substring(jsonStr.indexOf("```") + 3, jsonStr.lastIndexOf("```"));
                        }
                        structuredResult = objectMapper.readTree(jsonStr.trim());
                        if (request.action() == NovelAiAction.SUGGEST_CONTINUATION) {
                            if (!structuredResult.has("options") || !structuredResult.get("options").isArray() || structuredResult.get("options").size() != 3) {
                                throw new IllegalStateException("O JSON de sugestão não contém exatamente 3 opções.");
                            }
                        }
                        if (request.action() == NovelAiAction.GENERATE_CHAPTER_TITLE) {
                            validateChapterTitleOptions(structuredResult);
                        }
                        if (request.action() == NovelAiAction.REVIEW_CHAPTER) {
                            validateChapterReview(structuredResult);
                        }
                        textContent = null; 
                    }
                    default -> {
                        if (textContent == null || textContent.trim().isEmpty()) {
                            throw new IllegalStateException("A IA retornou um texto vazio.");
                        }
                        if (textContent.contains("```json") || textContent.contains("```html")) {
                            throw new IllegalStateException("A IA retornou código ao invés de prosa.");
                        }
                        if (textContent.startsWith("Aqui está") || textContent.startsWith("Claro,")) {
                            throw new IllegalStateException("A IA retornou explicações indevidas.");
                        }
                        textContent = textContent.replaceAll("\\*\\*(.*?)\\*\\*", "$1");
                        textContent = textContent.replaceAll("\\*(.*?)\\*", "$1");
                        textContent = textContent.replaceAll("__(.*?)__", "$1");
                        textContent = textContent.replaceAll("_(.*?)_", "$1");
                        if (request.action() == NovelAiAction.REWRITE_SELECTION) {
                            NovelPromptBuilder.validateRewrite(request.selectedText(), textContent);
                        }
                    }
                }
            } catch (Exception e) {
                creditService.releaseCredits(userId, request.idempotencyKey(), usage.getId());
                usage.setReleasedCredits(prepared.getEstimatedCredits());
                usage.setCreditUsageStatus(CreditUsageStatus.RELEASED);
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "A IA não retornou um formato válido.");
            }

            creditService.captureCredits(userId, request.idempotencyKey(), actualCredits, usage.getId());
            usage.setChargedCredits(actualCredits.min(prepared.getEstimatedCredits()));
            usage.setCreditUsageStatus(CreditUsageStatus.CAPTURED);
            
            usage.setStatus("success");
            usage.setFallbackUsed(false);
            usage.setContextTruncated(false);
            
            usageRepository.save(usage);

            try {
                historyService.record(usage, request.action(), textContent, structuredResult);
            } catch (RuntimeException historyError) {
                logger.error("Falha ao registrar histórico da geração {}", usage.getId(), historyError);
            }

            return new NovelAiResponse(
                    usage.getId().toString(),
                    request.action(),
                    textContent,
                    structuredResult,
                    usage.getProvider(),
                    usage.getModel(),
                    usage.getFallbackUsed(),
                    usage.getInputTokens(),
                    usage.getCachedInputTokens(),
                    usage.getOutputTokens(),
                    usage.getTotalTokens(),
                    usage.getProviderReportedCostUsd(),
                    usage.getCalculatedCostUsd(),
                    usage.getEffectiveCostUsd(),
                    usage.getCostSource(),
                    usage.getDurationMs(),
                    usage.getContextTruncated(),
                    usage.getCreatedAt()
            );

        } catch (Exception e) {
            if (usage.getId() != null) {
                usage.setStatus("error");
                usage.setErrorMessage(e.getClass().getSimpleName());
                usageRepository.save(usage);
            }
            if (e instanceof ResponseStatusException) {
                throw (ResponseStatusException) e;
            }
            if (e instanceof AiRateLimitExceededException) {
                throw (AiRateLimitExceededException) e;
            }
            if (e instanceof AiInsufficientCreditsException) {
                throw (AiInsufficientCreditsException) e;
            }
            if (e instanceof AiProviderUnavailableException) {
                throw (AiProviderUnavailableException) e;
            }
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "AI generation failed: " + e.getMessage());
        } finally {
            userLocks.remove(userId);
        }
    }

    private void validateChapterTitleOptions(JsonNode structuredResult) {
        JsonNode options = structuredResult.path("options");
        if (!options.isArray() || options.size() != 3) {
            throw new IllegalStateException("O JSON de titulos nao contem exatamente 3 opcoes.");
        }

        java.util.Set<String> normalized = new java.util.HashSet<>();
        for (JsonNode option : options) {
            String title = option.isTextual() ? option.asText().trim() : "";
            int wordCount = title.isEmpty() ? 0 : title.split("\\s+").length;
            if (wordCount < 2 || wordCount > 8 || !normalized.add(title.toLowerCase(java.util.Locale.ROOT))) {
                throw new IllegalStateException("O JSON de titulos contem uma opcao invalida.");
            }
        }
    }

    private void validateChapterReview(JsonNode structuredResult) {
        java.util.Set<String> stages = java.util.Set.of("opening", "development", "climax", "resolution", "unknown");
        java.util.Set<String> categories = java.util.Set.of("coherence", "clarity", "pacing", "continuity", "character", "style");
        java.util.Set<String> severities = java.util.Set.of("critical", "important", "optional");
        if (!structuredResult.path("chapterStage").isTextual()
                || !stages.contains(structuredResult.path("chapterStage").asText())
                || structuredResult.path("summary").asText().isBlank()
                || !structuredResult.path("findings").isArray()
                || !structuredResult.path("strengths").isArray()
                || !structuredResult.path("limitations").isArray()) {
            throw new IllegalStateException("O JSON da revisão está incompleto.");
        }
        for (JsonNode finding : structuredResult.path("findings")) {
            if (!categories.contains(finding.path("category").asText())
                    || !severities.contains(finding.path("severity").asText())
                    || finding.path("issue").asText().isBlank()
                    || finding.path("evidence").asText().isBlank()
                    || finding.path("impact").asText().isBlank()
                    || finding.path("suggestion").asText().isBlank()) {
                throw new IllegalStateException("O JSON da revisão contém um apontamento inválido.");
            }
        }
    }

    private void enforcePerRequestCreditLimit(BigDecimal estimatedCredits) {
        BigDecimal maximumCredits = quotaConfig.getMaximumCreditsPerGeneration();
        if (estimatedCredits.compareTo(maximumCredits) > 0) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "A geração ultrapassa o limite seguro de "
                            + maximumCredits.stripTrailingZeros().toPlainString()
                            + " créditos por solicitação."
            );
        }
    }

    private BigDecimal calculateRequiredCredits(
            NovelAiAction action,
            long inputTokens,
            long outputTokens) {
        if (action == NovelAiAction.REWRITE_SELECTION
                || action == NovelAiAction.IMPROVE_DIALOGUE
                || action == NovelAiAction.ADD_TENSION) {
            return aiCreditCalculator.calculateRequiredCredits(
                    inputTokens,
                    outputTokens,
                    quotaConfig.getSelectionEditMinimumChargeCredits(),
                    quotaConfig.getSelectionEditIncrementCredits()
            );
        }
        return aiCreditCalculator.calculateRequiredCredits(
                inputTokens,
                outputTokens,
                quotaConfig.getMinimumChargeCredits()
        );
    }

    private NovelAiResponse replayOrReject(
            AiGenerationUsage usage,
            NovelAiRequest request
    ) {
        if ("success".equals(usage.getStatus())) {
            Optional<AiGenerationHistoryItem> history = historyService.findByUsageId(usage.getId());
            if (history.isPresent()) {
                AiGenerationHistoryItem stored = history.get();
                return new NovelAiResponse(
                        usage.getId().toString(),
                        request.action(),
                        stored.content(),
                        stored.structuredResult(),
                        usage.getProvider(),
                        usage.getModel(),
                        usage.getFallbackUsed(),
                        usage.getInputTokens(),
                        usage.getCachedInputTokens(),
                        usage.getOutputTokens(),
                        usage.getTotalTokens(),
                        usage.getProviderReportedCostUsd(),
                        usage.getCalculatedCostUsd(),
                        usage.getEffectiveCostUsd(),
                        usage.getCostSource(),
                        usage.getDurationMs(),
                        usage.getContextTruncated(),
                        usage.getCreatedAt()
                );
            }
            return new NovelAiResponse(
                    usage.getId().toString(),
                    request.action(),
                    "Cached generation (see history). Content is not re-returned.",
                    null,
                    usage.getProvider(),
                    usage.getModel(),
                    usage.getFallbackUsed(),
                    usage.getInputTokens(),
                    usage.getCachedInputTokens(),
                    usage.getOutputTokens(),
                    usage.getTotalTokens(),
                    usage.getProviderReportedCostUsd(),
                    usage.getCalculatedCostUsd(),
                    usage.getEffectiveCostUsd(),
                    usage.getCostSource(),
                    usage.getDurationMs(),
                    usage.getContextTruncated(),
                    usage.getCreatedAt()
            );
        }
        if ("pending".equals(usage.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A request with this idempotency key is already running."
            );
        }
        throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "This idempotency key has already been used."
        );
    }
}
