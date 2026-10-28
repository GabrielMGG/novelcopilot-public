package com.novelcopilot.backend.repository;

import com.novelcopilot.backend.entity.AiGeneration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AiGenerationRepository extends JpaRepository<AiGeneration, Integer> {

    List<AiGeneration> findByStoryId(Integer storyId);

    List<AiGeneration> findByChapterId(Integer chapterId);

    void deleteByStoryId(Integer storyId);
}