package com.centricorp.backend.service;

import com.centricorp.backend.dto.RegistroMedidorRequestDTO;
import com.centricorp.backend.dto.RegistroMedidorResponseDTO;

import java.util.List;

public interface RegistroMedidorService {

    /** Inserta un nuevo registro (consumo es manejado por trigger de BD) */
    RegistroMedidorResponseDTO create(RegistroMedidorRequestDTO dto);

    List<RegistroMedidorResponseDTO> findAll();

    /**
     * Retorna los registros correspondientes al mes y año indicados.
     * Usado por GET /api/medidores/reporte?mes=X&anio=Y para exportación.
     */
    List<RegistroMedidorResponseDTO> findReporte(int mes, int anio);
}
