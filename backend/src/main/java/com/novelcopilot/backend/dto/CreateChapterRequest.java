package com.novelcopilot.backend.dto;

public record CreateChapterRequest(
        String title,
        Integer chapterOrder,
        String type,
        String goal,
        String summary,
        String content
) {
}