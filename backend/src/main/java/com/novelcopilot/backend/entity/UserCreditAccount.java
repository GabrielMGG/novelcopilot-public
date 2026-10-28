package com.novelcopilot.backend.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_credit_account")
public class UserCreditAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String userId;
    
    @Column(nullable = false)
    private BigDecimal availableCredits = BigDecimal.ZERO;
    
    @Column(nullable = false)
    private BigDecimal reservedCredits = BigDecimal.ZERO;
    
    @Column(nullable = false)
    private BigDecimal lifetimeGrantedCredits = BigDecimal.ZERO;
    
    @Column(nullable = false)
    private BigDecimal lifetimeUsedCredits = BigDecimal.ZERO;
    
    private String currentQuotaProfileCode;
    private LocalDateTime currentCycleStart;
    private LocalDateTime currentCycleEnd;
    
    @Version
    @Column(nullable = false)
    private Long version = 0L;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public BigDecimal getAvailableCredits() { return availableCredits; }
    public void setAvailableCredits(BigDecimal availableCredits) { this.availableCredits = availableCredits; }
    public BigDecimal getReservedCredits() { return reservedCredits; }
    public void setReservedCredits(BigDecimal reservedCredits) { this.reservedCredits = reservedCredits; }
    public BigDecimal getLifetimeGrantedCredits() { return lifetimeGrantedCredits; }
    public void setLifetimeGrantedCredits(BigDecimal lifetimeGrantedCredits) { this.lifetimeGrantedCredits = lifetimeGrantedCredits; }
    public BigDecimal getLifetimeUsedCredits() { return lifetimeUsedCredits; }
    public void setLifetimeUsedCredits(BigDecimal lifetimeUsedCredits) { this.lifetimeUsedCredits = lifetimeUsedCredits; }
    public String getCurrentQuotaProfileCode() { return currentQuotaProfileCode; }
    public void setCurrentQuotaProfileCode(String currentQuotaProfileCode) { this.currentQuotaProfileCode = currentQuotaProfileCode; }
    public LocalDateTime getCurrentCycleStart() { return currentCycleStart; }
    public void setCurrentCycleStart(LocalDateTime currentCycleStart) { this.currentCycleStart = currentCycleStart; }
    public LocalDateTime getCurrentCycleEnd() { return currentCycleEnd; }
    public void setCurrentCycleEnd(LocalDateTime currentCycleEnd) { this.currentCycleEnd = currentCycleEnd; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
