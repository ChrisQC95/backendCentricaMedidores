package com.centricorp.backend.security;

import org.springframework.security.access.AccessDeniedException;

public final class TenantGuard {

    private TenantGuard() {
    }

    public static boolean isSuperAdmin() {
        return SecurityUtils.isSuperAdmin();
    }

    public static String requireTenant() {
        String tenantId = TenantContext.getCurrentTenant();
        if (tenantId == null || tenantId.isBlank()) {
            throw new AccessDeniedException("Tenant no disponible");
        }
        return tenantId;
    }

    public static void rejectSuperAdminMutation() {
        if (isSuperAdmin()) {
            throw new AccessDeniedException("SUPERADMIN es solo lectura en endpoints operativos");
        }
    }
}
