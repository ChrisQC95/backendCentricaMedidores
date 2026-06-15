package com.centricorp.backend.service.impl;

import com.centricorp.backend.dto.EmpresaDTO;
import com.centricorp.backend.entity.Empresa;
import com.centricorp.backend.exception.ResourceNotFoundException;
import com.centricorp.backend.repository.EmpresaRepository;
import com.centricorp.backend.security.SecurityUtils;
import com.centricorp.backend.security.TenantContext;
import com.centricorp.backend.service.EmpresaService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmpresaServiceImpl implements EmpresaService {

    private final EmpresaRepository empresaRepository;

    @Override
    public Page<EmpresaDTO> findAll(int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "razonSocial"));

        Page<Empresa> empresas = SecurityUtils.isSuperAdmin()
                ? empresaRepository.findAll(pageable)
                : empresaRepository.findByTenantId(currentTenant(), pageable);

        return empresas.map(this::toDTO);
    }

    @Override
    public EmpresaDTO findById(String ruc) {
        return toDTO(getOrThrow(ruc));
    }

    @Override
    @Transactional
    public EmpresaDTO create(EmpresaDTO dto) {
        boolean exists = SecurityUtils.isSuperAdmin()
                ? empresaRepository.existsById(dto.getRuc())
                : empresaRepository.existsByRucAndTenantId(dto.getRuc(), currentTenant());

        if (exists) {
            throw new IllegalArgumentException("Ya existe una empresa con RUC: " + dto.getRuc());
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

    private Empresa getOrThrow(String ruc) {
        return SecurityUtils.isSuperAdmin()
                ? empresaRepository.findById(ruc)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa", ruc))
                : empresaRepository.findByRucAndTenantId(ruc, currentTenant())
                .orElseThrow(() -> new ResourceNotFoundException("Empresa", ruc));
    }

    private String currentTenant() {
        return TenantContext.getCurrentTenant();
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
