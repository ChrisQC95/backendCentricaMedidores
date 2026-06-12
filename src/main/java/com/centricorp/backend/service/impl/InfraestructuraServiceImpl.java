package com.centricorp.backend.service.impl;

import com.centricorp.backend.dto.InfraestructuraRequestDTO;
import com.centricorp.backend.dto.InfraestructuraResponseDTO;
import com.centricorp.backend.entity.Empresa;
import com.centricorp.backend.entity.Infraestructura;
import com.centricorp.backend.exception.ResourceNotFoundException;
import com.centricorp.backend.repository.EmpresaRepository;
import com.centricorp.backend.repository.InfraestructuraRepository;
import com.centricorp.backend.repository.RegistroMedidorRepository;
import com.centricorp.backend.service.InfraestructuraService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InfraestructuraServiceImpl implements InfraestructuraService {

    private final InfraestructuraRepository  infraRepo;
    private final EmpresaRepository          empresaRepo;
    /**
     * Inyectado para calcular los totales de consumo separados por tipo_servicio.
     * Garantiza que NUNCA se mezclen kWh (Electricidad) con m³ (Agua).
     */
    private final RegistroMedidorRepository  medidorRepo;

    @Override
    public List<InfraestructuraResponseDTO> findAll() {
        return infraRepo.findAll().stream().map(this::toDTO).toList();
    }

    @Override
    public InfraestructuraResponseDTO findById(Integer id) {
        return toDTO(getOrThrow(id));
    }

    @Override
    public List<InfraestructuraResponseDTO> findByEmpresaRuc(String ruc) {
        // Verificar que la empresa exista antes de retornar lista vacía
        if (!empresaRepo.existsById(ruc)) {
            throw new ResourceNotFoundException("Empresa", ruc);
        }
        return infraRepo.findByEmpresaRuc(ruc).stream().map(this::toDTO).toList();
    }

    @Override
    @Transactional
    public InfraestructuraResponseDTO create(InfraestructuraRequestDTO dto) {
        Empresa empresa = empresaRepo.findById(dto.getEmpresaRuc())
                .orElseThrow(() -> new ResourceNotFoundException("Empresa", dto.getEmpresaRuc()));

        Infraestructura parent = null;
        if (dto.getParentId() != null) {
            parent = getOrThrow(dto.getParentId());
        }

        Infraestructura infra = Infraestructura.builder()
                .empresa(empresa)
                .parent(parent)
                .tipo(dto.getTipo())
                .nombre(dto.getNombre())
                .glosa(dto.getGlosa())
                .espacioName(dto.getEspacioName())
                .build();

        return toDTO(infraRepo.save(infra));
    }

    @Override
    @Transactional
    public InfraestructuraResponseDTO update(Integer id, InfraestructuraRequestDTO dto) {
        Infraestructura infra = getOrThrow(id);

        // Actualizar empresa si cambió
        if (dto.getEmpresaRuc() != null && !dto.getEmpresaRuc().equals(infra.getEmpresa().getRuc())) {
            Empresa empresa = empresaRepo.findById(dto.getEmpresaRuc())
                    .orElseThrow(() -> new ResourceNotFoundException("Empresa", dto.getEmpresaRuc()));
            infra.setEmpresa(empresa);
        }

        // Actualizar parent si cambió
        if (dto.getParentId() != null) {
            infra.setParent(getOrThrow(dto.getParentId()));
        } else {
            infra.setParent(null); // permite mover nodo a raíz
        }

        if (dto.getTipo()   != null) infra.setTipo(dto.getTipo());
        if (dto.getNombre() != null) infra.setNombre(dto.getNombre());
        infra.setGlosa(dto.getGlosa());

        // espacioName se actualiza siempre (puede ponerse a null explícitamente)
        infra.setEspacioName(dto.getEspacioName());

        return toDTO(infraRepo.save(infra));
    }

    @Override
    @Transactional
    public void delete(Integer id) {
        Infraestructura infra = getOrThrow(id);
        infraRepo.delete(infra);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private Infraestructura getOrThrow(Integer id) {
        return infraRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Infraestructura", id));
    }

    /**
     * Mapea la entidad al DTO de respuesta calculando los totales de consumo
     * de forma estrictamente separada por tipo_servicio.
     *
     * La query del repositorio filtra por infraestructura_id AND tipo_servicio,
     * por lo que es matemáticamente imposible sumar kWh con m³.
     * Si no hay registros con consumo calculado, devuelve BigDecimal.ZERO.
     */
    private InfraestructuraResponseDTO toDTO(Infraestructura i) {
        // Consumo Electricidad (tipo_servicio = 1) — puede ser null si no hay registros
        BigDecimal consumoElectricidad = medidorRepo
                .sumConsumoByInfraestructuraIdAndTipoServicio(i.getId(), 1);

        // Consumo Agua (tipo_servicio = 2) — puede ser null si no hay registros
        BigDecimal consumoAgua = medidorRepo
                .sumConsumoByInfraestructuraIdAndTipoServicio(i.getId(), 2);

        return InfraestructuraResponseDTO.builder()
                .id(i.getId())
                .empresaRuc(i.getEmpresa().getRuc())
                .empresaRazonSocial(i.getEmpresa().getRazonSocial())
                .parentId(i.getParent()  != null ? i.getParent().getId()    : null)
                .parentNombre(i.getParent() != null ? i.getParent().getNombre() : null)
                .tipo(i.getTipo())
                .nombre(i.getNombre())
                .glosa(i.getGlosa())
                .espacioName(i.getEspacioName())
                .totalConsumoElectricidad(consumoElectricidad != null ? consumoElectricidad : BigDecimal.ZERO)
                .totalConsumoAgua(consumoAgua         != null ? consumoAgua         : BigDecimal.ZERO)
                .createdAt(i.getCreatedAt())
                .updatedAt(i.getUpdatedAt())
                .build();
    }
}
