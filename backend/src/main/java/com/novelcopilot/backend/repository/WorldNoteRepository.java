package com.novelcopilot.backend.repository;

import com.novelcopilot.backend.entity.WorldNote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorldNoteRepository extends JpaRepository<WorldNote, Integer> {

    List<WorldNote> findByStoryId(Integer storyId);
    List<WorldNote> findByStoryIdAndIdIn(Integer storyId, List<Integer> ids);
    java.util.Optional<WorldNote> findByIdAndStoryId(Integer id, Integer storyId);

    void deleteByStoryId(Integer storyId);
}
