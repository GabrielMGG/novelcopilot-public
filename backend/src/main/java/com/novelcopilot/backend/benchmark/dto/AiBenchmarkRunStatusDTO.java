package com.novelcopilot.backend.benchmark.dto;

import com.novelcopilot.backend.benchmark.entity.AiBenchmarkRun;

import java.math.BigDecimal;
import java.util.List;
import java.util.ArrayList;

public class AiBenchmarkRunStatusDTO {
    private Long id;
    private AiBenchmarkRun run;
    private String suiteType;
    private int expectedParticipantCount;
    private int expectedCases;
    private int expectedResponses;
    private long completedResponses;
    private long failedResponses;
    private long completeCases;
    private long incompleteCases;
    private long evaluableResponses;
    private long evaluatedResponses;
    private boolean evaluationCompleted;
    private boolean reportAvailable;
    private BigDecimal totalCostUsd;
    private List<Object> responses = new ArrayList<>();
    private List<String> warnings = new ArrayList<>();

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public AiBenchmarkRun getRun() { return run; }
    public void setRun(AiBenchmarkRun run) { this.run = run; }
    public String getSuiteType() { return suiteType; }
    public void setSuiteType(String suiteType) { this.suiteType = suiteType; }
    public int getExpectedParticipantCount() { return expectedParticipantCount; }
    public void setExpectedParticipantCount(int expectedParticipantCount) { this.expectedParticipantCount = expectedParticipantCount; }
    public int getExpectedCases() { return expectedCases; }
    public void setExpectedCases(int expectedCases) { this.expectedCases = expectedCases; }
    public int getExpectedResponses() { return expectedResponses; }
    public void setExpectedResponses(int expectedResponses) { this.expectedResponses = expectedResponses; }
    public long getCompletedResponses() { return completedResponses; }
    public void setCompletedResponses(long completedResponses) { this.completedResponses = completedResponses; }
    public long getFailedResponses() { return failedResponses; }
    public void setFailedResponses(long failedResponses) { this.failedResponses = failedResponses; }
    public long getCompleteCases() { return completeCases; }
    public void setCompleteCases(long completeCases) { this.completeCases = completeCases; }
    public long getIncompleteCases() { return incompleteCases; }
    public void setIncompleteCases(long incompleteCases) { this.incompleteCases = incompleteCases; }
    public long getEvaluableResponses() { return evaluableResponses; }
    public void setEvaluableResponses(long evaluableResponses) { this.evaluableResponses = evaluableResponses; }
    public long getEvaluatedResponses() { return evaluatedResponses; }
    public void setEvaluatedResponses(long evaluatedResponses) { this.evaluatedResponses = evaluatedResponses; }
    public boolean isEvaluationCompleted() { return evaluationCompleted; }
    public void setEvaluationCompleted(boolean evaluationCompleted) { this.evaluationCompleted = evaluationCompleted; }
    public boolean isReportAvailable() { return reportAvailable; }
    public void setReportAvailable(boolean reportAvailable) { this.reportAvailable = reportAvailable; }
    public BigDecimal getTotalCostUsd() { return totalCostUsd; }
    public void setTotalCostUsd(BigDecimal totalCostUsd) { this.totalCostUsd = totalCostUsd; }
    public List<Object> getResponses() { return responses; }
    public void setResponses(List<Object> responses) { this.responses = responses; }
    public List<String> getWarnings() { return warnings; }
    public void setWarnings(List<String> warnings) { this.warnings = warnings; }
}
