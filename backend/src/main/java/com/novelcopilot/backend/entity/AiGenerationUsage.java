package com.novelcopilot.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ai_generation_usage")
public class AiGenerationUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, columnDefinition = "CHAR(36)")
    private String userId;

    @Column(name = "story_id")
    private Integer storyId;

    @Column(name = "chapter_id")
    private Integer chapterId;

    @Column(name = "action", nullable = false, length = 50)
    private String action;

    @Column(name = "provider", nullable = false, length = 50)
    private String provider;

    @Column(name = "model", nullable = false, length = 100)
    private String model;

    @Column(name = "fallback_used")
    private Boolean fallbackUsed;

    @Column(name = "input_tokens")
    private Integer inputTokens;

    @Column(name = "cached_input_tokens")
    private Integer cachedInputTokens;

    @Column(name = "output_tokens")
    private Integer outputTokens;

    @Column(name = "total_tokens")
    private Integer totalTokens;

    @Column(name = "provider_reported_cost_usd", precision = 18, scale = 10)
    private BigDecimal providerReportedCostUsd;

    @Column(name = "calculated_cost_usd", precision = 18, scale = 10)
    private BigDecimal calculatedCostUsd;

    @Column(name = "effective_cost_usd", precision = 18, scale = 10)
    private BigDecimal effectiveCostUsd;

    @Column(name = "cost_source", length = 50)
    private String costSource;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "status", length = 50)
    private String status;

    @Column(name = "error_code")
    private String errorMessage;

    // Technical quota accounting
    @Column(name = "estimated_credits", precision = 18, scale = 4)
    private BigDecimal estimatedCredits;
    @Column(name = "reserved_credits", precision = 18, scale = 4)
    private BigDecimal reservedCredits;
    @Column(name = "charged_credits", precision = 18, scale = 4)
    private BigDecimal chargedCredits;
    @Column(name = "released_credits", precision = 18, scale = 4)
    private BigDecimal releasedCredits;

    @Enumerated(EnumType.STRING)
    @Column(name = "credit_usage_status", length = 20)
    private CreditUsageStatus creditUsageStatus = CreditUsageStatus.NOT_REQUIRED;

    @Column(name = "credit_transaction_id")
    private Long creditTransactionId;

    @Column(name = "context_truncated")
    private Boolean contextTruncated;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "idempotency_key", length = 100)
    private String idempotencyKey;

    @Column(name = "context_diagnostics", columnDefinition = "TEXT")
    private String contextDiagnostics;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public Integer getStoryId() { return storyId; }
    public void setStoryId(Integer storyId) { this.storyId = storyId; }
    public Integer getChapterId() { return chapterId; }
    public void setChapterId(Integer chapterId) { this.chapterId = chapterId; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public Boolean getFallbackUsed() { return fallbackUsed; }
    public void setFallbackUsed(Boolean fallbackUsed) { this.fallbackUsed = fallbackUsed; }
    public Integer getInputTokens() { return inputTokens; }
    public void setInputTokens(Integer inputTokens) { this.inputTokens = inputTokens; }
    public Integer getCachedInputTokens() { return cachedInputTokens; }
    public void setCachedInputTokens(Integer cachedInputTokens) { this.cachedInputTokens = cachedInputTokens; }
    public Integer getOutputTokens() { return outputTokens; }
    public void setOutputTokens(Integer outputTokens) { this.outputTokens = outputTokens; }
    public Integer getTotalTokens() { return totalTokens; }
    public void setTotalTokens(Integer totalTokens) { this.totalTokens = totalTokens; }
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
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public BigDecimal getEstimatedCredits() { return estimatedCredits; }
    public void setEstimatedCredits(BigDecimal estimatedCredits) { this.estimatedCredits = estimatedCredits; }
    public BigDecimal getReservedCredits() { return reservedCredits; }
    public void setReservedCredits(BigDecimal reservedCredits) { this.reservedCredits = reservedCredits; }
    public BigDecimal getChargedCredits() { return chargedCredits; }
    public void setChargedCredits(BigDecimal chargedCredits) { this.chargedCredits = chargedCredits; }
    public BigDecimal getReleasedCredits() { return releasedCredits; }
    public void setReleasedCredits(BigDecimal releasedCredits) { this.releasedCredits = releasedCredits; }
    public CreditUsageStatus getCreditUsageStatus() { return creditUsageStatus; }
    public void setCreditUsageStatus(CreditUsageStatus creditUsageStatus) { this.creditUsageStatus = creditUsageStatus; }
    public Long getCreditTransactionId() { return creditTransactionId; }
    public void setCreditTransactionId(Long creditTransactionId) { this.creditTransactionId = creditTransactionId; }
    public Boolean getContextTruncated() { return contextTruncated; }
    public void setContextTruncated(Boolean contextTruncated) { this.contextTruncated = contextTruncated; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public String getContextDiagnostics() { return contextDiagnostics; }
    public void setContextDiagnostics(String contextDiagnostics) { this.contextDiagnostics = contextDiagnostics; }
}
