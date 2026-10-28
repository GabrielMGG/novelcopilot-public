package com.novelcopilot.backend.benchmark.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ai_benchmark_evaluations")
public class AiBenchmarkEvaluation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "response_id", nullable = false)
    private Long responseId;

    @Column(name = "evaluator_id", nullable = false)
    private String evaluatorId;

    @Column(name = "literary_quality")
    private Integer literaryQuality;

    @Column(name = "context_coherence")
    private Integer contextCoherence;

    @Column(name = "instruction_following")
    private Integer instructionFollowing;

    @Column(name = "language_naturalness")
    private Integer languageNaturalness;

    @Column(name = "repetition_control")
    private Integer repetitionControl;

    @Column(name = "style_preservation")
    private Integer stylePreservation;

    @Column(name = "overall_score", precision = 5, scale = 2)
    private BigDecimal overallScore;

    @Column(name = "fact_preservation")
    private Integer factPreservation;

    @Column(name = "character_preservation")
    private Integer characterPreservation;

    @Column(name = "narrative_progression")
    private Integer narrativeProgression;

    @Column(name = "no_contradictions")
    private Integer noContradictions;

    @Column(name = "compatibility_status")
    private String compatibilityStatus;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "preference")
    private String preference;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Long getResponseId() { return responseId; }
    public void setResponseId(Long responseId) { this.responseId = responseId; }
    
    public String getEvaluatorId() { return evaluatorId; }
    public void setEvaluatorId(String evaluatorId) { this.evaluatorId = evaluatorId; }
    
    public Integer getLiteraryQuality() { return literaryQuality; }
    public void setLiteraryQuality(Integer literaryQuality) { this.literaryQuality = literaryQuality; }
    
    public Integer getContextCoherence() { return contextCoherence; }
    public void setContextCoherence(Integer contextCoherence) { this.contextCoherence = contextCoherence; }
    
    public Integer getInstructionFollowing() { return instructionFollowing; }
    public void setInstructionFollowing(Integer instructionFollowing) { this.instructionFollowing = instructionFollowing; }
    
    public Integer getLanguageNaturalness() { return languageNaturalness; }
    public void setLanguageNaturalness(Integer languageNaturalness) { this.languageNaturalness = languageNaturalness; }
    
    public Integer getRepetitionControl() { return repetitionControl; }
    public void setRepetitionControl(Integer repetitionControl) { this.repetitionControl = repetitionControl; }
    
    public Integer getStylePreservation() { return stylePreservation; }
    public void setStylePreservation(Integer stylePreservation) { this.stylePreservation = stylePreservation; }
    
    public BigDecimal getOverallScore() { return overallScore; }
    public void setOverallScore(BigDecimal overallScore) { this.overallScore = overallScore; }
    
    public Integer getFactPreservation() { return factPreservation; }
    public void setFactPreservation(Integer factPreservation) { this.factPreservation = factPreservation; }
    
    public Integer getCharacterPreservation() { return characterPreservation; }
    public void setCharacterPreservation(Integer characterPreservation) { this.characterPreservation = characterPreservation; }
    
    public Integer getNarrativeProgression() { return narrativeProgression; }
    public void setNarrativeProgression(Integer narrativeProgression) { this.narrativeProgression = narrativeProgression; }
    
    public Integer getNoContradictions() { return noContradictions; }
    public void setNoContradictions(Integer noContradictions) { this.noContradictions = noContradictions; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    
    public String getPreference() { return preference; }
    public void setPreference(String preference) { this.preference = preference; }
    
    public String getCompatibilityStatus() { return compatibilityStatus; }
    public void setCompatibilityStatus(String compatibilityStatus) { this.compatibilityStatus = compatibilityStatus; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
