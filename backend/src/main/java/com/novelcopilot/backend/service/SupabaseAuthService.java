package com.novelcopilot.backend.service;

import com.novelcopilot.backend.dto.AuthenticatedUser;
import com.novelcopilot.backend.dto.SupabaseUserResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@Service
public class SupabaseAuthService {
    private final RestClient restClient;
    private final String supabaseUrl;
    private final String supabaseAnonKey;

    public SupabaseAuthService(
            RestClient.Builder restClientBuilder,
            @Value("${supabase.url}") String supabaseUrl,
            @Value("${supabase.anon-key}") String supabaseAnonKey
    ) {
        this.restClient = restClientBuilder.build();
        this.supabaseUrl = supabaseUrl;
        this.supabaseAnonKey = supabaseAnonKey;
    }

    public AuthenticatedUser getAuthenticatedUser(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token de autenticacao ausente.");
        }

        try {
            SupabaseUserResponse response = restClient.get()
                    .uri(supabaseUrl + "/auth/v1/user")
                    .header("apikey", supabaseAnonKey)
                    .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                    .retrieve()
                    .body(SupabaseUserResponse.class);

            if (response == null || response.getId() == null) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token de autenticacao invalido.");
            }

            String displayName = readString(response.getUserMetadata(), "full_name");
            String avatarUrl = readString(response.getUserMetadata(), "avatar_url");
            String authProvider = readString(response.getAppMetadata(), "provider");

            return new AuthenticatedUser(
                    response.getId(),
                    response.getEmail(),
                    displayName,
                    avatarUrl,
                    authProvider
            );
        } catch (RestClientResponseException exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token de autenticacao invalido.");
        }
    }

    private String readString(Map<String, Object> metadata, String key) {
        if (metadata == null) {
            return null;
        }

        Object value = metadata.get(key);

        if (value == null) {
            return null;
        }

        return value.toString();
    }
}