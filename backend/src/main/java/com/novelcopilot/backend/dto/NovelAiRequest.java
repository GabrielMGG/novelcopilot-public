package com.novelcopilot.backend.dto;

import java.util.List;

public record NovelAiRequest(
    NovelAiAction action,
    Integer storyId,
    Integer chapterId,
    String selectedText,
    String instruction,
    String language,
    String tone,
    String pov,
    Integer desiredLength,
    String reviewType,
    List<Integer> characterIds,
    List<Integer> worldNoteIds,
    Boolean includePreviousChapters,
    String idempotencyKey,
    String currentText
) {
    public NovelAiRequest(
            NovelAiAction action,
            Integer storyId,
            Integer chapterId,
            String selectedText,
            String instruction,
            String language,
            String tone,
            String pov,
            Integer desiredLength,
            String reviewType,
            List<Integer> characterIds,
            List<Integer> worldNoteIds,
            Boolean includePreviousChapters,
            String idempotencyKey
    ) {
        this(
                action,
                storyId,
                chapterId,
                selectedText,
                instruction,
                language,
                tone,
                pov,
                desiredLength,
                reviewType,
                characterIds,
                worldNoteIds,
                includePreviousChapters,
                idempotencyKey,
                null
        );
    }
}
