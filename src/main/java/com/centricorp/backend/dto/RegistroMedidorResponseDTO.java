package com.centricorp.backend.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * DTO de salida para un registro de medidor.
 * Incluye 'consumo' (calculado por BD) e información de la infraestructura relacionada.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegistroMedidorResponseDTO {

    private Integer id;

    /** ID del nodo de infraestructura */
    private Integer infraestructuraId;

    /** Nombre del nodo de infraestructura (para mostrar en tablas/reportes) */
    private String infraestructuraNombre;

    /** Tipo del nodo de infraestructura */
    private String infraestructuraTipo;

    /** RUC de la empresa (para filtros en el reporte) */
    private String empresaRuc;

    /** Razón social de la empresa */
    private String empresaRazonSocial;

    private String fotoUrl;
    private BigDecimal voltaje;

    /**
     * Consumo calculado por trigger de la BD.
     * Puede ser null si no existe un registro previo del mismo medidor
     * (primer registro = sin consumo de referencia anterior).
     */
    private BigDecimal consumo;

    private LocalDate fechaRegistro;
    private String observacion;
    
    /** Tipo de servicio: 1=Luz, 2=Agua */
    private Integer tipoServicio;
    
    private OffsetDateTime createdAt;
}
