package com.centricorp.backend.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;

@Getter
public class CustomUserDetails extends User {

    private final String tenantId;

    public CustomUserDetails(String username, String password, Collection<? extends GrantedAuthority> authorities, String tenantId) {
        super(username, password, authorities);
        this.tenantId = tenantId;
    }
}
