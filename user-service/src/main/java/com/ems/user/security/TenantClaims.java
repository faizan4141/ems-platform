package com.ems.user.security;

import com.ems.user.exception.MissingTenantClaimException;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.UUID;

public final class TenantClaims {

    private TenantClaims() {
    }

    public static UUID requireTenantId(Jwt jwt) {
        String value = jwt.getClaimAsString("tenant_id");
        if (value == null || value.isBlank()) {
            throw new MissingTenantClaimException();
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            throw new MissingTenantClaimException();
        }
    }
}