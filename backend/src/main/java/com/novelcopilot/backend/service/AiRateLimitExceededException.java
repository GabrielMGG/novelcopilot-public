package com.novelcopilot.backend.service;

public class AiRateLimitExceededException extends RuntimeException {

    private final String scope;
    private final long retryAfterSeconds;

    public AiRateLimitExceededException(String scope, long retryAfterSeconds) {
        super("Limite de uso da IA excedido. Tente novamente em "
                + retryAfterSeconds + " segundos.");
        this.scope = scope;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public String getScope() {
        return scope;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
