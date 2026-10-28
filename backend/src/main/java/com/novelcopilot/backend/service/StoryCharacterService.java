package com.novelcopilot.backend.service;

import com.novelcopilot.backend.dto.CreateStoryCharacterRequest;
import com.novelcopilot.backend.dto.UpdateStoryCharacterRequest;
import com.novelcopilot.backend.entity.StoryCharacter;
import com.novelcopilot.backend.repository.StoryCharacterRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class StoryCharacterService {

    private final StoryCharacterRepository storyCharacterRepository;

    public StoryCharacterService(StoryCharacterRepository storyCharacterRepository) {
        this.storyCharacterRepository = storyCharacterRepository;
    }

    public List<StoryCharacter> listCharactersByStory(Integer storyId) {
        return storyCharacterRepository.findByStoryId(storyId);
    }

    public Optional<StoryCharacter> getCharacterById(Integer id) {
        return storyCharacterRepository.findById(id);
    }

    public StoryCharacter createCharacter(Integer storyId, CreateStoryCharacterRequest request) {
        StoryCharacter character = new StoryCharacter();

        character.setStoryId(storyId);
        character.setName(request.name());
        character.setRole(request.role());
        character.setImportance(request.importance());
        character.setNarrativeRole(request.narrativeRole());
        character.setDescription(request.description());
        character.setAppearance(request.appearance());
        character.setPersonality(request.personality());
        character.setGoal(request.goal());
        character.setBackstory(request.backstory());
        character.setConflict(request.conflict());
        character.setRelationships(request.relationships());
        character.setNotes(request.notes());
        character.setImageUrl(request.imageUrl());
        character.setImagePrompt(request.imagePrompt());
        character.setCreatedAt(LocalDateTime.now());
        character.setUpdatedAt(LocalDateTime.now());

        return storyCharacterRepository.save(character);
    }

    public Optional<StoryCharacter> updateCharacter(Integer id, UpdateStoryCharacterRequest request) {
        return storyCharacterRepository.findById(id)
                .map(character -> {
                    character.setName(request.name());
                    character.setRole(request.role());
                    character.setImportance(request.importance());
                    character.setNarrativeRole(request.narrativeRole());
                    character.setDescription(request.description());
                    character.setAppearance(request.appearance());
                    character.setPersonality(request.personality());
                    character.setGoal(request.goal());
                    character.setBackstory(request.backstory());
                    character.setConflict(request.conflict());
                    character.setRelationships(request.relationships());
                    character.setNotes(request.notes());
                    character.setImageUrl(request.imageUrl());
                    character.setImagePrompt(request.imagePrompt());
                    character.setUpdatedAt(LocalDateTime.now());

                    return storyCharacterRepository.save(character);
                });
    }

    public boolean deleteCharacter(Integer id) {
        if (!storyCharacterRepository.existsById(id)) {
            return false;
        }

        storyCharacterRepository.deleteById(id);
        return true;
    }
}