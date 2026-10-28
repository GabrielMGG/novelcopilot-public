package com.novelcopilot.backend.dto;

public record UpdateChapterRequest(
        String title,
        Integer chapterOrder,
        String type,
        String goal,
        String summary,
        String content,
        Long version
) {
}
