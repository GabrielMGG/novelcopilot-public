package com.novelcopilot.backend.repository;

import com.novelcopilot.backend.entity.Story;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StoryRepository extends JpaRepository<Story, Integer> {
    List<Story> findByUserId(String userId);

    List<Story> findByUserIdAndDeletedAtIsNull(String userId);

    List<Story> findByUserIdAndDeletedAtIsNotNull(String userId);

    List<Story> findByUserIdAndDeletedAtIsNotNullOrderByDeletedAtDesc(String userId);

    Optional<Story> findByIdAndUserId(Integer id, String userId);

    boolean existsByIdAndUserId(Integer id, String userId);
}