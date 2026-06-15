package com.centricorp.backend.dto;

import jakarta.validation.constraints.*;
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
    @NotNull(message = "El ID de infraestructura es requerido")
    @Positive(message = "El ID de infraestructura debe ser un número positivo")
    private Integer infraestructuraId;

    /** URL de la foto del medidor (ya subida por el frontend — puede ser null) */
    private String fotoUrl;

    /** Voltaje medido — campo requerido, no puede ser negativo */
    @NotNull(message = "El voltaje es requerido")
    @DecimalMin(value = "0.0", inclusive = true, message = "El voltaje no puede ser negativo")
    @Digits(integer = 8, fraction = 2, message = "El voltaje debe tener máximo 8 dígitos enteros y 2 decimales")
    private BigDecimal voltaje;

    /** Observación libre — opcional */
    @Size(max = 1000, message = "La observación no puede superar 1000 caracteres")
    private String observacion;

    /**
     * Fecha del registro. Si el frontend no la envía,
     * el servicio usará LocalDate.now() como fallback.
     */
    private LocalDate fechaRegistro;

    /** Tipo de servicio: 1=Luz, 2=Agua */
    @Min(value = 1, message = "El tipo de servicio debe ser 1 (Luz) o 2 (Agua)")
    @Max(value = 2, message = "El tipo de servicio debe ser 1 (Luz) o 2 (Agua)")
    private Integer tipoServicio;
}
