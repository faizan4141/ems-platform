package com.ems.tenant.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Objects;
import java.util.UUID;

public final class TenantAccess {

    private TenantAccess() {
    }

    /**
     * Platform admins may view any tenant; everyone else only their own.
     */
    public static boolean canView(Authentication auth, Jwt jwt, UUID tenantId) {
        boolean platformAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> Objects.equals(a.getAuthority(), "ROLE_PLATFORM_ADMIN"));
        if (platformAdmin) {
            return true;
        }
        return tenantId.toString().equals(jwt.getClaimAsString("tenant_id"));
    }
}