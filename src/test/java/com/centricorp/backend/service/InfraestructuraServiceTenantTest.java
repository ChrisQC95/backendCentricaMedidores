package com.centricorp.backend.service;

import com.centricorp.backend.dto.InfraestructuraRequestDTO;
import com.centricorp.backend.entity.Empresa;
import com.centricorp.backend.entity.TipoNivel;
import com.centricorp.backend.exception.ResourceNotFoundException;
import com.centricorp.backend.repository.EmpresaRepository;
import com.centricorp.backend.repository.InfraestructuraRepository;
import com.centricorp.backend.repository.RegistroMedidorRepository;
import com.centricorp.backend.security.TenantContext;
import com.centricorp.backend.service.impl.InfraestructuraServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class InfraestructuraServiceTenantTest {

    private final InfraestructuraRepository infraRepository = mock(InfraestructuraRepository.class);
    private final EmpresaRepository empresaRepository = mock(EmpresaRepository.class);
    private final RegistroMedidorRepository medidorRepository = mock(RegistroMedidorRepository.class);
    private final InfraestructuraServiceImpl service = new InfraestructuraServiceImpl(
            infraRepository,
            empresaRepository,
            medidorRepository);

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void createRejectsParentFromAnotherTenant() {
        authenticate("ROLE_ADMIN");
        TenantContext.setCurrentTenant("tenant-a");
        InfraestructuraRequestDTO dto = new InfraestructuraRequestDTO();
        dto.setEmpresaRuc("12345678901");
        dto.setTipo(TipoNivel.PISO);
        dto.setNombre("Piso 1");
        dto.setParentId(99);
        when(empresaRepository.findByRucAndTenantId("12345678901", "tenant-a"))
                .thenReturn(Optional.of(Empresa.builder().ruc("12345678901").razonSocial("Empresa").build()));
        when(infraRepository.findByIdAndTenantId(99, "tenant-a")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.create(dto));
        verify(infraRepository, never()).save(any());
    }

    @Test
    void superAdminCannotCreateInfraestructuraFromOperationalEndpoint() {
        authenticate("ROLE_SUPERADMIN");
        InfraestructuraRequestDTO dto = new InfraestructuraRequestDTO();

        assertThrows(AccessDeniedException.class, () -> service.create(dto));
        verify(infraRepository, never()).save(any());
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
