package com.centricorp.backend.security;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.io.IOException;
import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class JwtAuthFilterTest {

    private final JwtService jwtService = mock(JwtService.class);
    private final UserDetailsService userDetailsService = mock(UserDetailsService.class);
    private final JwtAuthFilter filter = new JwtAuthFilter(jwtService, userDetailsService);

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void rejectsTokenWhenTenantClaimDoesNotMatchDatabaseUser() throws ServletException, IOException {
        CustomUserDetails user = new CustomUserDetails(
                "user",
                "password",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")),
                "tenant-db");

        when(jwtService.extractUsername("token")).thenReturn("user");
        when(userDetailsService.loadUserByUsername("user")).thenReturn(user);
        when(jwtService.isTokenValid("token", user)).thenReturn(true);
        when(jwtService.extractClaim(eq("token"), any(Function.class))).thenReturn("tenant-token");

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertEquals(401, response.getStatus());
    }

    @Test
    void rejectsNormalUserWhenTenantClaimIsMissing() throws ServletException, IOException {
        CustomUserDetails user = new CustomUserDetails(
                "user",
                "password",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")),
                "tenant-db");

        when(jwtService.extractUsername("token")).thenReturn("user");
        when(userDetailsService.loadUserByUsername("user")).thenReturn(user);
        when(jwtService.isTokenValid("token", user)).thenReturn(true);
        when(jwtService.extractClaim(eq("token"), any(Function.class))).thenReturn(null);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertEquals(401, response.getStatus());
    }
}
