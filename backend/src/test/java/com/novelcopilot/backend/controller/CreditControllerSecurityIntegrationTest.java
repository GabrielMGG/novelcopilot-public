package com.novelcopilot.backend.controller;

import com.novelcopilot.backend.dto.AuthenticatedUser;
import com.novelcopilot.backend.entity.CreditTransaction;
import com.novelcopilot.backend.entity.CreditTransactionStatus;
import com.novelcopilot.backend.entity.CreditTransactionType;
import com.novelcopilot.backend.entity.UserCreditAccount;
import com.novelcopilot.backend.repository.CreditTransactionRepository;
import com.novelcopilot.backend.repository.UserCreditAccountRepository;
import com.novelcopilot.backend.service.SupabaseAuthService;
import com.novelcopilot.backend.service.UserProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CreditControllerSecurityIntegrationTest {

    private static final String USER_A_ID = "user-a";
    private static final String USER_B_ID = "user-b";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SupabaseAuthService supabaseAuthService;

    @MockitoBean
    private UserProfileService userProfileService;

    @MockitoBean
    private UserCreditAccountRepository accountRepository;

    @MockitoBean
    private CreditTransactionRepository transactionRepository;

    @BeforeEach
    void setUp() {
        when(supabaseAuthService.getAuthenticatedUser("Bearer token-a"))
                .thenReturn(authenticatedUser(USER_A_ID));
        when(supabaseAuthService.getAuthenticatedUser("Bearer token-b"))
                .thenReturn(authenticatedUser(USER_B_ID));
        when(userProfileService.findOrCreateFromAuthUser(any())).thenReturn(null);
    }

    @Test
    void unauthenticatedRequestReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/credits/balance"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void forgedUserHeaderCannotChangeBalanceOwner() throws Exception {
        when(accountRepository.findByUserId(USER_A_ID))
                .thenReturn(Optional.of(account(USER_A_ID, "10.0")));
        when(accountRepository.findByUserId(USER_B_ID))
                .thenReturn(Optional.of(account(USER_B_ID, "999.0")));

        mockMvc.perform(get("/api/credits/balance")
                        .header("Authorization", "Bearer token-a")
                        .header("X-User-Id", USER_B_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availableCredits").value(10.0));

        verify(accountRepository).findByUserId(USER_A_ID);
        verify(accountRepository, never()).findByUserId(USER_B_ID);
    }

    @Test
    void eachTokenReceivesOnlyItsOwnTransactions() throws Exception {
        PageRequest pageable = PageRequest.of(0, 10);
        when(transactionRepository.findAllByUserIdAndTypeNotInOrderByCreatedAtDescIdDesc(
                eq(USER_A_ID), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(
                        List.of(transaction(USER_A_ID, "user-a-entry")),
                        pageable,
                        1));
        when(transactionRepository.findAllByUserIdAndTypeNotInOrderByCreatedAtDescIdDesc(
                eq(USER_B_ID), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(
                        List.of(transaction(USER_B_ID, "user-b-entry")),
                        pageable,
                        1));

        mockMvc.perform(get("/api/credits/transactions")
                        .header("Authorization", "Bearer token-a")
                        .header("X-User-Id", USER_B_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].reason").value("user-a-entry"));

        mockMvc.perform(get("/api/credits/transactions")
                        .header("Authorization", "Bearer token-b"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].reason").value("user-b-entry"));

        verify(transactionRepository).findAllByUserIdAndTypeNotInOrderByCreatedAtDescIdDesc(
                eq(USER_A_ID), any(), any(Pageable.class));
        verify(transactionRepository).findAllByUserIdAndTypeNotInOrderByCreatedAtDescIdDesc(
                eq(USER_B_ID), any(), any(Pageable.class));
    }

    private AuthenticatedUser authenticatedUser(String userId) {
        return new AuthenticatedUser(
                userId,
                userId + "@example.com",
                userId,
                null,
                "email");
    }

    private UserCreditAccount account(String userId, String availableCredits) {
        UserCreditAccount account = new UserCreditAccount();
        account.setUserId(userId);
        account.setAvailableCredits(new BigDecimal(availableCredits));
        account.setReservedCredits(BigDecimal.ZERO);
        return account;
    }

    private CreditTransaction transaction(String userId, String reason) {
        CreditTransaction transaction = new CreditTransaction();
        transaction.setUserId(userId);
        transaction.setType(CreditTransactionType.GRANT);
        transaction.setStatus(CreditTransactionStatus.COMPLETED);
        transaction.setCredits(BigDecimal.ONE);
        transaction.setReason(reason);
        transaction.setCreatedAt(LocalDateTime.of(2026, 7, 27, 12, 0));
        return transaction;
    }
}
