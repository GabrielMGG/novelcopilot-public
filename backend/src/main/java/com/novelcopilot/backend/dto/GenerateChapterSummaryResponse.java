package com.novelcopilot.backend.dto;

import java.time.LocalDateTime;

public record GenerateChapterSummaryResponse(
        Integer chapterId,
        String summary,
        Long chapterVersion,
        String summaryContentHash,
        LocalDateTime summaryGeneratedAt
) {
}
