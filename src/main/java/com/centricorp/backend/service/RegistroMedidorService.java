package com.centricorp.backend.service;

import com.centricorp.backend.dto.RegistroMedidorRequestDTO;
import com.centricorp.backend.dto.RegistroMedidorResponseDTO;
import org.springframework.data.domain.Page;

import java.util.List;

public interface RegistroMedidorService {

    /** Inserta un nuevo registro (consumo es manejado por trigger de BD) */
    RegistroMedidorResponseDTO create(RegistroMedidorRequestDTO dto);

    /**
     * Lista paginada de registros. Soporta filtro opcional por tipoServicio.
     *
     * @param tipoServicio 1=Luz, 2=Agua, null=Ambos
     * @param page         Número de página (0-indexed)
     * @param size         Tamaño de página
     */
    Page<RegistroMedidorResponseDTO> findAll(Integer tipoServicio, int page, int size);

    /**
     * Retorna los registros del mes y año indicados (lista plana para reporte Excel).
     *
     * @param mes          1-12
     * @param anio         Año de 4 dígitos
     * @param tipoServicio 1=Luz, 2=Agua, null=Ambos (sin filtro de tipo)
     */
    List<RegistroMedidorResponseDTO> findReporte(int mes, int anio, Integer tipoServicio);
}
