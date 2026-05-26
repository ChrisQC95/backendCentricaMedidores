package com.centricorp.backend.dto;

import com.centricorp.backend.entity.TipoNivel;
import lombok.*;

import java.time.OffsetDateTime;

/**
 * DTO de salida (response) para un nodo de Infraestructura.
 * Expone IDs de FK en lugar de objetos anidados completos para evitar
 * referencias circulares (parent -> parent -> ...) en la serialización JSON.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InfraestructuraResponseDTO {

    private Integer id;

    /** RUC de la empresa */
    private String empresaRuc;

    /** Nombre de la empresa (para mostrar en el frontend) */
    private String empresaRazonSocial;

    /** ID del nodo padre (null si es raíz) */
    private Integer parentId;

    /** Nombre del nodo padre (null si es raíz) */
    private String parentNombre;

    private TipoNivel tipo;
    private String nombre;
    private String glosa;

    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
