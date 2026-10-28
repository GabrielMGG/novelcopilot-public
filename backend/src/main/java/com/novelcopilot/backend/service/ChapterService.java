package com.novelcopilot.backend.service;

import com.novelcopilot.backend.dto.CreateChapterRequest;
import com.novelcopilot.backend.dto.UpdateChapterRequest;
import com.novelcopilot.backend.entity.Chapter;
import com.novelcopilot.backend.repository.ChapterRepository;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ChapterService {

    private final ChapterRepository chapterRepository;

    public ChapterService(ChapterRepository chapterRepository) {
        this.chapterRepository = chapterRepository;
    }

    public List<Chapter> listChaptersByStory(Integer storyId) {
        return chapterRepository.findByStoryId(storyId);
    }

    public Optional<Chapter> getChapterById(Integer id) {
        return chapterRepository.findById(id);
    }

    public Chapter createChapter(Integer storyId, CreateChapterRequest request) {
        Chapter chapter = new Chapter();

        chapter.setStoryId(storyId);
        chapter.setTitle(request.title());
        chapter.setChapterOrder(request.chapterOrder());
        chapter.setType(request.type());
        chapter.setGoal(request.goal());
        chapter.setSummary(request.summary());
        chapter.setContent(request.content());
        chapter.setCreatedAt(LocalDateTime.now());
        chapter.setUpdatedAt(LocalDateTime.now());

        if (chapter.getType() == null || chapter.getType().isBlank()) {
            chapter.setType("chapter");
        }

        return chapterRepository.save(chapter);
    }

    @Transactional
    public Optional<Chapter> updateChapter(
            Integer id,
            UpdateChapterRequest request
    ) {
        if (request.version() == null) {
            throw new IllegalArgumentException("A versão do capítulo é obrigatória.");
        }

        return chapterRepository.findById(id)
                .map(chapter -> {
                    if (!request.version().equals(chapter.getVersion())) {
                        throw new ChapterVersionConflictException(
                                id,
                                request.version(),
                                chapter.getVersion()
                        );
                    }

                    chapter.setTitle(request.title());
                    chapter.setChapterOrder(request.chapterOrder());
                    chapter.setType(request.type());
                    chapter.setGoal(request.goal());
                    chapter.setSummary(request.summary());
                    chapter.setContent(request.content());
                    chapter.setUpdatedAt(LocalDateTime.now());

                    try {
                        return chapterRepository.saveAndFlush(chapter);
                    } catch (ObjectOptimisticLockingFailureException exception) {
                        throw new ChapterVersionConflictException(
                                id,
                                request.version(),
                                null,
                                exception
                        );
                    }
                });
    }

    public boolean deleteChapter(Integer id) {
        if (!chapterRepository.existsById(id)) {
            return false;
        }

        chapterRepository.deleteById(id);
        return true;
    }
}
