package com.novelcopilot.backend.service;

import com.novelcopilot.backend.dto.AiGenerationResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Component
@Primary
public class GroqAiProvider implements AiProvider {

    private final RestClient restClient;
    private final String apiKey;
    private final String model;

    public GroqAiProvider(
            @Value("${ai.groq.api-key:}") String apiKey,
            @Value("${ai.groq.model:openai/gpt-oss-120b}") String model
    ) {
        this.restClient = RestClient.builder()
                .baseUrl("https://api.groq.com/openai/v1")
                .build();

        this.apiKey = apiKey;
        this.model = model;
    }

    @Override
    public AiGenerationResult generate(String instructions, String prompt, String modelId, Integer maxTokens) {
        return generateInternal(instructions, prompt, modelId, maxTokens, false);
    }

    @Override
    public AiGenerationResult generatePrecise(
            String instructions,
            String prompt,
            String modelId,
            Integer maxTokens
    ) {
        return generateInternal(instructions, prompt, modelId, maxTokens, true);
    }

    private AiGenerationResult generateInternal(
            String instructions,
            String prompt,
            String modelId,
            Integer maxTokens,
            boolean precise
    ) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("A variavel GROQ_API_KEY nao foi configurada.");
        }

        String targetModel = (modelId != null && !modelId.isBlank()) ? modelId : model;
        int limit = maxTokens != null ? maxTokens : 4096;

        Map<String, Object> body = buildRequestBody(instructions, prompt, targetModel, limit, precise);
        long startNanos = System.nanoTime();

        GroqResponse response = restClient.post()
                .uri("/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .body(body)
                .retrieve()
                .body(GroqResponse.class);

        long durationMs = (System.nanoTime() - startNanos) / 1_000_000;

        if (response == null || response.choices() == null || response.choices().isEmpty()) {
            throw new IllegalStateException("A Groq nao retornou uma resposta de texto.");
        }

        GroqMessage message = response.choices().getFirst().message();

        if (message == null || message.content() == null || message.content().isBlank()) {
            throw new IllegalStateException("A Groq retornou uma resposta vazia.");
        }

        Long inputTokens = null;
        Long outputTokens = null;
        Long totalTokens = null;
        Long cachedInputTokens = null;

        if (response.usage() != null) {
            inputTokens = response.usage().prompt_tokens();
            outputTokens = response.usage().completion_tokens();
            totalTokens = response.usage().total_tokens();

            if (response.usage().prompt_tokens_details() != null) {
                cachedInputTokens = response.usage().prompt_tokens_details().cached_tokens();
            }
        }

        return new AiGenerationResult(
                message.content(),
                providerName(),
                targetModel,
                inputTokens,
                cachedInputTokens,
                outputTokens,
                totalTokens,
                durationMs
        );
    }

    Map<String, Object> buildRequestBody(
            String instructions,
            String prompt,
            String targetModel,
            int limit
    ) {
        return buildRequestBody(instructions, prompt, targetModel, limit, false);
    }

    Map<String, Object> buildRequestBody(
            String instructions,
            String prompt,
            String targetModel,
            int limit,
            boolean precise
    ) {
        Map<String, Object> body = new java.util.HashMap<>(Map.of(
                "model", targetModel,
                "messages", List.of(
                        Map.of(
                                "role", "system",
                                "content", instructions
                        ),
                        Map.of(
                                "role", "user",
                                "content", prompt
                        )
                ),
                "max_tokens", limit
        ));

        if (targetModel.equalsIgnoreCase("qwen/qwen3.6-27b")) {
            body.put("reasoning_effort", "none");
            body.put("temperature", precise ? 0.1 : 0.7);
            body.put("top_p", 0.8);
            body.put("presence_penalty", precise ? 0.0 : 1.5);
        } else if (targetModel.contains("gpt-oss")) {
            body.put("reasoning_effort", "low");
        } else {
            body.put("temperature", precise ? 0.1 : 0.85);
            body.put("top_p", 1.0);
        }
        return body;
    }

    @Override
    public String providerName() {
        return "Groq";
    }

    @Override
    public String modelName() {
        return model;
    }

    private record GroqResponse(
            List<GroqChoice> choices,
            GroqUsage usage
    ) {
    }

    private record GroqChoice(GroqMessage message) {
    }

    private record GroqMessage(String content) {
    }

    private record GroqUsage(
            Long prompt_tokens,
            Long completion_tokens,
            Long total_tokens,
            GroqPromptTokensDetails prompt_tokens_details
    ) {
    }

    private record GroqPromptTokensDetails(Long cached_tokens) {
    }
}
