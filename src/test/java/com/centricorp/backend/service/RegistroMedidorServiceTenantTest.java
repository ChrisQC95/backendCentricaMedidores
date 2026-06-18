package com.centricorp.backend.service;

import com.centricorp.backend.dto.RegistroMedidorRequestDTO;
import com.centricorp.backend.exception.ResourceNotFoundException;
import com.centricorp.backend.repository.InfraestructuraRepository;
import com.centricorp.backend.repository.RegistroMedidorRepository;
import com.centricorp.backend.security.TenantContext;
import com.centricorp.backend.service.impl.RegistroMedidorServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class RegistroMedidorServiceTenantTest {

    private final RegistroMedidorRepository registroRepository = mock(RegistroMedidorRepository.class);
    private final InfraestructuraRepository infraRepository = mock(InfraestructuraRepository.class);
    private final RegistroMedidorServiceImpl service = new RegistroMedidorServiceImpl(registroRepository, infraRepository);

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void createRejectsInfraestructuraFromAnotherTenant() {
        authenticate("ROLE_ADMIN");
        TenantContext.setCurrentTenant("tenant-a");
        RegistroMedidorRequestDTO dto = new RegistroMedidorRequestDTO();
        dto.setInfraestructuraId(77);
        dto.setVoltaje(BigDecimal.valueOf(100));
        dto.setTipoServicio(1);
        when(infraRepository.findByIdAndTenantId(77, "tenant-a")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.create(dto));
        verify(registroRepository, never()).save(any());
    }

    @Test
    void superAdminCannotCreateMedidorFromOperationalEndpoint() {
        authenticate("ROLE_SUPERADMIN");
        RegistroMedidorRequestDTO dto = new RegistroMedidorRequestDTO();

        assertThrows(AccessDeniedException.class, () -> service.create(dto));
        verify(registroRepository, never()).save(any());
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
