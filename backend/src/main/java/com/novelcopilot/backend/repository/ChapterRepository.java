package com.novelcopilot.backend.repository;

import com.novelcopilot.backend.entity.Chapter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChapterRepository extends JpaRepository<Chapter, Integer> {
    List<Chapter> findByStoryId(Integer storyId);
    List<Chapter> findByStoryIdOrderByChapterOrderAsc(Integer storyId);
    java.util.Optional<Chapter> findByIdAndStoryId(Integer id, Integer storyId);

    void deleteByStoryId(Integer storyId);
}

