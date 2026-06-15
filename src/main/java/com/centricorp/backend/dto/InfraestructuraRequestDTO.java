package com.centricorp.backend.dto;

import com.centricorp.backend.entity.TipoNivel;
import jakarta.validation.constraints.*;
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
    @NotBlank(message = "El RUC de la empresa es requerido")
    @Pattern(regexp = "\\d{11}", message = "El RUC debe tener exactamente 11 dígitos numéricos")
    private String empresaRuc;

    /**
     * ID del nodo padre (null si es nodo raíz, ej. un edificio sin padre).
     * Permite construir el árbol jerárquico.
     */
    @Positive(message = "El ID del padre debe ser un número positivo")
    private Integer parentId;

    /** Tipo del nivel según el ENUM tipo_nivel de PostgreSQL */
    @NotNull(message = "El tipo de nivel es requerido")
    private TipoNivel tipo;

    /** Nombre del nivel (ej. "Torre A", "Piso 3", "Oficina 301") */
    @NotBlank(message = "El nombre es requerido")
    @Size(min = 1, max = 150, message = "El nombre debe tener entre 1 y 150 caracteres")
    private String nombre;

    /** Descripción adicional libre */
    @Size(max = 2000, message = "La glosa no puede superar 2000 caracteres")
    private String glosa;

    /**
     * Tipo de espacio: 1 = Oficina, 2 = Almacén.
     * Puede ser null para nodos que no son un espacio concreto (edificio, piso).
     */
    @Min(value = 1, message = "El tipo de espacio debe ser 1 (Oficina), 2 (Almacén) o 3 (Centro Comercial)")
    @Max(value = 3, message = "El tipo de espacio debe ser 1 (Oficina), 2 (Almacén) o 3 (Centro Comercial)")
    private Integer espacioName;
}
