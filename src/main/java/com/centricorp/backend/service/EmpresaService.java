package com.centricorp.backend.service;

import com.centricorp.backend.dto.EmpresaDTO;
import org.springframework.data.domain.Page;

public interface EmpresaService {
    /**
     * Lista paginada de empresas.
     *
     * @param page Número de página (0-indexed)
     * @param size Tamaño de página
     */
    Page<EmpresaDTO> findAll(int page, int size);

    Page<EmpresaDTO> search(String q, int page, int size);

    EmpresaDTO findById(String ruc);
    EmpresaDTO create(EmpresaDTO dto);
    EmpresaDTO update(String ruc, EmpresaDTO dto);
    void delete(String ruc);
}
