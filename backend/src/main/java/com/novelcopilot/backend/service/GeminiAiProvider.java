package com.novelcopilot.backend.service;

import com.novelcopilot.backend.dto.AiGenerationResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Component
public class GeminiAiProvider implements AiProvider {

    private final RestClient restClient;
    private final String apiKey;
    private final String model;

    public GeminiAiProvider(
            @Value("${ai.gemini.api-key:}") String apiKey,
            @Value("${ai.gemini.model:gemini-3.5-flash}") String model
    ) {
        this.restClient = RestClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com/v1beta")
                .build();

        this.apiKey = apiKey;
        this.model = model;
    }

    @Override
    public AiGenerationResult generate(String instructions, String prompt, String modelId, Integer maxTokens) {
        return generateInternal(instructions, prompt, modelId, maxTokens, 0.85);
    }

    @Override
    public AiGenerationResult generatePrecise(
            String instructions,
            String prompt,
            String modelId,
            Integer maxTokens
    ) {
        return generateInternal(instructions, prompt, modelId, maxTokens, 0.1);
    }

    private AiGenerationResult generateInternal(
            String instructions,
            String prompt,
            String modelId,
            Integer maxTokens,
            double temperature
    ) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(("A variavel GEMINI_API_KEY nao foi configurada."));
        }

        String targetModel = (modelId != null && !modelId.isBlank()) ? modelId : model;

        int outputLimit = maxTokens != null && maxTokens > 0 ? maxTokens : 4096;
        Map<String, Object> body = Map.of(
                "system_instruction", Map.of(
                        "parts", List.of(Map.of("text", instructions))
                ),
                "contents", List.of(Map.of(
                        "role", "user",
                        "parts", List.of(Map.of("text", prompt))
                )),
                "generationConfig", Map.of(
                        "temperature", temperature,
                        "maxOutputTokens", outputLimit
                )
        );

        long startNanos = System.nanoTime();

        GeminiResponse response = restClient.post()
                .uri("/models/{model}:generateContent", targetModel)
                .header("x-goog-api-key", apiKey)
                .body(body)
                .retrieve()
                .body(GeminiResponse.class);

        long durationMs = (System.nanoTime() - startNanos) / 1_000_000;

        if (response == null || response.candidates() == null || response.candidates().isEmpty()) {
            throw new IllegalStateException("O Gemini nao retornou uma resposta de texto.");
        }

        GeminiContent content = response.candidates().getFirst().content();

        if (content == null || content.parts() == null || content.parts().isEmpty()) {
            throw new IllegalStateException("O Gemini retornou uma resposta vazia.");
        }

        String text = content.parts().stream()
                .map(GeminiPart::text)
                .filter(t -> t != null && !t.isBlank())
                .reduce("", String::concat);

        Long inputTokens = null;
        Long outputTokens = null;
        Long totalTokens = null;
        Long cachedInputTokens = null;

        if (response.usageMetadata() != null) {
            inputTokens = response.usageMetadata().promptTokenCount();
            outputTokens = response.usageMetadata().candidatesTokenCount();
            totalTokens = response.usageMetadata().totalTokenCount();
            cachedInputTokens = response.usageMetadata().cachedContentTokenCount();
        }

        return new AiGenerationResult(
                text,
                providerName(),
                targetModel,
                inputTokens,
                cachedInputTokens,
                outputTokens,
                totalTokens,
                durationMs
        );
    }

    @Override
    public String providerName() {
        return "Gemini";
    }

    @Override
    public String modelName() {
        return model;
    }

    private record GeminiResponse(
            List<GeminiCandidate> candidates,
            GeminiUsageMetadata usageMetadata
    ) {
    }

    private record GeminiCandidate(GeminiContent content) {
    }

    private record GeminiContent(List<GeminiPart> parts) {
    }

    private record GeminiPart(String text) {
    }

    private record GeminiUsageMetadata(
            Long promptTokenCount,
            Long cachedContentTokenCount,
            Long candidatesTokenCount,
            Long totalTokenCount
    ) {
    }
}
