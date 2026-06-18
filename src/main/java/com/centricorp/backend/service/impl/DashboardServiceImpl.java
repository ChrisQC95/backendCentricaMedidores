package com.centricorp.backend.service.impl;

import com.centricorp.backend.dto.DashboardStatsDTO;
import com.centricorp.backend.dto.DashboardStatsDTO.ActividadRecienteDTO;
import com.centricorp.backend.dto.DashboardStatsDTO.ConsumoMensualDTO;
import com.centricorp.backend.dto.DashboardStatsDTO.MedidoresPorMesDTO;
import com.centricorp.backend.entity.RegistroMedidor;
import com.centricorp.backend.entity.TipoNivel;
import com.centricorp.backend.repository.EmpresaRepository;
import com.centricorp.backend.repository.InfraestructuraRepository;
import com.centricorp.backend.repository.RegistroMedidorRepository;
import com.centricorp.backend.security.SecurityUtils;
import com.centricorp.backend.security.TenantGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardServiceImpl {

    private final EmpresaRepository empresaRepo;
    private final InfraestructuraRepository infraRepo;
    private final RegistroMedidorRepository medidorRepo;

    private static final String[] MESES_CORTOS = {
            "Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic"
    };

    public DashboardStatsDTO getStats(Integer tipoServicio) {
        if (tipoServicio == null) tipoServicio = 1;

        boolean superAdmin = SecurityUtils.isSuperAdmin();
        String tenantId = superAdmin ? null : TenantGuard.requireTenant();

        long totalEmpresas = superAdmin ? empresaRepo.count() : empresaRepo.countByTenantId(tenantId);
        long totalEdificios = superAdmin
                ? infraRepo.countByTipo(TipoNivel.UNIDAD)
                : infraRepo.countByTipoAndTenantId(TipoNivel.UNIDAD, tenantId);
        long totalPisos = superAdmin
                ? infraRepo.countByTipo(TipoNivel.PISO)
                : infraRepo.countByTipoAndTenantId(TipoNivel.PISO, tenantId);
        long totalEnst = superAdmin
                ? infraRepo.countByTipo(TipoNivel.ENST)
                : infraRepo.countByTipoAndTenantId(TipoNivel.ENST, tenantId);
        long totalPuntosMedicion = superAdmin ? infraRepo.count() : infraRepo.countByTenantId(tenantId);

        BigDecimal totalElectricidad = Objects.requireNonNullElse(
                superAdmin
                        ? medidorRepo.sumTotalConsumoByTipoServicio(1)
                        : medidorRepo.sumTotalConsumoByTipoServicioAndTenantId(1, tenantId),
                BigDecimal.ZERO);
        BigDecimal totalAgua = Objects.requireNonNullElse(
                superAdmin
                        ? medidorRepo.sumTotalConsumoByTipoServicio(2)
                        : medidorRepo.sumTotalConsumoByTipoServicioAndTenantId(2, tenantId),
                BigDecimal.ZERO);

        LocalDate hoy = LocalDate.now();
        LocalDate inicioMes = hoy.withDayOfMonth(1);
        long conLecturaMes = superAdmin
                ? medidorRepo.countDistinctInfraestructuraByFechaRegistroBetween(inicioMes, hoy)
                : medidorRepo.countDistinctInfraestructuraByFechaRegistroBetweenAndTenantId(inicioMes, hoy, tenantId);
        long pendingReadings = Math.max(0L, totalPuntosMedicion - conLecturaMes);

        LocalDate inicioMesAnt = inicioMes.minusMonths(1);
        LocalDate finMesAnt = inicioMes.minusDays(1);

        BigDecimal consumoMesAct = Objects.requireNonNullElse(
                superAdmin
                        ? medidorRepo.sumConsumoByFechaRegistroBetween(inicioMes, hoy)
                        : medidorRepo.sumConsumoByFechaRegistroBetweenAndTenantId(inicioMes, hoy, tenantId),
                BigDecimal.ZERO);
        BigDecimal consumoMesAnt = Objects.requireNonNullElse(
                superAdmin
                        ? medidorRepo.sumConsumoByFechaRegistroBetween(inicioMesAnt, finMesAnt)
                        : medidorRepo.sumConsumoByFechaRegistroBetweenAndTenantId(inicioMesAnt, finMesAnt, tenantId),
                BigDecimal.ZERO);

        Double consumoTendenciaPct = null;
        if (consumoMesAnt.compareTo(BigDecimal.ZERO) != 0) {
            consumoTendenciaPct = consumoMesAct.subtract(consumoMesAnt)
                    .divide(consumoMesAnt, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .doubleValue();
        }

        LocalDate seisMesesAtras = hoy.minusMonths(5).withDayOfMonth(1);
        List<String> meses = buildMonthSeries(seisMesesAtras);

        List<Object[]> rawMedidores = superAdmin
                ? medidorRepo.countByMes(seisMesesAtras, tipoServicio)
                : medidorRepo.countByMesAndTenantId(seisMesesAtras, tipoServicio, tenantId);
        List<MedidoresPorMesDTO> medidoresPorMes = buildMedidoresPorMes(rawMedidores, seisMesesAtras);

        List<Object[]> rawLuz = superAdmin
                ? medidorRepo.sumConsumoByMes(seisMesesAtras, 1)
                : medidorRepo.sumConsumoByMesAndTenantId(seisMesesAtras, 1, tenantId);
        List<Object[]> rawAgua = superAdmin
                ? medidorRepo.sumConsumoByMes(seisMesesAtras, 2)
                : medidorRepo.sumConsumoByMesAndTenantId(seisMesesAtras, 2, tenantId);
        List<ConsumoMensualDTO> consumoMensualLuz = buildConsumoMensual(rawLuz, meses);
        List<ConsumoMensualDTO> consumoMensualAgua = buildConsumoMensual(rawAgua, meses);

        List<RegistroMedidor> recientes = superAdmin
                ? medidorRepo.findTop10ByOrderByCreatedAtDesc()
                : medidorRepo.findTop10ByTenantIdOrderByCreatedAtDesc(tenantId);
        List<ActividadRecienteDTO> actividad = recientes.stream()
                .map(r -> ActividadRecienteDTO.builder()
                        .id(r.getId())
                        .puntoMedicion(r.getInfraestructura().getNombre())
                        .tipo(r.getInfraestructura().getTipo().name())
                        .empresaNombre(r.getInfraestructura().getEmpresa().getRazonSocial())
                        .voltaje(r.getVoltaje())
                        .consumo(r.getConsumo())
                        .fechaRegistro(r.getFechaRegistro().toString())
                        .createdAt(r.getCreatedAt() != null
                                ? r.getCreatedAt().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                                : null)
                        .tipoServicio(r.getTipoServicio())
                        .build())
                .toList();

        return DashboardStatsDTO.builder()
                .totalEmpresas(totalEmpresas)
                .totalElectricidad(totalElectricidad)
                .totalAgua(totalAgua)
                .totalPuntosMedicion(totalPuntosMedicion)
                .totalEdificios(totalEdificios)
                .totalPisos(totalPisos)
                .totalEnst(totalEnst)
                .pendingReadings(pendingReadings)
                .consumoTendenciaPct(consumoTendenciaPct)
                .medidoresPorMes(medidoresPorMes)
                .consumoMensualLuz(consumoMensualLuz)
                .consumoMensualAgua(consumoMensualAgua)
                .actividadReciente(actividad)
                .build();
    }

    private List<MedidoresPorMesDTO> buildMedidoresPorMes(List<Object[]> raw, LocalDate desde) {
        Map<String, Long> byMonth = new LinkedHashMap<>();
        raw.forEach(row -> byMonth.put((String) row[0], (Long) row[1]));
        return buildMonthSeries(desde).stream()
                .map(ym -> MedidoresPorMesDTO.builder()
                        .mes(mesLabel(ym))
                        .registrados(byMonth.getOrDefault(ym, 0L))
                        .build())
                .toList();
    }

    private List<ConsumoMensualDTO> buildConsumoMensual(List<Object[]> raw, List<String> meses) {
        Map<String, BigDecimal> byMonth = new LinkedHashMap<>();
        raw.forEach(row -> byMonth.put((String) row[0], (BigDecimal) row[1]));
        return meses.stream()
                .map(ym -> ConsumoMensualDTO.builder()
                        .mes(mesLabel(ym))
                        .consumoLuz(byMonth.getOrDefault(ym, BigDecimal.ZERO))
                        .consumoAgua(BigDecimal.ZERO)
                        .build())
                .toList();
    }

    private List<String> buildMonthSeries(LocalDate desde) {
        List<String> series = new ArrayList<>();
        LocalDate cursor = desde;
        LocalDate now = LocalDate.now();
        while (!cursor.isAfter(now)) {
            series.add(String.format("%04d-%02d", cursor.getYear(), cursor.getMonthValue()));
            cursor = cursor.plusMonths(1);
        }
        return series;
    }

    private String mesLabel(String ym) {
        int mes = Integer.parseInt(ym.substring(5, 7));
        int anio = Integer.parseInt(ym.substring(2, 4));
        return MESES_CORTOS[mes - 1] + "-" + String.format("%02d", anio);
    }
}
