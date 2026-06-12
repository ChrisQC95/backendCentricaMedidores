package com.centricorp.backend.repository;

import com.centricorp.backend.entity.RegistroMedidor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Repository para la tabla "registro_medidores".
 *
 * Método personalizado:
 *   findByFechaRegistroBetween — extrae registros en un rango de fechas
 *   para la generación del reporte / exportación a Excel.
 */
@Repository
public interface RegistroMedidorRepository extends JpaRepository<RegistroMedidor, Integer> {

    /**
     * Retorna registros cuya fecha_registro esté entre 'inicio' y 'fin' (inclusive)
     * y filtra por el tipo de servicio.
     * Usado por GET /api/medidores/reporte?mes=X&anio=Y&tipoServicio=Z
     *
     * @param inicio Primer día del periodo (ej. 2025-05-01)
     * @param fin    Último día del periodo  (ej. 2025-05-31)
     * @param tipoServicio 1=Luz, 2=Agua
     */
    List<RegistroMedidor> findByFechaRegistroBetweenAndTipoServicio(LocalDate inicio, LocalDate fin, Integer tipoServicio);

    /**
     * Cuenta registros agrupados por año-mes para el gráfico de barras del Dashboard, filtrado por tipo de servicio.
     * Retorna Object[] { "YYYY-MM" (String), count (Long) } ordenado ASC.
     */
    @Query("SELECT FUNCTION('TO_CHAR', r.fechaRegistro, 'YYYY-MM'), COUNT(r) " +
           "FROM RegistroMedidor r " +
           "WHERE r.fechaRegistro >= :inicio AND r.tipoServicio = :tipoServicio " +
           "GROUP BY FUNCTION('TO_CHAR', r.fechaRegistro, 'YYYY-MM') " +
           "ORDER BY FUNCTION('TO_CHAR', r.fechaRegistro, 'YYYY-MM') ASC")
    List<Object[]> countByMes(@Param("inicio") LocalDate inicio, @Param("tipoServicio") Integer tipoServicio);

    /**
     * Sumatoria de consumo agrupada por año-mes para el gráfico de líneas, filtrado por tipo de servicio.
     * Retorna Object[] { "YYYY-MM", sumConsumo (BigDecimal) }.
     * Excluye registros con consumo NULL (primer registro sin referencia).
     */
    @Query("SELECT FUNCTION('TO_CHAR', r.fechaRegistro, 'YYYY-MM'), SUM(r.consumo) " +
           "FROM RegistroMedidor r " +
           "WHERE r.fechaRegistro >= :inicio AND r.consumo IS NOT NULL AND r.tipoServicio = :tipoServicio " +
           "GROUP BY FUNCTION('TO_CHAR', r.fechaRegistro, 'YYYY-MM') " +
           "ORDER BY FUNCTION('TO_CHAR', r.fechaRegistro, 'YYYY-MM') ASC")
    List<Object[]> sumConsumoByMes(@Param("inicio") LocalDate inicio, @Param("tipoServicio") Integer tipoServicio);
    
    /**
     * Retorna todos los registros filtrados por tipo de servicio.
     */
    List<RegistroMedidor> findByTipoServicio(Integer tipoServicio);

    /**
     * Los 5 registros más recientes (por createdAt DESC) para la sección
     * de Actividad Reciente del Dashboard.
     */
    List<RegistroMedidor> findTop5ByOrderByCreatedAtDesc();

    /**
     * Suma el consumo de una infraestructura específica filtrada por tipo de servicio.
     * Retorna null si no hay registros con consumo != NULL para ese par (infraestructura, tipo).
     * Se usa en InfraestructuraServiceImpl.toDTO para calcular totalConsumoElectricidad
     * y totalConsumoAgua de forma estricta — NUNCA mezcla tipos distintos.
     *
     * @param infraestructuraId ID del nodo de infraestructura
     * @param tipoServicio      1=Electricidad, 2=Agua
     */
    @Query("SELECT SUM(r.consumo) FROM RegistroMedidor r " +
           "WHERE r.infraestructura.id = :infraestructuraId " +
           "AND r.tipoServicio = :tipoServicio " +
           "AND r.consumo IS NOT NULL")
    java.math.BigDecimal sumConsumoByInfraestructuraIdAndTipoServicio(
            @Param("infraestructuraId") Integer infraestructuraId,
            @Param("tipoServicio") Integer tipoServicio);

    /**
     * Retorna registros en un rango de fechas SIN filtro de tipo de servicio.
     * Usado cuando el reporte exporta "Ambos" tipos (tipoServicio = 0).
     * Cada registro individual tiene su propio tipoServicio para que el frontend
     * muestre la unidad correcta (kWh o m³) por fila.
     *
     * @param inicio Primer día del periodo
     * @param fin    Último día del periodo
     */
    List<RegistroMedidor> findByFechaRegistroBetween(LocalDate inicio, LocalDate fin);
}

