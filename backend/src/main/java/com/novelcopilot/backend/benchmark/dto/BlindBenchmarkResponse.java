package com.novelcopilot.backend.benchmark.dto;

import com.novelcopilot.backend.benchmark.entity.AiBenchmarkResponse;

public class BlindBenchmarkResponse {
    private Long id;
    private Long runId;
    private String caseId;
    private String anonymousLabel;
    private String content;
    private String status;

    public BlindBenchmarkResponse(AiBenchmarkResponse response) {
        this.id = response.getId();
        this.runId = response.getRunId();
        this.caseId = response.getCaseId();
        this.anonymousLabel = response.getAnonymousLabel();
        this.content = response.getContent();
        this.status = response.getStatus();
    }

    // Getters
    public Long getId() { return id; }
    public Long getRunId() { return runId; }
    public String getCaseId() { return caseId; }
    public String getAnonymousLabel() { return anonymousLabel; }
    public String getContent() { return content; }
    public String getStatus() { return status; }
}
