package com.novelcopilot.backend.security;

import com.novelcopilot.backend.dto.AuthenticatedUser;
import com.novelcopilot.backend.service.SupabaseAuthService;
import com.novelcopilot.backend.service.UserProfileService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.server.ResponseStatusException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

@Component
public class SupabaseAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(SupabaseAuthenticationFilter.class);

    private final SupabaseAuthService supabaseAuthService;
    private final UserProfileService userProfileService;
    private final AdminAuthorizationService adminAuthorizationService;

    public SupabaseAuthenticationFilter(
            SupabaseAuthService supabaseAuthService,
            UserProfileService userProfileService,
            AdminAuthorizationService adminAuthorizationService) {
        this.supabaseAuthService = supabaseAuthService;
        this.userProfileService = userProfileService;
        this.adminAuthorizationService = adminAuthorizationService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        log.debug("Auth Flow -> HTTP {}: {}", request.getMethod(), request.getRequestURI());

        String authorizationHeader = request.getHeader("Authorization");
        boolean authPresent = authorizationHeader != null && !authorizationHeader.trim().isEmpty();
        log.debug("Auth Flow -> Authorization presente: {}", authPresent);

        if (!authPresent) {
            filterChain.doFilter(request, response);
            return;
        }

        boolean bearerExtracted = authorizationHeader.startsWith("Bearer ") && authorizationHeader.length() > 7;
        log.debug("Auth Flow -> Bearer token extraído: {}", bearerExtracted);

        if (!bearerExtracted) {
            sendUnauthorized(response);
            return;
        }

        try {
            AuthenticatedUser authUser = supabaseAuthService.getAuthenticatedUser(authorizationHeader);
            userProfileService.findOrCreateFromAuthUser(authUser);
            log.debug("Auth Flow -> Validação concluída: true");

            AuthenticatedUserPrincipal principal = new AuthenticatedUserPrincipal(
                    authUser.getId(),
                    authUser.getEmail(),
                    authUser.getDisplayName(),
                    authUser.getAvatarUrl(),
                    authUser.getAuthProvider()
            );

            SupabaseAuthenticationToken authentication = new SupabaseAuthenticationToken(
                    principal,
                    adminAuthorizationService.authoritiesFor(principal.getId()));
            log.debug("Auth Flow -> Authentication criada: true");

            SecurityContextHolder.getContext().setAuthentication(authentication);
            log.debug("Auth Flow -> SecurityContext autenticado: {}", SecurityContextHolder.getContext().getAuthentication() != null && SecurityContextHolder.getContext().getAuthentication().isAuthenticated());

            filterChain.doFilter(request, response);
        } catch (ResponseStatusException e) {
            log.warn("Auth Flow -> Validação concluída: false (ResponseStatusException)");
            SecurityContextHolder.clearContext();
            sendUnauthorized(response);
        } catch (Exception e) {
            log.warn("Auth Flow -> Validação concluída: false (Exception: {})", e.getMessage());
            SecurityContextHolder.clearContext();
            sendUnauthorized(response);
        }
    }

    private void sendUnauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"status\":401,\"error\":\"Unauthorized\",\"message\":\"Token ausente, inválido ou expirado\"}");
    }
}
