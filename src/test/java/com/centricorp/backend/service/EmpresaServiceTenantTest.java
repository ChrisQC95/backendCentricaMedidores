package com.centricorp.backend.service;

import com.centricorp.backend.dto.EmpresaDTO;
import com.centricorp.backend.entity.Empresa;
import com.centricorp.backend.repository.EmpresaRepository;
import com.centricorp.backend.security.TenantContext;
import com.centricorp.backend.service.impl.EmpresaServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EmpresaServiceTenantTest {

    private final EmpresaRepository repository = mock(EmpresaRepository.class);
    private final EmpresaServiceImpl service = new EmpresaServiceImpl(repository);

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void normalUserCreatesEmpresaOnlyInsideCurrentTenant() {
        authenticate("ROLE_ADMIN");
        TenantContext.setCurrentTenant("tenant-a");
        EmpresaDTO dto = EmpresaDTO.builder().ruc("12345678901").razonSocial("Empresa Uno").build();
        when(repository.existsByRucAndTenantId("12345678901", "tenant-a")).thenReturn(false);
        when(repository.save(any(Empresa.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.create(dto);

        verify(repository).existsByRucAndTenantId("12345678901", "tenant-a");
        verify(repository).save(any(Empresa.class));
        verify(repository, never()).existsById(anyString());
    }

    @Test
    void superAdminCannotCreateEmpresaFromOperationalEndpoint() {
        authenticate("ROLE_SUPERADMIN");
        EmpresaDTO dto = EmpresaDTO.builder().ruc("12345678901").razonSocial("Empresa Uno").build();

        assertThrows(AccessDeniedException.class, () -> service.create(dto));
        verify(repository, never()).save(any(Empresa.class));
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
