package com.ems.tenant.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TenantAccessTest {

    private final UUID myTenant = UUID.randomUUID();

    private JwtAuthenticationToken tokenFor(String role, String tenantClaim) {
        Jwt.Builder builder = Jwt.withTokenValue("t").header("alg", "none").subject("u1");
        if (tenantClaim != null) {
            builder.claim("tenant_id", tenantClaim);
        }
        Jwt jwt = builder.build();
        return new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
    }

    @Test
    void platformAdmin_canViewAnyTenant() {
        JwtAuthenticationToken auth = tokenFor("PLATFORM_ADMIN", null);
        assertThat(TenantAccess.canView(auth, auth.getToken(), UUID.randomUUID())).isTrue();
    }

    @Test
    void tenantUser_canViewOwnTenant() {
        JwtAuthenticationToken auth = tokenFor("TENANT_ADMIN", myTenant.toString());
        assertThat(TenantAccess.canView(auth, auth.getToken(), myTenant)).isTrue();
    }

    @Test
    void tenantUser_cannotViewOtherTenant() {
        JwtAuthenticationToken auth = tokenFor("TENANT_ADMIN", myTenant.toString());
        assertThat(TenantAccess.canView(auth, auth.getToken(), UUID.randomUUID())).isFalse();
    }

    @Test
    void userWithoutTenantClaim_cannotViewAnyTenant() {
        JwtAuthenticationToken auth = tokenFor("TEACHER", null);
        assertThat(TenantAccess.canView(auth, auth.getToken(), myTenant)).isFalse();
    }
}