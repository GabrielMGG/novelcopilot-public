package com.novelcopilot.backend.service;

import com.novelcopilot.backend.entity.AiGeneration;
import com.novelcopilot.backend.entity.Chapter;
import com.novelcopilot.backend.entity.StoryCharacter;
import com.novelcopilot.backend.entity.StoryReference;
import com.novelcopilot.backend.entity.WorldNote;
import com.novelcopilot.backend.repository.AiGenerationRepository;
import com.novelcopilot.backend.repository.ChapterRepository;
import com.novelcopilot.backend.repository.StoryCharacterRepository;
import com.novelcopilot.backend.repository.StoryReferenceRepository;
import com.novelcopilot.backend.repository.StoryRepository;
import com.novelcopilot.backend.repository.WorldNoteRepository;
import com.novelcopilot.backend.security.AuthenticatedUserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class StoryAccessService {

    private final CurrentUserService currentUserService;
    private final StoryRepository storyRepository;
    private final ChapterRepository chapterRepository;
    private final StoryCharacterRepository storyCharacterRepository;
    private final WorldNoteRepository worldNoteRepository;
    private final StoryReferenceRepository storyReferenceRepository;
    private final AiGenerationRepository aiGenerationRepository;

    public StoryAccessService(
            CurrentUserService currentUserService,
            StoryRepository storyRepository,
            ChapterRepository chapterRepository,
            StoryCharacterRepository storyCharacterRepository,
            WorldNoteRepository worldNoteRepository,
            StoryReferenceRepository storyReferenceRepository,
            AiGenerationRepository aiGenerationRepository) {
        this.currentUserService = currentUserService;
        this.storyRepository = storyRepository;
        this.chapterRepository = chapterRepository;
        this.storyCharacterRepository = storyCharacterRepository;
        this.worldNoteRepository = worldNoteRepository;
        this.storyReferenceRepository = storyReferenceRepository;
        this.aiGenerationRepository = aiGenerationRepository;
    }

    public boolean currentUserOwnsStory(Integer storyId) {
        AuthenticatedUserPrincipal currentUser = currentUserService.getCurrentUser();
        return storyRepository.existsByIdAndUserId(storyId, currentUser.getId());
    }

    public void requireStoryAccess(Integer storyId) {
        if (!currentUserOwnsStory(storyId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Story não encontrada");
        }
    }

    public void requireChapterAccess(Integer chapterId) {
        Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chapter não encontrado"));
        requireStoryAccess(chapter.getStoryId());
    }

    public void requireCharacterAccess(Integer characterId) {
        StoryCharacter character = storyCharacterRepository.findById(characterId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Character não encontrado"));
        requireStoryAccess(character.getStoryId());
    }

    public void requireWorldNoteAccess(Integer worldNoteId) {
        WorldNote worldNote = worldNoteRepository.findById(worldNoteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "WorldNote não encontrada"));
        requireStoryAccess(worldNote.getStoryId());
    }

    public void requireReferenceAccess(Integer referenceId) {
        StoryReference reference = storyReferenceRepository.findById(referenceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reference não encontrada"));
        requireStoryAccess(reference.getStoryId());
    }

    public void requireAiGenerationAccess(Integer aiGenerationId) {
        AiGeneration aiGeneration = aiGenerationRepository.findById(aiGenerationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "AiGeneration não encontrada"));
        requireStoryAccess(aiGeneration.getStoryId());
    }
}
