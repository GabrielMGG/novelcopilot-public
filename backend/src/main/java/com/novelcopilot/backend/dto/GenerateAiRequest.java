package com.novelcopilot.backend.dto;

public record GenerateAiRequest(
        Integer chapterId,
        String actionType,
        String instruction,
        String currentText,
        String selectedText
){
}
