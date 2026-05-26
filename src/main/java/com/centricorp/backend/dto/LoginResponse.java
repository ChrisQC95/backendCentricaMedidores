package com.centricorp.backend.dto;

/**
 * Response body de POST /api/auth/login
 */
public record LoginResponse(String token, String username, String type) {

    /** Constructor de conveniencia que fija type = "Bearer" */
    public static LoginResponse of(String token, String username) {
        return new LoginResponse(token, username, "Bearer");
    }
}
