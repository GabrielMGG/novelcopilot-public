package com.novelcopilot.backend.service;

import com.novelcopilot.backend.dto.AiGenerationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class CloudflareAiProvider implements AiProvider {

    private static final Logger log = LoggerFactory.getLogger(CloudflareAiProvider.class);
    private static final String GEMMA_MODEL = "@cf/google/gemma-4-26b-a4b-it";
    private static final String GLM_MODEL = "@cf/zai-org/glm-4.7-flash";
    private static final String QWEN_MODEL = "@cf/qwen/qwen3-30b-a3b-fp8";

    private final RestClient restClient;
    private final String accountId;
    private final String authToken;
    private final String summaryAccountId;
    private final String summaryAuthToken;
    private final String model;

    @Autowired
    public CloudflareAiProvider(
            RestClient.Builder restClientBuilder,
            @Value("${ai.providers.cloudflare.account-id:}") String accountId,
            @Value("${ai.providers.cloudflare.auth-token:}") String authToken,
            @Value("${ai.providers.cloudflare.model:@cf/google/gemma-4-26b-a4b-it}") String model,
            @Value("${ai.providers.cloudflare.summary-account-id:}") String summaryAccountId,
            @Value("${ai.providers.cloudflare.summary-auth-token:}") String summaryAuthToken,
            @Value("${ai.providers.cloudflare.base-url:https://api.cloudflare.com/client/v4/accounts}") String baseUrl
    ) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(10));
        requestFactory.setReadTimeout(Duration.ofSeconds(60));

        this.restClient = restClientBuilder.clone()
                .requestFactory(requestFactory)
                .baseUrl(baseUrl)
                .build();
        this.accountId = accountId;
        this.authToken = authToken;
        this.summaryAccountId = summaryAccountId;
        this.summaryAuthToken = summaryAuthToken;
        this.model = model;
    }

    CloudflareAiProvider(RestClient restClient, String accountId, String authToken, String model) {
        this(restClient, accountId, authToken, model, "", "");
    }

    CloudflareAiProvider(
            RestClient restClient,
            String accountId,
            String authToken,
            String model,
            String summaryAccountId,
            String summaryAuthToken
    ) {
        this.restClient = restClient;
        this.accountId = accountId;
        this.authToken = authToken;
        this.summaryAccountId = summaryAccountId;
        this.summaryAuthToken = summaryAuthToken;
        this.model = model;
    }

    @Override
    public AiGenerationResult generate(String instructions, String prompt, String modelId, Integer maxTokens) {
        return generateInternal(
                instructions, prompt, modelId, maxTokens, 0.7,
                accountId, authToken,
                "CLOUDFLARE_ACCOUNT_ID", "CLOUDFLARE_AUTH_TOKEN"
        );
    }

    @Override
    public AiGenerationResult generatePrecise(
            String instructions,
            String prompt,
            String modelId,
            Integer maxTokens
    ) {
        return generateInternal(
                instructions, prompt, modelId, maxTokens, 0.1,
                accountId, authToken,
                "CLOUDFLARE_ACCOUNT_ID", "CLOUDFLARE_AUTH_TOKEN"
        );
    }

    @Override
    public AiGenerationResult generateSummary(
            String instructions,
            String prompt,
            String modelId,
            Integer maxTokens
    ) {
        if (modelId == null || modelId.isBlank()) {
            throw new IllegalStateException("A variável AI_SUMMARY_MODEL não foi configurada.");
        }
        return generateInternal(
                instructions, prompt, modelId, maxTokens, 0.1,
                summaryAccountId, summaryAuthToken,
                "CLOUDFLARE_SUMMARY_ACCOUNT_ID", "CLOUDFLARE_SUMMARY_AUTH_TOKEN"
        );
    }

    private AiGenerationResult generateInternal(
            String instructions,
            String prompt,
            String modelId,
            Integer maxTokens,
            double temperature,
            String targetAccountId,
            String targetAuthToken,
            String accountVariable,
            String tokenVariable
    ) {
        String targetModel = modelId != null && !modelId.isBlank() ? modelId : model;
        validateConfiguration(targetAccountId, targetAuthToken, targetModel, accountVariable, tokenVariable);
        int outputLimit = maxTokens != null && maxTokens > 0 ? maxTokens : 4096;
        Map<String, Object> body = buildRequestBody(
                instructions,
                prompt,
                targetModel,
                outputLimit,
                temperature
        );
        long startNanos = System.nanoTime();

        CloudflareResponse response;
        try {
            response = restClient.post()
                    .uri("/{accountId}/ai/v1/chat/completions", targetAccountId)
                    .header("Authorization", "Bearer " + targetAuthToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, providerResponse) -> {
                        int statusCode = providerResponse.getStatusCode().value();
                        throw new IllegalStateException(sanitizedError(statusCode));
                    })
                    .body(CloudflareResponse.class);
        } catch (RestClientException exception) {
            throw new IllegalStateException("A Cloudflare não conseguiu concluir a geração.");
        }

        long durationMs = (System.nanoTime() - startNanos) / 1_000_000;

        if (response == null || response.choices() == null || response.choices().isEmpty()) {
            throw new IllegalStateException("A Cloudflare não retornou uma resposta de texto.");
        }

        Long inputTokens = null;
        Long cachedInputTokens = null;
        Long outputTokens = null;
        Long totalTokens = null;

        if (response.usage() != null) {
            inputTokens = response.usage().prompt_tokens();
            outputTokens = response.usage().completion_tokens();
            totalTokens = response.usage().total_tokens();
            if (response.usage().prompt_tokens_details() != null) {
                cachedInputTokens = response.usage().prompt_tokens_details().cached_tokens();
            }
        }

        CloudflareChoice choice = response.choices().getFirst();
        CloudflareMessage message = choice.message();
        if (message == null || message.content() == null || message.content().isBlank()) {
            int reasoningChars = message == null ? 0 : message.reasoningLength();
            String finishReason = sanitizedFinishReason(choice.finish_reason());
            log.warn(
                    "CLOUDFLARE_EMPTY_RESPONSE finishReason={} promptTokens={} completionTokens={} totalTokens={} reasoningChars={}",
                    finishReason,
                    inputTokens,
                    outputTokens,
                    totalTokens,
                    reasoningChars
            );

            if ("length".equals(finishReason) || reasoningChars > 0) {
                throw new IllegalStateException(
                        "A Cloudflare consumiu o limite antes de produzir o texto final."
                );
            }
            throw new IllegalStateException("A Cloudflare retornou uma resposta vazia.");
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
            int outputLimit
    ) {
        return buildRequestBody(instructions, prompt, targetModel, outputLimit, 0.7);
    }

    Map<String, Object> buildRequestBody(
            String instructions,
            String prompt,
            String targetModel,
            int outputLimit,
            double temperature
    ) {
        String userPrompt = QWEN_MODEL.equals(targetModel)
                ? prompt + "\n\n/no_think"
                : prompt;
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", targetModel);
        body.put(
                "messages",
                List.of(
                    Map.of("role", "system", "content", instructions),
                    Map.of("role", "user", "content", userPrompt)
                )
        );
        body.put("temperature", temperature);
        body.put("max_completion_tokens", outputLimit);
        if (GEMMA_MODEL.equals(targetModel)
                || GLM_MODEL.equals(targetModel)
                || QWEN_MODEL.equals(targetModel)) {
            body.put("chat_template_kwargs", Map.of("enable_thinking", false));
        }
        body.put("store", false);
        return body;
    }

    private void validateConfiguration(
            String targetAccountId,
            String targetAuthToken,
            String targetModel,
            String accountVariable,
            String tokenVariable
    ) {
        if (targetAccountId == null || targetAccountId.isBlank()) {
            throw new IllegalStateException("A variável " + accountVariable + " não foi configurada.");
        }
        if (targetAuthToken == null || targetAuthToken.isBlank()) {
            throw new IllegalStateException("A variável " + tokenVariable + " não foi configurada.");
        }
        if (targetModel == null || targetModel.isBlank()) {
            throw new IllegalStateException("O modelo da Cloudflare não foi configurado.");
        }
    }

    private String sanitizedError(int statusCode) {
        return switch (statusCode) {
            case 401, 403 -> "A Cloudflare recusou as credenciais configuradas.";
            case 429 -> "O limite de uso da Cloudflare foi atingido.";
            default -> "A Cloudflare não conseguiu concluir a geração.";
        };
    }

    String sanitizedFinishReason(String finishReason) {
        if (finishReason == null || finishReason.isBlank()) {
            return "missing";
        }
        return switch (finishReason) {
            case "stop", "length", "content_filter", "tool_calls" -> finishReason;
            default -> "other";
        };
    }

    @Override
    public String providerName() {
        return "Cloudflare";
    }

    @Override
    public String modelName() {
        return model;
    }

    private record CloudflareResponse(
            List<CloudflareChoice> choices,
            CloudflareUsage usage
    ) {
    }

    private record CloudflareChoice(
            CloudflareMessage message,
            String finish_reason
    ) {
    }

    private record CloudflareMessage(
            String content,
            String reasoning,
            String reasoning_content
    ) {
        int reasoningLength() {
            int reasoningSize = reasoning == null ? 0 : reasoning.length();
            int reasoningContentSize = reasoning_content == null ? 0 : reasoning_content.length();
            return Math.max(reasoningSize, reasoningContentSize);
        }
    }

    private record CloudflareUsage(
            Long prompt_tokens,
            Long completion_tokens,
            Long total_tokens,
            CloudflarePromptTokensDetails prompt_tokens_details
    ) {
    }

    private record CloudflarePromptTokensDetails(Long cached_tokens) {
    }
}
