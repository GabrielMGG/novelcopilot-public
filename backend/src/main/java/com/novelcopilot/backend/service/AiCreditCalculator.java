package com.novelcopilot.backend.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class AiCreditCalculator {

    public BigDecimal calculateRequiredCredits(
            long inputTokens,
            long outputTokens,
            BigDecimal minimumChargeCredits) {
        return calculateRequiredCredits(
                inputTokens,
                outputTokens,
                minimumChargeCredits,
                BigDecimal.ONE
        );
    }

    public BigDecimal calculateRequiredCredits(
            long inputTokens,
            long outputTokens,
            BigDecimal minimumChargeCredits,
            BigDecimal incrementCredits) {
        if (minimumChargeCredits == null || minimumChargeCredits.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("O consumo mínimo deve ser maior que zero");
        }
        if (incrementCredits == null || incrementCredits.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("O incremento de consumo deve ser maior que zero");
        }

        long safeInputTokens = Math.max(0, inputTokens);
        long safeOutputTokens = Math.max(0, outputTokens);
        BigDecimal rawCredits = BigDecimal.valueOf(safeInputTokens + safeOutputTokens)
                .divide(BigDecimal.valueOf(1000), 10, RoundingMode.HALF_UP);
        BigDecimal roundedCredits = rawCredits
                .divide(incrementCredits, 0, RoundingMode.CEILING)
                .multiply(incrementCredits);

        BigDecimal chargedCredits = roundedCredits.max(minimumChargeCredits);
        
        if (chargedCredits.compareTo(BigDecimal.ZERO) <= 0) {
            return minimumChargeCredits;
        }
        return chargedCredits;
    }
}
