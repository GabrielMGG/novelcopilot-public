package com.novelcopilot.backend.service;

import com.novelcopilot.backend.dto.CreateWorldNoteRequest;
import com.novelcopilot.backend.dto.UpdateWorldNoteRequest;
import com.novelcopilot.backend.entity.WorldNote;
import com.novelcopilot.backend.repository.WorldNoteRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class WorldNoteService {

    private final WorldNoteRepository worldNoteRepository;

    public WorldNoteService(WorldNoteRepository worldNoteRepository) {
        this.worldNoteRepository = worldNoteRepository;
    }

    public List<WorldNote> listWorldNotesByStory(Integer storyId) {
        return worldNoteRepository.findByStoryId(storyId);
    }

    public Optional<WorldNote> getWorldNoteById(Integer id) {
        return worldNoteRepository.findById(id);
    }

    public WorldNote createWorldNote(Integer storyId, CreateWorldNoteRequest request) {
        WorldNote worldNote = new WorldNote();

        worldNote.setStoryId(storyId);
        worldNote.setTitle(request.title());
        worldNote.setCategory(textOrDefault(request.category(), "OTHER"));
        worldNote.setImportance(textOrDefault(request.importance(), "MEDIUM"));
        worldNote.setStatus(textOrDefault(request.status(), "CANON"));
        worldNote.setTags(request.tags());
        worldNote.setContent(request.content());
        worldNote.setCreatedAt(LocalDateTime.now());
        worldNote.setUpdatedAt(LocalDateTime.now());

        return worldNoteRepository.save(worldNote);
    }

    public Optional<WorldNote> updateWorldNote(Integer id, UpdateWorldNoteRequest request) {
        return worldNoteRepository.findById(id)
                .map(worldNote -> {
                    worldNote.setTitle(request.title());
                    worldNote.setCategory(textOrDefault(request.category(), "OTHER"));
                    worldNote.setImportance(textOrDefault(request.importance(), "MEDIUM"));
                    worldNote.setStatus(textOrDefault(request.status(), "CANON"));
                    worldNote.setTags(request.tags());
                    worldNote.setContent(request.content());
                    worldNote.setUpdatedAt(LocalDateTime.now());

                    return worldNoteRepository.save(worldNote);
                });
    }

    public boolean deleteWorldNote(Integer id) {
        if (!worldNoteRepository.existsById(id)) {
            return false;
        }

        worldNoteRepository.deleteById(id);
        return true;
    }

    private String textOrDefault(String value, String defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }

        return value;
    }
}