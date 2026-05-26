package com.centricorp.backend.service.impl;

import com.centricorp.backend.dto.InfraestructuraRequestDTO;
import com.centricorp.backend.dto.InfraestructuraResponseDTO;
import com.centricorp.backend.entity.Empresa;
import com.centricorp.backend.entity.Infraestructura;
import com.centricorp.backend.exception.ResourceNotFoundException;
import com.centricorp.backend.repository.EmpresaRepository;
import com.centricorp.backend.repository.InfraestructuraRepository;
import com.centricorp.backend.service.InfraestructuraService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InfraestructuraServiceImpl implements InfraestructuraService {

    private final InfraestructuraRepository infraRepo;
    private final EmpresaRepository empresaRepo;

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

        if (dto.getTipo() != null) infra.setTipo(dto.getTipo());
        if (dto.getNombre() != null) infra.setNombre(dto.getNombre());
        infra.setGlosa(dto.getGlosa());

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

    private InfraestructuraResponseDTO toDTO(Infraestructura i) {
        return InfraestructuraResponseDTO.builder()
                .id(i.getId())
                .empresaRuc(i.getEmpresa().getRuc())
                .empresaRazonSocial(i.getEmpresa().getRazonSocial())
                .parentId(i.getParent() != null ? i.getParent().getId() : null)
                .parentNombre(i.getParent() != null ? i.getParent().getNombre() : null)
                .tipo(i.getTipo())
                .nombre(i.getNombre())
                .glosa(i.getGlosa())
                .createdAt(i.getCreatedAt())
                .updatedAt(i.getUpdatedAt())
                .build();
    }
}
