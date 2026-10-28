package com.novelcopilot.backend.controller;

import com.novelcopilot.backend.dto.CreateStoryRequest;
import com.novelcopilot.backend.dto.UpdateStoryRequest;
import com.novelcopilot.backend.entity.Story;
import com.novelcopilot.backend.security.AuthenticatedUserPrincipal;
import com.novelcopilot.backend.service.CurrentUserService;
import com.novelcopilot.backend.service.StoryAccessService;
import com.novelcopilot.backend.service.StoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/stories")
public class StoryController {

    private final StoryService storyService;
    private final CurrentUserService currentUserService;
    private final StoryAccessService storyAccessService;

    public StoryController(
            StoryService storyService,
            CurrentUserService currentUserService,
            StoryAccessService storyAccessService
    ) {
        this.storyService = storyService;
        this.currentUserService = currentUserService;
        this.storyAccessService = storyAccessService;
    }

    @GetMapping
    public List<Story> listStories() {
        AuthenticatedUserPrincipal currentUser = currentUserService.getCurrentUser();
        return storyService.listStoriesByUser(currentUser.getId());
    }

    @GetMapping("/trash")
    public List<Story> listTrashStories() {
        AuthenticatedUserPrincipal currentUser = currentUserService.getCurrentUser();
        return storyService.listTrashStoriesByUser(currentUser.getId());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Story> getStoryById(@PathVariable Integer id) {
        storyAccessService.requireStoryAccess(id);
        return storyService.getStoryById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Story createStory(@RequestBody CreateStoryRequest request) {
        AuthenticatedUserPrincipal currentUser = currentUserService.getCurrentUser();
        return storyService.createStory(currentUser.getId(), request);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Story> updateStory(
            @PathVariable Integer id,
            @RequestBody UpdateStoryRequest request
    ) {
        storyAccessService.requireStoryAccess(id);
        return storyService.updateStory(id, request)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStory(@PathVariable Integer id) {
        storyAccessService.requireStoryAccess(id);
        if (storyService.moveStoryToTrash(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @PatchMapping("/{id}/restore")
    public ResponseEntity<Story> restoreStory(@PathVariable Integer id) {
        storyAccessService.requireStoryAccess(id);
        return storyService.restoreStory(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}/permanent")
    public ResponseEntity<Void> permanentlyDeleteStory(@PathVariable Integer id) {
        storyAccessService.requireStoryAccess(id);
        if (storyService.permanentlyDeleteStory(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}