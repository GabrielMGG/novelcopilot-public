package com.novelcopilot.backend.repository;

import com.novelcopilot.backend.entity.ModelPricing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface ModelPricingRepository extends JpaRepository<ModelPricing, Long> {
    
    @Query("SELECT m FROM ModelPricing m WHERE LOWER(m.provider) = LOWER(:provider) AND m.model = :model AND m.active = true AND m.effectiveFrom <= :now AND (m.effectiveUntil IS NULL OR m.effectiveUntil > :now)")
    Optional<ModelPricing> findActivePricing(@Param("provider") String provider, @Param("model") String model, @Param("now") LocalDateTime now);
}
