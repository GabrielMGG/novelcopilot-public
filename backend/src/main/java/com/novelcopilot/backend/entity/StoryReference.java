package com.novelcopilot.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "story_references")
public class StoryReference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "story_id")
    private Integer storyId;

    private String title;

    @Column(name = "reference_type")
    private String referenceType;

    @Column(name = "what_i_like", columnDefinition = "TEXT")
    private String whatILike;

    @Column(name = "inspiration_points", columnDefinition = "TEXT")
    private String inspirationPoints;

    @Column(name = "avoid_copying", columnDefinition = "TEXT")
    private String avoidCopying;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Integer getId() {
        return id;
    }

    public Integer getStoryId() {
        return storyId;
    }

    public void setStoryId(Integer storyId) {
        this.storyId = storyId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getReferenceType() {
        return referenceType;
    }

    public void setReferenceType(String referenceType) {
        this.referenceType = referenceType;
    }

    public String getWhatILike() {
        return whatILike;
    }

    public void setWhatILike(String whatILike) {
        this.whatILike = whatILike;
    }

    public String getInspirationPoints() {
        return inspirationPoints;
    }

    public void setInspirationPoints(String inspirationPoints) {
        this.inspirationPoints = inspirationPoints;
    }

    public String getAvoidCopying() {
        return avoidCopying;
    }

    public void setAvoidCopying(String avoidCopying) {
        this.avoidCopying = avoidCopying;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}