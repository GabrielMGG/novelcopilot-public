package com.novelcopilot.backend.benchmark.service;

import com.novelcopilot.backend.benchmark.dto.AiBenchmarkCase;
import com.novelcopilot.backend.benchmark.config.AiBenchmarkParticipantProperties;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class AiBenchmarkEstimateService {

    private final AiBenchmarkParticipantProperties participantProperties;

    public AiBenchmarkEstimateService(AiBenchmarkParticipantProperties participantProperties) {
        this.participantProperties = participantProperties;
    }

    public AiBenchmarkEstimate calculateEstimate(List<AiBenchmarkCase> cases, List<String> participantIds) {
        return calculateEstimate(cases, participantIds, 0);
    }

    public AiBenchmarkEstimate calculateEstimate(
            List<AiBenchmarkCase> cases,
            List<String> participantIds,
            int maxRetries
    ) {
        if (participantIds == null || participantIds.isEmpty()) {
            return new AiBenchmarkEstimate(
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO
            );
        }
        
        int caseCount = cases.size();
        int providerCount = participantIds.size();
        int totalCalls = caseCount * providerCount;

        long totalCharacters = 0;
        long totalMaxExpectedOutput = 0;

        for (AiBenchmarkCase c : cases) {
            String context = c.getStoryContext() != null ? c.getStoryContext() : "";
            String content = c.getChapterContent() != null ? c.getChapterContent() : "";
            String excerpt = c.getSelectedExcerpt() != null ? c.getSelectedExcerpt() : "";
            String instruction = c.getUserInstruction() != null ? c.getUserInstruction() : "";
            
            totalCharacters += context.length() + content.length() + excerpt.length() + instruction.length();
            totalMaxExpectedOutput += (c.getMaxExpectedLength() != null ? c.getMaxExpectedLength() : 500);
        }

        // estimatedInputTokens = ceil(totalCharacters / 3.0)
        long baseInputTokens = (long) Math.ceil(totalCharacters / 3.0);
        long estimatedInputTokensPerProvider = (long) (baseInputTokens * 1.25); // 25% safety margin
        long estimatedOutputTokensPerProvider = (long) (totalMaxExpectedOutput * 1.25);

        BigDecimal maxEstimatedCost = BigDecimal.ZERO;
        
        for (String participantId : participantIds) {
            AiBenchmarkParticipantProperties.Participant p = participantProperties.getParticipants().get(participantId);
            if (p != null) {
                BigDecimal pCost = p.getInputPricePerMillion()
                    .multiply(BigDecimal.valueOf(estimatedInputTokensPerProvider))
                    .divide(new BigDecimal("1000000"), 8, RoundingMode.HALF_UP)
                    .add(
                        p.getOutputPricePerMillion()
                        .multiply(BigDecimal.valueOf(estimatedOutputTokensPerProvider))
                        .divide(new BigDecimal("1000000"), 8, RoundingMode.HALF_UP)
                    );
                maxEstimatedCost = maxEstimatedCost.add(pCost);
            }
        }
        
        BigDecimal baseEstimatedCost =
                maxEstimatedCost.setScale(4, RoundingMode.HALF_UP);
        int attemptMultiplier = Math.max(0, maxRetries) + 1;

        return new AiBenchmarkEstimate(
            caseCount, 
            providerCount, 
            totalCalls,
            totalCalls * attemptMultiplier,
            totalCharacters, 
            estimatedInputTokensPerProvider * providerCount, 
            estimatedOutputTokensPerProvider * providerCount,
            baseEstimatedCost,
            maxEstimatedCost
                    .multiply(BigDecimal.valueOf(attemptMultiplier))
                    .setScale(4, RoundingMode.HALF_UP)
        );
    }
    
    public record AiBenchmarkEstimate(
        int caseCount,
        int providerCount,
        int totalCalls,
        int maxProviderCalls,
        long totalCharacters,
        long estimatedInputTokens,
        long estimatedOutputTokens,
        BigDecimal baseEstimatedCost,
        BigDecimal maxEstimatedCost
    ) {}
}
