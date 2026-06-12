package com.centricorp.backend.service.impl;

import com.centricorp.backend.dto.DashboardStatsDTO;
import com.centricorp.backend.dto.DashboardStatsDTO.*;
import com.centricorp.backend.entity.TipoNivel;
import com.centricorp.backend.repository.EmpresaRepository;
import com.centricorp.backend.repository.InfraestructuraRepository;
import com.centricorp.backend.repository.RegistroMedidorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Lógica de negocio para el Dashboard administrativo.
 * Devuelve toda la data en un solo DTO; el frontend filtra en memoria por tipo de servicio.
 */
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl {

    private final EmpresaRepository         empresaRepo;
    private final InfraestructuraRepository  infraRepo;
    private final RegistroMedidorRepository  medidorRepo;

    /** Etiquetas cortas de mes en español (índice 0 = Enero) */
    private static final String[] MESES_CORTOS = {
            "Ene","Feb","Mar","Abr","May","Jun","Jul","Ago","Sep","Oct","Nov","Dic"
    };

    public DashboardStatsDTO getStats(Integer tipoServicio) {
        if (tipoServicio == null) tipoServicio = 1;

        // ── KPI Cards ──────────────────────────────────────────────────────────
        long totalEmpresas       = empresaRepo.count();
        long totalEdificios      = infraRepo.countByTipo(TipoNivel.UNIDAD);
        long totalPisos          = infraRepo.countByTipo(TipoNivel.PISO);
        long totalEnst           = infraRepo.countByTipo(TipoNivel.ENST);
        long totalPuntosMedicion = infraRepo.count();

        BigDecimal totalElectricidad = Objects.requireNonNullElse(medidorRepo.sumTotalConsumoByTipoServicio(1), BigDecimal.ZERO);
        BigDecimal totalAgua         = Objects.requireNonNullElse(medidorRepo.sumTotalConsumoByTipoServicio(2), BigDecimal.ZERO);

        // ── Pendientes de lectura en el mes actual ─────────────────────────────
        LocalDate hoy          = LocalDate.now();
        LocalDate inicioMes    = hoy.withDayOfMonth(1);
        long conLecturaMes     = medidorRepo.countDistinctInfraestructuraByFechaRegistroBetween(inicioMes, hoy);
        long pendingReadings   = Math.max(0L, totalPuntosMedicion - conLecturaMes);

        // ── Tendencia de consumo: mes actual vs mes anterior ───────────────────
        LocalDate inicioMesAnt = inicioMes.minusMonths(1);
        LocalDate finMesAnt    = inicioMes.minusDays(1);

        BigDecimal consumoMesAct = Objects.requireNonNullElse(
                medidorRepo.sumConsumoByFechaRegistroBetween(inicioMes, hoy), BigDecimal.ZERO);
        BigDecimal consumoMesAnt = Objects.requireNonNullElse(
                medidorRepo.sumConsumoByFechaRegistroBetween(inicioMesAnt, finMesAnt), BigDecimal.ZERO);

        Double consumoTendenciaPct = null;
        if (consumoMesAnt.compareTo(BigDecimal.ZERO) != 0) {
            consumoTendenciaPct = consumoMesAct.subtract(consumoMesAnt)
                    .divide(consumoMesAnt, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .doubleValue();
        }

        // ── Últimos 6 meses base ───────────────────────────────────────────────
        LocalDate seisMesesAtras = hoy.minusMonths(5).withDayOfMonth(1);
        List<String> meses       = buildMonthSeries(seisMesesAtras);

        // ── Lecturas por mes (total, sin filtro de servicio) ──────────────────
        List<Object[]> rawMedidores = medidorRepo.countByMes(seisMesesAtras, tipoServicio);
        List<MedidoresPorMesDTO> medidoresPorMes = buildMedidoresPorMes(rawMedidores, seisMesesAtras);

        // ── Consumo mensual separado por servicio ─────────────────────────────
        List<Object[]> rawLuz   = medidorRepo.sumConsumoByMes(seisMesesAtras, 1);
        List<Object[]> rawAgua  = medidorRepo.sumConsumoByMes(seisMesesAtras, 2);
        List<ConsumoMensualDTO> consumoMensualLuz  = buildConsumoMensual(rawLuz,  meses);
        List<ConsumoMensualDTO> consumoMensualAgua = buildConsumoMensual(rawAgua, meses);

        // ── Actividad Reciente: últimos 10 registros ──────────────────────────
        List<ActividadRecienteDTO> actividad = medidorRepo.findTop10ByOrderByCreatedAtDesc()
                .stream()
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

    // ── Helpers ───────────────────────────────────────────────────────────────

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

    /**
     * Construye una lista de ConsumoMensualDTO desde una raw query.
     * Como ahora el DTO tiene consumoLuz/consumoAgua, el campo que se use
     * depende del llamador; aquí siempre se escribe consumoLuz para unificidad
     * — en el DTO compuesto el frontend puede mezclar ambas series.
     */
    private List<ConsumoMensualDTO> buildConsumoMensual(List<Object[]> raw, List<String> meses) {
        Map<String, BigDecimal> byMonth = new LinkedHashMap<>();
        raw.forEach(row -> byMonth.put((String) row[0], (BigDecimal) row[1]));
        return meses.stream()
                .map(ym -> ConsumoMensualDTO.builder()
                        .mes(mesLabel(ym))
                        .consumoLuz(byMonth.getOrDefault(ym, BigDecimal.ZERO))
                        .consumoAgua(BigDecimal.ZERO) // placeholder; el llamador sobreescribe si necesita
                        .build())
                .toList();
    }

    private List<String> buildMonthSeries(LocalDate desde) {
        List<String> series = new ArrayList<>();
        LocalDate cursor = desde;
        LocalDate now    = LocalDate.now();
        while (!cursor.isAfter(now)) {
            series.add(String.format("%04d-%02d", cursor.getYear(), cursor.getMonthValue()));
            cursor = cursor.plusMonths(1);
        }
        return series;
    }

    private String mesLabel(String ym) {
        int mes  = Integer.parseInt(ym.substring(5, 7));
        int anio = Integer.parseInt(ym.substring(2, 4));
        return MESES_CORTOS[mes - 1] + "-" + String.format("%02d", anio);
    }
}
