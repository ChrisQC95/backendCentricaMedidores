package com.centricorp.backend.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.DefaultCsrfToken;

import java.time.Duration;
import java.util.UUID;

public class RequestAwareCookieCsrfTokenRepository implements CsrfTokenRepository {

    private static final String COOKIE_NAME = "XSRF-TOKEN";
    private static final String HEADER_NAME = "X-XSRF-TOKEN";
    private static final String PARAMETER_NAME = "_csrf";

    @Override
    public CsrfToken generateToken(HttpServletRequest request) {
        return new DefaultCsrfToken(HEADER_NAME, PARAMETER_NAME, UUID.randomUUID().toString());
    }

    @Override
    public void saveToken(CsrfToken token, HttpServletRequest request, HttpServletResponse response) {
        boolean secureCookie = isSecureRequest(request);
        String value = token != null ? token.getToken() : "";
        Duration maxAge = token != null ? Duration.ofHours(8) : Duration.ZERO;

        ResponseCookie cookie = ResponseCookie.from(COOKIE_NAME, value)
                .httpOnly(false)
                .secure(secureCookie)
                .sameSite(secureCookie ? "None" : "Lax")
                .path("/")
                .maxAge(maxAge)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    @Override
    public CsrfToken loadToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (COOKIE_NAME.equals(cookie.getName()) && cookie.getValue() != null && !cookie.getValue().isBlank()) {
                    return new DefaultCsrfToken(HEADER_NAME, PARAMETER_NAME, cookie.getValue());
                }
            }
        }

        String headerToken = request.getHeader(HEADER_NAME);
        if (headerToken != null && !headerToken.isBlank()) {
            return new DefaultCsrfToken(HEADER_NAME, PARAMETER_NAME, headerToken);
        }

        return null;
    }

    private boolean isSecureRequest(HttpServletRequest request) {
        String forwardedProto = request.getHeader("X-Forwarded-Proto");
        return request.isSecure() || "https".equalsIgnoreCase(forwardedProto);
    }
}
