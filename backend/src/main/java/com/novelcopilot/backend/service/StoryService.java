package com.novelcopilot.backend.service;

import com.novelcopilot.backend.dto.CreateStoryRequest;
import com.novelcopilot.backend.dto.UpdateStoryRequest;
import com.novelcopilot.backend.entity.Chapter;
import com.novelcopilot.backend.entity.Story;
import com.novelcopilot.backend.repository.AiGenerationRepository;
import com.novelcopilot.backend.repository.ChapterRepository;
import com.novelcopilot.backend.repository.StoryCharacterRepository;
import com.novelcopilot.backend.repository.StoryReferenceRepository;
import com.novelcopilot.backend.repository.StoryRepository;
import com.novelcopilot.backend.repository.WorldNoteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class StoryService {

    private final StoryRepository storyRepository;
    private final ChapterRepository chapterRepository;
    private final StoryCharacterRepository storyCharacterRepository;
    private final WorldNoteRepository worldNoteRepository;
    private final StoryReferenceRepository storyReferenceRepository;
    private final AiGenerationRepository aiGenerationRepository;

    public StoryService(
            StoryRepository storyRepository,
            ChapterRepository chapterRepository,
            StoryCharacterRepository storyCharacterRepository,
            WorldNoteRepository worldNoteRepository,
            StoryReferenceRepository storyReferenceRepository,
            AiGenerationRepository aiGenerationRepository
    ) {
        this.storyRepository = storyRepository;
        this.chapterRepository = chapterRepository;
        this.storyCharacterRepository = storyCharacterRepository;
        this.worldNoteRepository = worldNoteRepository;
        this.storyReferenceRepository = storyReferenceRepository;
        this.aiGenerationRepository = aiGenerationRepository;
    }

    public List<Story> listStoriesByUser(String userId) {
        return storyRepository.findByUserIdAndDeletedAtIsNull(userId);
    }

    public List<Story> listTrashStoriesByUser(String userId) {
        return storyRepository.findByUserIdAndDeletedAtIsNotNullOrderByDeletedAtDesc(userId);
    }

    public Optional<Story> getStoryById(Integer id) {
        return storyRepository.findById(id);
    }

    @Transactional
    public Story createStory(String userId, CreateStoryRequest request) {
        LocalDateTime now = LocalDateTime.now();

        Story story = new Story();

        story.setUserId(userId);
        story.setTitle(request.title());
        story.setGenre(request.genre());
        story.setTone(request.tone());
        story.setSummary(request.summary());
        story.setMainIdea(request.mainIdea());
        story.setCoverImagePath(normalizeCoverImageUrl(request.coverImagePath()));
        story.setStatus("draft");
        story.setCreatedAt(now);
        story.setUpdatedAt(now);

        Story savedStory = storyRepository.save(story);

        createInitialChapter(savedStory.getId(), now);

        return savedStory;
    }

    public Optional<Story> updateStory(
            Integer id,
            UpdateStoryRequest request
    ) {
        return storyRepository.findById(id)
                .map(story -> {
                    story.setTitle(request.title());
                    story.setGenre(request.genre());
                    story.setTone(request.tone());
                    story.setSummary(request.summary());
                    story.setMainIdea(request.mainIdea());
                    story.setCoverImagePath(normalizeCoverImageUrl(request.coverImagePath()));
                    story.setStatus(request.status());
                    story.setUpdatedAt(LocalDateTime.now());

                    return storyRepository.save(story);
                });
    }

    @Transactional
    public boolean moveStoryToTrash(Integer id) {
        return storyRepository.findById(id)
                .map(story -> {
                    story.setDeletedAt(LocalDateTime.now());
                    story.setUpdatedAt(LocalDateTime.now());
                    storyRepository.save(story);
                    return true;
                })
                .orElse(false);
    }

    @Transactional
    public Optional<Story> restoreStory(Integer id) {
        return storyRepository.findById(id)
                .filter(story -> story.getDeletedAt() != null)
                .map(story -> {
                    story.setDeletedAt(null);
                    story.setUpdatedAt(LocalDateTime.now());
                    return storyRepository.save(story);
                });
    }

    @Transactional
    public boolean permanentlyDeleteStory(Integer id) {
        return storyRepository.findById(id)
                .filter(story -> story.getDeletedAt() != null)
                .map(story -> {
                    aiGenerationRepository.deleteByStoryId(story.getId());
                    chapterRepository.deleteByStoryId(story.getId());
                    storyCharacterRepository.deleteByStoryId(story.getId());
                    worldNoteRepository.deleteByStoryId(story.getId());
                    storyReferenceRepository.deleteByStoryId(story.getId());
                    storyRepository.delete(story);
                    return true;
                })
                .orElse(false);
    }

    private void createInitialChapter(Integer storyId, LocalDateTime now) {
        Chapter chapter = new Chapter();

        chapter.setStoryId(storyId);
        chapter.setTitle("Capítulo 1");
        chapter.setChapterOrder(1);
        chapter.setType("chapter");
        chapter.setGoal("");
        chapter.setSummary("");
        chapter.setContent("");
        chapter.setCreatedAt(now);
        chapter.setUpdatedAt(now);

        chapterRepository.save(chapter);
    }

    private String normalizeCoverImageUrl(String coverImageUrl) {
        if (coverImageUrl == null || coverImageUrl.isBlank()) {
            return null;
        }
        return coverImageUrl.trim();
    }
}