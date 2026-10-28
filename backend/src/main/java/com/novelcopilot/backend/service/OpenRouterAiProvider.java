package com.novelcopilot.backend.service;

import com.novelcopilot.backend.dto.AiGenerationResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import jakarta.annotation.PostConstruct;

@Component
public class OpenRouterAiProvider implements AiProvider {

    private final RestClient restClient;
    private final String apiKey;
    private final boolean enabled;

    public OpenRouterAiProvider(
            @Value("${ai.providers.openrouter.enabled:false}") boolean enabled,
            @Value("${ai.providers.openrouter.api-key:}") String apiKey,
            @Value("${ai.providers.openrouter.base-url:https://openrouter.ai/api/v1}") String baseUrl,
            @Value("${ai.providers.openrouter.http-referer:http://localhost:5173}") String httpReferer,
            @Value("${ai.providers.openrouter.app-title:NovelCopilot Benchmark}") String appTitle
    ) {
        this.enabled = enabled;
        this.apiKey = apiKey;
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("HTTP-Referer", httpReferer)
                .defaultHeader("X-Title", appTitle)
                .build();
    }

    @PostConstruct
    public void init() {
        if (enabled && (apiKey == null || apiKey.isBlank())) {
            throw new IllegalStateException("OPENROUTER_API_KEY não configurada.");
        }
    }

    @Override
    public AiGenerationResult generate(String instructions, String prompt, String modelId, Integer maxTokens) {
        return generateInternal(instructions, prompt, modelId, maxTokens, 0.7);
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
        if (!enabled) {
            throw new IllegalStateException("OpenRouter is not enabled.");
        }
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("A variavel OPENROUTER_API_KEY nao foi configurada.");
        }
        if (modelId == null || modelId.isBlank()) {
            throw new IllegalArgumentException("modelId is required for OpenRouter");
        }

        Map<String, Object> body = buildRequestBody(instructions, prompt, modelId, maxTokens, temperature);

        long startNanos = System.nanoTime();

        OpenRouterResponse response = restClient.post()
                .uri("/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                    int statusCode = res.getStatusCode().value();
                    String errorBody = new String(res.getBody().readAllBytes());
                    if (statusCode == 402) {
                        throw new RuntimeException("Cota do provedor OpenRouter esgotada. " + errorBody);
                    } else if (statusCode == 404) {
                        throw new RuntimeException("404 Not Found: Modelo ou rota indisponivel no OpenRouter. " + errorBody);
                    } else if (statusCode == 429) {
                        throw new RuntimeException("429 Too Many Requests: Rate limit excedido no OpenRouter. " + errorBody);
                    } else if (statusCode == 400 || statusCode == 401 || statusCode == 403) {
                        throw new RuntimeException(statusCode + " Error: Erro fatal no OpenRouter. " + errorBody);
                    }
                    throw new RuntimeException("Client Error " + statusCode + ": " + errorBody);
                })
                .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                    throw new RuntimeException("5xx Server Error from OpenRouter");
                })
                .body(OpenRouterResponse.class);

        long durationMs = (System.nanoTime() - startNanos) / 1_000_000;

        if (response == null || response.choices() == null || response.choices().isEmpty()) {
            throw new IllegalStateException("OpenRouter nao retornou uma resposta de texto.");
        }

        OpenRouterMessage message = response.choices().getFirst().message();

        if (message == null || message.content() == null || message.content().isBlank()) {
            throw new IllegalStateException("OpenRouter retornou uma resposta vazia.");
        }

        Long inputTokens = null;
        Long outputTokens = null;
        Long totalTokens = null;
        java.math.BigDecimal reportedCost = null;

        if (response.usage() != null) {
            inputTokens = response.usage().prompt_tokens();
            outputTokens = response.usage().completion_tokens();
            totalTokens = response.usage().total_tokens();
            if (response.usage().cost() != null) {
                reportedCost = new java.math.BigDecimal(response.usage().cost().toString());
            } else if (response.usage().total_cost() != null) {
                reportedCost = new java.math.BigDecimal(response.usage().total_cost().toString());
            }
        }

        return new AiGenerationResult(
                message.content(),
                providerName(),
                modelId,
                inputTokens,
                null,
                outputTokens,
                totalTokens,
                durationMs,
                reportedCost
        );
    }

    Map<String, Object> buildRequestBody(
            String instructions,
            String prompt,
            String modelId,
            Integer maxTokens
    ) {
        return buildRequestBody(instructions, prompt, modelId, maxTokens, 0.7);
    }

    Map<String, Object> buildRequestBody(
            String instructions,
            String prompt,
            String modelId,
            Integer maxTokens,
            double temperature
    ) {
        int outputLimit = maxTokens != null && maxTokens > 0 ? maxTokens : 4096;
        Map<String, Object> body = new java.util.HashMap<>(Map.of(
                "model", modelId,
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
                "temperature", temperature,
                "max_tokens", outputLimit,
                "provider", Map.of("data_collection", "deny")
        ));

        if ("deepseek/deepseek-v4-flash:free".equalsIgnoreCase(modelId)) {
            body.put("reasoning", Map.of("effort", "none"));
        }
        return body;
    }

    @Override
    public String providerName() {
        return "OpenRouter";
    }

    @Override
    public String modelName() {
        return "dynamic";
    }

    private record OpenRouterResponse(
            List<OpenRouterChoice> choices,
            OpenRouterUsage usage
    ) {}

    private record OpenRouterChoice(OpenRouterMessage message) {}

    private record OpenRouterMessage(String content) {}

    private record OpenRouterUsage(
            Long prompt_tokens,
            Long completion_tokens,
            Long total_tokens,
            Double cost,
            Double total_cost,
            Map<String, Object> cost_details
    ) {}
}
