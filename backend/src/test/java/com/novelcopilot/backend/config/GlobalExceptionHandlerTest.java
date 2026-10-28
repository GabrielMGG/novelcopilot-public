package com.novelcopilot.backend.config;

import com.novelcopilot.backend.service.AiInsufficientCreditsException;
import com.novelcopilot.backend.service.AiProviderUnavailableException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void preservesControlledHttpStatus() {
        var response = handler.handleResponseStatusException(
                new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Requisição inválida."
                )
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Requisição inválida.", response.getBody().get("message"));
    }

    @Test
    void exposesStructuredInsufficientCreditsError() {
        var response = handler.handleAiInsufficientCredits(
                new AiInsufficientCreditsException(
                        new BigDecimal("100"),
                        BigDecimal.ZERO
                )
        );

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, response.getStatusCode());
        assertEquals(
                "AI_INSUFFICIENT_CREDITS",
                response.getBody().get("code")
        );
    }

    @Test
    void providerFailureDoesNotExposeTheInternalCause() {
        var response = handler.handleAiProviderUnavailable(
                new AiProviderUnavailableException(
                        "Groq",
                        new RuntimeException("secret provider response")
                )
        );

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertEquals("AI_PROVIDER_ERROR", response.getBody().get("code"));
        assertEquals(
                "O provedor de IA está temporariamente indisponível.",
                response.getBody().get("message")
        );
    }
}
