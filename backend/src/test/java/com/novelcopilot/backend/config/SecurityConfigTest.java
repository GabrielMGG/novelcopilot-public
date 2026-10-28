package com.novelcopilot.backend.config;

import com.novelcopilot.backend.security.SupabaseAuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

class SecurityConfigTest {

    @Test
    void usesOnlyTheConfiguredFrontendOrigins() {
        SecurityConfig securityConfig = new SecurityConfig(
                mock(SupabaseAuthenticationFilter.class),
                new String[]{" https://app.example.com ", "https://preview.example.com"}
        );

        CorsConfigurationSource source = securityConfig.corsConfigurationSource();
        CorsConfiguration configuration = source.getCorsConfiguration(new MockHttpServletRequest());

        assertNotNull(configuration);
        assertEquals(
                List.of("https://app.example.com", "https://preview.example.com"),
                configuration.getAllowedOrigins()
        );
        assertEquals(
                List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"),
                configuration.getAllowedMethods()
        );
        assertEquals(
                List.of("Authorization", "Content-Type", "Origin", "Accept"),
                configuration.getAllowedHeaders()
        );
        assertEquals(Boolean.TRUE, configuration.getAllowCredentials());
    }
}
