package com.centricorp.backend.service;

import com.centricorp.backend.dto.RegistroMedidorRequestDTO;
import com.centricorp.backend.dto.RegistroMedidorResponseDTO;

import java.util.List;

public interface RegistroMedidorService {

    /** Inserta un nuevo registro (consumo es manejado por trigger de BD) */
    RegistroMedidorResponseDTO create(RegistroMedidorRequestDTO dto);

    List<RegistroMedidorResponseDTO> findAll(Integer tipoServicio);

    /**
     * Retorna los registros del mes y año indicados.
     *
     * @param mes          1-12
     * @param anio         Año de 4 dígitos
     * @param tipoServicio 1=Luz, 2=Agua, null=Ambos (sin filtro de tipo)
     */
    List<RegistroMedidorResponseDTO> findReporte(int mes, int anio, Integer tipoServicio);
}

