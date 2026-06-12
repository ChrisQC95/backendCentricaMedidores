package com.centricorp.backend.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Configuración principal de Spring Security.
 *
 * Política:
 * - CSRF deshabilitado (API REST stateless)
 * - CORS habilitado (configuración permisiva para desarrollo)
 * - Sesión STATELESS (sin HttpSession)
 * - Rutas públicas: POST /api/auth/login
 * - Resto de /api/** requiere autenticación JWT
 *
 * IMPORTANTE: NoOpPasswordEncoder es TEMPORAL para desarrollo.
 * En producción reemplazar por BCryptPasswordEncoder.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final UserDetailsService userDetailsService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // ── Deshabilitar CSRF (API REST stateless no lo necesita) ──────────
                .csrf(AbstractHttpConfigurer::disable)

                // ── CORS ──────────────────────────────────────────────────────────
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // ── Autorización de rutas ─────────────────────────────────────────
                .authorizeHttpRequests(auth -> auth
                        // El login es público
                        .requestMatchers("/api/auth/login").permitAll()

                        // Endpoints exclusivos para ADMIN
                        .requestMatchers("/api/dashboard/**").hasAuthority("ROLE_ADMIN")
                        .requestMatchers("/api/medidores/exportar").hasAuthority("ROLE_ADMIN")
                        .requestMatchers(org.springframework.http.HttpMethod.DELETE, "/api/medidores/**").hasAuthority("ROLE_ADMIN")
                        
                        // Lectura de empresas e infraestructura (ADMIN y USUARIO)
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/empresas/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_USUARIO")
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/infraestructura/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_USUARIO")

                        // Modificación de empresas e infraestructura (ADMIN exclusivo)
                        .requestMatchers("/api/empresas/**").hasAuthority("ROLE_ADMIN")
                        .requestMatchers("/api/infraestructura/**").hasAuthority("ROLE_ADMIN")

                        // CRUD de medidores (Lectura, Creación y Actualización) para ADMIN y USUARIO
                        .requestMatchers("/api/medidores/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_USUARIO")

                        // Cualquier otra ruta también autenticada
                        .anyRequest().authenticated())

                // ── Política de sesión: STATELESS ─────────────────────────────────
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // ── Proveedor de autenticación ────────────────────────────────────
                .authenticationProvider(authenticationProvider())

                // ── Filtro JWT antes del filtro estándar de usuario/contraseña ────
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * CORS: permite cualquier origen, método y header durante desarrollo.
     * En producción restringir allowedOrigins al dominio del frontend.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    /**
     * DaoAuthenticationProvider: usa nuestro UserDetailsService y el
     * PasswordEncoder.
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    /**
     * AuthenticationManager requerido por el AuthController.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * ⚠ TEMPORAL: NoOpPasswordEncoder — contraseñas en texto plano.
     * Reemplazar por BCryptPasswordEncoder antes de ir a producción.
     */
    @Bean
    @SuppressWarnings("deprecation")
    public PasswordEncoder passwordEncoder() {
        return NoOpPasswordEncoder.getInstance();
    }
}
