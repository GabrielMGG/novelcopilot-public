package com.novelcopilot.backend.dto;

public record SuggestContinuationOption(
        String type,
        String title,
        String summary,
        String snippet
) {
}
