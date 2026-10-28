package com.novelcopilot.backend.controller;

import com.novelcopilot.backend.dto.GenerateAiRequest;
import com.novelcopilot.backend.dto.GenerateAiResponse;
import com.novelcopilot.backend.dto.GenerateChapterSummaryResponse;
import com.novelcopilot.backend.entity.AiGeneration;
import com.novelcopilot.backend.repository.AiGenerationRepository;
import com.novelcopilot.backend.service.AiGenerationService;
import com.novelcopilot.backend.service.ChapterService;
import com.novelcopilot.backend.service.StoryAccessService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class AiGenerationController {

    private final AiGenerationRepository aiGenerationRepository;
    private final AiGenerationService aiGenerationService;
    private final ChapterService chapterService;
    private final StoryAccessService storyAccessService;

    public AiGenerationController(
            AiGenerationRepository aiGenerationRepository,
            AiGenerationService aiGenerationService,
            ChapterService chapterService,
            StoryAccessService storyAccessService
    ) {
        this.aiGenerationRepository = aiGenerationRepository;
        this.aiGenerationService = aiGenerationService;
        this.chapterService = chapterService;
        this.storyAccessService = storyAccessService;
    }

    @GetMapping("/stories/{storyId}/ai-generations")
    public ResponseEntity<List<AiGeneration>> listAiGenerationsByStory(@PathVariable Integer storyId) {
        storyAccessService.requireStoryAccess(storyId);
        return ResponseEntity.ok(aiGenerationRepository.findByStoryId(storyId));
    }

    @GetMapping("/chapters/{chapterId}/ai-generations")
    public ResponseEntity<List<AiGeneration>> listAiGenerationsByChapter(@PathVariable Integer chapterId) {
        storyAccessService.requireChapterAccess(chapterId);
        return ResponseEntity.ok(aiGenerationRepository.findByChapterId(chapterId));
    }

    @PostMapping("/stories/{storyId}/ai/generate")
    public ResponseEntity<GenerateAiResponse> generate(
            @PathVariable Integer storyId,
            @RequestBody GenerateAiRequest request
    ) {
        storyAccessService.requireStoryAccess(storyId);

        if (request.chapterId() != null) {
            storyAccessService.requireChapterAccess(request.chapterId());
            boolean chapterBelongsToStory = chapterService.getChapterById(request.chapterId())
                    .map(chapter -> chapter.getStoryId().equals(storyId))
                    .orElse(false);
            if (!chapterBelongsToStory) {
                return ResponseEntity.notFound().build();
            }
        }

        return ResponseEntity.ok(aiGenerationService.generate(storyId, request));
    }

    @PostMapping("/stories/{storyId}/chapters/{chapterId}/summary")
    public ResponseEntity<GenerateChapterSummaryResponse> generateChapterSummary(
            @PathVariable Integer storyId,
            @PathVariable Integer chapterId
    ) {
        storyAccessService.requireStoryAccess(storyId);
        storyAccessService.requireChapterAccess(chapterId);

        boolean chapterBelongsToStory = chapterService.getChapterById(chapterId)
                .map(chapter -> chapter.getStoryId().equals(storyId))
                .orElse(false);

        if (!chapterBelongsToStory) {
            return ResponseEntity.notFound().build();
        }

        GenerateChapterSummaryResponse response =
                aiGenerationService.generateChapterSummary(storyId, chapterId);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/ai-generations/{id}")
    public ResponseEntity<AiGeneration> getAiGenerationById(@PathVariable Integer id) {
        storyAccessService.requireAiGenerationAccess(id);
        return aiGenerationRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/ai-generations/{id}")
    public ResponseEntity<Void> deleteAiGeneration(@PathVariable Integer id) {
        storyAccessService.requireAiGenerationAccess(id);
        return aiGenerationRepository.findById(id)
                .map(aiGeneration -> {
                    aiGenerationRepository.delete(aiGeneration);
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
