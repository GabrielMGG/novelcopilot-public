package com.novelcopilot.backend.repository;

import com.novelcopilot.backend.entity.QuotaProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QuotaProfileRepository extends JpaRepository<QuotaProfile, String> {
}
