package com.novelcopilot.backend.benchmark.repository;

import com.novelcopilot.backend.benchmark.entity.AiBenchmarkEvaluation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AiBenchmarkEvaluationRepository extends JpaRepository<AiBenchmarkEvaluation, Long> {
    Optional<AiBenchmarkEvaluation> findByResponseIdAndEvaluatorId(Long responseId, String evaluatorId);
}
