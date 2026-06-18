package com.centricorp.backend.service;

import com.centricorp.backend.dto.InfraestructuraRequestDTO;
import com.centricorp.backend.dto.InfraestructuraResponseDTO;
import org.springframework.data.domain.Page;

import java.util.List;

public interface InfraestructuraService {
    /**
     * Lista paginada de infraestructura.
     *
     * @param page Número de página (0-indexed)
     * @param size Tamaño de página
     */
    Page<InfraestructuraResponseDTO> findAll(int page, int size);

    Page<InfraestructuraResponseDTO> search(String empresaRuc, String q, int page, int size);

    InfraestructuraResponseDTO findById(Integer id);

    /**
     * Retorna la lista plana de todos los nodos de infraestructura
     * de una empresa. Usado para cargar selectores en el frontend (no pagina).
     */
    List<InfraestructuraResponseDTO> findByEmpresaRuc(String ruc);

    InfraestructuraResponseDTO create(InfraestructuraRequestDTO dto);
    InfraestructuraResponseDTO update(Integer id, InfraestructuraRequestDTO dto);
    void delete(Integer id);
}
