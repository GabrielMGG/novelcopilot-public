package com.novelcopilot.backend.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminAuthorizationServiceTest {

    @Test
    void emptyConfigurationGrantsNoAuthorities() {
        AdminAuthorizationService service = new AdminAuthorizationService("");

        assertTrue(service.authoritiesFor("any-user").isEmpty());
        assertTrue(service.authoritiesFor(null).isEmpty());
    }

    @Test
    void grantsAdminOnlyToExactConfiguredUserIds() {
        AdminAuthorizationService service =
                new AdminAuthorizationService(" admin-a,admin-b , admin-a ");

        assertEquals(
                "ROLE_ADMIN",
                service.authoritiesFor("admin-a").getFirst().getAuthority());
        assertEquals(
                "ROLE_ADMIN",
                service.authoritiesFor("admin-b").getFirst().getAuthority());
        assertTrue(service.authoritiesFor("ADMIN-A").isEmpty());
        assertTrue(service.authoritiesFor("admin-a@example.com").isEmpty());
        assertTrue(service.authoritiesFor("ordinary-user").isEmpty());
    }
}
