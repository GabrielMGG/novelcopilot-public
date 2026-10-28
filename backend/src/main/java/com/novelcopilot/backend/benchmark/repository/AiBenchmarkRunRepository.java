package com.novelcopilot.backend.benchmark.repository;

import com.novelcopilot.backend.benchmark.entity.AiBenchmarkRun;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AiBenchmarkRunRepository extends JpaRepository<AiBenchmarkRun, Long> {
    List<AiBenchmarkRun> findByCreatedAtGreaterThanEqual(LocalDateTime start);
}
