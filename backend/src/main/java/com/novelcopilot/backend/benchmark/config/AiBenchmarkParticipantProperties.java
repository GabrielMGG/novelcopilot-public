package com.novelcopilot.backend.benchmark.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "ai.benchmark")
public class AiBenchmarkParticipantProperties {

    private Map<String, Participant> participants;

    public Map<String, Participant> getParticipants() {
        return participants;
    }

    public void setParticipants(Map<String, Participant> participants) {
        this.participants = participants;
    }

    public static class Participant {
        private boolean enabled;
        private String provider;
        private String model;
        private String displayName;
        private boolean referenceModel;
        private BigDecimal inputPricePerMillion;
        private BigDecimal cachedInputPricePerMillion;
        private BigDecimal outputPricePerMillion;
        private int contextWindow;
        private int maxOutputTokens;
        private boolean candidate;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public boolean isReferenceModel() {
            return referenceModel;
        }

        public void setReferenceModel(boolean referenceModel) {
            this.referenceModel = referenceModel;
        }

        public BigDecimal getInputPricePerMillion() {
            return inputPricePerMillion;
        }

        public void setInputPricePerMillion(BigDecimal inputPricePerMillion) {
            this.inputPricePerMillion = inputPricePerMillion;
        }

        public BigDecimal getCachedInputPricePerMillion() {
            return cachedInputPricePerMillion;
        }

        public void setCachedInputPricePerMillion(BigDecimal cachedInputPricePerMillion) {
            this.cachedInputPricePerMillion = cachedInputPricePerMillion;
        }

        public BigDecimal getOutputPricePerMillion() {
            return outputPricePerMillion;
        }

        public void setOutputPricePerMillion(BigDecimal outputPricePerMillion) {
            this.outputPricePerMillion = outputPricePerMillion;
        }

        public int getContextWindow() {
            return contextWindow;
        }

        public void setContextWindow(int contextWindow) {
            this.contextWindow = contextWindow;
        }

        public int getMaxOutputTokens() {
            return maxOutputTokens;
        }

        public void setMaxOutputTokens(int maxOutputTokens) {
            this.maxOutputTokens = maxOutputTokens;
        }

        public boolean isCandidate() {
            return candidate;
        }

        public void setCandidate(boolean candidate) {
            this.candidate = candidate;
        }
    }
}
