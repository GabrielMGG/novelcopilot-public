package com.novelcopilot.backend.dto;

public record CreateStoryCharacterRequest(
        String name,
        String role,
        String importance,
        String narrativeRole,
        String description,
        String appearance,
        String personality,
        String goal,
        String backstory,
        String conflict,
        String relationships,
        String notes,
        String imageUrl,
        String imagePrompt
) {
}