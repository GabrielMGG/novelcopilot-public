package com.novelcopilot.backend.repository;

import com.novelcopilot.backend.entity.CreditCycle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface CreditCycleRepository extends JpaRepository<CreditCycle, Long> {
    Optional<CreditCycle> findByUserIdAndCycleStartAndQuotaProfileCode(String userId, LocalDateTime cycleStart, String quotaProfileCode);
}
