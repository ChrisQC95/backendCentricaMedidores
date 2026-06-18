package com.centricorp.backend.service.impl;

import com.centricorp.backend.dto.EmpresaDTO;
import com.centricorp.backend.entity.Empresa;
import com.centricorp.backend.exception.ResourceNotFoundException;
import com.centricorp.backend.repository.EmpresaRepository;
import com.centricorp.backend.security.SecurityUtils;
import com.centricorp.backend.security.TenantGuard;
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

    private static final int MAX_PAGE_SIZE = 100;

    private final EmpresaRepository empresaRepository;

    @Override
    public Page<EmpresaDTO> findAll(int page, int size) {
        PageRequest pageable = PageRequest.of(normalizePage(page), normalizeSize(size), Sort.by(Sort.Direction.ASC, "razonSocial"));

        Page<Empresa> empresas = SecurityUtils.isSuperAdmin()
                ? empresaRepository.findAll(pageable)
                : empresaRepository.findByTenantId(TenantGuard.requireTenant(), pageable);

        return empresas.map(this::toDTO);
    }

    @Override
    public Page<EmpresaDTO> search(String q, int page, int size) {
        String term = q == null ? "" : q.trim();
        PageRequest pageable = PageRequest.of(normalizePage(page), normalizeSearchSize(size), Sort.by(Sort.Direction.ASC, "razonSocial"));

        Page<Empresa> empresas = SecurityUtils.isSuperAdmin()
                ? empresaRepository.findByRucContainingIgnoreCaseOrRazonSocialContainingIgnoreCase(term, term, pageable)
                : empresaRepository.findByTenantIdAndRucContainingIgnoreCaseOrTenantIdAndRazonSocialContainingIgnoreCase(
                        TenantGuard.requireTenant(), term, TenantGuard.requireTenant(), term, pageable);

        return empresas.map(this::toDTO);
    }

    @Override
    public EmpresaDTO findById(String ruc) {
        return toDTO(getOrThrow(ruc));
    }

    @Override
    @Transactional
    public EmpresaDTO create(EmpresaDTO dto) {
        TenantGuard.rejectSuperAdminMutation();
        String tenantId = TenantGuard.requireTenant();
        boolean exists = empresaRepository.existsByRucAndTenantId(dto.getRuc(), tenantId);

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
        TenantGuard.rejectSuperAdminMutation();
        Empresa empresa = getOrThrow(ruc);
        empresa.setRazonSocial(dto.getRazonSocial());
        return toDTO(empresaRepository.save(empresa));
    }

    @Override
    @Transactional
    public void delete(String ruc) {
        TenantGuard.rejectSuperAdminMutation();
        Empresa empresa = getOrThrow(ruc);
        empresaRepository.delete(empresa);
    }

    private Empresa getOrThrow(String ruc) {
        return SecurityUtils.isSuperAdmin()
                ? empresaRepository.findById(ruc)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa", ruc))
                : empresaRepository.findByRucAndTenantId(ruc, TenantGuard.requireTenant())
                .orElseThrow(() -> new ResourceNotFoundException("Empresa", ruc));
    }

    private int normalizePage(int page) {
        return Math.max(page, 0);
    }

    private int normalizeSize(int size) {
        return Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
    }

    private int normalizeSearchSize(int size) {
        return Math.min(Math.max(size, 1), 50);
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
