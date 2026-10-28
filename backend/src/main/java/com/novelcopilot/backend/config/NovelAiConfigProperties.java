package com.novelcopilot.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "novelcopilot.ai")
public class NovelAiConfigProperties {
    private boolean enabled;
    private String provider;
    private String primaryModel;
    private String fallbackModel;
    private boolean fallbackEnabled;
    private int timeout;
    private double defaultTemperature;
    private int maxOutputTokens;
    private int maxContextTokens;
    private int maxRetries;
    private int maxInstructionLength;
    private int maxCharactersIncluded;
    private int maxWorldNotesIncluded;
    private int maxPreviousChaptersIncluded;
    private SummaryConfig summary = new SummaryConfig();
    
    private ContextProfilesConfig context = new ContextProfilesConfig();

    public static class SummaryConfig {
        private String provider = "cloudflare";
        private String model = "@cf/google/gemma-4-26b-a4b-it";
        private int maxOutputTokens = 1400;

        public String getProvider() { return provider; }
        public void setProvider(String provider) { this.provider = provider; }
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        public int getMaxOutputTokens() { return maxOutputTokens; }
        public void setMaxOutputTokens(int maxOutputTokens) { this.maxOutputTokens = maxOutputTokens; }
    }

    public static class ProfileBudget {
        private int idealInputTokens;
        private int maxCommonTokens;
        private int maxExceptionalTokens;
        private int maxOutputTokens;
        private int maxCharacters;
        private int maxWorldNotes;
        private int maxPreviousChapters;

        public ProfileBudget() {}
        public ProfileBudget(int idealInputTokens, int maxCommonTokens, int maxExceptionalTokens, int maxOutputTokens, int maxCharacters, int maxWorldNotes, int maxPreviousChapters) {
            this.idealInputTokens = idealInputTokens;
            this.maxCommonTokens = maxCommonTokens;
            this.maxExceptionalTokens = maxExceptionalTokens;
            this.maxOutputTokens = maxOutputTokens;
            this.maxCharacters = maxCharacters;
            this.maxWorldNotes = maxWorldNotes;
            this.maxPreviousChapters = maxPreviousChapters;
        }

        // Getters and setters
        public int getIdealInputTokens() { return idealInputTokens; }
        public void setIdealInputTokens(int idealInputTokens) { this.idealInputTokens = idealInputTokens; }
        public int getMaxCommonTokens() { return maxCommonTokens; }
        public void setMaxCommonTokens(int maxCommonTokens) { this.maxCommonTokens = maxCommonTokens; }
        public int getMaxExceptionalTokens() { return maxExceptionalTokens; }
        public void setMaxExceptionalTokens(int maxExceptionalTokens) { this.maxExceptionalTokens = maxExceptionalTokens; }
        public int getMaxOutputTokens() { return maxOutputTokens; }
        public void setMaxOutputTokens(int maxOutputTokens) { this.maxOutputTokens = maxOutputTokens; }
        public int getMaxCharacters() { return maxCharacters; }
        public void setMaxCharacters(int maxCharacters) { this.maxCharacters = maxCharacters; }
        public int getMaxWorldNotes() { return maxWorldNotes; }
        public void setMaxWorldNotes(int maxWorldNotes) { this.maxWorldNotes = maxWorldNotes; }
        public int getMaxPreviousChapters() { return maxPreviousChapters; }
        public void setMaxPreviousChapters(int maxPreviousChapters) { this.maxPreviousChapters = maxPreviousChapters; }
    }

    public static class ContextProfilesConfig {
        private ProfileBudget selectionEdit = new ProfileBudget(1500, 3000, 5000, 1000, 2, 2, 0);
        private ProfileBudget inlineLowCost = new ProfileBudget(1000, 2500, 3000, 250, 2, 1, 1);
        private ProfileBudget sceneContinuity = new ProfileBudget(5000, 8000, 12000, 1000, 5, 5, 3);
        private ProfileBudget continuationSuggestions = new ProfileBudget(5000, 8000, 10000, 700, 5, 5, 3);
        private ProfileBudget chapterFromBeats = new ProfileBudget(8000, 12000, 18000, 2500, 8, 8, 5);
        private ProfileBudget chapterTitle = new ProfileBudget(2500, 4000, 6000, 100, 0, 0, 0);
        private ProfileBudget chapterReview = new ProfileBudget(7000, 10000, 14000, 1200, 5, 5, 3);

