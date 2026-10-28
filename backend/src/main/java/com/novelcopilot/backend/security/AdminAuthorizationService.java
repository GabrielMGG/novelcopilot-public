package com.novelcopilot.backend.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AdminAuthorizationService {

    private static final GrantedAuthority ADMIN_AUTHORITY =
            new SimpleGrantedAuthority("ROLE_ADMIN");

    private final Set<String> adminUserIds;

    public AdminAuthorizationService(
            @Value("${novelcopilot.security.admin-user-ids:}") String configuredUserIds) {
        this.adminUserIds = Arrays.stream(configuredUserIds.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }

    public List<GrantedAuthority> authoritiesFor(String userId) {
        if (userId != null && adminUserIds.contains(userId)) {
            return List.of(ADMIN_AUTHORITY);
        }
        return List.of();
    }
}
