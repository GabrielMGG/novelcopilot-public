package com.novelcopilot.backend.repository;

import com.novelcopilot.backend.entity.StoryCharacter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StoryCharacterRepository extends JpaRepository<StoryCharacter, Integer> {

    List<StoryCharacter> findByStoryId(Integer storyId);
    List<StoryCharacter> findByStoryIdAndIdIn(Integer storyId, List<Integer> ids);
    java.util.Optional<StoryCharacter> findByIdAndStoryId(Integer id, Integer storyId);

    void deleteByStoryId(Integer storyId);
}
