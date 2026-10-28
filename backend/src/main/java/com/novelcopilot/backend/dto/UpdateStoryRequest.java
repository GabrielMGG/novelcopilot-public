package com.novelcopilot.backend.dto;

public record UpdateStoryRequest(
        String title,
        String genre,
        String tone,
        String summary,
        String mainIdea,
        String status,
        String coverImagePath) {
}
