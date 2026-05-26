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
     * Retorna registros cuya fecha_registro esté entre 'inicio' y 'fin' (inclusive).
     * Usado por GET /api/medidores/reporte?mes=X&anio=Y
     *
     * @param inicio Primer día del periodo (ej. 2025-05-01)
     * @param fin    Último día del periodo  (ej. 2025-05-31)
     */
    List<RegistroMedidor> findByFechaRegistroBetween(LocalDate inicio, LocalDate fin);

    /**
     * Cuenta registros agrupados por año-mes para el gráfico de barras del Dashboard.
     * Retorna Object[] { "YYYY-MM" (String), count (Long) } ordenado ASC.
     */
    @Query("SELECT FUNCTION('TO_CHAR', r.fechaRegistro, 'YYYY-MM'), COUNT(r) " +
           "FROM RegistroMedidor r " +
           "WHERE r.fechaRegistro >= :inicio " +
           "GROUP BY FUNCTION('TO_CHAR', r.fechaRegistro, 'YYYY-MM') " +
           "ORDER BY FUNCTION('TO_CHAR', r.fechaRegistro, 'YYYY-MM') ASC")
    List<Object[]> countByMes(@Param("inicio") LocalDate inicio);

    /**
     * Sumatoria de consumo agrupada por año-mes para el gráfico de líneas.
     * Retorna Object[] { "YYYY-MM", sumConsumo (BigDecimal) }.
     * Excluye registros con consumo NULL (primer registro sin referencia).
     */
    @Query("SELECT FUNCTION('TO_CHAR', r.fechaRegistro, 'YYYY-MM'), SUM(r.consumo) " +
           "FROM RegistroMedidor r " +
           "WHERE r.fechaRegistro >= :inicio AND r.consumo IS NOT NULL " +
           "GROUP BY FUNCTION('TO_CHAR', r.fechaRegistro, 'YYYY-MM') " +
           "ORDER BY FUNCTION('TO_CHAR', r.fechaRegistro, 'YYYY-MM') ASC")
    List<Object[]> sumConsumoByMes(@Param("inicio") LocalDate inicio);

    /**
     * Los 5 registros más recientes (por createdAt DESC) para la sección
     * de Actividad Reciente del Dashboard.
     */
    List<RegistroMedidor> findTop5ByOrderByCreatedAtDesc();
}
