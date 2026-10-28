package com.novelcopilot.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.LocalDateTime;


@Entity
@Table(name = "chapters")

public class Chapter{
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Integer id;

@Column(name = "story_id")
private Integer storyId;

private String title;

@Column(name = "chapter_order")
private Integer chapterOrder;

private String type;

private String goal;

@Column(columnDefinition = "TEXT")
private String summary;

@Column(name = "summary_content_hash", length = 64)
private String summaryContentHash;

@Column(name = "summary_generated_at")
private LocalDateTime summaryGeneratedAt;

@Column(name = "summary_provider", length = 50)
private String summaryProvider;

@Column(name = "summary_model", length = 255)
private String summaryModel;

@Column(columnDefinition = "TEXT")
private String content;

@Column(name = "created_at")
private LocalDateTime createdAt;

@Column(name = "updated_at")
private LocalDateTime updatedAt;

@Version
@Column(nullable = false)
private Long version = 0L;

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

    public Integer getChapterOrder() {
        return chapterOrder;
    }

    public void setChapterOrder(Integer chapterOrder) {
        this.chapterOrder = chapterOrder;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getGoal() {
        return goal;
    }

    public void setGoal(String goal) {
        this.goal = goal;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getSummaryContentHash() {
        return summaryContentHash;
    }

    public void setSummaryContentHash(String summaryContentHash) {
        this.summaryContentHash = summaryContentHash;
    }

    public LocalDateTime getSummaryGeneratedAt() {
        return summaryGeneratedAt;
    }

    public void setSummaryGeneratedAt(LocalDateTime summaryGeneratedAt) {
        this.summaryGeneratedAt = summaryGeneratedAt;
    }

    public String getSummaryProvider() {
        return summaryProvider;
    }

    public void setSummaryProvider(String summaryProvider) {
        this.summaryProvider = summaryProvider;
    }

    public String getSummaryModel() {
        return summaryModel;
    }

    public void setSummaryModel(String summaryModel) {
        this.summaryModel = summaryModel;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
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

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
