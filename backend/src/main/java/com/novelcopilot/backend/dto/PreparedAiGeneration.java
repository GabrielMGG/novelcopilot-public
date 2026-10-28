package com.novelcopilot.backend.dto;

import com.novelcopilot.backend.dto.ContextProfile;
import java.math.BigDecimal;
import java.util.Map;

public class PreparedAiGeneration {
    private String action;
    private ContextProfile profile;
    private String finalContext;
    private String systemPrompt;
    private String actionPrompt;
    private Integer estimatedInputTokens;
    private Integer maxOutputTokens;
    private BigDecimal estimatedProviderCostUsd;
    private BigDecimal estimatedCommercialValueUsd;
    private BigDecimal estimatedCredits;
    private Map<String, Object> diagnostics;
    private String promptHash;

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public ContextProfile getProfile() { return profile; }
    public void setProfile(ContextProfile profile) { this.profile = profile; }
    public String getFinalContext() { return finalContext; }
    public void setFinalContext(String finalContext) { this.finalContext = finalContext; }
    public String getSystemPrompt() { return systemPrompt; }
    public void setSystemPrompt(String systemPrompt) { this.systemPrompt = systemPrompt; }
    public String getActionPrompt() { return actionPrompt; }
    public void setActionPrompt(String actionPrompt) { this.actionPrompt = actionPrompt; }
    public Integer getEstimatedInputTokens() { return estimatedInputTokens; }
    public void setEstimatedInputTokens(Integer estimatedInputTokens) { this.estimatedInputTokens = estimatedInputTokens; }
    public Integer getMaxOutputTokens() { return maxOutputTokens; }
    public void setMaxOutputTokens(Integer maxOutputTokens) { this.maxOutputTokens = maxOutputTokens; }
    public BigDecimal getEstimatedProviderCostUsd() { return estimatedProviderCostUsd; }
    public void setEstimatedProviderCostUsd(BigDecimal estimatedProviderCostUsd) { this.estimatedProviderCostUsd = estimatedProviderCostUsd; }
    public BigDecimal getEstimatedCommercialValueUsd() { return estimatedCommercialValueUsd; }
    public void setEstimatedCommercialValueUsd(BigDecimal estimatedCommercialValueUsd) { this.estimatedCommercialValueUsd = estimatedCommercialValueUsd; }
    public BigDecimal getEstimatedCredits() { return estimatedCredits; }
    public void setEstimatedCredits(BigDecimal estimatedCredits) { this.estimatedCredits = estimatedCredits; }
    public Map<String, Object> getDiagnostics() { return diagnostics; }
    public void setDiagnostics(Map<String, Object> diagnostics) { this.diagnostics = diagnostics; }
    public String getPromptHash() { return promptHash; }
    public void setPromptHash(String promptHash) { this.promptHash = promptHash; }
}
