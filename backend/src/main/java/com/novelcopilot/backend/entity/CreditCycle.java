package com.novelcopilot.backend.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "credit_cycle", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"userId", "cycleStart", "quotaProfileCode"})
})
public class CreditCycle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String userId;
    
    @Column(nullable = false)
    private String quotaProfileCode;
    
    @Column(nullable = false)
    private LocalDateTime cycleStart;
    
    private LocalDateTime cycleEnd;
    
    @Column(nullable = false)
    private BigDecimal grantedCredits = BigDecimal.ZERO;
    
    @Column(nullable = false)
    private BigDecimal usedCredits = BigDecimal.ZERO;
    
    @Column(nullable = false)
    private BigDecimal reservedCredits = BigDecimal.ZERO;
    
    @Column(nullable = false)
    private BigDecimal expiredCredits = BigDecimal.ZERO;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CreditCycleStatus status;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getQuotaProfileCode() { return quotaProfileCode; }
    public void setQuotaProfileCode(String quotaProfileCode) { this.quotaProfileCode = quotaProfileCode; }
    public LocalDateTime getCycleStart() { return cycleStart; }
    public void setCycleStart(LocalDateTime cycleStart) { this.cycleStart = cycleStart; }
    public LocalDateTime getCycleEnd() { return cycleEnd; }
    public void setCycleEnd(LocalDateTime cycleEnd) { this.cycleEnd = cycleEnd; }
    public BigDecimal getGrantedCredits() { return grantedCredits; }
    public void setGrantedCredits(BigDecimal grantedCredits) { this.grantedCredits = grantedCredits; }
    public BigDecimal getUsedCredits() { return usedCredits; }
    public void setUsedCredits(BigDecimal usedCredits) { this.usedCredits = usedCredits; }
    public BigDecimal getReservedCredits() { return reservedCredits; }
    public void setReservedCredits(BigDecimal reservedCredits) { this.reservedCredits = reservedCredits; }
    public BigDecimal getExpiredCredits() { return expiredCredits; }
    public void setExpiredCredits(BigDecimal expiredCredits) { this.expiredCredits = expiredCredits; }
    public CreditCycleStatus getStatus() { return status; }
    public void setStatus(CreditCycleStatus status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
