package com.novelcopilot.backend.dto;

public record CreateStoryRequest(
        String title,
        String genre,
        String tone,
        String summary,
        String mainIdea,
        String coverImagePath
) {
}