package com.novelcopilot.backend.dto;

public class AuthenticatedUser {
    private String id;
    private String email;
    private String displayName;
    private String avatarUrl;
    private String authProvider;

    public AuthenticatedUser(
            String id,
            String email,
            String displayName,
            String avatarUrl,
            String authProvider
    ) {
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