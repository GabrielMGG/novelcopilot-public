package com.novelcopilot.backend.repository;

import com.novelcopilot.backend.entity.AiGenerationUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AiGenerationUsageRepository extends JpaRepository<AiGenerationUsage, Long> {
    Optional<AiGenerationUsage> findByUserIdAndIdempotencyKey(String userId, String idempotencyKey);
}
