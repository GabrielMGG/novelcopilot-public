package com.novelcopilot.backend.service;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class AiProviderResolver {

    private final Map<String, AiProvider> providersMap;

    public AiProviderResolver(List<AiProvider> providers) {
        this.providersMap = providers.stream()
                .collect(Collectors.toMap(
                        p -> p.providerName().toLowerCase(),
                        Function.identity(),
                        (existing, replacement) -> existing
                ));
    }

    public AiProvider resolve(String providerName) {
        if (providerName == null || providerName.isBlank()) {
            throw new IllegalArgumentException("O nome do provider não pode ser nulo ou vazio.");
        }
        
        AiProvider provider = providersMap.get(providerName.toLowerCase().trim());
        if (provider == null) {
            throw new IllegalArgumentException("Provider desconhecido: " + providerName + ". Valores suportados: " + providersMap.keySet());
        }
        return provider;
    }
}
