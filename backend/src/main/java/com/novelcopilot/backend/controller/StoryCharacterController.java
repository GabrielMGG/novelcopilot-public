package com.novelcopilot.backend.controller;

import com.novelcopilot.backend.dto.CreateStoryCharacterRequest;
import com.novelcopilot.backend.dto.UpdateStoryCharacterRequest;
import com.novelcopilot.backend.entity.StoryCharacter;
import com.novelcopilot.backend.service.StoryAccessService;
import com.novelcopilot.backend.service.StoryCharacterService;
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
public class StoryCharacterController {

    private final StoryCharacterService storyCharacterService;
    private final StoryAccessService storyAccessService;

    public StoryCharacterController(
            StoryCharacterService storyCharacterService,
            StoryAccessService storyAccessService
    ) {
        this.storyCharacterService = storyCharacterService;
        this.storyAccessService = storyAccessService;
    }

    @GetMapping("/stories/{storyId}/characters")
    public List<StoryCharacter> listCharactersByStory(@PathVariable Integer storyId) {
        storyAccessService.requireStoryAccess(storyId);
        return storyCharacterService.listCharactersByStory(storyId);
    }

    @PostMapping("/stories/{storyId}/characters")
    public StoryCharacter createCharacter(
            @PathVariable Integer storyId,
            @RequestBody CreateStoryCharacterRequest request
    ) {
        storyAccessService.requireStoryAccess(storyId);
        return storyCharacterService.createCharacter(storyId, request);
    }

    @GetMapping("/characters/{id}")
    public ResponseEntity<StoryCharacter> getCharacterById(@PathVariable Integer id) {
        storyAccessService.requireCharacterAccess(id);
        return storyCharacterService.getCharacterById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/characters/{id}")
    public ResponseEntity<StoryCharacter> updateCharacter(
            @PathVariable Integer id,
            @RequestBody UpdateStoryCharacterRequest request
    ) {
        storyAccessService.requireCharacterAccess(id);
        return storyCharacterService.updateCharacter(id, request)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/characters/{id}")
    public ResponseEntity<Void> deleteCharacter(@PathVariable Integer id) {
        storyAccessService.requireCharacterAccess(id);
        boolean deleted = storyCharacterService.deleteCharacter(id);

        if (!deleted) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.noContent().build();
    }
}
