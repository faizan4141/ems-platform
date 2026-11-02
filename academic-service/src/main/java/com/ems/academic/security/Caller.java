package com.ems.academic.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.UUID;

/** Who is making the request, read from the verified token. */
public record Caller(UUID tenantId, String subject, String name, boolean admin) {

    public static Caller of(Jwt jwt, Authentication authentication) {
        boolean admin = authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_TENANT_ADMIN".equals(a.getAuthority()));
        return new Caller(TenantClaims.requireTenantId(jwt), jwt.getSubject(), Actors.name(jwt), admin);
    }
}