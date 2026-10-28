package com.novelcopilot.backend.security;

public class AuthenticatedUserPrincipal {
    private final String id;
    private final String email;
    private final String displayName;
    private final String avatarUrl;
    private final String authProvider;

    public AuthenticatedUserPrincipal(String id, String email, String displayName, String avatarUrl, String authProvider) {
        this.id = id;
        this.email = email;
        this.displayName = displayName;
        this.avatarUrl = avatarUrl;
        this.authProvider = authProvider;
    }

    public String getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public String getAuthProvider() {
        return authProvider;
    }
}
