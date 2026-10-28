package com.novelcopilot.backend.service;

import com.novelcopilot.backend.benchmark.config.AiBenchmarkParticipantProperties;
import com.novelcopilot.backend.benchmark.config.AiBenchmarkParticipantProperties.Participant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.Optional;

/**
 * Calcula o custo estimado de chamadas de IA com base nos tokens consumidos, 
 * lendo os preços diretamente da configuração dos participantes.
 */
@Component
public class AiCostCalculator {

    private static final Logger log = LoggerFactory.getLogger(AiCostCalculator.class);
    private static final BigDecimal ONE_MILLION = new BigDecimal("1000000");

    private final AiBenchmarkParticipantProperties participantProperties;
    private final com.novelcopilot.backend.repository.ModelPricingRepository modelPricingRepository;

    public AiCostCalculator(AiBenchmarkParticipantProperties participantProperties, com.novelcopilot.backend.repository.ModelPricingRepository modelPricingRepository) {
        this.participantProperties = participantProperties;
        this.modelPricingRepository = modelPricingRepository;
    }

    public record CalculationResult(BigDecimal costUsd, String source, String warning) {}

    /**
     * Tenta resolver o preço e calcula o custo estimado em USD.
     */
    public CalculationResult calculateCost(
            String participantId,
            String provider,
            String model,
            long inputTokens,
            long cachedInputTokens,
            long outputTokens
    ) {
        BigDecimal inputPricePerMillion = null;
        BigDecimal cachedInputPricePerMillion = null;
        BigDecimal outputPricePerMillion = null;
        String warning = null;

        if (participantId != null) {
            Participant p = null;
            Map<String, Participant> participants = participantProperties.getParticipants();
            if (participants != null && participants.containsKey(participantId)) {
                p = participants.get(participantId);
            }
            if (p != null) {
                inputPricePerMillion = p.getInputPricePerMillion();
                cachedInputPricePerMillion = p.getCachedInputPricePerMillion();
                outputPricePerMillion = p.getOutputPricePerMillion();
            } else {
                warning = "Participante de benchmark não encontrado: " + participantId;
                log.warn(warning);
                return new CalculationResult(null, "UNAVAILABLE", warning);
            }
        } else {
            java.util.Optional<com.novelcopilot.backend.entity.ModelPricing> pricingOpt = modelPricingRepository.findActivePricing(provider, model, java.time.LocalDateTime.now());
            if (pricingOpt.isPresent()) {
                com.novelcopilot.backend.entity.ModelPricing pricing = pricingOpt.get();
                inputPricePerMillion = pricing.getInputPriceUsdPerMillion();
                cachedInputPricePerMillion = pricing.getCachedInputPriceUsdPerMillion();
                outputPricePerMillion = pricing.getOutputPriceUsdPerMillion();
            }
        }

        if (inputPricePerMillion == null || outputPricePerMillion == null) {
            warning = "Preço não configurado para o provedor/modelo: " + provider + "/" + model;
            log.warn(warning);
            return new CalculationResult(null, "UNAVAILABLE", warning);
        }

        long nonCachedInputTokens = Math.max(0, inputTokens - cachedInputTokens);

        BigDecimal inputCost = inputPricePerMillion
                .multiply(BigDecimal.valueOf(nonCachedInputTokens))
                .divide(ONE_MILLION, 10, RoundingMode.HALF_UP);

        BigDecimal cachedCost = BigDecimal.ZERO;
        if (cachedInputPricePerMillion != null) {
            cachedCost = cachedInputPricePerMillion
                    .multiply(BigDecimal.valueOf(cachedInputTokens))
                    .divide(ONE_MILLION, 10, RoundingMode.HALF_UP);
        } else {
            // Se não tem preço específico de cache, aplica o preço normal de input
            cachedCost = inputPricePerMillion
                    .multiply(BigDecimal.valueOf(cachedInputTokens))
                    .divide(ONE_MILLION, 10, RoundingMode.HALF_UP);
        }

        BigDecimal outputCost = outputPricePerMillion
                .multiply(BigDecimal.valueOf(outputTokens))
                .divide(ONE_MILLION, 10, RoundingMode.HALF_UP);

        BigDecimal totalCost = inputCost.add(cachedCost).add(outputCost);
        return new CalculationResult(totalCost, "CALCULATED", null);
    }



    /**
     * Calcula o custo estimado em USD usando preços diretos.
     */
    public BigDecimal calculateCost(
            BigDecimal inputPricePerMillion,
            BigDecimal outputPricePerMillion,
            long inputTokens,
            long outputTokens
    ) {
        if (inputPricePerMillion == null || outputPricePerMillion == null) return null;

        BigDecimal inputCost = inputPricePerMillion
                .multiply(BigDecimal.valueOf(inputTokens))
                .divide(ONE_MILLION, 10, RoundingMode.HALF_UP);

        BigDecimal outputCost = outputPricePerMillion
                .multiply(BigDecimal.valueOf(outputTokens))
                .divide(ONE_MILLION, 10, RoundingMode.HALF_UP);

        return inputCost.add(outputCost);
    }
}
