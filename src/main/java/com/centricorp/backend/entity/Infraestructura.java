package com.centricorp.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;

/**
 * Mapea la tabla: infraestructura
 *
 * Puntos clave:
 * 1. Auto-referencia: parent_id -> ManyToOne hacia sí mismo (árbol jerárquico).
 * 2. tipo -> ENUM PostgreSQL "tipo_nivel" mapeado con @JdbcTypeCode(SqlTypes.NAMED_ENUM)
 *            para que Hibernate 6+ sepa que es un tipo nombrado de PostgreSQL.
 * 3. empresa_ruc -> FK a Empresa.
 */
@Entity
@Table(name = "infraestructura")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Infraestructura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    /** FK a empresas.ruc */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_ruc", nullable = false)
    private Empresa empresa;

    /**
     * Auto-referencia: un nodo puede tener un nodo padre del mismo tipo.
     * NULL indica que es nodo raíz (ej. un edificio sin padre).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Infraestructura parent;

    /**
     * ENUM PostgreSQL "tipo_nivel".
     * Se usa @JdbcTypeCode(SqlTypes.NAMED_ENUM) para que Hibernate 6/7 mapee
     * correctamente al tipo ENUM nativo de PostgreSQL sin conversión de strings.
     */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "tipo", nullable = false, columnDefinition = "tipo_nivel")
    private TipoNivel tipo;

    @Column(name = "nombre", nullable = false, length = 150)
    private String nombre;

    /** Campo opcional de descripción libre */
    @Column(name = "glosa", columnDefinition = "TEXT")
    private String glosa;

    /**
     * Tipo de espacio dentro de la infraestructura.
     * 1 = Oficina, 2 = Almacén.
     * Es nullable para nodos que no representan un espacio físico concreto
     * (ej. edificios, pisos).
     * IMPORTANTE: la columna "espacio_name" debe existir en la BD antes de
     * iniciar el backend (migración SQL manual o script de Supabase).
     */
    @Column(name = "espacio_name")
    private Integer espacioName;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    @UpdateTimestamp
    private OffsetDateTime updatedAt;
}
