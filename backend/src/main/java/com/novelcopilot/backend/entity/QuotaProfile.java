package com.novelcopilot.backend.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "quota_profile")
public class QuotaProfile {
    @Id
    private String code;
    
    private String description;
    private BigDecimal monthlyCredits;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getMonthlyCredits() { return monthlyCredits; }
    public void setMonthlyCredits(BigDecimal monthlyCredits) { this.monthlyCredits = monthlyCredits; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
