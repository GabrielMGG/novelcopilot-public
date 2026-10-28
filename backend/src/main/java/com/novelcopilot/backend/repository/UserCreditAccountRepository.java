package com.novelcopilot.backend.repository;

import com.novelcopilot.backend.entity.UserCreditAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserCreditAccountRepository extends JpaRepository<UserCreditAccount, Long> {
    Optional<UserCreditAccount> findByUserId(String userId);
}
