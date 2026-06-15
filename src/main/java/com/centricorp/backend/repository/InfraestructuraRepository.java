package com.centricorp.backend.repository;

import com.centricorp.backend.entity.Infraestructura;
import com.centricorp.backend.entity.TipoNivel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InfraestructuraRepository extends JpaRepository<Infraestructura, Integer> {

    /**
     * Carga empresa y parent en un solo JOIN FETCH para erradicar N+1
     * al llamar a i.empresa.getRuc() / i.parent.getNombre() en toDTO().
     */
    @EntityGraph(attributePaths = {"empresa", "parent"})
    Page<Infraestructura> findByTenantId(String tenantId, Pageable pageable);

    @EntityGraph(attributePaths = {"empresa", "parent"})
    Page<Infraestructura> findAll(Pageable pageable);

    Optional<Infraestructura> findByIdAndTenantId(Integer id, String tenantId);

    /** Lista plana sin paginar, usada por selectores del frontend (combo boxes). */
    @EntityGraph(attributePaths = {"empresa", "parent"})
    List<Infraestructura> findByEmpresaRuc(String ruc);

    @EntityGraph(attributePaths = {"empresa", "parent"})
    List<Infraestructura> findByEmpresaRucAndTenantId(String ruc, String tenantId);

    long countByTipo(TipoNivel tipo);

    long countByTipoAndTenantId(TipoNivel tipo, String tenantId);

    long countByTenantId(String tenantId);
}
