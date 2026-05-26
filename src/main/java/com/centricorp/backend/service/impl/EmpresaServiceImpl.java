package com.centricorp.backend.service.impl;

import com.centricorp.backend.dto.EmpresaDTO;
import com.centricorp.backend.entity.Empresa;
import com.centricorp.backend.exception.ResourceNotFoundException;
import com.centricorp.backend.repository.EmpresaRepository;
import com.centricorp.backend.service.EmpresaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmpresaServiceImpl implements EmpresaService {

    private final EmpresaRepository empresaRepository;

    @Override
    public List<EmpresaDTO> findAll() {
        return empresaRepository.findAll().stream()
                .map(this::toDTO)
                .toList();
    }

    @Override
    public EmpresaDTO findById(String ruc) {
        return toDTO(getOrThrow(ruc));
    }

    @Override
    @Transactional
    public EmpresaDTO create(EmpresaDTO dto) {
        if (empresaRepository.existsById(dto.getRuc())) {
            throw new IllegalArgumentException(
                    "Ya existe una empresa con RUC: " + dto.getRuc());
        }
        Empresa empresa = Empresa.builder()
                .ruc(dto.getRuc())
                .razonSocial(dto.getRazonSocial())
                .build();
        return toDTO(empresaRepository.save(empresa));
    }

    @Override
    @Transactional
    public EmpresaDTO update(String ruc, EmpresaDTO dto) {
        Empresa empresa = getOrThrow(ruc);
        empresa.setRazonSocial(dto.getRazonSocial());
        return toDTO(empresaRepository.save(empresa));
    }

    @Override
    @Transactional
    public void delete(String ruc) {
        Empresa empresa = getOrThrow(ruc);
        empresaRepository.delete(empresa);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private Empresa getOrThrow(String ruc) {
        return empresaRepository.findById(ruc)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa", ruc));
    }

    private EmpresaDTO toDTO(Empresa e) {
        return EmpresaDTO.builder()
                .ruc(e.getRuc())
                .razonSocial(e.getRazonSocial())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }
}
