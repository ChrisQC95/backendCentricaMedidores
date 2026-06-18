package com.centricorp.backend.repository;

import com.centricorp.backend.entity.RegistroMedidor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface RegistroMedidorRepository extends JpaRepository<RegistroMedidor, Integer> {

    // ─── Consultas únicas (sin paginar) ─────────────────────────────────────────

    Optional<RegistroMedidor> findByIdAndTenantId(Integer id, String tenantId);

    // ─── Listados paginados con @EntityGraph para erradicar N+1 ─────────────────

    /**
     * Carga RegistroMedidor + infraestructura + empresa en un solo JOIN FETCH.
     * Erradica el N+1 que se producía al acceder a r.infraestructura.empresa
     * dentro del método toDTO() del servicio.
     */
    @EntityGraph(attributePaths = {"infraestructura", "infraestructura.empresa"})
    Page<RegistroMedidor> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"infraestructura", "infraestructura.empresa"})
    Page<RegistroMedidor> findByTenantId(String tenantId, Pageable pageable);

    @EntityGraph(attributePaths = {"infraestructura", "infraestructura.empresa"})
    Page<RegistroMedidor> findByTipoServicio(Integer tipoServicio, Pageable pageable);

    @EntityGraph(attributePaths = {"infraestructura", "infraestructura.empresa"})
    Page<RegistroMedidor> findByTipoServicioAndTenantId(Integer tipoServicio, String tenantId, Pageable pageable);

    // ─── Listas planas (reporte Excel — sin paginar, solo filtradas por rango) ──

    @EntityGraph(attributePaths = {"infraestructura", "infraestructura.empresa"})
    List<RegistroMedidor> findByFechaRegistroBetween(LocalDate inicio, LocalDate fin);

    @EntityGraph(attributePaths = {"infraestructura", "infraestructura.empresa"})
    List<RegistroMedidor> findByFechaRegistroBetweenAndTenantId(LocalDate inicio, LocalDate fin, String tenantId);

    @EntityGraph(attributePaths = {"infraestructura", "infraestructura.empresa"})
    List<RegistroMedidor> findByFechaRegistroBetweenAndTipoServicio(LocalDate inicio, LocalDate fin, Integer tipoServicio);

    @EntityGraph(attributePaths = {"infraestructura", "infraestructura.empresa"})
    List<RegistroMedidor> findByFechaRegistroBetweenAndTipoServicioAndTenantId(
            LocalDate inicio,
            LocalDate fin,
            Integer tipoServicio,
            String tenantId);

    // ─── Consultas de agregación (Dashboard, Stats) ──────────────────────────────

    @Query("SELECT FUNCTION('TO_CHAR', r.fechaRegistro, 'YYYY-MM'), COUNT(r) " +
           "FROM RegistroMedidor r " +
           "WHERE r.fechaRegistro >= :inicio AND r.tipoServicio = :tipoServicio " +
           "GROUP BY FUNCTION('TO_CHAR', r.fechaRegistro, 'YYYY-MM') " +
           "ORDER BY FUNCTION('TO_CHAR', r.fechaRegistro, 'YYYY-MM') ASC")
    List<Object[]> countByMes(@Param("inicio") LocalDate inicio, @Param("tipoServicio") Integer tipoServicio);

    @Query("SELECT FUNCTION('TO_CHAR', r.fechaRegistro, 'YYYY-MM'), COUNT(r) " +
           "FROM RegistroMedidor r " +
           "WHERE r.fechaRegistro >= :inicio AND r.tipoServicio = :tipoServicio AND r.tenantId = :tenantId " +
           "GROUP BY FUNCTION('TO_CHAR', r.fechaRegistro, 'YYYY-MM') " +
           "ORDER BY FUNCTION('TO_CHAR', r.fechaRegistro, 'YYYY-MM') ASC")
    List<Object[]> countByMesAndTenantId(
            @Param("inicio") LocalDate inicio,
            @Param("tipoServicio") Integer tipoServicio,
            @Param("tenantId") String tenantId);

    @Query("SELECT FUNCTION('TO_CHAR', r.fechaRegistro, 'YYYY-MM'), SUM(r.consumo) " +
           "FROM RegistroMedidor r " +
           "WHERE r.fechaRegistro >= :inicio AND r.consumo IS NOT NULL AND r.tipoServicio = :tipoServicio " +
           "GROUP BY FUNCTION('TO_CHAR', r.fechaRegistro, 'YYYY-MM') " +
           "ORDER BY FUNCTION('TO_CHAR', r.fechaRegistro, 'YYYY-MM') ASC")
    List<Object[]> sumConsumoByMes(@Param("inicio") LocalDate inicio, @Param("tipoServicio") Integer tipoServicio);

    @Query("SELECT FUNCTION('TO_CHAR', r.fechaRegistro, 'YYYY-MM'), SUM(r.consumo) " +
           "FROM RegistroMedidor r " +
           "WHERE r.fechaRegistro >= :inicio AND r.consumo IS NOT NULL " +
           "AND r.tipoServicio = :tipoServicio AND r.tenantId = :tenantId " +
           "GROUP BY FUNCTION('TO_CHAR', r.fechaRegistro, 'YYYY-MM') " +
           "ORDER BY FUNCTION('TO_CHAR', r.fechaRegistro, 'YYYY-MM') ASC")
    List<Object[]> sumConsumoByMesAndTenantId(
            @Param("inicio") LocalDate inicio,
            @Param("tipoServicio") Integer tipoServicio,
            @Param("tenantId") String tenantId);

    @Query("SELECT SUM(r.consumo) FROM RegistroMedidor r WHERE r.tipoServicio = :tipoServicio AND r.consumo IS NOT NULL")
    BigDecimal sumTotalConsumoByTipoServicio(@Param("tipoServicio") Integer tipoServicio);

    @Query("SELECT SUM(r.consumo) FROM RegistroMedidor r " +
           "WHERE r.tipoServicio = :tipoServicio AND r.consumo IS NOT NULL AND r.tenantId = :tenantId")
    BigDecimal sumTotalConsumoByTipoServicioAndTenantId(
            @Param("tipoServicio") Integer tipoServicio,
            @Param("tenantId") String tenantId);

    @Query("SELECT SUM(r.consumo) FROM RegistroMedidor r " +
           "WHERE r.infraestructura.id = :infraestructuraId " +
           "AND r.tipoServicio = :tipoServicio " +
           "AND r.consumo IS NOT NULL")
    BigDecimal sumConsumoByInfraestructuraIdAndTipoServicio(
            @Param("infraestructuraId") Integer infraestructuraId,
            @Param("tipoServicio") Integer tipoServicio);

    @Query("SELECT SUM(r.consumo) FROM RegistroMedidor r " +
           "WHERE r.infraestructura.id = :infraestructuraId " +
           "AND r.tipoServicio = :tipoServicio " +
           "AND r.tenantId = :tenantId " +
           "AND r.consumo IS NOT NULL")
    BigDecimal sumConsumoByInfraestructuraIdAndTipoServicioAndTenantId(
            @Param("infraestructuraId") Integer infraestructuraId,
            @Param("tipoServicio") Integer tipoServicio,
            @Param("tenantId") String tenantId);

    @Query("""
            SELECT r.infraestructura.id, r.tipoServicio, COALESCE(SUM(r.consumo), 0)
            FROM RegistroMedidor r
            WHERE r.infraestructura.id IN :ids
              AND (:tenantId IS NULL OR r.tenantId = :tenantId)
              AND r.consumo IS NOT NULL
            GROUP BY r.infraestructura.id, r.tipoServicio
            """)
    List<Object[]> sumConsumoByInfraestructuraIds(
            @Param("ids") Collection<Integer> ids,
            @Param("tenantId") String tenantId);

    @Query("SELECT COUNT(DISTINCT r.infraestructura.id) FROM RegistroMedidor r " +
           "WHERE r.fechaRegistro >= :inicio AND r.fechaRegistro <= :fin")
    long countDistinctInfraestructuraByFechaRegistroBetween(
            @Param("inicio") LocalDate inicio,
            @Param("fin") LocalDate fin);

    @Query("SELECT COUNT(DISTINCT r.infraestructura.id) FROM RegistroMedidor r " +
           "WHERE r.fechaRegistro >= :inicio AND r.fechaRegistro <= :fin AND r.tenantId = :tenantId")
    long countDistinctInfraestructuraByFechaRegistroBetweenAndTenantId(
            @Param("inicio") LocalDate inicio,
            @Param("fin") LocalDate fin,
            @Param("tenantId") String tenantId);

    @Query("SELECT SUM(r.consumo) FROM RegistroMedidor r " +
           "WHERE r.fechaRegistro >= :inicio AND r.fechaRegistro <= :fin AND r.consumo IS NOT NULL")
    BigDecimal sumConsumoByFechaRegistroBetween(
            @Param("inicio") LocalDate inicio,
            @Param("fin") LocalDate fin);

    @Query("SELECT SUM(r.consumo) FROM RegistroMedidor r " +
           "WHERE r.fechaRegistro >= :inicio AND r.fechaRegistro <= :fin " +
           "AND r.consumo IS NOT NULL AND r.tenantId = :tenantId")
    BigDecimal sumConsumoByFechaRegistroBetweenAndTenantId(
            @Param("inicio") LocalDate inicio,
            @Param("fin") LocalDate fin,
            @Param("tenantId") String tenantId);

    @EntityGraph(attributePaths = {"infraestructura", "infraestructura.empresa"})
    List<RegistroMedidor> findTop5ByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"infraestructura", "infraestructura.empresa"})
    List<RegistroMedidor> findTop5ByTenantIdOrderByCreatedAtDesc(String tenantId);

    @EntityGraph(attributePaths = {"infraestructura", "infraestructura.empresa"})
    List<RegistroMedidor> findTop10ByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"infraestructura", "infraestructura.empresa"})
    List<RegistroMedidor> findTop10ByTenantIdOrderByCreatedAtDesc(String tenantId);
}
