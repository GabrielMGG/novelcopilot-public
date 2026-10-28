package com.novelcopilot.backend.repository;

import com.novelcopilot.backend.entity.CreditTransaction;
import com.novelcopilot.backend.entity.CreditTransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;

@Repository
public interface CreditTransactionRepository extends JpaRepository<CreditTransaction, Long> {
    Optional<CreditTransaction> findByUserIdAndIdempotencyKey(String userId, String idempotencyKey);
    Page<CreditTransaction> findAllByUserIdOrderByCreatedAtDescIdDesc(
            String userId,
            Pageable pageable
    );
    Page<CreditTransaction> findAllByUserIdAndTypeNotInOrderByCreatedAtDescIdDesc(
            String userId,
            Collection<CreditTransactionType> excludedTypes,
            Pageable pageable
    );
}
