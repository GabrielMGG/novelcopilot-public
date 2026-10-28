package com.novelcopilot.backend.benchmark.controller;

import com.novelcopilot.backend.dto.AuthenticatedUser;
import com.novelcopilot.backend.service.SupabaseAuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
        properties = {
                "server.port=8081",
                "ai.benchmark.enabled=true",
                "novelcopilot.security.admin-user-ids=user123"
        })
@ActiveProfiles({"benchmark", "test"})
public class AiBenchmarkControllerSecurityTest {

    private final RestTemplate restTemplate = new RestTemplate();
    private final String baseUrl = "http://localhost:8081/internal/benchmark";

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        public SupabaseAuthService mockSupabaseAuthService() {
            SupabaseAuthService mockService = mock(SupabaseAuthService.class);
            AuthenticatedUser mockUser = new AuthenticatedUser("user123", "test@example.com", "Test User", null, "email");
            when(mockService.getAuthenticatedUser("Bearer valid-token")).thenReturn(mockUser);
            AuthenticatedUser ordinaryUser = new AuthenticatedUser("ordinary-user", "ordinary@example.com", "Ordinary User", null, "email");
            when(mockService.getAuthenticatedUser("Bearer ordinary-token")).thenReturn(ordinaryUser);
            // Any other token will throw or return null by default
            return mockService;
        }
    }

    @Test
    public void testGetConfigWithoutToken_ShouldReturn401() {
        try {
            restTemplate.getForEntity(baseUrl + "/config", String.class);
            fail("Should have thrown HttpClientErrorException");
        } catch (HttpClientErrorException e) {
            assertEquals(HttpStatus.UNAUTHORIZED, e.getStatusCode());
        }
    }

    @Test
    public void testPostEstimateWithoutToken_ShouldReturn401() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        HttpEntity<String> request = new HttpEntity<>("{}", headers);
        
        try {
            restTemplate.postForEntity(baseUrl + "/estimate", request, String.class);
            fail("Should have thrown HttpClientErrorException");
        } catch (HttpClientErrorException e) {
            assertEquals(HttpStatus.UNAUTHORIZED, e.getStatusCode());
        }
    }

    @Test
    public void testPostRunWithoutToken_ShouldReturn401() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        HttpEntity<String> request = new HttpEntity<>("{}", headers);
        
        try {
            restTemplate.postForEntity(baseUrl + "/run", request, String.class);
            fail("Should have thrown HttpClientErrorException");
        } catch (HttpClientErrorException e) {
            assertEquals(HttpStatus.UNAUTHORIZED, e.getStatusCode());
        }
    }

    @Test
    public void testPostRunWithInvalidToken_ShouldReturn401() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        headers.set("Authorization", "Bearer invalid-token");
        HttpEntity<String> request = new HttpEntity<>("{}", headers);
        
        try {
            restTemplate.postForEntity(baseUrl + "/run", request, String.class);
            fail("Should have thrown HttpClientErrorException");
        } catch (HttpClientErrorException e) {
            assertEquals(HttpStatus.UNAUTHORIZED, e.getStatusCode());
        }
    }

    @Test
    public void testOrdinaryUserWithForgedAdminHeader_ShouldReturn403() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer ordinary-token");
        headers.set("X-Admin", "true");
        HttpEntity<String> request = new HttpEntity<>(headers);

        try {
            restTemplate.exchange(baseUrl + "/config", HttpMethod.GET, request, String.class);
            fail("Should have thrown HttpClientErrorException");
        } catch (HttpClientErrorException e) {
            assertEquals(HttpStatus.FORBIDDEN, e.getStatusCode());
        }
    }

    @Test
    public void testOptions_ShouldBePermitted() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Origin", "http://localhost:5173");
        headers.set("Access-Control-Request-Method", "POST");
        HttpEntity<String> request = new HttpEntity<>(headers);
        
        ResponseEntity<String> response = restTemplate.exchange(baseUrl + "/run", HttpMethod.OPTIONS, request, String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void testGetConfigWithToken_ShouldReturn200() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer valid-token");
        HttpEntity<String> request = new HttpEntity<>(headers);
        
        ResponseEntity<String> response =
                restTemplate.exchange(baseUrl + "/config", HttpMethod.GET, request, String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void testPostEstimateWithToken_ShouldReturn200() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer valid-token");
        headers.set("Content-Type", "application/json");
        HttpEntity<String> request = new HttpEntity<>("{\"caseIds\":[],\"participantIds\":[]}", headers);
        
        ResponseEntity<String> response = restTemplate.postForEntity(baseUrl + "/estimate", request, String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void testPostRunWithToken_ShouldReturn400_WhenMissingArgs() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer valid-token");
        headers.set("Content-Type", "application/json");
        HttpEntity<String> request = new HttpEntity<>("{\"caseIds\":[],\"participantIds\":[]}", headers);
        
        try {
            fail("Should have thrown HttpClientErrorException. Status was: " + restTemplate.postForEntity(baseUrl + "/run", request, String.class).getStatusCode());
        } catch (HttpClientErrorException e) {
            assertTrue(e.getStatusCode() == HttpStatus.BAD_REQUEST || e.getStatusCode() == HttpStatus.CONFLICT); 
        }
    }
}
