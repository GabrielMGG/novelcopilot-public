package com.novelcopilot.backend.service;

import com.novelcopilot.backend.config.AiQuotaProperties;
import com.novelcopilot.backend.entity.QuotaProfile;
import com.novelcopilot.backend.entity.UserCreditAccount;
import com.novelcopilot.backend.repository.QuotaProfileRepository;
import com.novelcopilot.backend.repository.UserCreditAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DevelopmentCreditServiceTest {

    @Mock
    private AiQuotaProperties quotaProperties;
    @Mock
    private CreditService creditService;
    @Mock
    private QuotaProfileRepository quotaProfileRepository;
    @Mock
    private UserCreditAccountRepository accountRepository;

    private DevelopmentCreditService service;

    @BeforeEach
    void setUp() {
        service = new DevelopmentCreditService(
                quotaProperties,
                creditService,
                quotaProfileRepository,
                accountRepository
        );
    }

    @Test
    void disabledModeOnlyReadsTheCurrentBalance() {
        UserCreditAccount account = accountWithBalance("12.5");
        when(accountRepository.findByUserId("user-1"))
                .thenReturn(Optional.of(account));

        BigDecimal balance = service.prepareAndGetAvailableCredits("user-1");

        assertEquals(new BigDecimal("12.5"), balance);
        verify(creditService, never()).grantMonthlyCredits(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void enabledModeUsesOneStableGrantKey() {
        QuotaProfile starter = new QuotaProfile();
        starter.setCode("DEMO");
        starter.setMonthlyCredits(new BigDecimal("500"));
        when(quotaProperties.isDevelopmentCreditsEnabled()).thenReturn(true);
        when(quotaProperties.getDevelopmentQuotaProfileCode()).thenReturn("DEMO");
        when(quotaProfileRepository.findById("DEMO")).thenReturn(Optional.of(starter));
        when(accountRepository.findByUserId("user-1"))
                .thenReturn(Optional.of(accountWithBalance("500")));

        BigDecimal balance = service.prepareAndGetAvailableCredits("user-1");

        assertEquals(new BigDecimal("500"), balance);
        verify(creditService).grantMonthlyCredits(
                "user-1",
                starter,
                "development:initial:user-1"
        );
    }

    private UserCreditAccount accountWithBalance(String balance) {
        UserCreditAccount account = new UserCreditAccount();
        account.setUserId("user-1");
        account.setAvailableCredits(new BigDecimal(balance));
        return account;
    }
}
