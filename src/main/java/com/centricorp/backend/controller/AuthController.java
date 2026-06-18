package com.centricorp.backend.controller;

import com.centricorp.backend.dto.LoginRequest;
import com.centricorp.backend.dto.LoginResponse;
import com.centricorp.backend.security.CustomUserDetails;
import com.centricorp.backend.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
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

    private static final String ACCESS_TOKEN_COOKIE = "access_token";

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @Value("${app.jwt.expiration}")
    private long jwtExpirationMs;

    /**
     * Login endpoint.
     *
     * Request:  { "username": "admin", "password": "centricorp2026*" }
     * Response: { "username": "admin", "role": "ADMIN" }
     *
     * Si las credenciales son incorrectas → 401 Unauthorized.
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request, HttpServletRequest servletRequest) {
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
            ResponseCookie cookie = buildAccessTokenCookie(token, Duration.ofMillis(jwtExpirationMs), servletRequest);

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, cookie.toString())
                    .body(LoginResponse.of(userDetails.getUsername(), role));

        } catch (BadCredentialsException ex) {
            return ResponseEntity
                    .status(401)
                    .body(Map.of("error", "Credenciales incorrectas"));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        ResponseCookie cookie = buildAccessTokenCookie("", Duration.ZERO, request);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .build();
    }

    @GetMapping("/csrf")
    public ResponseEntity<Map<String, String>> csrf(CsrfToken token) {
        return ResponseEntity.ok(Map.of(
                "headerName", token.getHeaderName(),
                "parameterName", token.getParameterName(),
                "token", token.getToken()
        ));
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String role = userDetails.getAuthorities().stream()
                .findFirst()
                .map(auth -> auth.getAuthority().replace("ROLE_", ""))
                .orElse("USUARIO");
        String tenantId = userDetails instanceof CustomUserDetails custom ? custom.getTenantId() : null;

        return ResponseEntity.ok(Map.of(
                "username", userDetails.getUsername(),
                "role", role,
                "tenantId", tenantId != null ? tenantId : ""
        ));
    }

    private ResponseCookie buildAccessTokenCookie(String token, Duration maxAge, HttpServletRequest request) {
        boolean secureCookie = isSecureRequest(request);
        return ResponseCookie.from(ACCESS_TOKEN_COOKIE, token)
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite(secureCookie ? "None" : "Lax")
                .path("/")
                .maxAge(maxAge)
                .build();
    }

    private boolean isSecureRequest(HttpServletRequest request) {
        String forwardedProto = request.getHeader("X-Forwarded-Proto");
        return request.isSecure() || "https".equalsIgnoreCase(forwardedProto);
    }
}
