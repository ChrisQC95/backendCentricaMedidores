package com.centricorp.backend.service.impl;

import com.centricorp.backend.dto.RegistroMedidorRequestDTO;
import com.centricorp.backend.dto.RegistroMedidorResponseDTO;
import com.centricorp.backend.entity.Infraestructura;
import com.centricorp.backend.entity.RegistroMedidor;
import com.centricorp.backend.exception.ResourceNotFoundException;
import com.centricorp.backend.repository.InfraestructuraRepository;
import com.centricorp.backend.repository.RegistroMedidorRepository;
import com.centricorp.backend.security.SecurityUtils;
import com.centricorp.backend.security.TenantGuard;
import com.centricorp.backend.service.RegistroMedidorService;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.Period;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RegistroMedidorServiceImpl implements RegistroMedidorService {

    private static final int MAX_REPORTE_ROWS = 5000;
    private static final int MAX_PAGE_SIZE = 100;

    private final RegistroMedidorRepository registroRepo;
    private final InfraestructuraRepository infraRepo;

    @Override
    @Transactional
    public RegistroMedidorResponseDTO create(RegistroMedidorRequestDTO dto) {
        TenantGuard.rejectSuperAdminMutation();
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
        PageRequest pageable = PageRequest.of(normalizePage(page), normalizeSize(size), Sort.by(Sort.Direction.DESC, "fechaRegistro", "createdAt"));

        Page<RegistroMedidor> registros;
        if (SecurityUtils.isSuperAdmin()) {
            registros = tipoServicio != null
                    ? registroRepo.findByTipoServicio(tipoServicio, pageable)
                    : registroRepo.findAll(pageable);
        } else {
            registros = tipoServicio != null
                    ? registroRepo.findByTipoServicioAndTenantId(tipoServicio, TenantGuard.requireTenant(), pageable)
                    : registroRepo.findByTenantId(TenantGuard.requireTenant(), pageable);
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
                    ? registroRepo.findByFechaRegistroBetweenAndTenantId(inicio, fin, TenantGuard.requireTenant())
                    : registroRepo.findByFechaRegistroBetweenAndTipoServicioAndTenantId(
                    inicio, fin, tipoServicio, TenantGuard.requireTenant());
        }

        if (registros.size() > MAX_REPORTE_ROWS) {
            throw new IllegalArgumentException(
                    "El reporte supera el maximo de " + MAX_REPORTE_ROWS + " filas. Reduzca el rango o filtre por servicio."
            );
        }

        return registros.stream().map(this::toDTO).toList();
    }

    @Override
    public ByteArrayInputStream generarReporteExcel(LocalDate desde, LocalDate hasta, Integer tipoServicio) {
        if (desde == null || hasta == null) {
            throw new IllegalArgumentException("Las fechas desde y hasta son requeridas.");
        }
        if (desde.isAfter(hasta)) {
            throw new IllegalArgumentException("La fecha desde no puede ser posterior a la fecha hasta.");
        }
        if (Period.between(desde, hasta.plusDays(1)).toTotalMonths() > 3) {
            throw new IllegalArgumentException("El rango del reporte no puede superar 3 meses.");
        }
        if (tipoServicio != null && tipoServicio != 1 && tipoServicio != 2) {
            throw new IllegalArgumentException("El tipo de servicio debe ser 1 (Luz), 2 (Agua), o no indicarse.");
        }

        List<RegistroMedidor> registros = loadReporte(desde, hasta, tipoServicio);
        if (registros.size() > MAX_REPORTE_ROWS) {
            throw new IllegalArgumentException(
                    "El reporte supera el maximo de " + MAX_REPORTE_ROWS + " filas. Reduzca el rango o filtre por servicio."
            );
        }

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Reporte");
            Row header = sheet.createRow(0);
            String[] headers = {
                    "ID", "Fecha", "Empresa RUC", "Empresa", "Infraestructura ID", "Infraestructura",
                    "Tipo Infraestructura", "Tipo Servicio", "Lectura", "Consumo", "Observacion"
            };
            for (int i = 0; i < headers.length; i++) {
                header.createCell(i).setCellValue(headers[i]);
            }

            int rowIndex = 1;
            for (RegistroMedidor registro : registros) {
                RegistroMedidorResponseDTO dto = toDTO(registro);
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(dto.getId());
                row.createCell(1).setCellValue(dto.getFechaRegistro() != null ? dto.getFechaRegistro().toString() : "");
                row.createCell(2).setCellValue(dto.getEmpresaRuc());
                row.createCell(3).setCellValue(dto.getEmpresaRazonSocial());
                row.createCell(4).setCellValue(dto.getInfraestructuraId());
                row.createCell(5).setCellValue(dto.getInfraestructuraNombre());
                row.createCell(6).setCellValue(dto.getInfraestructuraTipo());
                row.createCell(7).setCellValue(dto.getTipoServicio() != null && dto.getTipoServicio() == 2 ? "Agua" : "Electricidad");
                row.createCell(8).setCellValue(dto.getVoltaje() != null ? dto.getVoltaje().doubleValue() : 0);
                row.createCell(9).setCellValue(dto.getConsumo() != null ? dto.getConsumo().doubleValue() : 0);
                row.createCell(10).setCellValue(dto.getObservacion() != null ? dto.getObservacion() : "");
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo generar el reporte Excel", ex);
        }
    }

    private List<RegistroMedidor> loadReporte(LocalDate inicio, LocalDate fin, Integer tipoServicio) {
        if (SecurityUtils.isSuperAdmin()) {
            return tipoServicio == null
                    ? registroRepo.findByFechaRegistroBetween(inicio, fin)
                    : registroRepo.findByFechaRegistroBetweenAndTipoServicio(inicio, fin, tipoServicio);
        }

        String tenantId = TenantGuard.requireTenant();
        return tipoServicio == null
                ? registroRepo.findByFechaRegistroBetweenAndTenantId(inicio, fin, tenantId)
                : registroRepo.findByFechaRegistroBetweenAndTipoServicioAndTenantId(inicio, fin, tipoServicio, tenantId);
    }

    private Infraestructura getInfraestructuraOrThrow(Integer id) {
        return SecurityUtils.isSuperAdmin()
                ? infraRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Infraestructura", id))
                : infraRepo.findByIdAndTenantId(id, TenantGuard.requireTenant())
                .orElseThrow(() -> new ResourceNotFoundException("Infraestructura", id));
    }

    private RegistroMedidor getRegistroOrFallback(RegistroMedidor saved) {
        return SecurityUtils.isSuperAdmin()
                ? registroRepo.findById(saved.getId()).orElse(saved)
                : registroRepo.findByIdAndTenantId(saved.getId(), TenantGuard.requireTenant()).orElse(saved);
    }

    private int normalizePage(int page) {
        return Math.max(page, 0);
    }

    private int normalizeSize(int size) {
        return Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
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
