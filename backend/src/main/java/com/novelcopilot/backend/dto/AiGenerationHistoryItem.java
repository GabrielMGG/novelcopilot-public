package com.novelcopilot.backend.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public record AiGenerationHistoryItem(
        Long id,
        Long usageId,
        String action,
        String outputType,
        String content,
        @JsonProperty("structuredResult") JsonNode structuredResult,
        String provider,
        String model,
        LocalDateTime createdAt
) {}
