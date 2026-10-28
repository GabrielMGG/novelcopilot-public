package com.novelcopilot.backend.repository;

import com.novelcopilot.backend.entity.AiUsageRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface AiUsageRecordRepository extends JpaRepository<AiUsageRecord, Long> {

    List<AiUsageRecord> findByUserId(String userId);

    List<AiUsageRecord> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    List<AiUsageRecord> findByUserIdAndCreatedAtBetween(String userId, LocalDateTime start, LocalDateTime end);
}
