package com.novelcopilot.backend.dto;

public record GenerateAiResponse(
        Integer generationId,
        String provider,
        String model,
        String output,
        String status
) {
}