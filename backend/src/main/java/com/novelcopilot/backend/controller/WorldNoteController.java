package com.novelcopilot.backend.controller;

import com.novelcopilot.backend.dto.CreateWorldNoteRequest;
import com.novelcopilot.backend.dto.UpdateWorldNoteRequest;
import com.novelcopilot.backend.entity.WorldNote;
import com.novelcopilot.backend.service.StoryAccessService;
import com.novelcopilot.backend.service.WorldNoteService;
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
public class WorldNoteController {
    private final WorldNoteService worldNoteService;
    private final StoryAccessService storyAccessService;

    public WorldNoteController(
            WorldNoteService worldNoteService,
            StoryAccessService storyAccessService
    ) {
        this.worldNoteService = worldNoteService;
        this.storyAccessService = storyAccessService;
    }

    @GetMapping("/stories/{storyId}/world-notes")
    public ResponseEntity<List<WorldNote>> listWorldNotesByStory(@PathVariable Integer storyId) {
        storyAccessService.requireStoryAccess(storyId);
        return ResponseEntity.ok(worldNoteService.listWorldNotesByStory(storyId));
    }

    @PostMapping("/stories/{storyId}/world-notes")
    public ResponseEntity<WorldNote> createWorldNote(
            @PathVariable Integer storyId,
            @RequestBody CreateWorldNoteRequest request
    ) {
        storyAccessService.requireStoryAccess(storyId);
        return ResponseEntity.ok(worldNoteService.createWorldNote(storyId, request));
    }

    @GetMapping("/world-notes/{id}")
    public ResponseEntity<WorldNote> getWorldNoteById(@PathVariable Integer id) {
        storyAccessService.requireWorldNoteAccess(id);
        return worldNoteService.getWorldNoteById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/world-notes/{id}")
    public ResponseEntity<WorldNote> updateWorldNote(
            @PathVariable Integer id,
            @RequestBody UpdateWorldNoteRequest request
    ) {
        storyAccessService.requireWorldNoteAccess(id);
        return worldNoteService.updateWorldNote(id, request)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/world-notes/{id}")
    public ResponseEntity<Void> deleteWorldNote(@PathVariable Integer id) {
        storyAccessService.requireWorldNoteAccess(id);
        worldNoteService.deleteWorldNote(id);
        return ResponseEntity.noContent().build();
    }
}