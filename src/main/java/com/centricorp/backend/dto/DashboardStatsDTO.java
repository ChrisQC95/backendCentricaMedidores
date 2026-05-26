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

    /** Número de nodos de infraestructura con tipo UNIDAD (edificios) */
    private long totalEdificios;

    /** Número de nodos de infraestructura con tipo PISO */
    private long totalPisos;

    /** Número de nodos de infraestructura con tipo ENST */
    private long totalEnst;

    // ── Gráfico de Barras: Registros de medidores por mes ─────────────────────

    /** Lista de los últimos 6 meses con conteo de medidores registrados */
    private List<MedidoresPorMesDTO> medidoresPorMes;

    // ── Gráfico de Líneas: Consumo mensual ────────────────────────────────────

    /** Lista de los últimos 6 meses con sumatoria de consumo */
    private List<ConsumoMensualDTO> consumoMensual;

    // ── Actividad Reciente ─────────────────────────────────────────────────────

    /** Últimos 5 registros de medidores insertados en la BD */
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
        /** Sumatoria de consumo (calculado por trigger) en el mes */
        private BigDecimal consumo;
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
    }
}
