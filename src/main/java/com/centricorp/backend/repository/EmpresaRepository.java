package com.centricorp.backend.repository;

import com.centricorp.backend.entity.Empresa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository para la tabla "empresas".
 * PK es String (RUC VARCHAR 11).
 */
@Repository
public interface EmpresaRepository extends JpaRepository<Empresa, String> {
    // Spring Data provee: findAll, findById, save, deleteById, existsById
}
