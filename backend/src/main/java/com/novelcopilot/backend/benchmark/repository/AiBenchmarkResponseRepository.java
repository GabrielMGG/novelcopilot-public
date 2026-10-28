package com.novelcopilot.backend.benchmark.repository;

import com.novelcopilot.backend.benchmark.entity.AiBenchmarkResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AiBenchmarkResponseRepository extends JpaRepository<AiBenchmarkResponse, Long> {
    List<AiBenchmarkResponse> findByRunId(Long runId);
}
