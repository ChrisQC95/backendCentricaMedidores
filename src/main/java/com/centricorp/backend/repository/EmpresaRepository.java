package com.centricorp.backend.repository;

import com.centricorp.backend.entity.Empresa;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmpresaRepository extends JpaRepository<Empresa, String> {

    Page<Empresa> findByTenantId(String tenantId, Pageable pageable);

    long countByTenantId(String tenantId);

    Optional<Empresa> findByRucAndTenantId(String ruc, String tenantId);

    boolean existsByRucAndTenantId(String ruc, String tenantId);
}
