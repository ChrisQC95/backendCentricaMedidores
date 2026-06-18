package com.centricorp.backend.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Objects;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String AUTH_HEADER = "Authorization";
    private static final String ACCESS_TOKEN_COOKIE = "access_token";

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        final String authHeader = request.getHeader(AUTH_HEADER);
        final String jwt = resolveToken(request, authHeader);

        if (jwt == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            final String username = jwtService.extractUsername(jwt);

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                if (jwtService.isTokenValid(jwt, userDetails)) {
                    validateAndSetTenantContext(jwt, userDetails, username, response);
                    if (response.isCommitted()) {
                        return;
                    }

                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }

            filterChain.doFilter(request, response);
        } catch (ExpiredJwtException ex) {
            log.warn("JWT expirado: {}", ex.getMessage());
            filterChain.doFilter(request, response);
        } catch (SignatureException | MalformedJwtException ex) {
            log.warn("JWT invalido: {}", ex.getMessage());
            filterChain.doFilter(request, response);
        } catch (Exception ex) {
            log.error("Error procesando JWT: {}", ex.getMessage());
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private void validateAndSetTenantContext(
            String jwt,
            UserDetails userDetails,
            String username,
            HttpServletResponse response
    ) throws IOException {
        String tokenTenantId = jwtService.extractClaim(jwt, claims -> claims.get("tenant_id", String.class));
        boolean isSuperAdmin = userDetails.getAuthorities().stream()
                .anyMatch(auth -> "ROLE_SUPERADMIN".equals(auth.getAuthority()));

        if (userDetails instanceof CustomUserDetails customUserDetails) {
            String userTenantId = customUserDetails.getTenantId();

            if (!isSuperAdmin) {
                if (userTenantId == null || userTenantId.isBlank()
                        || tokenTenantId == null || tokenTenantId.isBlank()
                        || !Objects.equals(tokenTenantId, userTenantId)) {
                    log.warn("JWT rechazado por tenant_id inconsistente para usuario {}", username);
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token invalido");
                    return;
                }

                TenantContext.setCurrentTenant(userTenantId);
                return;
            }

            if (userTenantId != null && !userTenantId.isBlank()) {
                TenantContext.setCurrentTenant(userTenantId);
            }
        }
    }

    private String resolveToken(HttpServletRequest request, String authHeader) {
        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            return authHeader.substring(BEARER_PREFIX.length());
        }

        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if (ACCESS_TOKEN_COOKIE.equals(cookie.getName()) && cookie.getValue() != null && !cookie.getValue().isBlank()) {
                return cookie.getValue();
            }
        }

        return null;
    }
}
