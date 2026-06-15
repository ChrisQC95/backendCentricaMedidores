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
import com.centricorp.backend.security.TenantContext;
import com.centricorp.backend.service.InfraestructuraService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InfraestructuraServiceImpl implements InfraestructuraService {

    private final InfraestructuraRepository infraRepo;
    private final EmpresaRepository empresaRepo;
    private final RegistroMedidorRepository medidorRepo;

    @Override
    public Page<InfraestructuraResponseDTO> findAll(int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "nombre"));

        Page<Infraestructura> items = SecurityUtils.isSuperAdmin()
                ? infraRepo.findAll(pageable)
                : infraRepo.findByTenantId(currentTenant(), pageable);

        return items.map(this::toDTO);
    }

    @Override
    public InfraestructuraResponseDTO findById(Integer id) {
        return toDTO(getOrThrow(id));
    }

    @Override
    public List<InfraestructuraResponseDTO> findByEmpresaRuc(String ruc) {
        getEmpresaOrThrow(ruc);

        List<Infraestructura> items = SecurityUtils.isSuperAdmin()
                ? infraRepo.findByEmpresaRuc(ruc)
                : infraRepo.findByEmpresaRucAndTenantId(ruc, currentTenant());

        return items.stream().map(this::toDTO).toList();
    }

    @Override
    @Transactional
    public InfraestructuraResponseDTO create(InfraestructuraRequestDTO dto) {
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

        return toDTO(infraRepo.save(infra));
    }

    @Override
    @Transactional
    public InfraestructuraResponseDTO update(Integer id, InfraestructuraRequestDTO dto) {
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

        return toDTO(infraRepo.save(infra));
    }

    @Override
    @Transactional
    public void delete(Integer id) {
        Infraestructura infra = getOrThrow(id);
        infraRepo.delete(infra);
    }

    private Infraestructura getOrThrow(Integer id) {
        return SecurityUtils.isSuperAdmin()
                ? infraRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Infraestructura", id))
                : infraRepo.findByIdAndTenantId(id, currentTenant())
                .orElseThrow(() -> new ResourceNotFoundException("Infraestructura", id));
    }

    private Empresa getEmpresaOrThrow(String ruc) {
        return SecurityUtils.isSuperAdmin()
                ? empresaRepo.findById(ruc)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa", ruc))
                : empresaRepo.findByRucAndTenantId(ruc, currentTenant())
                .orElseThrow(() -> new ResourceNotFoundException("Empresa", ruc));
    }

    private BigDecimal sumConsumo(Integer infraestructuraId, Integer tipoServicio) {
        return SecurityUtils.isSuperAdmin()
                ? medidorRepo.sumConsumoByInfraestructuraIdAndTipoServicio(infraestructuraId, tipoServicio)
                : medidorRepo.sumConsumoByInfraestructuraIdAndTipoServicioAndTenantId(
                infraestructuraId, tipoServicio, currentTenant());
    }

    private String currentTenant() {
        return TenantContext.getCurrentTenant();
    }

    private InfraestructuraResponseDTO toDTO(Infraestructura i) {
        BigDecimal consumoElectricidad = sumConsumo(i.getId(), 1);
        BigDecimal consumoAgua = sumConsumo(i.getId(), 2);

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
                .totalConsumoElectricidad(consumoElectricidad != null ? consumoElectricidad : BigDecimal.ZERO)
                .totalConsumoAgua(consumoAgua != null ? consumoAgua : BigDecimal.ZERO)
                .createdAt(i.getCreatedAt())
                .updatedAt(i.getUpdatedAt())
                .build();
    }
}
