package com.centricorp.backend.dto;

/**
 * Request body para POST /api/auth/login
 */
public record LoginRequest(String username, String password) {}
