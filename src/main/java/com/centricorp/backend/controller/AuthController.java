package com.centricorp.backend.controller;

import com.centricorp.backend.dto.LoginRequest;
import com.centricorp.backend.dto.LoginResponse;
import com.centricorp.backend.security.CustomUserDetails;
import com.centricorp.backend.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controlador de autenticación.
 *
 * Endpoints:
 *   POST /api/auth/login — valida credenciales y retorna JWT
 *
 * Este endpoint es público (ver SecurityConfig).
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    /**
     * Login endpoint.
     *
     * Request:  { "username": "admin", "password": "centricorp2026*" }
     * Response: { "token": "eyJ...", "username": "admin", "type": "Bearer" }
     *
     * Si las credenciales son incorrectas → 401 Unauthorized.
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            // Delegar autenticación al AuthenticationManager
            // Este invoca DaoAuthenticationProvider → UserDetailsServiceImpl → BCryptPasswordEncoder
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.username(),
                            request.password()
                    )
            );

            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            // Extraer el rol (ej. "ROLE_ADMIN" -> "ADMIN")
            String role = userDetails.getAuthorities().stream()
                    .findFirst()
                    .map(auth -> auth.getAuthority().replace("ROLE_", ""))
                    .orElse("USUARIO");
                    
            Map<String, Object> extraClaims = new java.util.HashMap<>();
            extraClaims.put("rol", role);
            if (userDetails instanceof CustomUserDetails custom) {
                extraClaims.put("tenant_id", custom.getTenantId());
            }

            String token = jwtService.generateToken(extraClaims, userDetails);

            return ResponseEntity.ok(LoginResponse.of(token, userDetails.getUsername()));

        } catch (BadCredentialsException ex) {
            return ResponseEntity
                    .status(401)
                    .body(Map.of("error", "Credenciales incorrectas"));
        }
    }
}
