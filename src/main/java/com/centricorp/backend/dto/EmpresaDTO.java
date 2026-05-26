package com.centricorp.backend.dto;

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

    /** RUC — 11 dígitos, PK de la tabla */
    private String ruc;

    private String razonSocial;

    // Solo se incluye en respuestas (null en requests de escritura)
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
