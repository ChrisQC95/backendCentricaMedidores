package com.centricorp.backend.dto;

import com.centricorp.backend.entity.TipoNivel;
import lombok.*;

/**
 * DTO de entrada (request) para crear o actualizar un nodo de Infraestructura.
 * El frontend envía estos campos; el backend resuelve las entidades relacionadas.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InfraestructuraRequestDTO {

    /** RUC de la empresa a la que pertenece este nodo */
    private String empresaRuc;

    /**
     * ID del nodo padre (null si es nodo raíz, ej. un edificio sin padre).
     * Permite construir el árbol jerárquico.
     */
    private Integer parentId;

    /** Tipo del nivel según el ENUM tipo_nivel de PostgreSQL */
    private TipoNivel tipo;

    /** Nombre del nivel (ej. "Torre A", "Piso 3", "Oficina 301") */
    private String nombre;

    /** Descripción adicional libre */
    private String glosa;

    /**
     * Tipo de espacio: 1 = Oficina, 2 = Almacén.
     * Puede ser null para nodos que no son un espacio concreto (edificio, piso).
     */
    private Integer espacioName;
}
