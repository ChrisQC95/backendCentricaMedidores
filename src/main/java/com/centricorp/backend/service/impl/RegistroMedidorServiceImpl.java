package com.centricorp.backend.service.impl;

import com.centricorp.backend.dto.RegistroMedidorRequestDTO;
import com.centricorp.backend.dto.RegistroMedidorResponseDTO;
import com.centricorp.backend.entity.Infraestructura;
import com.centricorp.backend.entity.RegistroMedidor;
import com.centricorp.backend.exception.ResourceNotFoundException;
import com.centricorp.backend.repository.InfraestructuraRepository;
import com.centricorp.backend.repository.RegistroMedidorRepository;
import com.centricorp.backend.security.SecurityUtils;
import com.centricorp.backend.security.TenantContext;
import com.centricorp.backend.service.RegistroMedidorService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RegistroMedidorServiceImpl implements RegistroMedidorService {

    private final RegistroMedidorRepository registroRepo;
    private final InfraestructuraRepository infraRepo;

    @Override
    @Transactional
    public RegistroMedidorResponseDTO create(RegistroMedidorRequestDTO dto) {
        if (dto.getTipoServicio() != null && dto.getTipoServicio() != 1 && dto.getTipoServicio() != 2) {
            throw new IllegalArgumentException("El tipo de servicio debe ser 1 (Luz) o 2 (Agua).");
        }

        Infraestructura infra = getInfraestructuraOrThrow(dto.getInfraestructuraId());

        RegistroMedidor registro = RegistroMedidor.builder()
                .infraestructura(infra)
                .fotoUrl(dto.getFotoUrl())
                .voltaje(dto.getVoltaje())
                .observacion(dto.getObservacion())
                .fechaRegistro(dto.getFechaRegistro() != null ? dto.getFechaRegistro() : LocalDate.now())
                .tipoServicio(dto.getTipoServicio() != null ? dto.getTipoServicio() : 1)
                .build();

        RegistroMedidor saved = registroRepo.save(registro);
        return toDTO(getRegistroOrFallback(saved));
    }

    @Override
    public Page<RegistroMedidorResponseDTO> findAll(Integer tipoServicio, int page, int size) {
        // Siempre ordenar por fecha descendente para mayor relevancia al usuario
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "fechaRegistro", "createdAt"));

        Page<RegistroMedidor> registros;
        if (SecurityUtils.isSuperAdmin()) {
            registros = tipoServicio != null
                    ? registroRepo.findByTipoServicio(tipoServicio, pageable)
                    : registroRepo.findAll(pageable);
        } else {
            registros = tipoServicio != null
                    ? registroRepo.findByTipoServicioAndTenantId(tipoServicio, currentTenant(), pageable)
                    : registroRepo.findByTenantId(currentTenant(), pageable);
        }

        return registros.map(this::toDTO);
    }

    @Override
    public List<RegistroMedidorResponseDTO> findReporte(int mes, int anio, Integer tipoServicio) {
        if (mes < 1 || mes > 12) {
            throw new IllegalArgumentException("El mes debe estar entre 1 y 12.");
        }
        if (tipoServicio != null && tipoServicio != 1 && tipoServicio != 2) {
            throw new IllegalArgumentException("El tipo de servicio debe ser 1 (Luz) o 2 (Agua), o no indicarse para obtener ambos.");
        }

        YearMonth ym = YearMonth.of(anio, mes);
        LocalDate inicio = ym.atDay(1);
        LocalDate fin = ym.atEndOfMonth();

        List<RegistroMedidor> registros;
        if (SecurityUtils.isSuperAdmin()) {
            registros = tipoServicio == null
                    ? registroRepo.findByFechaRegistroBetween(inicio, fin)
                    : registroRepo.findByFechaRegistroBetweenAndTipoServicio(inicio, fin, tipoServicio);
        } else {
            registros = tipoServicio == null
                    ? registroRepo.findByFechaRegistroBetweenAndTenantId(inicio, fin, currentTenant())
                    : registroRepo.findByFechaRegistroBetweenAndTipoServicioAndTenantId(
                    inicio, fin, tipoServicio, currentTenant());
        }

        return registros.stream().map(this::toDTO).toList();
    }

    private Infraestructura getInfraestructuraOrThrow(Integer id) {
        return SecurityUtils.isSuperAdmin()
                ? infraRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Infraestructura", id))
                : infraRepo.findByIdAndTenantId(id, currentTenant())
                .orElseThrow(() -> new ResourceNotFoundException("Infraestructura", id));
    }

    private RegistroMedidor getRegistroOrFallback(RegistroMedidor saved) {
        return SecurityUtils.isSuperAdmin()
                ? registroRepo.findById(saved.getId()).orElse(saved)
                : registroRepo.findByIdAndTenantId(saved.getId(), currentTenant()).orElse(saved);
    }

    private String currentTenant() {
        return TenantContext.getCurrentTenant();
    }

    private RegistroMedidorResponseDTO toDTO(RegistroMedidor r) {
        Infraestructura infra = r.getInfraestructura();
        return RegistroMedidorResponseDTO.builder()
                .id(r.getId())
                .infraestructuraId(infra.getId())
                .infraestructuraNombre(infra.getNombre())
                .infraestructuraTipo(infra.getTipo() != null ? infra.getTipo().name() : null)
                .empresaRuc(infra.getEmpresa().getRuc())
                .empresaRazonSocial(infra.getEmpresa().getRazonSocial())
                .fotoUrl(r.getFotoUrl())
                .voltaje(r.getVoltaje())
                .consumo(r.getConsumo())
                .fechaRegistro(r.getFechaRegistro())
                .observacion(r.getObservacion())
                .tipoServicio(r.getTipoServicio())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
