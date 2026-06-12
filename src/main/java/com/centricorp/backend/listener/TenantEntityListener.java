package com.centricorp.backend.listener;

import com.centricorp.backend.security.TenantContext;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.Method;

/**
 * Escucha eventos del ciclo de vida de JPA (PrePersist, PreUpdate).
 * Inyecta automáticamente el tenant_id actual en las entidades que lo soportan.
 */
@Slf4j
public class TenantEntityListener {

    @PrePersist
    @PreUpdate
    public void setTenant(Object entity) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        // 1. Si no hay contexto de seguridad (procesos background), salimos
        if (authentication == null || !authentication.isAuthenticated()) {
            return;
        }

        // 2. Verificar si es SUPERADMIN
        boolean isSuperAdmin = authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_SUPERADMIN"));

        // Si es SUPERADMIN, NO sobreescribimos el tenantId.
        // Permitimos que se guarde el valor que venga en el payload de la entidad.
        if (isSuperAdmin) {
            return;
        }

        // 3. Si es ADMIN u OPERADOR, forzamos obligatoriamente el tenant_id del contexto
        String tenantId = TenantContext.getCurrentTenant();
        if (tenantId != null) {
            try {
                Method setTenantIdMethod = entity.getClass().getMethod("setTenantId", String.class);
                // Forzamos la sobreescritura ignorando cualquier valor que haya mandado el frontend
                setTenantIdMethod.invoke(entity, tenantId);
            } catch (NoSuchMethodException e) {
                // La entidad no tiene tenantId, ignorar
            } catch (Exception e) {
                log.error("Error al forzar tenantId en entidad {}", entity.getClass().getSimpleName(), e);
            }
        }
    }
}
