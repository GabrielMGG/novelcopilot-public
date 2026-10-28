package com.novelcopilot.backend.config;

import com.novelcopilot.backend.service.AiRateLimitExceededException;
import com.novelcopilot.backend.service.AiInsufficientCreditsException;
import com.novelcopilot.backend.service.AiProviderUnavailableException;
import com.novelcopilot.backend.service.ChapterVersionConflictException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(AiRateLimitExceededException.class)
    public ResponseEntity<Map<String, Object>> handleAiRateLimitExceeded(
            AiRateLimitExceededException ex
    ) {
        log.warn("AI rate limit exceeded for scope {}", ex.getScope());
        Map<String, Object> body = new HashMap<>();
        body.put("status", HttpStatus.TOO_MANY_REQUESTS.value());
        body.put("error", "Too Many Requests");
        body.put("code", "AI_RATE_LIMIT_EXCEEDED");
        body.put("message", ex.getMessage());
        body.put("scope", ex.getScope());
        body.put("retryAfterSeconds", ex.getRetryAfterSeconds());
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(ex.getRetryAfterSeconds()))
                .body(body);
    }

    @ExceptionHandler(AiInsufficientCreditsException.class)
    public ResponseEntity<Map<String, Object>> handleAiInsufficientCredits(
            AiInsufficientCreditsException ex
    ) {
        log.info(
                "Insufficient AI credits: required={}, available={}",
                ex.getRequiredCredits(),
                ex.getAvailableCredits()
        );
        Map<String, Object> body = new HashMap<>();
        body.put("status", HttpStatus.TOO_MANY_REQUESTS.value());
        body.put("error", "AI quota exceeded");
        body.put("code", "AI_INSUFFICIENT_CREDITS");
        body.put("message", ex.getMessage());
        body.put("requiredCredits", ex.getRequiredCredits());
        body.put("availableCredits", ex.getAvailableCredits());
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(body);
    }

    @ExceptionHandler(AiProviderUnavailableException.class)
    public ResponseEntity<Map<String, Object>> handleAiProviderUnavailable(
            AiProviderUnavailableException ex
    ) {
        log.error("AI provider {} failed", ex.getProvider(), ex);
        Map<String, Object> body = new HashMap<>();
        body.put("status", HttpStatus.SERVICE_UNAVAILABLE.value());
        body.put("error", "Service Unavailable");
        body.put("code", "AI_PROVIDER_ERROR");
        body.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatusException(
            ResponseStatusException ex
    ) {
        HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        String message = ex.getReason() != null
                ? ex.getReason()
                : "Não foi possível concluir a operação.";
        log.warn("Request failed with status {}: {}", status.value(), message);
        return buildErrorResponse(status, status.getReasonPhrase(), message);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.warn("Validation error (400): {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage());
    }

    @ExceptionHandler(ChapterVersionConflictException.class)
    public ResponseEntity<Map<String, Object>> handleChapterVersionConflict(
            ChapterVersionConflictException ex
    ) {
        log.warn(
                "Chapter version conflict for chapter {}: requested={}, current={}",
                ex.getChapterId(),
                ex.getRequestedVersion(),
                ex.getCurrentVersion()
        );
        Map<String, Object> body = new HashMap<>();
        body.put("status", HttpStatus.CONFLICT.value());
        body.put("error", "Conflict");
        body.put("code", "CHAPTER_VERSION_CONFLICT");
        body.put("message", ex.getMessage());
        body.put("chapterId", ex.getChapterId());
        body.put("requestedVersion", ex.getRequestedVersion());
        if (ex.getCurrentVersion() != null) {
            body.put("currentVersion", ex.getCurrentVersion());
        }
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalStateException(IllegalStateException ex) {
        log.warn("State error (409): {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.CONFLICT, "Conflict", ex.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDeniedException(AccessDeniedException ex) {
        log.warn("Access denied (403)");
        return buildErrorResponse(HttpStatus.FORBIDDEN, "Forbidden", "Você não tem permissão para executar esta operação.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneralException(Exception ex) {
        log.error("Unexpected error (500)", ex);
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", "Ocorreu um erro interno inesperado.");
    }

    private ResponseEntity<Map<String, Object>> buildErrorResponse(HttpStatus status, String error, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("status", status.value());
        body.put("error", error);
        body.put("message", message);
        return ResponseEntity.status(status).body(body);
    }
}
