package com.novelcopilot.backend.repository;

import com.novelcopilot.backend.entity.AiGenerationHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AiGenerationHistoryRepository extends JpaRepository<AiGenerationHistory, Long> {

    Optional<AiGenerationHistory> findByUsageId(Long usageId);

    Page<AiGenerationHistory> findByUserIdAndStoryIdAndChapterId(
            String userId,
            Integer storyId,
            Integer chapterId,
            Pageable pageable
    );
}
