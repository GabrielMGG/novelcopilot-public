package com.novelcopilot.backend.service;

import com.novelcopilot.backend.entity.*;
import com.novelcopilot.backend.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class CreditService {
    private final UserCreditAccountRepository accountRepository;
    private final CreditCycleRepository cycleRepository;
    private final CreditTransactionRepository transactionRepository;
    private final QuotaProfileRepository quotaProfileRepository;

    public CreditService(UserCreditAccountRepository accountRepository,
                         CreditCycleRepository cycleRepository,
                         CreditTransactionRepository transactionRepository,
                         QuotaProfileRepository quotaProfileRepository) {
        this.accountRepository = accountRepository;
        this.cycleRepository = cycleRepository;
        this.transactionRepository = transactionRepository;
        this.quotaProfileRepository = quotaProfileRepository;
    }

    @Transactional
    public void activateDevelopmentCycle(String userId, String quotaProfileCode) {
        QuotaProfile quotaProfile = quotaProfileRepository.findById(quotaProfileCode)
                .orElseThrow(() -> new IllegalArgumentException("QuotaProfile not found: " + quotaProfileCode));
        
        BigDecimal grantedCredits = quotaProfile.getMonthlyCredits();
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime cycleEnd = now.plusMonths(1);
        
        var cycleOpt = cycleRepository.findByUserIdAndCycleStartAndQuotaProfileCode(userId, now, quotaProfileCode);
        if (cycleOpt.isPresent()) {
            return; // Idempotent
        }

        UserCreditAccount account = accountRepository.findByUserId(userId)
                .orElseGet(() -> {
                    UserCreditAccount newAcc = new UserCreditAccount();
                    newAcc.setUserId(userId);
                    return newAcc;
                });
                
        CreditCycle cycle = new CreditCycle();
        cycle.setUserId(userId);
        cycle.setQuotaProfileCode(quotaProfileCode);
        cycle.setCycleStart(now);
        cycle.setCycleEnd(cycleEnd);
        cycle.setGrantedCredits(grantedCredits);
        cycle.setStatus(CreditCycleStatus.ACTIVE);
        cycle = cycleRepository.save(cycle);
        
        account.setAvailableCredits(account.getAvailableCredits().add(grantedCredits));
        account.setLifetimeGrantedCredits(account.getLifetimeGrantedCredits().add(grantedCredits));
        account.setCurrentQuotaProfileCode(quotaProfileCode);
        account.setCurrentCycleStart(now);
        account.setCurrentCycleEnd(cycleEnd);
        accountRepository.save(account);
        
        CreditTransaction tx = new CreditTransaction();
        tx.setUserId(userId);
        tx.setCycleId(cycle.getId());
        tx.setIdempotencyKey("GRANT_" + cycle.getId() + "_" + now.toEpochSecond(java.time.ZoneOffset.UTC));
        tx.setType(CreditTransactionType.GRANT);
        tx.setStatus(CreditTransactionStatus.COMPLETED);
        tx.setCredits(grantedCredits);
        tx.setBalanceBefore(account.getAvailableCredits().subtract(grantedCredits));
        tx.setBalanceAfter(account.getAvailableCredits());
        tx.setReservedBefore(account.getReservedCredits());
        tx.setReservedAfter(account.getReservedCredits());
        tx.setReason("Monthly grant");
        transactionRepository.save(tx);
    }
    
    @Transactional
    public void reserveCredits(String userId, BigDecimal credits, String idempotencyKey, Long generationUsageId) {
        var existingTx = transactionRepository.findByUserIdAndIdempotencyKey(userId, idempotencyKey);
        if (existingTx.isPresent()) {
            return;
        }

        UserCreditAccount account = accountRepository.findByUserId(userId)
                .orElseThrow(() -> new AiInsufficientCreditsException(
                        credits,
                        BigDecimal.ZERO
                ));
                
        if (account.getAvailableCredits().compareTo(credits) < 0) {
            throw new AiInsufficientCreditsException(
                    credits,
                    account.getAvailableCredits()
            );
        }
        
        BigDecimal balanceBefore = account.getAvailableCredits();
        BigDecimal reservedBefore = account.getReservedCredits();
        
        account.setAvailableCredits(account.getAvailableCredits().subtract(credits));
        account.setReservedCredits(account.getReservedCredits().add(credits));
        accountRepository.save(account);
        
        CreditTransaction tx = new CreditTransaction();
        tx.setUserId(userId);
        tx.setGenerationUsageId(generationUsageId);
        tx.setIdempotencyKey(idempotencyKey);
        tx.setType(CreditTransactionType.RESERVE);
        tx.setStatus(CreditTransactionStatus.COMPLETED);
        tx.setCredits(credits);
        tx.setBalanceBefore(balanceBefore);
        tx.setBalanceAfter(account.getAvailableCredits());
        tx.setReservedBefore(reservedBefore);
        tx.setReservedAfter(account.getReservedCredits());
        transactionRepository.save(tx);
    }
    
    @Transactional
    public void captureCredits(String userId, String idempotencyKey, BigDecimal actualCredits, Long generationUsageId) {
        CreditTransaction reserveTx = transactionRepository.findByUserIdAndIdempotencyKey(userId, idempotencyKey)
                .orElseThrow(() -> new IllegalStateException("Reserve transaction not found"));
                
        if (reserveTx.getType() != CreditTransactionType.RESERVE) {
            throw new IllegalStateException("Cannot capture non-reserve transaction");
        }

        var captureIdempotencyKey = idempotencyKey + "_CAPTURE";
        if (transactionRepository.findByUserIdAndIdempotencyKey(userId, captureIdempotencyKey).isPresent()) {
            return;
        }

        UserCreditAccount account = accountRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("Account not found"));
                
        BigDecimal reservedAmount = reserveTx.getCredits();
        BigDecimal captureAmount = actualCredits.min(reservedAmount);
        BigDecimal releaseAmount = reservedAmount.subtract(captureAmount);
        
        BigDecimal balanceBefore = account.getAvailableCredits();
        BigDecimal reservedBefore = account.getReservedCredits();
        
        account.setReservedCredits(account.getReservedCredits().subtract(reservedAmount));
        account.setAvailableCredits(account.getAvailableCredits().add(releaseAmount));
        account.setLifetimeUsedCredits(account.getLifetimeUsedCredits().add(captureAmount));
        accountRepository.save(account);
        
        CreditTransaction tx = new CreditTransaction();
        tx.setUserId(userId);
        tx.setGenerationUsageId(generationUsageId);
        tx.setIdempotencyKey(captureIdempotencyKey);
        tx.setType(CreditTransactionType.CAPTURE);
        tx.setStatus(CreditTransactionStatus.COMPLETED);
        tx.setCredits(captureAmount);
        tx.setBalanceBefore(balanceBefore);
        tx.setBalanceAfter(account.getAvailableCredits());
        tx.setReservedBefore(reservedBefore);
        tx.setReservedAfter(account.getReservedCredits());
        transactionRepository.save(tx);
    }
    
    @Transactional
    public void releaseCredits(String userId, String idempotencyKey, Long generationUsageId) {
        CreditTransaction reserveTx = transactionRepository.findByUserIdAndIdempotencyKey(userId, idempotencyKey)
                .orElseThrow(() -> new IllegalStateException("Reserve transaction not found"));
                
        var releaseIdempotencyKey = idempotencyKey + "_RELEASE";
        if (transactionRepository.findByUserIdAndIdempotencyKey(userId, releaseIdempotencyKey).isPresent()) {
            return;
        }

        UserCreditAccount account = accountRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("Account not found"));
                
        BigDecimal reservedAmount = reserveTx.getCredits();
        
        BigDecimal balanceBefore = account.getAvailableCredits();
        BigDecimal reservedBefore = account.getReservedCredits();
        
        account.setReservedCredits(account.getReservedCredits().subtract(reservedAmount));
        account.setAvailableCredits(account.getAvailableCredits().add(reservedAmount));
        accountRepository.save(account);
        
        CreditTransaction tx = new CreditTransaction();
        tx.setUserId(userId);
        tx.setGenerationUsageId(generationUsageId);
        tx.setIdempotencyKey(releaseIdempotencyKey);
        tx.setType(CreditTransactionType.RELEASE);
        tx.setStatus(CreditTransactionStatus.RELEASED);
        tx.setCredits(reservedAmount);
        tx.setBalanceBefore(balanceBefore);
        tx.setBalanceAfter(account.getAvailableCredits());
        tx.setReservedBefore(reservedBefore);
        tx.setReservedAfter(account.getReservedCredits());
        transactionRepository.save(tx);
    }

    @Transactional
    public void grantMonthlyCredits(String userId, QuotaProfile quotaProfile, String idempotencyKey) {
        var existingTx = transactionRepository.findByUserIdAndIdempotencyKey(userId, idempotencyKey);
        if (existingTx.isPresent()) {
            return;
        }

        UserCreditAccount account = accountRepository.findByUserId(userId)
                .orElseGet(() -> {
                    UserCreditAccount newAcc = new UserCreditAccount();
                    newAcc.setUserId(userId);
                    return newAcc;
                });

        BigDecimal grantedCredits = quotaProfile.getMonthlyCredits();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime cycleEnd = now.plusMonths(1);

        CreditCycle cycle = new CreditCycle();
        cycle.setUserId(userId);
        cycle.setQuotaProfileCode(quotaProfile.getCode());
        cycle.setCycleStart(now);
        cycle.setCycleEnd(cycleEnd);
        cycle.setGrantedCredits(grantedCredits);
        cycle.setStatus(CreditCycleStatus.ACTIVE);
        cycle = cycleRepository.save(cycle);

        account.setAvailableCredits(account.getAvailableCredits().add(grantedCredits));
        account.setLifetimeGrantedCredits(account.getLifetimeGrantedCredits().add(grantedCredits));
        account.setCurrentQuotaProfileCode(quotaProfile.getCode());
        account.setCurrentCycleStart(now);
        account.setCurrentCycleEnd(cycleEnd);
        accountRepository.save(account);

        CreditTransaction tx = new CreditTransaction();
        tx.setUserId(userId);
        tx.setCycleId(cycle.getId());
        tx.setIdempotencyKey(idempotencyKey);
        tx.setType(CreditTransactionType.GRANT);
        tx.setStatus(CreditTransactionStatus.COMPLETED);
        tx.setCredits(grantedCredits);
        tx.setBalanceBefore(account.getAvailableCredits().subtract(grantedCredits));
        tx.setBalanceAfter(account.getAvailableCredits());
        tx.setReservedBefore(account.getReservedCredits());
        tx.setReservedAfter(account.getReservedCredits());
        tx.setReason("Monthly AI quota grant");
        transactionRepository.save(tx);
    }
}
