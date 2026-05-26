package com.centricorp.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

/**
 * Mapea la tabla: usuarios
 * Usada por Spring Security para autenticación.
 */
@Entity
@Table(name = "usuarios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "username", unique = true, nullable = false, length = 50)
    private String username;

    /**
     * Almacena la contraseña. Por ahora en texto plano (NoOpPasswordEncoder).
     * En producción debe migrarse a BCrypt.
     */
    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "rol", length = 20)
    private String rol;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;
}
