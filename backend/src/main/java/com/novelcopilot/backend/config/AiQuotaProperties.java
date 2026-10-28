package com.novelcopilot.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

@Configuration
@ConfigurationProperties(prefix = "novelcopilot.credits")
public class AiQuotaProperties {
    private BigDecimal minimumChargeCredits = BigDecimal.ONE;
    private BigDecimal selectionEditMinimumChargeCredits = new BigDecimal("0.10");
    private BigDecimal selectionEditIncrementCredits = new BigDecimal("0.10");
    private BigDecimal maximumCreditsPerGeneration = new BigDecimal("10");
    private boolean developmentCreditsEnabled = false;
    private String developmentQuotaProfileCode = "DEMO";

    public BigDecimal getMinimumChargeCredits() { return minimumChargeCredits; }
    public void setMinimumChargeCredits(BigDecimal minimumChargeCredits) { this.minimumChargeCredits = minimumChargeCredits; }
    public BigDecimal getSelectionEditMinimumChargeCredits() { return selectionEditMinimumChargeCredits; }
    public void setSelectionEditMinimumChargeCredits(BigDecimal selectionEditMinimumChargeCredits) { this.selectionEditMinimumChargeCredits = selectionEditMinimumChargeCredits; }
    public BigDecimal getSelectionEditIncrementCredits() { return selectionEditIncrementCredits; }
    public void setSelectionEditIncrementCredits(BigDecimal selectionEditIncrementCredits) { this.selectionEditIncrementCredits = selectionEditIncrementCredits; }
    public BigDecimal getMaximumCreditsPerGeneration() { return maximumCreditsPerGeneration; }
    public void setMaximumCreditsPerGeneration(BigDecimal maximumCreditsPerGeneration) { this.maximumCreditsPerGeneration = maximumCreditsPerGeneration; }
    public boolean isDevelopmentCreditsEnabled() { return developmentCreditsEnabled; }
    public void setDevelopmentCreditsEnabled(boolean developmentCreditsEnabled) { this.developmentCreditsEnabled = developmentCreditsEnabled; }
    public String getDevelopmentQuotaProfileCode() { return developmentQuotaProfileCode; }
    public void setDevelopmentQuotaProfileCode(String developmentQuotaProfileCode) { this.developmentQuotaProfileCode = developmentQuotaProfileCode; }
}
