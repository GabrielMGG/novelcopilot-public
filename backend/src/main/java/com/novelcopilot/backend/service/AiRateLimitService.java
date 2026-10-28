package com.novelcopilot.backend.service;

import com.novelcopilot.backend.config.AiRateLimitProperties;
import com.novelcopilot.backend.entity.AiRateLimitCounter;
import com.novelcopilot.backend.repository.AiRateLimitCounterRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Service
public class AiRateLimitService {

    static final String GENERAL_SCOPE = "AI_GENERAL";
    static final String EXPENSIVE_SCOPE = "AI_EXPENSIVE";
    static final String BENCHMARK_RUN_SCOPE = "BENCHMARK_RUN";
    static final String BENCHMARK_RETRY_SCOPE = "BENCHMARK_RETRY";

    private static final int MAX_CONCURRENCY_RETRIES = 10;

    private final AiRateLimitCounterRepository repository;
    private final AiRateLimitProperties properties;
    private final TransactionTemplate transactionTemplate;

    public AiRateLimitService(
            AiRateLimitCounterRepository repository,
            AiRateLimitProperties properties,
            TransactionTemplate transactionTemplate
    ) {
        this.repository = repository;
        this.properties = properties;
        this.transactionTemplate = transactionTemplate;
    }

    public void checkAiGeneration(String userId, boolean expensive) {
        List<Policy> policies = new ArrayList<>();
        policies.add(new Policy(
                GENERAL_SCOPE,
                properties.getGeneralLimit(),
                properties.getGeneralWindowSeconds()
        ));
        if (expensive) {
            policies.add(new Policy(
                    EXPENSIVE_SCOPE,
                    properties.getExpensiveLimit(),
                    properties.getExpensiveWindowSeconds()
            ));
        }
        acquire(userId, policies);
    }

    public void checkBenchmarkRun(String userId) {
        acquire(userId, List.of(new Policy(
                BENCHMARK_RUN_SCOPE,
                properties.getBenchmarkRunLimit(),
                properties.getBenchmarkRunWindowSeconds()
        )));
    }

    public void checkBenchmarkRetry(String userId) {
        acquire(userId, List.of(new Policy(
                BENCHMARK_RETRY_SCOPE,
                properties.getBenchmarkRetryLimit(),
                properties.getBenchmarkRetryWindowSeconds()
        )));
    }

    private void acquire(String userId, List<Policy> policies) {
        if (!properties.isEnabled()) {
            return;
        }
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("O usuário é obrigatório para limitar o uso da IA.");
        }

        Instant now = Instant.now();
        for (int attempt = 1; attempt <= MAX_CONCURRENCY_RETRIES; attempt++) {
            try {
                transactionTemplate.executeWithoutResult(status -> {
                    for (Policy policy : policies) {
                        acquirePolicy(userId, policy, now);
                    }
                });
                return;
            } catch (DataIntegrityViolationException exception) {
                if (attempt == MAX_CONCURRENCY_RETRIES) {
                    throw exception;
                }
                Thread.onSpinWait();
            }
        }
    }

    private void acquirePolicy(String userId, Policy policy, Instant now) {
        int maximum = requirePositive(policy.maximum(), policy.scope() + " limit");
        int windowSeconds = requirePositive(
                policy.windowSeconds(),
                policy.scope() + " window"
        );
        long epochSecond = now.getEpochSecond();
        long windowStartEpoch = epochSecond - Math.floorMod(epochSecond, windowSeconds);
        LocalDateTime windowStartedAt = LocalDateTime.ofInstant(
                Instant.ofEpochSecond(windowStartEpoch),
                ZoneOffset.UTC
        );
        LocalDateTime updatedAt = LocalDateTime.ofInstant(now, ZoneOffset.UTC);

        var existing = repository.findByUserIdAndScopeAndWindowStartedAt(
                userId,
                policy.scope(),
                windowStartedAt
        );

        if (existing.isEmpty()) {
            repository.deleteExpiredWindows(
                    userId,
                    policy.scope(),
                    windowStartedAt
            );
            AiRateLimitCounter counter = new AiRateLimitCounter();
            counter.setUserId(userId);
            counter.setScope(policy.scope());
            counter.setWindowStartedAt(windowStartedAt);
            counter.setRequestCount(1);
            counter.setCreatedAt(updatedAt);
            counter.setUpdatedAt(updatedAt);
            repository.saveAndFlush(counter);
            return;
        }

        int updated = repository.incrementIfBelowLimit(
                existing.get().getId(),
                maximum,
                updatedAt
        );
        if (updated == 0) {
            long retryAfter = Math.max(
                    1,
                    windowStartEpoch + windowSeconds - epochSecond
            );
            throw new AiRateLimitExceededException(policy.scope(), retryAfter);
        }
    }

    private int requirePositive(int value, String property) {
        if (value <= 0) {
            throw new IllegalStateException(property + " deve ser maior que zero.");
        }
        return value;
    }

    private record Policy(String scope, int maximum, int windowSeconds) {
    }
}
