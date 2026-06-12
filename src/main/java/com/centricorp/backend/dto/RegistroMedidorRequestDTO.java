package com.centricorp.backend.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO de entrada para registrar un medidor.
 *
 * Campos excluidos intencionalmente:
 *   - consumo: calculado por trigger en la BD, nunca se envía desde el cliente.
 *   - createdAt: gestionado por Hibernate (@CreationTimestamp).
 *
 * El campo fotoUrl recibe un String con la URL ya subida desde el frontend
 * (el frontend maneja la subida de la foto; el backend solo almacena la URL).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegistroMedidorRequestDTO {

    /** ID del nodo de infraestructura (piso/espacio) al que pertenece el medidor */
    private Integer infraestructuraId;

    /** URL de la foto del medidor (ya subida por el frontend — puede ser null) */
    private String fotoUrl;

    /** Voltaje medido — campo requerido */
    private BigDecimal voltaje;

    /** Observación libre — opcional */
    private String observacion;

    /**
     * Fecha del registro. Si el frontend no la envía,
     * el servicio usará LocalDate.now() como fallback.
     */
    private LocalDate fechaRegistro;

    /** Tipo de servicio: 1=Luz, 2=Agua */
    private Integer tipoServicio;
}
