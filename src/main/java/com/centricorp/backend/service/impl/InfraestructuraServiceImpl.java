package com.centricorp.backend.service.impl;

import com.centricorp.backend.dto.InfraestructuraRequestDTO;
import com.centricorp.backend.dto.InfraestructuraResponseDTO;
import com.centricorp.backend.entity.Empresa;
import com.centricorp.backend.entity.Infraestructura;
import com.centricorp.backend.exception.ResourceNotFoundException;
import com.centricorp.backend.repository.EmpresaRepository;
import com.centricorp.backend.repository.InfraestructuraRepository;
import com.centricorp.backend.repository.RegistroMedidorRepository;
import com.centricorp.backend.security.SecurityUtils;
import com.centricorp.backend.security.TenantGuard;
import com.centricorp.backend.service.InfraestructuraService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InfraestructuraServiceImpl implements InfraestructuraService {

    private static final int MAX_PAGE_SIZE = 100;

    private final InfraestructuraRepository infraRepo;
    private final EmpresaRepository empresaRepo;
    private final RegistroMedidorRepository medidorRepo;

    @Override
    public Page<InfraestructuraResponseDTO> findAll(int page, int size) {
        PageRequest pageable = PageRequest.of(normalizePage(page), normalizeSize(size), Sort.by(Sort.Direction.ASC, "nombre"));

        Page<Infraestructura> items = SecurityUtils.isSuperAdmin()
                ? infraRepo.findAll(pageable)
                : infraRepo.findByTenantId(TenantGuard.requireTenant(), pageable);

        Map<Integer, Map<Integer, BigDecimal>> consumos = loadConsumos(items.getContent());
        return items.map(item -> toDTO(item, consumos));
    }

    @Override
    public Page<InfraestructuraResponseDTO> search(String empresaRuc, String q, int page, int size) {
        String term = q == null ? "" : q.trim();
        String ruc = empresaRuc == null ? "" : empresaRuc.trim();
        PageRequest pageable = PageRequest.of(normalizePage(page), normalizeSearchSize(size), Sort.by(Sort.Direction.ASC, "nombre"));

        Page<Infraestructura> items;
        if (SecurityUtils.isSuperAdmin()) {
            items = ruc.isBlank()
                    ? infraRepo.findByNombreContainingIgnoreCase(term, pageable)
                    : infraRepo.findByEmpresaRucAndNombreContainingIgnoreCase(ruc, term, pageable);
        } else {
            String tenantId = TenantGuard.requireTenant();
            items = ruc.isBlank()
                    ? infraRepo.findByTenantIdAndNombreContainingIgnoreCase(tenantId, term, pageable)
                    : infraRepo.findByEmpresaRucAndTenantIdAndNombreContainingIgnoreCase(ruc, tenantId, term, pageable);
        }

        Map<Integer, Map<Integer, BigDecimal>> consumos = loadConsumos(items.getContent());
        return items.map(item -> toDTO(item, consumos));
    }

    @Override
    public InfraestructuraResponseDTO findById(Integer id) {
        Infraestructura item = getOrThrow(id);
        return toDTO(item, loadConsumos(List.of(item)));
    }

    @Override
    public List<InfraestructuraResponseDTO> findByEmpresaRuc(String ruc) {
        getEmpresaOrThrow(ruc);

        List<Infraestructura> items = SecurityUtils.isSuperAdmin()
                ? infraRepo.findByEmpresaRuc(ruc)
                : infraRepo.findByEmpresaRucAndTenantId(ruc, TenantGuard.requireTenant());

        Map<Integer, Map<Integer, BigDecimal>> consumos = loadConsumos(items);
        return items.stream().map(item -> toDTO(item, consumos)).toList();
    }

    @Override
    @Transactional
    public InfraestructuraResponseDTO create(InfraestructuraRequestDTO dto) {
        TenantGuard.rejectSuperAdminMutation();
        Empresa empresa = getEmpresaOrThrow(dto.getEmpresaRuc());

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

        Infraestructura saved = infraRepo.save(infra);
        return toDTO(saved, loadConsumos(List.of(saved)));
    }

    @Override
    @Transactional
    public InfraestructuraResponseDTO update(Integer id, InfraestructuraRequestDTO dto) {
        TenantGuard.rejectSuperAdminMutation();
        Infraestructura infra = getOrThrow(id);

        if (dto.getEmpresaRuc() != null && !dto.getEmpresaRuc().equals(infra.getEmpresa().getRuc())) {
            infra.setEmpresa(getEmpresaOrThrow(dto.getEmpresaRuc()));
        }

        if (dto.getParentId() != null) {
            infra.setParent(getOrThrow(dto.getParentId()));
        } else {
            infra.setParent(null);
        }

        if (dto.getTipo() != null) infra.setTipo(dto.getTipo());
        if (dto.getNombre() != null) infra.setNombre(dto.getNombre());
        infra.setGlosa(dto.getGlosa());
        infra.setEspacioName(dto.getEspacioName());

        Infraestructura saved = infraRepo.save(infra);
        return toDTO(saved, loadConsumos(List.of(saved)));
    }

    @Override
    @Transactional
    public void delete(Integer id) {
        TenantGuard.rejectSuperAdminMutation();
        Infraestructura infra = getOrThrow(id);
        infraRepo.delete(infra);
    }

    private Infraestructura getOrThrow(Integer id) {
        return SecurityUtils.isSuperAdmin()
                ? infraRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Infraestructura", id))
                : infraRepo.findByIdAndTenantId(id, TenantGuard.requireTenant())
                .orElseThrow(() -> new ResourceNotFoundException("Infraestructura", id));
    }

    private Empresa getEmpresaOrThrow(String ruc) {
        return SecurityUtils.isSuperAdmin()
                ? empresaRepo.findById(ruc)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa", ruc))
                : empresaRepo.findByRucAndTenantId(ruc, TenantGuard.requireTenant())
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

    private Map<Integer, Map<Integer, BigDecimal>> loadConsumos(List<Infraestructura> infraestructuras) {
        List<Integer> ids = infraestructuras.stream()
                .map(Infraestructura::getId)
                .toList();
        if (ids.isEmpty()) {
            return Map.of();
        }

        String tenantId = SecurityUtils.isSuperAdmin() ? null : TenantGuard.requireTenant();
        List<Object[]> rows = medidorRepo.sumConsumoByInfraestructuraIds(ids, tenantId);
        Map<Integer, Map<Integer, BigDecimal>> consumos = new HashMap<>();

        for (Object[] row : rows) {
            Integer infraestructuraId = ((Number) row[0]).intValue();
            Integer tipoServicio = ((Number) row[1]).intValue();
            BigDecimal total = (BigDecimal) row[2];
            consumos.computeIfAbsent(infraestructuraId, ignored -> new HashMap<>())
                    .put(tipoServicio, total);
        }

        return consumos;
    }

    private InfraestructuraResponseDTO toDTO(Infraestructura i, Map<Integer, Map<Integer, BigDecimal>> consumos) {
        Map<Integer, BigDecimal> consumoPorTipo = consumos.getOrDefault(i.getId(), Map.of());
        BigDecimal consumoElectricidad = consumoPorTipo.getOrDefault(1, BigDecimal.ZERO);
        BigDecimal consumoAgua = consumoPorTipo.getOrDefault(2, BigDecimal.ZERO);

        return InfraestructuraResponseDTO.builder()
                .id(i.getId())
                .empresaRuc(i.getEmpresa().getRuc())
                .empresaRazonSocial(i.getEmpresa().getRazonSocial())
                .parentId(i.getParent() != null ? i.getParent().getId() : null)
                .parentNombre(i.getParent() != null ? i.getParent().getNombre() : null)
                .tipo(i.getTipo())
                .nombre(i.getNombre())
                .glosa(i.getGlosa())
                .espacioName(i.getEspacioName())
                .totalConsumoElectricidad(consumoElectricidad)
                .totalConsumoAgua(consumoAgua)
                .createdAt(i.getCreatedAt())
                .updatedAt(i.getUpdatedAt())
                .build();
    }
}
