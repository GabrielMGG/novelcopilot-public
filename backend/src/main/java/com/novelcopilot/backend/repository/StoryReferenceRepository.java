package com.novelcopilot.backend.repository;

import com.novelcopilot.backend.entity.StoryReference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StoryReferenceRepository extends JpaRepository<StoryReference, Integer> {

    List<StoryReference> findByStoryId(Integer storyId);

    void deleteByStoryId(Integer storyId);
}