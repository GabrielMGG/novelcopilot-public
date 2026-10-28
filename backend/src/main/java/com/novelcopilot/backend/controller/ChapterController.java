package com.novelcopilot.backend.controller;

import com.novelcopilot.backend.dto.CreateChapterRequest;
import com.novelcopilot.backend.dto.UpdateChapterRequest;
import com.novelcopilot.backend.entity.Chapter;
import com.novelcopilot.backend.service.ChapterService;
import com.novelcopilot.backend.service.StoryAccessService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ChapterController {

    private final ChapterService chapterService;
    private final StoryAccessService storyAccessService;

    public ChapterController(
            ChapterService chapterService,
            StoryAccessService storyAccessService
    ) {
        this.chapterService = chapterService;
        this.storyAccessService = storyAccessService;
    }

    @GetMapping("/stories/{storyId}/chapters")
    public ResponseEntity<List<Chapter>> listChaptersByStory(@PathVariable Integer storyId) {
        storyAccessService.requireStoryAccess(storyId);
        return ResponseEntity.ok(chapterService.listChaptersByStory(storyId));
    }

    @PostMapping("/stories/{storyId}/chapters")
    public ResponseEntity<Chapter> createChapter(
            @PathVariable Integer storyId,
            @RequestBody CreateChapterRequest request
    ) {
        storyAccessService.requireStoryAccess(storyId);
        return ResponseEntity.ok(chapterService.createChapter(storyId, request));
    }

    @GetMapping("/chapters/{id}")
    public ResponseEntity<Chapter> getChapterById(@PathVariable Integer id) {
        storyAccessService.requireChapterAccess(id);
        return chapterService.getChapterById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/chapters/{id}")
    public ResponseEntity<Chapter> updateChapter(
            @PathVariable Integer id,
            @RequestBody UpdateChapterRequest request
    ) {
        storyAccessService.requireChapterAccess(id);
        return chapterService.updateChapter(id, request)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/chapters/{id}")
    public ResponseEntity<Void> deleteChapter(@PathVariable Integer id) {
        storyAccessService.requireChapterAccess(id);
        chapterService.deleteChapter(id);
        return ResponseEntity.noContent().build();
    }
}