package com.centricorp.backend.aspect;

import com.centricorp.backend.security.TenantContext;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.hibernate.Session;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Aspecto AOP que se ejecuta antes de cualquier operación en la capa de
 * persistencia/servicios.
 * Habilita el filtro de Hibernate (tenantFilter) automáticamente si el usuario
 * autenticado
 * tiene un tenant asignado y no es SUPERADMIN.
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class TenantFilterAspect {

    private final EntityManager entityManager;

    /**
     * Intercepta la ejecución de cualquier método en los paquetes de repository o
     * service.
     * Se usa "execution(* com.centricorp.backend.repository..*(..))" para asegurar
     * que la sesión de Hibernate ya esté en curso.
     */
    @Before("execution(* com.centricorp.backend.repository..*(..))")
    public void enableTenantFilter(JoinPoint joinPoint) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // 1. Si no hay autenticación, no hacemos nada (ej. procesos internos asíncronos
        // o pre-login)
        if (authentication == null || !authentication.isAuthenticated()) {
            return;
        }

        // 2. Regla del SUPERADMIN: si tiene este rol, saltamos el filtro para que vea
        boolean isSuperAdmin = authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_SUPERADMIN"));

        if (isSuperAdmin) {
            return; // No activar el filtro
        }

        // 3. Extraer el tenant del contexto transaccional actual
        String tenantId = TenantContext.getCurrentTenant();

        // 4. Activar filtro en la sesión de Hibernate si existe un tenantId
        if (tenantId != null) {
            try {
                Session session = entityManager.unwrap(Session.class);
                session.enableFilter("tenantFilter").setParameter("tenantId", tenantId);
            } catch (Exception e) {
                log.warn("No se pudo activar el filtro Multi-Tenant (tenantFilter) en la sesión actual: {}",
                        e.getMessage());
            }
        }
    }
}
