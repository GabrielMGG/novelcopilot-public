package com.novelcopilot.backend.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "model_pricing")
public class ModelPricing {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String provider;
    private String model;
    private BigDecimal inputPriceUsdPerMillion;
    private BigDecimal cachedInputPriceUsdPerMillion;
    private BigDecimal outputPriceUsdPerMillion;
    private LocalDateTime effectiveFrom;
    private LocalDateTime effectiveUntil;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public BigDecimal getInputPriceUsdPerMillion() { return inputPriceUsdPerMillion; }
    public void setInputPriceUsdPerMillion(BigDecimal inputPriceUsdPerMillion) { this.inputPriceUsdPerMillion = inputPriceUsdPerMillion; }
    public BigDecimal getCachedInputPriceUsdPerMillion() { return cachedInputPriceUsdPerMillion; }
    public void setCachedInputPriceUsdPerMillion(BigDecimal cachedInputPriceUsdPerMillion) { this.cachedInputPriceUsdPerMillion = cachedInputPriceUsdPerMillion; }
    public BigDecimal getOutputPriceUsdPerMillion() { return outputPriceUsdPerMillion; }
    public void setOutputPriceUsdPerMillion(BigDecimal outputPriceUsdPerMillion) { this.outputPriceUsdPerMillion = outputPriceUsdPerMillion; }
    public LocalDateTime getEffectiveFrom() { return effectiveFrom; }
    public void setEffectiveFrom(LocalDateTime effectiveFrom) { this.effectiveFrom = effectiveFrom; }
    public LocalDateTime getEffectiveUntil() { return effectiveUntil; }
    public void setEffectiveUntil(LocalDateTime effectiveUntil) { this.effectiveUntil = effectiveUntil; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
