package com.novelcopilot.backend.service;

import com.novelcopilot.backend.dto.AuthenticatedUser;
import com.novelcopilot.backend.entity.UserProfile;
import com.novelcopilot.backend.repository.UserProfileRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class UserProfileService {
    private final UserProfileRepository userProfileRepository;

    public UserProfileService(UserProfileRepository userProfileRepository) {
        this.userProfileRepository = userProfileRepository;
    }

    public UserProfile findOrCreateFromAuthUser(AuthenticatedUser authUser) {
        return userProfileRepository.findById(authUser.getId())
                .map(existingUser -> updateExistingUser(existingUser, authUser))
                .orElseGet(() -> createUser(authUser));
    }

    private UserProfile createUser(AuthenticatedUser authUser) {
        LocalDateTime now = LocalDateTime.now();

        UserProfile user = new UserProfile();
        user.setId(authUser.getId());
        user.setEmail(authUser.getEmail());
        user.setDisplayName(authUser.getDisplayName());
        user.setAvatarUrl(authUser.getAvatarUrl());
        user.setAuthProvider(authUser.getAuthProvider());
        user.setCreatedAt(now);
        user.setUpdatedAt(now);

        return userProfileRepository.save(user);
    }

    private UserProfile updateExistingUser(UserProfile user, AuthenticatedUser authUser) {
        user.setEmail(authUser.getEmail());
        user.setDisplayName(authUser.getDisplayName());
        user.setAvatarUrl(authUser.getAvatarUrl());
        user.setAuthProvider(authUser.getAuthProvider());
        user.setUpdatedAt(LocalDateTime.now());

        return userProfileRepository.save(user);
    }
}