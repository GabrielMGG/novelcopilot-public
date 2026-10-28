package com.novelcopilot.backend.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.Collections;

public class SupabaseAuthenticationToken extends AbstractAuthenticationToken {
    private final AuthenticatedUserPrincipal principal;

    public SupabaseAuthenticationToken(AuthenticatedUserPrincipal principal) {
        this(principal, Collections.emptyList());
    }

    public SupabaseAuthenticationToken(
            AuthenticatedUserPrincipal principal,
            Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.principal = principal;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public AuthenticatedUserPrincipal getPrincipal() {
        return principal;
    }
}
