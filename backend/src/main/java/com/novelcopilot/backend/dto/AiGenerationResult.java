package com.novelcopilot.backend.dto;

public record AiGenerationResult(
        String content,
        String provider,
        String model,
        Long inputTokens,
        Long cachedInputTokens,
        Long outputTokens,
        Long totalTokens,
        Long durationMs,
        java.math.BigDecimal providerReportedCostUsd
) {
    public AiGenerationResult(
            String content,
            String provider,
            String model,
            Long inputTokens,
            Long cachedInputTokens,
            Long outputTokens,
            Long totalTokens,
            Long durationMs
    ) {
        this(content, provider, model, inputTokens, cachedInputTokens, outputTokens, totalTokens, durationMs, null);
    }
}
