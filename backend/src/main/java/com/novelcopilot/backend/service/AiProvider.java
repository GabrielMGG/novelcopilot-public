package com.novelcopilot.backend.service;

import com.novelcopilot.backend.dto.AiGenerationResult;

public interface AiProvider {

    AiGenerationResult generate(String instructions, String prompt, String modelId, Integer maxTokens);

    default AiGenerationResult generate(String instructions, String prompt, String modelId) {
        return generate(instructions, prompt, modelId, null);
    }

    default AiGenerationResult generate(String instructions, String prompt) {
        return generate(instructions, prompt, null, null);
    }

    default AiGenerationResult generatePrecise(
            String instructions,
            String prompt,
            String modelId,
            Integer maxTokens
    ) {
        return generate(instructions, prompt, modelId, maxTokens);
    }

    default AiGenerationResult generateSummary(
            String instructions,
            String prompt,
            String modelId,
            Integer maxTokens
    ) {
        return generatePrecise(instructions, prompt, modelId, maxTokens);
    }

    String providerName();

    String modelName();
}
