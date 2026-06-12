package com.centricorp.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

/**
 * Mapea la tabla: registro_medidores
 *
 * Nota: el campo "consumo" es calculado por la BD (no se inserta ni actualiza
 * desde Java).
 * Se configura con insertable=false, updatable=false.
 */
@Entity
@Table(name = "registro_medidores")
@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "tenantId", type = String.class))
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
@EntityListeners(com.centricorp.backend.listener.TenantEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegistroMedidor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    /** FK a infraestructura.id — no puede ser NULL */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "infraestructura_id", nullable = false)
    private Infraestructura infraestructura;

    /** URL de foto del medidor (opcional) */
    @Column(name = "foto_url", columnDefinition = "TEXT")
    private String fotoUrl;

    /** Voltaje medido — requerido */
    @Column(name = "voltaje", nullable = false, precision = 10, scale = 2)
    private BigDecimal voltaje;

    /**
     * Consumo calculado por la base de datos.
     * insertable=false, updatable=false para que JPA nunca lo escriba.
     */
    @Column(name = "consumo", precision = 10, scale = 2, insertable = false, updatable = false)
    private BigDecimal consumo;

    /** Fecha del registro — DEFAULT CURRENT_DATE en BD */
    @Column(name = "fecha_registro")
    private LocalDate fechaRegistro;

    /** Observación libre */
    @Column(name = "observacion", columnDefinition = "TEXT")
    private String observacion;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "tipo_servicio")
    private Integer tipoServicio;
    // Nota: 1 = Electricidad, 2 = Agua

    @Column(name = "tenant_id", length = 20)
    private String tenantId;
}
