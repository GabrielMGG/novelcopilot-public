package com.novelcopilot.backend.controller;

import com.novelcopilot.backend.entity.StoryReference;
import com.novelcopilot.backend.repository.StoryReferenceRepository;
import com.novelcopilot.backend.service.StoryAccessService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
public class StoryReferenceController {

    private final StoryReferenceRepository storyReferenceRepository;
    private final StoryAccessService storyAccessService;

    public StoryReferenceController(
            StoryReferenceRepository storyReferenceRepository,
            StoryAccessService storyAccessService
    ) {
        this.storyReferenceRepository = storyReferenceRepository;
        this.storyAccessService = storyAccessService;
    }

    @GetMapping("/stories/{storyId}/references")
    public ResponseEntity<List<StoryReference>> listReferencesByStory(@PathVariable Integer storyId) {
        storyAccessService.requireStoryAccess(storyId);
        return ResponseEntity.ok(storyReferenceRepository.findByStoryId(storyId));
    }

    @PostMapping("/stories/{storyId}/references")
    public ResponseEntity<StoryReference> createReference(
            @PathVariable Integer storyId,
            @RequestBody StoryReference reference
    ) {
        storyAccessService.requireStoryAccess(storyId);
        
        reference.setStoryId(storyId);
        reference.setCreatedAt(LocalDateTime.now());
        reference.setUpdatedAt(LocalDateTime.now());

        return ResponseEntity.ok(storyReferenceRepository.save(reference));
    }

    @GetMapping("/references/{id}")
    public ResponseEntity<StoryReference> getReferenceById(@PathVariable Integer id) {
        storyAccessService.requireReferenceAccess(id);
        return storyReferenceRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/references/{id}")
    public ResponseEntity<StoryReference> updateReference(
            @PathVariable Integer id,
            @RequestBody StoryReference updatedReference
    ) {
        storyAccessService.requireReferenceAccess(id);
        return storyReferenceRepository.findById(id)
                .map(reference -> {
                    reference.setTitle(updatedReference.getTitle());
                    reference.setReferenceType(updatedReference.getReferenceType());
                    reference.setWhatILike(updatedReference.getWhatILike());
                    reference.setInspirationPoints(updatedReference.getInspirationPoints());
                    reference.setAvoidCopying(updatedReference.getAvoidCopying());
                    reference.setNotes(updatedReference.getNotes());
                    reference.setUpdatedAt(LocalDateTime.now());

                    return ResponseEntity.ok(storyReferenceRepository.save(reference));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/references/{id}")
    public ResponseEntity<Void> deleteReference(@PathVariable Integer id) {
        storyAccessService.requireReferenceAccess(id);
        return storyReferenceRepository.findById(id)
                .map(reference -> {
                    storyReferenceRepository.delete(reference);
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
}