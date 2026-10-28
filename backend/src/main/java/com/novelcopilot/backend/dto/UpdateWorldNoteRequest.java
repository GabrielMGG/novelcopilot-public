package com.novelcopilot.backend.dto;

public record UpdateWorldNoteRequest(
        String title,
        String category,
        String importance,
        String status,
        String tags,
        String content
) {
}