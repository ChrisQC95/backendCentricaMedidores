package com.centricorp.backend.dto;

/**
 * Response body de POST /api/auth/login
 */
public record LoginResponse(String username, String role) {

    public static LoginResponse of(String username, String role) {
        return new LoginResponse(username, role);
    }
}
