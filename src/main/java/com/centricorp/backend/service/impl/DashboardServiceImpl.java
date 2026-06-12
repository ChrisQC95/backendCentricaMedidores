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
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Lógica de negocio para el Dashboard administrativo.
 * Agrega datos de múltiples repositorios en un solo DTO de respuesta.
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
        if (tipoServicio == null) tipoServicio = 1; // Default a Luz

        // ── KPI Cards ──────────────────────────────────────────────────────────
        long totalEmpresas  = empresaRepo.count();
        long totalEdificios = infraRepo.countByTipo(TipoNivel.UNIDAD);
        long totalPisos     = infraRepo.countByTipo(TipoNivel.PISO);
        long totalEnst      = infraRepo.countByTipo(TipoNivel.ENST);

        // ── Últimos 6 meses ────────────────────────────────────────────────────
        LocalDate seisMesesAtras = LocalDate.now().minusMonths(5).withDayOfMonth(1);

        // ── Gráfico de Barras: Medidores por mes ───────────────────────────────
        List<Object[]> rawMedidores = medidorRepo.countByMes(seisMesesAtras, tipoServicio);
        List<MedidoresPorMesDTO> medidoresPorMes = buildMedidoresPorMes(rawMedidores, seisMesesAtras);

        // ── Gráfico de Líneas: Consumo mensual ─────────────────────────────────
        List<Object[]> rawConsumo = medidorRepo.sumConsumoByMes(seisMesesAtras, tipoServicio);
        List<ConsumoMensualDTO> consumoMensual = buildConsumoMensual(rawConsumo, seisMesesAtras);

        // ── Actividad Reciente ─────────────────────────────────────────────────
        List<ActividadRecienteDTO> actividad = medidorRepo.findTop5ByOrderByCreatedAtDesc()
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
                        .build())
                .toList();

        return DashboardStatsDTO.builder()
                .totalEmpresas(totalEmpresas)
                .totalEdificios(totalEdificios)
                .totalPisos(totalPisos)
                .totalEnst(totalEnst)
                .medidoresPorMes(medidoresPorMes)
                .consumoMensual(consumoMensual)
                .actividadReciente(actividad)
                .build();
    }

    // ── Helpers de transformación ──────────────────────────────────────────────

    /**
     * Genera la lista de los últimos 6 meses con conteo de registros.
     * Rellena con 0 los meses sin datos para que el gráfico sea siempre continuo.
     */
    private List<MedidoresPorMesDTO> buildMedidoresPorMes(
            List<Object[]> raw, LocalDate desde) {

        // Convertir resultado JPQL a Map "YYYY-MM" → count
        Map<String, Long> byMonth = new LinkedHashMap<>();
        raw.forEach(row -> byMonth.put((String) row[0], (Long) row[1]));

        return buildMonthSeries(desde).stream()
                .map(ym -> MedidoresPorMesDTO.builder()
                        .mes(mesLabel(ym))
                        .registrados(byMonth.getOrDefault(ym, 0L))
                        .build())
                .toList();
    }

    private List<ConsumoMensualDTO> buildConsumoMensual(
            List<Object[]> raw, LocalDate desde) {

        Map<String, BigDecimal> byMonth = new LinkedHashMap<>();
        raw.forEach(row -> byMonth.put((String) row[0], (BigDecimal) row[1]));

        return buildMonthSeries(desde).stream()
                .map(ym -> ConsumoMensualDTO.builder()
                        .mes(mesLabel(ym))
                        .consumo(byMonth.getOrDefault(ym, BigDecimal.ZERO))
                        .build())
                .toList();
    }

    /** Genera la secuencia de 6 meses en formato "YYYY-MM" desde 'desde' hasta hoy */
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

    /** Convierte "YYYY-MM" a etiqueta corta "Ene-26" */
    private String mesLabel(String ym) {
        int mes  = Integer.parseInt(ym.substring(5, 7));
        int anio = Integer.parseInt(ym.substring(2, 4));
        return MESES_CORTOS[mes - 1] + "-" + String.format("%02d", anio);
    }
}
