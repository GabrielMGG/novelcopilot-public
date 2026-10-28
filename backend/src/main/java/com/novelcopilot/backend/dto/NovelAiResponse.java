package com.novelcopilot.backend.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record NovelAiResponse(
    String requestId,
    NovelAiAction action,
    String content,
    @JsonProperty("structuredResult") JsonNode structuredResult,
    String provider,
    String model,
    Boolean fallbackUsed,
    Integer inputTokens,
    Integer cachedInputTokens,
    Integer outputTokens,
    Integer totalTokens,
    BigDecimal providerReportedCostUsd,
    BigDecimal calculatedCostUsd,
    BigDecimal effectiveCostUsd,
    String costSource,
    Long durationMs,
    Boolean contextTruncated,
    LocalDateTime createdAt
) {}
