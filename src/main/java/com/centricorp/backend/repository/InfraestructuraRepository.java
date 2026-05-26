package com.centricorp.backend.repository;

import com.centricorp.backend.entity.Infraestructura;
import com.centricorp.backend.entity.TipoNivel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository para la tabla "infraestructura".
 *
 * Métodos personalizados:
 *   findByEmpresaRuc  — nodos por empresa (para selectores en el frontend).
 *   countByTipo       — conteo de nodos por tipo (para KPI cards del Dashboard).
 */
@Repository
public interface InfraestructuraRepository extends JpaRepository<Infraestructura, Integer> {

    /**
     * Retorna todos los nodos de infraestructura que pertenecen a la empresa con el RUC dado.
     * Se usa en GET /api/infraestructura/empresa/{ruc} para cargar selectores en el frontend.
     */
    List<Infraestructura> findByEmpresaRuc(String ruc);

    /**
     * Cuenta nodos por tipo — usado por el Dashboard para las KPI cards
     * (UNIDAD = edificios, PISO, ENST = arrendatarios).
     */
    long countByTipo(TipoNivel tipo);
}
