package com.centricorp.backend.service;

import com.centricorp.backend.dto.InfraestructuraRequestDTO;
import com.centricorp.backend.dto.InfraestructuraResponseDTO;

import java.util.List;

public interface InfraestructuraService {
    List<InfraestructuraResponseDTO> findAll();
    InfraestructuraResponseDTO findById(Integer id);

    /**
     * Retorna la lista plana de todos los nodos de infraestructura
     * de una empresa. Usado para cargar selectores en el frontend.
     */
    List<InfraestructuraResponseDTO> findByEmpresaRuc(String ruc);

    InfraestructuraResponseDTO create(InfraestructuraRequestDTO dto);
    InfraestructuraResponseDTO update(Integer id, InfraestructuraRequestDTO dto);
    void delete(Integer id);
}
