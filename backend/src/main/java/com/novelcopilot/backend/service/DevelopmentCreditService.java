package com.novelcopilot.backend.service;

import com.novelcopilot.backend.config.AiQuotaProperties;
import com.novelcopilot.backend.entity.QuotaProfile;
import com.novelcopilot.backend.repository.QuotaProfileRepository;
import com.novelcopilot.backend.repository.UserCreditAccountRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class DevelopmentCreditService {

    private static final String DEVELOPMENT_GRANT_PREFIX = "development:initial:";

    private final AiQuotaProperties quotaProperties;
    private final CreditService creditService;
    private final QuotaProfileRepository quotaProfileRepository;
    private final UserCreditAccountRepository accountRepository;

    public DevelopmentCreditService(
            AiQuotaProperties quotaProperties,
            CreditService creditService,
            QuotaProfileRepository quotaProfileRepository,
            UserCreditAccountRepository accountRepository
    ) {
        this.quotaProperties = quotaProperties;
        this.creditService = creditService;
        this.quotaProfileRepository = quotaProfileRepository;
        this.accountRepository = accountRepository;
    }

    public BigDecimal prepareAndGetAvailableCredits(String userId) {
        if (quotaProperties.isDevelopmentCreditsEnabled()) {
            String quotaProfileCode = quotaProperties.getDevelopmentQuotaProfileCode();
            QuotaProfile quotaProfile = quotaProfileRepository.findById(quotaProfileCode)
                    .orElseThrow(() -> new IllegalStateException(
                            "Perfil de cota não encontrado: " + quotaProfileCode
                    ));
            creditService.grantMonthlyCredits(
                    userId,
                    quotaProfile,
                    DEVELOPMENT_GRANT_PREFIX + userId
            );
        }

        return accountRepository.findByUserId(userId)
                .map(account -> account.getAvailableCredits())
                .orElse(BigDecimal.ZERO);
    }
}
