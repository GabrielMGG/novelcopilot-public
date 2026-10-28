package com.novelcopilot.backend.service;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class GroqAiProviderTest {

    @Test
    void usesNonThinkingProfileForQwenCreativeWriting() {
        GroqAiProvider provider = new GroqAiProvider("dummy", "qwen/qwen3.6-27b");

        Map<String, Object> body = provider.buildRequestBody(
                "Write literary prose.",
                "Continue the chapter.",
                "qwen/qwen3.6-27b",
                1200
        );

        assertEquals("qwen/qwen3.6-27b", body.get("model"));
        assertEquals(1200, body.get("max_tokens"));
        assertEquals("none", body.get("reasoning_effort"));
        assertEquals(0.7, body.get("temperature"));
        assertEquals(0.8, body.get("top_p"));
        assertEquals(1.5, body.get("presence_penalty"));
    }

    @Test
    void keepsGptOssOnLowReasoningWithoutSamplingParameters() {
        GroqAiProvider provider = new GroqAiProvider("dummy", "openai/gpt-oss-120b");

        Map<String, Object> body = provider.buildRequestBody(
                "Write literary prose.",
                "Continue the chapter.",
                "openai/gpt-oss-120b",
                1200
        );

        assertEquals("low", body.get("reasoning_effort"));
        assertFalse(body.containsKey("temperature"));
        assertFalse(body.containsKey("top_p"));
        assertFalse(body.containsKey("presence_penalty"));
    }
}
