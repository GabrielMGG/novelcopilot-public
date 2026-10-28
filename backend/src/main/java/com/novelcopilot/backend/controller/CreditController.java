package com.novelcopilot.backend.controller;

import com.novelcopilot.backend.entity.CreditTransaction;
import com.novelcopilot.backend.entity.CreditTransactionType;
import com.novelcopilot.backend.entity.UserCreditAccount;
import com.novelcopilot.backend.repository.CreditTransactionRepository;
import com.novelcopilot.backend.repository.UserCreditAccountRepository;
import com.novelcopilot.backend.security.AuthenticatedUserPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/credits")
public class CreditController {

    private static final Set<CreditTransactionType> TECHNICAL_TRANSACTION_TYPES =
            Set.of(CreditTransactionType.RESERVE, CreditTransactionType.RELEASE);

    private final UserCreditAccountRepository accountRepository;
    private final CreditTransactionRepository transactionRepository;

    public CreditController(UserCreditAccountRepository accountRepository, CreditTransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    @GetMapping("/balance")
    public ResponseEntity<?> getBalance(
            @AuthenticationPrincipal AuthenticatedUserPrincipal user) {
        UserCreditAccount account = accountRepository.findByUserId(user.getId()).orElse(null);
        if (account == null) {
            return ResponseEntity.notFound().build();
        }
        Map<String, Object> response = new HashMap<>();
        response.put("availableCredits", account.getAvailableCredits());
        response.put("reservedCredits", account.getReservedCredits());
        response.put("currentQuotaProfile", account.getCurrentQuotaProfileCode());
        response.put("cycleStart", account.getCurrentCycleStart());
        response.put("cycleEnd", account.getCurrentCycleEnd());
        response.put("updatedAt", account.getUpdatedAt());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/transactions")
    public ResponseEntity<Page<Map<String, Object>>> getTransactions(
            @AuthenticationPrincipal AuthenticatedUserPrincipal user,
            Pageable pageable) {
        Page<CreditTransaction> txs =
                transactionRepository.findAllByUserIdAndTypeNotInOrderByCreatedAtDescIdDesc(
                        user.getId(),
                        TECHNICAL_TRANSACTION_TYPES,
                        pageable
                );

        Page<Map<String, Object>> response = txs.map(tx -> {
            Map<String, Object> map = new HashMap<>();
            map.put("date", tx.getCreatedAt());
            map.put("type", tx.getType());
            map.put("status", tx.getStatus());
            map.put("credits", tx.getCredits());
            map.put("reason", tx.getReason());
            return map;
        });

        return ResponseEntity.ok(response);
    }
}
