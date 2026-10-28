package com.novelcopilot.backend.service;

import org.springframework.stereotype.Component;

/**
 * Estimador determinístico de tokens baseado em aproximação por caracteres.
 * Utilizado para gerenciar orçamentos de contexto sem depender de chamadas externas de tokenização.
 */
@Component
public class TokenEstimator {
    
    // Estimativa conservadora para Llama 3: 1 token = ~4 caracteres.
    private static final double CHARS_PER_TOKEN = 4.0;

    public int estimateTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        return (int) Math.ceil(text.length() / CHARS_PER_TOKEN);
    }
    
    public int estimateTokens(int charCount) {
        if (charCount <= 0) {
            return 0;
        }
        return (int) Math.ceil(charCount / CHARS_PER_TOKEN);
    }
    
    public int tokensToChars(int tokens) {
        return (int) Math.floor(tokens * CHARS_PER_TOKEN);
    }
}
