package com.novelcopilot.backend.benchmark.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AiBenchmarkCase {
    private String id;
    private String internalName;
    private String category;
    private String expectedLanguage;
    private String storyContext;
    private String chapterContent;
    private String selectedExcerpt;
    private String userInstruction;
    private String actionType;
    private Integer maxExpectedLength;
    private String evaluationCriteria;
    
    private String genre;
    private String characters;
    private String establishedFacts;
    private String narrativeObjective;
    private String exactTask;
    private String mandatoryElements;
    private String restrictions;
    private String tone;
    private String pov;
    private String wordRange;
    private String outputFormat;
    private String approvalStatus;

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getInternalName() { return internalName; }
    public void setInternalName(String internalName) { this.internalName = internalName; }
    
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    
    public String getExpectedLanguage() { return expectedLanguage; }
    public void setExpectedLanguage(String expectedLanguage) { this.expectedLanguage = expectedLanguage; }
    
    public String getStoryContext() { return storyContext; }
    public void setStoryContext(String storyContext) { this.storyContext = storyContext; }
    
    public String getChapterContent() { return chapterContent; }
    public void setChapterContent(String chapterContent) { this.chapterContent = chapterContent; }
    
    public String getSelectedExcerpt() { return selectedExcerpt; }
    public void setSelectedExcerpt(String selectedExcerpt) { this.selectedExcerpt = selectedExcerpt; }
    
    public String getUserInstruction() { return userInstruction; }
    public void setUserInstruction(String userInstruction) { this.userInstruction = userInstruction; }
    
    public String getActionType() { return actionType; }
    public void setActionType(String actionType) { this.actionType = actionType; }
    
    public Integer getMaxExpectedLength() { return maxExpectedLength; }
    public void setMaxExpectedLength(Integer maxExpectedLength) { this.maxExpectedLength = maxExpectedLength; }
    
    public String getEvaluationCriteria() { return evaluationCriteria; }
    public void setEvaluationCriteria(String evaluationCriteria) { this.evaluationCriteria = evaluationCriteria; }

    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre; }
    public String getCharacters() { return characters; }
    public void setCharacters(String characters) { this.characters = characters; }
    public String getEstablishedFacts() { return establishedFacts; }
    public void setEstablishedFacts(String establishedFacts) { this.establishedFacts = establishedFacts; }
    public String getNarrativeObjective() { return narrativeObjective; }
    public void setNarrativeObjective(String narrativeObjective) { this.narrativeObjective = narrativeObjective; }
    public String getExactTask() { return exactTask; }
    public void setExactTask(String exactTask) { this.exactTask = exactTask; }
    public String getMandatoryElements() { return mandatoryElements; }
    public void setMandatoryElements(String mandatoryElements) { this.mandatoryElements = mandatoryElements; }
    public String getRestrictions() { return restrictions; }
    public void setRestrictions(String restrictions) { this.restrictions = restrictions; }
    public String getTone() { return tone; }
    public void setTone(String tone) { this.tone = tone; }
    public String getPov() { return pov; }
    public void setPov(String pov) { this.pov = pov; }
    public String getWordRange() { return wordRange; }
    public void setWordRange(String wordRange) { this.wordRange = wordRange; }
    public String getOutputFormat() { return outputFormat; }
    public void setOutputFormat(String outputFormat) { this.outputFormat = outputFormat; }
    public String getApprovalStatus() { return approvalStatus; }
    public void setApprovalStatus(String approvalStatus) { this.approvalStatus = approvalStatus; }

    @JsonProperty("instruction")
    public String getInstruction() {
        if (exactTask != null && !exactTask.isEmpty()) {
            return exactTask;
        }
        return userInstruction;
    }
}
