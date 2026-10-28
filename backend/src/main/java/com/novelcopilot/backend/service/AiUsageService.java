package com.novelcopilot.backend.service;

import com.novelcopilot.backend.dto.AiGenerationResult;
import com.novelcopilot.backend.entity.AiUsageRecord;
import com.novelcopilot.backend.entity.AiUsageStatus;
import com.novelcopilot.backend.repository.AiUsageRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AiUsageService {

    private static final Logger log = LoggerFactory.getLogger(AiUsageService.class);

    private final AiUsageRecordRepository aiUsageRecordRepository;
    private final AiCostCalculator aiCostCalculator;

    public AiUsageService(
            AiUsageRecordRepository aiUsageRecordRepository,
            AiCostCalculator aiCostCalculator
    ) {
        this.aiUsageRecordRepository = aiUsageRecordRepository;
        this.aiCostCalculator = aiCostCalculator;
    }

    public void recordSuccess(
            String userId,
            Integer storyId,
            Integer chapterId,
            String actionType,
            AiGenerationResult result
    ) {
        try {
            AiCostCalculator.CalculationResult calcResult = aiCostCalculator.calculateCost(
                    null,
                    result.provider(),
                    result.model(),
                    safeZero(result.inputTokens()),
                    safeZero(result.cachedInputTokens()),
                    safeZero(result.outputTokens())
            );
            BigDecimal cost = calcResult.costUsd();

            AiUsageRecord record = new AiUsageRecord();
            record.setUserId(userId);
            record.setStoryId(storyId);
            record.setChapterId(chapterId);
            record.setActionType(actionType);
            record.setProvider(result.provider());
            record.setModel(result.model());
            record.setInputTokens(safeZero(result.inputTokens()));
            record.setCachedInputTokens(safeZero(result.cachedInputTokens()));
            record.setOutputTokens(safeZero(result.outputTokens()));
            record.setTotalTokens(safeZero(result.totalTokens()));
            record.setEstimatedCostUsd(cost);
            record.setDurationMs(safeZero(result.durationMs()));
            record.setStatus(AiUsageStatus.SUCCESS);
            record.setCreatedAt(LocalDateTime.now());

            aiUsageRecordRepository.save(record);
        } catch (Exception e) {
            log.error("Falha ao registrar metrica de uso de IA (sucesso). A geracao nao sera afetada.", e);
        }
    }

    public void recordError(
            String userId,
            Integer storyId,
            Integer chapterId,
            String actionType,
            String provider,
            String model,
            long durationMs,
            String errorCode
    ) {
        try {
            AiUsageRecord record = new AiUsageRecord();
            record.setUserId(userId);
            record.setStoryId(storyId);
            record.setChapterId(chapterId);
            record.setActionType(actionType);
            record.setProvider(provider);
            record.setModel(model);
            record.setInputTokens(0L);
            record.setCachedInputTokens(0L);
            record.setOutputTokens(0L);
            record.setTotalTokens(0L);
            record.setEstimatedCostUsd(null);
            record.setDurationMs(durationMs);
            record.setStatus(AiUsageStatus.ERROR);
            record.setErrorCode(sanitizeErrorCode(errorCode));
            record.setCreatedAt(LocalDateTime.now());

            aiUsageRecordRepository.save(record);
        } catch (Exception e) {
            log.error("Falha ao registrar metrica de uso de IA (erro). A geracao nao sera afetada.", e);
        }
    }

    public List<AiUsageRecord> findByPeriod(LocalDateTime start, LocalDateTime end) {
        return aiUsageRecordRepository.findByCreatedAtBetween(start, end);
    }

    public List<AiUsageRecord> findByUserAndPeriod(String userId, LocalDateTime start, LocalDateTime end) {
        return aiUsageRecordRepository.findByUserIdAndCreatedAtBetween(userId, start, end);
    }

    private long safeZero(Long value) {
        return value != null ? value : 0L;
    }

    private String sanitizeErrorCode(String errorCode) {
        if (errorCode == null) {
            return null;
        }
        String sanitized = errorCode.replaceAll("[^a-zA-Z0-9_\\-.:/ ]", "");
        if (sanitized.length() > 200) {
            sanitized = sanitized.substring(0, 200);
        }
        return sanitized;
    }
}
