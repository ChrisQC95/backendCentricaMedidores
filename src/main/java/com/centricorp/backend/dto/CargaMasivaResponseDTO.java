package com.centricorp.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CargaMasivaResponseDTO {
    private int procesados;
    private int exitosos;
    private int errores;
    private List<String> detalleErrores;
}
