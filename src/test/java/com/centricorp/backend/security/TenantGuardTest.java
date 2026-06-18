package com.centricorp.backend.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TenantGuardTest {

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void requireTenantReturnsCurrentTenant() {
        authenticate("ROLE_ADMIN");
        TenantContext.setCurrentTenant("tenant-a");

        assertEquals("tenant-a", TenantGuard.requireTenant());
    }

    @Test
    void requireTenantRejectsMissingTenant() {
        authenticate("ROLE_ADMIN");

        assertThrows(AccessDeniedException.class, TenantGuard::requireTenant);
    }

    @Test
    void rejectSuperAdminMutationBlocksSuperAdmin() {
        authenticate("ROLE_SUPERADMIN");

        assertThrows(AccessDeniedException.class, TenantGuard::rejectSuperAdminMutation);
    }

    private void authenticate(String role) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "user",
                        null,
                        List.of(new SimpleGrantedAuthority(role))
                )
        );
    }
}
