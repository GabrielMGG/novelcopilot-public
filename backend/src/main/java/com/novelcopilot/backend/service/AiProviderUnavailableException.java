package com.novelcopilot.backend.service;

public class AiProviderUnavailableException extends RuntimeException {

    private final String provider;

    public AiProviderUnavailableException(String provider, Throwable cause) {
        super("O provedor de IA está temporariamente indisponível.", cause);
        this.provider = provider;
    }

    public String getProvider() {
        return provider;
    }
}
