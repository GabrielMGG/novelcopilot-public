package com.novelcopilot.backend.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.novelcopilot.backend.dto.AiGenerationResult;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class CloudflareAiProviderTest {

    private static final String ACCOUNT_ID = "account-for-test";
    private static final String AUTH_TOKEN = "secret-token-for-test";
    private static final String SUMMARY_ACCOUNT_ID = "summary-account-for-test";
    private static final String SUMMARY_AUTH_TOKEN = "summary-secret-token-for-test";
    private static final String SUMMARY_MODEL = "@cf/meta/llama-3.2-3b-instruct";
    private static final String MODEL = "@cf/google/gemma-4-26b-a4b-it";
    private static final String LLAMA_MODEL = "@cf/meta/llama-3.3-70b-instruct-fp8-fast";
    private static final String GLM_MODEL = "@cf/zai-org/glm-4.7-flash";
    private static final String QWEN_MODEL = "@cf/qwen/qwen3-30b-a3b-fp8";
    private static final String GRANITE_MODEL = "@cf/ibm-granite/granite-4.0-h-micro";
    private static final String LLAMA4_SCOUT_MODEL = "@cf/meta/llama-4-scout-17b-16e-instruct";
    private static final String GENERATION_URL =
            "https://api.cloudflare.com/client/v4/accounts/" + ACCOUNT_ID + "/ai/v1/chat/completions";

    @Test
    void sendsProtectedOpenAiCompatibleRequestAndMapsUsage() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        CloudflareAiProvider provider = provider(builder);

        server.expect(once(), requestTo(GENERATION_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + AUTH_TOKEN))
                .andExpect(header(HttpHeaders.CONTENT_TYPE, "application/json"))
                .andExpect(jsonPath("$.model").value(MODEL))
                .andExpect(jsonPath("$.messages[0].role").value("system"))
                .andExpect(jsonPath("$.messages[0].content").value("Write literary prose."))
                .andExpect(jsonPath("$.messages[1].role").value("user"))
                .andExpect(jsonPath("$.messages[1].content").value("Continue the chapter."))
                .andExpect(jsonPath("$.temperature").value(0.7))
                .andExpect(jsonPath("$.max_completion_tokens").value(1000))
                .andExpect(jsonPath("$.chat_template_kwargs.enable_thinking").value(false))
                .andExpect(jsonPath("$.store").value(false))
                .andRespond(withSuccess(
                        """
                        {
                          "choices": [
                            {"message": {"content": "The generated continuation."}}
                          ],
                          "usage": {
                            "prompt_tokens": 3516,
                            "completion_tokens": 900,
                            "total_tokens": 4416,
                            "prompt_tokens_details": {"cached_tokens": 120}
                          }
                        }
                        """,
                        org.springframework.http.MediaType.APPLICATION_JSON
                ));

        AiGenerationResult result = provider.generate(
                "Write literary prose.",
                "Continue the chapter.",
                MODEL,
                1000
        );

        assertEquals("The generated continuation.", result.content());
        assertEquals("Cloudflare", result.provider());
        assertEquals(MODEL, result.model());
        assertEquals(3516L, result.inputTokens());
        assertEquals(120L, result.cachedInputTokens());
        assertEquals(900L, result.outputTokens());
        assertEquals(4416L, result.totalTokens());
        server.verify();
    }

    @Test
    void buildsLiteraryProfileWithoutSendingUnexpectedFields() {
        CloudflareAiProvider provider = new CloudflareAiProvider(
                RestClient.builder().build(),
                ACCOUNT_ID,
                AUTH_TOKEN,
                MODEL
        );

        Map<String, Object> body = provider.buildRequestBody(
                "System instructions",
                "User prompt",
                MODEL,
                1200
        );

        assertEquals(MODEL, body.get("model"));
        assertEquals(0.7, body.get("temperature"));
        assertEquals(1200, body.get("max_completion_tokens"));
        assertEquals(false, body.get("store"));
        assertFalse(body.containsKey("reasoning_effort"));
        assertEquals(
                Map.of("enable_thinking", false),
                body.get("chat_template_kwargs")
        );
        assertEquals(2, ((List<?>) body.get("messages")).size());
    }

    @Test
    void preciseGenerationUsesLowTemperatureForFactualMemory() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        CloudflareAiProvider provider = provider(builder);

        server.expect(once(), requestTo(GENERATION_URL))
                .andExpect(jsonPath("$.temperature").value(0.1))
                .andExpect(jsonPath("$.max_completion_tokens").value(1400))
                .andRespond(withSuccess(
                        "{\"choices\":[{\"message\":{\"content\":\"Memória factual.\"}}]}",
                        org.springframework.http.MediaType.APPLICATION_JSON
                ));

        AiGenerationResult result = provider.generatePrecise(
                "Extraia fatos.", "Capítulo.", MODEL, 1400
        );

        assertEquals("Memória factual.", result.content());
        server.verify();
    }

    @Test
    void summaryGenerationUsesOnlyDedicatedAccountTokenAndModel() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        CloudflareAiProvider provider = new CloudflareAiProvider(
                builder.baseUrl("https://api.cloudflare.com/client/v4/accounts").build(),
                ACCOUNT_ID,
                AUTH_TOKEN,
                MODEL,
                SUMMARY_ACCOUNT_ID,
                SUMMARY_AUTH_TOKEN
        );

        server.expect(once(), requestTo(
                        "https://api.cloudflare.com/client/v4/accounts/"
                                + SUMMARY_ACCOUNT_ID + "/ai/v1/chat/completions"
                ))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + SUMMARY_AUTH_TOKEN))
                .andExpect(jsonPath("$.model").value(SUMMARY_MODEL))
                .andExpect(jsonPath("$.temperature").value(0.1))
                .andExpect(jsonPath("$.max_completion_tokens").value(1400))
                .andExpect(jsonPath("$.store").value(false))
                .andRespond(withSuccess(
                        "{\"choices\":[{\"message\":{\"content\":\"Memória separada.\"}}]}",
                        org.springframework.http.MediaType.APPLICATION_JSON
                ));

        AiGenerationResult result = provider.generateSummary(
                "Extraia fatos.", "Capítulo.", SUMMARY_MODEL, 1400
        );

        assertEquals("Memória separada.", result.content());
        assertEquals(SUMMARY_MODEL, result.model());
        server.verify();
    }

    @Test
    void summaryGenerationNeverFallsBackToLiteraryCredentials() {
        CloudflareAiProvider provider = new CloudflareAiProvider(
                RestClient.builder().build(),
                ACCOUNT_ID,
                AUTH_TOKEN,
                MODEL,
                "",
                ""
        );

        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> provider.generateSummary(
                        "Extraia fatos.", "Capítulo.", SUMMARY_MODEL, 1400
                )
        );

        assertEquals(
                "A variável CLOUDFLARE_SUMMARY_ACCOUNT_ID não foi configurada.",
                error.getMessage()
        );
    }

    @Test
    void summaryGenerationRequiresDedicatedTokenAndExplicitModel() {
        CloudflareAiProvider provider = new CloudflareAiProvider(
                RestClient.builder().build(),
                ACCOUNT_ID,
                AUTH_TOKEN,
                MODEL,
                SUMMARY_ACCOUNT_ID,
                ""
        );

        IllegalStateException missingToken = assertThrows(
                IllegalStateException.class,
                () -> provider.generateSummary(
                        "Extraia fatos.", "Capítulo.", SUMMARY_MODEL, 1400
                )
        );
        IllegalStateException missingModel = assertThrows(
                IllegalStateException.class,
                () -> provider.generateSummary(
                        "Extraia fatos.", "Capítulo.", "", 1400
                )
        );

        assertEquals(
                "A variável CLOUDFLARE_SUMMARY_AUTH_TOKEN não foi configurada.",
                missingToken.getMessage()
        );
        assertEquals(
                "A variável AI_SUMMARY_MODEL não foi configurada.",
                missingModel.getMessage()
        );
    }

    @Test
    void buildsLlamaProfileWithoutGemmaThinkingParameter() {
        CloudflareAiProvider provider = new CloudflareAiProvider(
                RestClient.builder().build(),
                ACCOUNT_ID,
                AUTH_TOKEN,
                LLAMA_MODEL
        );

        Map<String, Object> body = provider.buildRequestBody(
                "System instructions",
                "User prompt",
                LLAMA_MODEL,
                1000
        );

        assertEquals(LLAMA_MODEL, body.get("model"));
        assertEquals(0.7, body.get("temperature"));
        assertEquals(1000, body.get("max_completion_tokens"));
        assertEquals(false, body.get("store"));
        assertFalse(body.containsKey("chat_template_kwargs"));
        assertFalse(body.containsKey("reasoning_effort"));
    }

    @Test
    void buildsGlmProfileWithThinkingDisabledForLiteraryGeneration() {
        CloudflareAiProvider provider = new CloudflareAiProvider(
                RestClient.builder().build(),
                ACCOUNT_ID,
                AUTH_TOKEN,
                GLM_MODEL
        );

        Map<String, Object> body = provider.buildRequestBody(
                "System instructions",
                "User prompt",
                GLM_MODEL,
                1000
        );

        assertEquals(GLM_MODEL, body.get("model"));
        assertEquals(0.7, body.get("temperature"));
        assertEquals(1000, body.get("max_completion_tokens"));
        assertEquals(false, body.get("store"));
        assertEquals(
                Map.of("enable_thinking", false),
                body.get("chat_template_kwargs")
        );
        List<?> messages = (List<?>) body.get("messages");
        Map<?, ?> userMessage = (Map<?, ?>) messages.get(1);
        assertEquals("User prompt", userMessage.get("content"));
        assertFalse(body.containsKey("reasoning_effort"));
    }

    @Test
    void buildsQwenProfileWithThinkingDisabledForLiteraryGeneration() {
        CloudflareAiProvider provider = new CloudflareAiProvider(
                RestClient.builder().build(),
                ACCOUNT_ID,
                AUTH_TOKEN,
                QWEN_MODEL
        );

        Map<String, Object> body = provider.buildRequestBody(
                "System instructions",
                "User prompt",
                QWEN_MODEL,
                1000
        );

        assertEquals(QWEN_MODEL, body.get("model"));
        assertEquals(0.7, body.get("temperature"));
        assertEquals(1000, body.get("max_completion_tokens"));
        assertEquals(false, body.get("store"));
        assertEquals(
                Map.of("enable_thinking", false),
                body.get("chat_template_kwargs")
        );
        List<?> messages = (List<?>) body.get("messages");
        Map<?, ?> userMessage = (Map<?, ?>) messages.get(1);
        assertEquals("User prompt\n\n/no_think", userMessage.get("content"));
        assertFalse(body.containsKey("reasoning_effort"));
    }

    @Test
    void buildsGraniteProfileWithoutReasoningParameters() {
        CloudflareAiProvider provider = new CloudflareAiProvider(
                RestClient.builder().build(),
                ACCOUNT_ID,
                AUTH_TOKEN,
                GRANITE_MODEL
        );

        Map<String, Object> body = provider.buildRequestBody(
                "System instructions",
                "User prompt",
                GRANITE_MODEL,
                1000
        );

        assertEquals(GRANITE_MODEL, body.get("model"));
        assertEquals(0.7, body.get("temperature"));
        assertEquals(1000, body.get("max_completion_tokens"));
        assertEquals(false, body.get("store"));
        assertFalse(body.containsKey("chat_template_kwargs"));
        assertFalse(body.containsKey("reasoning_effort"));
        List<?> messages = (List<?>) body.get("messages");
        Map<?, ?> userMessage = (Map<?, ?>) messages.get(1);
        assertEquals("User prompt", userMessage.get("content"));
    }

    @Test
    void buildsLlama4ScoutProfileWithoutReasoningParameters() {
        CloudflareAiProvider provider = new CloudflareAiProvider(
                RestClient.builder().build(),
                ACCOUNT_ID,
                AUTH_TOKEN,
                LLAMA4_SCOUT_MODEL
        );

        Map<String, Object> body = provider.buildRequestBody(
                "System instructions",
                "User prompt",
                LLAMA4_SCOUT_MODEL,
                1000
        );

        assertEquals(LLAMA4_SCOUT_MODEL, body.get("model"));
        assertEquals(0.7, body.get("temperature"));
        assertEquals(1000, body.get("max_completion_tokens"));
        assertEquals(false, body.get("store"));
        assertFalse(body.containsKey("chat_template_kwargs"));
        assertFalse(body.containsKey("reasoning_effort"));
        List<?> messages = (List<?>) body.get("messages");
        Map<?, ?> userMessage = (Map<?, ?>) messages.get(1);
        assertEquals("User prompt", userMessage.get("content"));
    }

    @Test
    void rejectsMissingConfigurationBeforeCallingProvider() {
        CloudflareAiProvider provider = new CloudflareAiProvider(
                RestClient.builder().build(),
                "",
                "",
                MODEL
        );

        assertThrows(
                IllegalStateException.class,
                () -> provider.generate("instructions", "prompt", MODEL, 1000)
        );
    }

    @Test
    void rejectsEmptyProviderResponse() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        CloudflareAiProvider provider = provider(builder);

        server.expect(requestTo(GENERATION_URL))
                .andRespond(withSuccess(
                        """
                        {"choices": [], "usage": null}
                        """,
                        org.springframework.http.MediaType.APPLICATION_JSON
                ));

        assertThrows(
                IllegalStateException.class,
                () -> provider.generate("instructions", "prompt", MODEL, 1000)
        );
        server.verify();
    }

    @Test
    void identifiesReasoningThatConsumedTheCompletionLimitWithoutLoggingItsContent() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        CloudflareAiProvider provider = provider(builder);
        Logger logger = (Logger) LoggerFactory.getLogger(CloudflareAiProvider.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            server.expect(requestTo(GENERATION_URL))
                    .andRespond(withSuccess(
                            """
                            {
                              "choices": [
                                {
                                  "message": {
                                    "content": null,
                                    "reasoning": "private-reasoning-must-not-be-logged"
                                  },
                                  "finish_reason": "length"
                                }
                              ],
                              "usage": {
                                "prompt_tokens": 3516,
                                "completion_tokens": 1000,
                                "total_tokens": 4516
                              }
                            }
                            """,
                            org.springframework.http.MediaType.APPLICATION_JSON
                    ));

            IllegalStateException error = assertThrows(
                    IllegalStateException.class,
                    () -> provider.generate("instructions", "prompt", MODEL, 1000)
            );

            assertEquals(
                    "A Cloudflare consumiu o limite antes de produzir o texto final.",
                    error.getMessage()
            );
            assertEquals(1, appender.list.size());
            ILoggingEvent event = appender.list.getFirst();
            assertEquals(Level.WARN, event.getLevel());
            assertEquals(
                    "CLOUDFLARE_EMPTY_RESPONSE finishReason=length promptTokens=3516 completionTokens=1000 totalTokens=4516 reasoningChars=36",
                    event.getFormattedMessage()
            );
            assertFalse(event.getFormattedMessage().contains("private-reasoning"));
            assertFalse(event.getFormattedMessage().contains(AUTH_TOKEN));
            server.verify();
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }

    @Test
    void sanitizesUnexpectedFinishReasonBeforeLogging() {
        CloudflareAiProvider provider = new CloudflareAiProvider(
                RestClient.builder().build(),
                ACCOUNT_ID,
                AUTH_TOKEN,
                MODEL
        );

        assertEquals("other", provider.sanitizedFinishReason("untrusted\nlog-value"));
        assertEquals("missing", provider.sanitizedFinishReason(null));
        assertEquals("stop", provider.sanitizedFinishReason("stop"));
    }

    @Test
    void sanitizesAuthenticationFailureWithoutExposingProviderBody() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        CloudflareAiProvider provider = provider(builder);

        server.expect(requestTo(GENERATION_URL))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED).body("sensitive provider body"));

        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> provider.generate("instructions", "prompt", MODEL, 1000)
        );

        assertEquals("A Cloudflare recusou as credenciais configuradas.", error.getMessage());
        assertFalse(error.getMessage().contains("sensitive provider body"));
        assertFalse(error.getMessage().contains(AUTH_TOKEN));
        server.verify();
    }

    @Test
    void reportsFreeTierLimitWithoutExposingProviderBody() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        CloudflareAiProvider provider = provider(builder);

        server.expect(requestTo(GENERATION_URL))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).body("quota details"));

        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> provider.generate("instructions", "prompt", MODEL, 1000)
        );

        assertEquals("O limite de uso da Cloudflare foi atingido.", error.getMessage());
        assertFalse(error.getMessage().contains("quota details"));
        server.verify();
    }

    @Test
    void sanitizesNetworkFailureWithoutExposingTechnicalDetails() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        CloudflareAiProvider provider = provider(builder);

        server.expect(requestTo(GENERATION_URL))
                .andRespond(request -> {
                    throw new ResourceAccessException("timeout with sensitive technical details");
                });

        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> provider.generate("instructions", "prompt", MODEL, 1000)
        );

        assertEquals("A Cloudflare não conseguiu concluir a geração.", error.getMessage());
        assertFalse(error.getMessage().contains("sensitive technical details"));
        server.verify();
    }

    private CloudflareAiProvider provider(RestClient.Builder builder) {
        return new CloudflareAiProvider(
                builder.baseUrl("https://api.cloudflare.com/client/v4/accounts").build(),
                ACCOUNT_ID,
                AUTH_TOKEN,
                MODEL
        );
    }
}
