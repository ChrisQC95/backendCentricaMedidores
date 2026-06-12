package com.centricorp.backend.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO de salida del endpoint GET /api/dashboard/stats.
 * Agrupa KPI cards, datos de gráficos y actividad reciente en un solo objeto.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardStatsDTO {

    // ── KPI Cards ──────────────────────────────────────────────────────────────

    /** Número total de empresas registradas */
    private long totalEmpresas;

    /** Consumo global eléctrico */
    private BigDecimal totalElectricidad;

    /** Consumo global de agua */
    private BigDecimal totalAgua;

    /** Total de infraestructuras (puntos de medición) */
    private long totalPuntosMedicion;

    /** Número de nodos de infraestructura con tipo UNIDAD (edificios) */
    private long totalEdificios;

    /** Número de nodos de infraestructura con tipo PISO */
    private long totalPisos;

    /** Número de nodos de infraestructura con tipo ENST */
    private long totalEnst;

    // ── Nuevos KPI: Pendientes + Tendencia ────────────────────────────────

    /** Medidores pendientes de lectura en el mes actual */
    private long pendingReadings;

    /**
     * Variación porcentual del consumo total entre el mes actual y el anterior.
     * Positivo = subió, Negativo = bajó.
     */
    private Double consumoTendenciaPct;

    // ── Gráfico de Barras: Registros de medidores por mes ─────────────────────

    /** Lista de los últimos 6 meses con conteo de medidores registrados */
    private List<MedidoresPorMesDTO> medidoresPorMes;

    // ── Gráficos de consumo por servicio ─────────────────────────────────

    /** Consumo mensual solo de electricidad (tipo_servicio=1) */
    private List<ConsumoMensualDTO> consumoMensualLuz;

    /** Consumo mensual solo de agua (tipo_servicio=2) */
    private List<ConsumoMensualDTO> consumoMensualAgua;

    // ── Actividad Reciente ───────────────────────────────────────────────

    /** Últimos 10 registros de medidores insertados en la BD */
    private List<ActividadRecienteDTO> actividadReciente;

    // ══════════════════════════════════════════════════════════════════════════
    // ── DTOs anidados ─────────────────────────────────────────────────────────
    // ══════════════════════════════════════════════════════════════════════════

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MedidoresPorMesDTO {
        /** Etiqueta corta del mes: "Ene", "Feb", etc. */
        private String mes;
        /** Número de registros de medidores en ese mes */
        private long registrados;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ConsumoMensualDTO {
        /** Etiqueta corta del mes */
        private String mes;
        /** Sumatoria de consumo de luz (kWh) */
        private BigDecimal consumoLuz;
        /** Sumatoria de consumo de agua (m³) */
        private BigDecimal consumoAgua;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ActividadRecienteDTO {
        private Integer id;
        /** Nombre del punto de medición (infraestructura.nombre) */
        private String puntoMedicion;
        /** Tipo del nodo: ENST, PISO, etc. */
        private String tipo;
        /** Empresa asociada */
        private String empresaNombre;
        /** Voltaje registrado */
        private BigDecimal voltaje;
        /** Consumo calculado (puede ser null) */
        private BigDecimal consumo;
        /** Fecha del registro en formato ISO */
        private String fechaRegistro;
        /** createdAt — para calcular tiempo relativo en el frontend */
        private String createdAt;
        /** Tipo de servicio: 1=Electricidad, 2=Agua */
        private Integer tipoServicio;
    }
}
