package com.novelcopilot.backend.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "credit_transaction", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"userId", "idempotencyKey"})
})
public class CreditTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String userId;
    
    private Long cycleId;
    private Long generationUsageId;
    
    @Column(nullable = false)
    private String idempotencyKey;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CreditTransactionType type;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CreditTransactionStatus status;
    
    @Column(nullable = false)
    private BigDecimal credits;
    
    @Column(nullable = false)
    private BigDecimal balanceBefore;
    
    @Column(nullable = false)
    private BigDecimal balanceAfter;
    
    @Column(nullable = false)
    private BigDecimal reservedBefore;
    
    @Column(nullable = false)
    private BigDecimal reservedAfter;
    
    private String reason;
    
    @Column(columnDefinition = "TEXT")
    private String metadata;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    void initializeTimestamps() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    void updateTimestamp() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public Long getCycleId() { return cycleId; }
    public void setCycleId(Long cycleId) { this.cycleId = cycleId; }
    public Long getGenerationUsageId() { return generationUsageId; }
    public void setGenerationUsageId(Long generationUsageId) { this.generationUsageId = generationUsageId; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public CreditTransactionType getType() { return type; }
    public void setType(CreditTransactionType type) { this.type = type; }
    public CreditTransactionStatus getStatus() { return status; }
    public void setStatus(CreditTransactionStatus status) { this.status = status; }
    public BigDecimal getCredits() { return credits; }
    public void setCredits(BigDecimal credits) { this.credits = credits; }
    public BigDecimal getBalanceBefore() { return balanceBefore; }
    public void setBalanceBefore(BigDecimal balanceBefore) { this.balanceBefore = balanceBefore; }
    public BigDecimal getBalanceAfter() { return balanceAfter; }
    public void setBalanceAfter(BigDecimal balanceAfter) { this.balanceAfter = balanceAfter; }
    public BigDecimal getReservedBefore() { return reservedBefore; }
    public void setReservedBefore(BigDecimal reservedBefore) { this.reservedBefore = reservedBefore; }
    public BigDecimal getReservedAfter() { return reservedAfter; }
    public void setReservedAfter(BigDecimal reservedAfter) { this.reservedAfter = reservedAfter; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