        public ProfileBudget getSelectionEdit() { return selectionEdit; }
        public void setSelectionEdit(ProfileBudget selectionEdit) { this.selectionEdit = selectionEdit; }
        public ProfileBudget getInlineLowCost() { return inlineLowCost; }
        public void setInlineLowCost(ProfileBudget inlineLowCost) { this.inlineLowCost = inlineLowCost; }
        public ProfileBudget getSceneContinuity() { return sceneContinuity; }
        public void setSceneContinuity(ProfileBudget sceneContinuity) { this.sceneContinuity = sceneContinuity; }
        public ProfileBudget getContinuationSuggestions() { return continuationSuggestions; }
        public void setContinuationSuggestions(ProfileBudget continuationSuggestions) { this.continuationSuggestions = continuationSuggestions; }
        public ProfileBudget getChapterFromBeats() { return chapterFromBeats; }
        public void setChapterFromBeats(ProfileBudget chapterFromBeats) { this.chapterFromBeats = chapterFromBeats; }
        public ProfileBudget getChapterTitle() { return chapterTitle; }
        public void setChapterTitle(ProfileBudget chapterTitle) { this.chapterTitle = chapterTitle; }
        public ProfileBudget getChapterReview() { return chapterReview; }
        public void setChapterReview(ProfileBudget chapterReview) { this.chapterReview = chapterReview; }
    }

    // Getters and Setters
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public String getPrimaryModel() { return primaryModel; }
    public void setPrimaryModel(String primaryModel) { this.primaryModel = primaryModel; }

    public String getFallbackModel() { return fallbackModel; }
    public void setFallbackModel(String fallbackModel) { this.fallbackModel = fallbackModel; }

    public boolean isFallbackEnabled() { return fallbackEnabled; }
    public void setFallbackEnabled(boolean fallbackEnabled) { this.fallbackEnabled = fallbackEnabled; }

    public int getTimeout() { return timeout; }
    public void setTimeout(int timeout) { this.timeout = timeout; }

    public double getDefaultTemperature() { return defaultTemperature; }
    public void setDefaultTemperature(double defaultTemperature) { this.defaultTemperature = defaultTemperature; }

    public int getMaxOutputTokens() { return maxOutputTokens; }
    public void setMaxOutputTokens(int maxOutputTokens) { this.maxOutputTokens = maxOutputTokens; }

    public int getMaxContextTokens() { return maxContextTokens; }
    public void setMaxContextTokens(int maxContextTokens) { this.maxContextTokens = maxContextTokens; }

    public int getMaxRetries() { return maxRetries; }
    public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }

    public int getMaxInstructionLength() { return maxInstructionLength; }
    public void setMaxInstructionLength(int maxInstructionLength) { this.maxInstructionLength = maxInstructionLength; }

    public int getMaxCharactersIncluded() { return maxCharactersIncluded; }
    public void setMaxCharactersIncluded(int maxCharactersIncluded) { this.maxCharactersIncluded = maxCharactersIncluded; }

    public int getMaxWorldNotesIncluded() { return maxWorldNotesIncluded; }
    public void setMaxWorldNotesIncluded(int maxWorldNotesIncluded) { this.maxWorldNotesIncluded = maxWorldNotesIncluded; }

    public int getMaxPreviousChaptersIncluded() { return maxPreviousChaptersIncluded; }
    public void setMaxPreviousChaptersIncluded(int maxPreviousChaptersIncluded) { this.maxPreviousChaptersIncluded = maxPreviousChaptersIncluded; }

    public SummaryConfig getSummary() { return summary; }
    public void setSummary(SummaryConfig summary) { this.summary = summary; }

    public ContextProfilesConfig getContext() { return context; }
    public void setContext(ContextProfilesConfig context) { this.context = context; }
}
