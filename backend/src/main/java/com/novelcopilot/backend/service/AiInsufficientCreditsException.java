package com.novelcopilot.backend.service;

import java.math.BigDecimal;

public class AiInsufficientCreditsException extends RuntimeException {

    private final BigDecimal requiredCredits;
    private final BigDecimal availableCredits;

    public AiInsufficientCreditsException(
            BigDecimal requiredCredits,
            BigDecimal availableCredits
    ) {
        super("Créditos insuficientes para esta geração.");
        this.requiredCredits = requiredCredits;
        this.availableCredits = availableCredits;
    }

    public BigDecimal getRequiredCredits() {
        return requiredCredits;
    }

    public BigDecimal getAvailableCredits() {
        return availableCredits;
    }
}
