package com.novelcopilot.backend.repository;

import com.novelcopilot.backend.entity.AiRateLimitCounter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface AiRateLimitCounterRepository extends JpaRepository<AiRateLimitCounter, Long> {

    Optional<AiRateLimitCounter> findByUserIdAndScopeAndWindowStartedAt(
            String userId,
            String scope,
            LocalDateTime windowStartedAt
    );

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update AiRateLimitCounter counter
               set counter.requestCount = counter.requestCount + 1,
                   counter.updatedAt = :updatedAt
             where counter.id = :id
               and counter.requestCount < :maximum
            """)
    int incrementIfBelowLimit(
            @Param("id") Long id,
            @Param("maximum") int maximum,
            @Param("updatedAt") LocalDateTime updatedAt
    );

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            delete from AiRateLimitCounter counter
             where counter.userId = :userId
               and counter.scope = :scope
               and counter.windowStartedAt < :currentWindow
            """)
    int deleteExpiredWindows(
            @Param("userId") String userId,
            @Param("scope") String scope,
            @Param("currentWindow") LocalDateTime currentWindow
    );
}
