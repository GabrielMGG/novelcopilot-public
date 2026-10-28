package com.novelcopilot.backend.benchmark.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ai_benchmark_responses")
public class AiBenchmarkResponse {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "run_id", nullable = false)
    private Long runId;

    @Column(name = "case_id")
    private String caseId;

    @Column(name = "participant_id")
    private String participantId;

    @Column(name = "anonymous_label", nullable = false)
    private String anonymousLabel; // A or B

    @Column(nullable = false)
    private String provider;

    @Column(nullable = false)
    private String model;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "input_tokens", nullable = false)
    private Long inputTokens = 0L;

    @Column(name = "cached_input_tokens", nullable = false)
    private Long cachedInputTokens = 0L;

    @Column(name = "output_tokens", nullable = false)
    private Long outputTokens = 0L;

    @Column(name = "total_tokens", nullable = false)
    private Long totalTokens = 0L;

    @Column(name = "estimated_cost_usd", precision = 18, scale = 10)
    private BigDecimal estimatedCostUsd;

    @Column(name = "duration_ms", nullable = false)
    private Long durationMs = 0L;

    @Column(nullable = false)
    private String status;

    @Column(name = "error_code")
    private String errorCode;

    @Column(name = "prompt_sha256", length = 64)
    private String promptSha256;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Long getRunId() { return runId; }
    public void setRunId(Long runId) { this.runId = runId; }
    
    public String getCaseId() {
        return caseId;
    }

    public void setCaseId(String caseId) {
        this.caseId = caseId;
    }

    public String getParticipantId() {
        return participantId;
    }

    public void setParticipantId(String participantId) {
        this.participantId = participantId;
    }

    public String getAnonymousLabel() { return anonymousLabel; }
    public void setAnonymousLabel(String anonymousLabel) { this.anonymousLabel = anonymousLabel; }
    
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    
    public Long getInputTokens() { return inputTokens; }
    public void setInputTokens(Long inputTokens) { this.inputTokens = inputTokens; }
    
    public Long getCachedInputTokens() { return cachedInputTokens; }
    public void setCachedInputTokens(Long cachedInputTokens) { this.cachedInputTokens = cachedInputTokens; }
    
    public Long getOutputTokens() { return outputTokens; }
    public void setOutputTokens(Long outputTokens) { this.outputTokens = outputTokens; }
    
    public Long getTotalTokens() { return totalTokens; }
    public void setTotalTokens(Long totalTokens) { this.totalTokens = totalTokens; }
    
    public BigDecimal getEstimatedCostUsd() { return estimatedCostUsd; }
    public void setEstimatedCostUsd(BigDecimal estimatedCostUsd) { this.estimatedCostUsd = estimatedCostUsd; }
    
    @Column(name = "provider_reported_cost_usd", precision = 18, scale = 10)
    private BigDecimal providerReportedCostUsd;

    @Column(name = "calculated_cost_usd", precision = 18, scale = 10)
    private BigDecimal calculatedCostUsd;

    @Column(name = "effective_cost_usd", precision = 18, scale = 10)
    private BigDecimal effectiveCostUsd;

    @Column(name = "cost_source")
    private String costSource;
    
    public BigDecimal getProviderReportedCostUsd() { return providerReportedCostUsd; }
    public void setProviderReportedCostUsd(BigDecimal providerReportedCostUsd) { this.providerReportedCostUsd = providerReportedCostUsd; }
    
    public BigDecimal getCalculatedCostUsd() { return calculatedCostUsd; }
    public void setCalculatedCostUsd(BigDecimal calculatedCostUsd) { this.calculatedCostUsd = calculatedCostUsd; }
    
    public BigDecimal getEffectiveCostUsd() { return effectiveCostUsd; }
    public void setEffectiveCostUsd(BigDecimal effectiveCostUsd) { this.effectiveCostUsd = effectiveCostUsd; }
    
    public String getCostSource() { return costSource; }
    public void setCostSource(String costSource) { this.costSource = costSource; }

    public Long getDurationMs() { return durationMs; }
    public void setDurationMs(Long durationMs) { this.durationMs = durationMs; }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }

    public String getPromptSha256() { return promptSha256; }
    public void setPromptSha256(String promptSha256) { this.promptSha256 = promptSha256; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
