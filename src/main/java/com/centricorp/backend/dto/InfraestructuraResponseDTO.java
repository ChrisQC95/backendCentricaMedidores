package com.centricorp.backend.dto;

import com.centricorp.backend.entity.TipoNivel;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * DTO de salida (response) para un nodo de Infraestructura.
 * Expone IDs de FK en lugar de objetos anidados completos para evitar
 * referencias circulares (parent -> parent -> ...) en la serialización JSON.
 *
 * Incluye consumos separados por tipo de servicio (Electricidad / Agua)
 * para que el frontend nunca mezcle unidades (kWh vs m³).
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

    /**
     * Tipo de espacio físico: 1 = Oficina, 2 = Almacén.
     * Null para nodos que no son espacios concretos (edificios, pisos).
     */
    private Integer espacioName;

    /**
     * Suma total de consumo de electricidad (tipo_servicio = 1) para todos
     * los registros de medidores asociados a esta infraestructura.
     * Valor = BigDecimal.ZERO si no hay registros.
     */
    private BigDecimal totalConsumoElectricidad;

    /**
     * Suma total de consumo de agua (tipo_servicio = 2) para todos
     * los registros de medidores asociados a esta infraestructura.
     * Valor = BigDecimal.ZERO si no hay registros.
     */
    private BigDecimal totalConsumoAgua;

    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
