package com.novelcopilot.backend.controller;

import com.novelcopilot.backend.entity.CreditTransaction;
import com.novelcopilot.backend.entity.CreditTransactionStatus;
import com.novelcopilot.backend.entity.CreditTransactionType;
import com.novelcopilot.backend.entity.UserCreditAccount;
import com.novelcopilot.backend.repository.CreditTransactionRepository;
import com.novelcopilot.backend.repository.UserCreditAccountRepository;
import com.novelcopilot.backend.security.AuthenticatedUserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CreditControllerTest {

    private static final String AUTHENTICATED_USER_ID = "user-a";
    private static final String OTHER_USER_ID = "user-b";

    private UserCreditAccountRepository accountRepository;
    private CreditTransactionRepository transactionRepository;
    private CreditController controller;
    private AuthenticatedUserPrincipal authenticatedUser;

    @BeforeEach
    void setUp() {
        accountRepository = mock(UserCreditAccountRepository.class);
        transactionRepository = mock(CreditTransactionRepository.class);
        controller = new CreditController(accountRepository, transactionRepository);
        authenticatedUser = new AuthenticatedUserPrincipal(
                AUTHENTICATED_USER_ID,
                "user-a@example.com",
                "User A",
                null,
                "email");
    }

    @Test
    void balanceUsesOnlyAuthenticatedUserId() {
        UserCreditAccount account = new UserCreditAccount();
        account.setUserId(AUTHENTICATED_USER_ID);
        account.setAvailableCredits(new BigDecimal("42.5"));
        account.setReservedCredits(new BigDecimal("3.0"));
        account.setCurrentQuotaProfileCode("DEMO");

        when(accountRepository.findByUserId(AUTHENTICATED_USER_ID))
                .thenReturn(Optional.of(account));

        ResponseEntity<?> response = controller.getBalance(authenticatedUser);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertEquals(new BigDecimal("42.5"), body.get("availableCredits"));
        assertEquals("DEMO", body.get("currentQuotaProfile"));
        verify(accountRepository).findByUserId(AUTHENTICATED_USER_ID);
        verify(accountRepository, never()).findByUserId(OTHER_USER_ID);
    }

    @Test
    void balanceReturnsNotFoundOnlyForAuthenticatedUserAccount() {
        when(accountRepository.findByUserId(AUTHENTICATED_USER_ID))
                .thenReturn(Optional.empty());

        ResponseEntity<?> response = controller.getBalance(authenticatedUser);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(accountRepository).findByUserId(AUTHENTICATED_USER_ID);
        verify(accountRepository, never()).findByUserId(OTHER_USER_ID);
    }

    @Test
    void transactionsUseOnlyAuthenticatedUserId() {
        PageRequest pageable = PageRequest.of(0, 10);
        CreditTransaction transaction = new CreditTransaction();
        transaction.setUserId(AUTHENTICATED_USER_ID);
        transaction.setType(CreditTransactionType.GRANT);
        transaction.setStatus(CreditTransactionStatus.COMPLETED);
        transaction.setCredits(new BigDecimal("25.0"));
        transaction.setReason("monthly-quota");
        transaction.setCreatedAt(LocalDateTime.of(2026, 7, 27, 12, 0));

        Set<CreditTransactionType> excludedTypes =
                Set.of(CreditTransactionType.RESERVE, CreditTransactionType.RELEASE);
        when(transactionRepository.findAllByUserIdAndTypeNotInOrderByCreatedAtDescIdDesc(
                AUTHENTICATED_USER_ID, excludedTypes, pageable))
                .thenReturn(new PageImpl<>(List.of(transaction), pageable, 1));

        ResponseEntity<Page<Map<String, Object>>> response =
                controller.getTransactions(authenticatedUser, pageable);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Page<Map<String, Object>> body = response.getBody();
        assertNotNull(body);
        assertEquals(1, body.getTotalElements());
        assertEquals(new BigDecimal("25.0"), body.getContent().getFirst().get("credits"));
        assertEquals("monthly-quota", body.getContent().getFirst().get("reason"));
        verify(transactionRepository).findAllByUserIdAndTypeNotInOrderByCreatedAtDescIdDesc(
                AUTHENTICATED_USER_ID, excludedTypes, pageable);
        verify(transactionRepository, never()).findAllByUserIdAndTypeNotInOrderByCreatedAtDescIdDesc(
                OTHER_USER_ID, excludedTypes, pageable);
    }
}
