package com.centricorp.backend.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.time.OffsetDateTime;

/**
 * DTO para Empresa — usado tanto en request (CREATE/UPDATE) como en response.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmpresaDTO {

    /** RUC — 11 dígitos numéricos, PK de la tabla */
    @NotBlank(message = "El RUC es requerido")
    @Pattern(regexp = "\\d{11}", message = "El RUC debe tener exactamente 11 dígitos numéricos")
    private String ruc;

    @NotBlank(message = "La razón social es requerida")
    @Size(min = 3, max = 200, message = "La razón social debe tener entre 3 y 200 caracteres")
    private String razonSocial;

    // Solo se incluye en respuestas (null en requests de escritura)
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
