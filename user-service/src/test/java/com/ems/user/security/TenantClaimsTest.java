package com.ems.user.security;

import com.ems.user.exception.MissingTenantClaimException;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TenantClaimsTest {

    private Jwt jwtWith(String tenantClaim) {
        Jwt.Builder b = Jwt.withTokenValue("t").header("alg", "none").subject("u1");
        if (tenantClaim != null) {
            b.claim("tenant_id", tenantClaim);
        }
        return b.build();
    }

    @Test
    void returnsTenantIdFromClaim() {
        UUID id = UUID.randomUUID();
        assertThat(TenantClaims.requireTenantId(jwtWith(id.toString()))).isEqualTo(id);
    }

    @Test
    void rejectsMissingClaim() {
        assertThatThrownBy(() -> TenantClaims.requireTenantId(jwtWith(null)))
                .isInstanceOf(MissingTenantClaimException.class);
    }

    @Test
    void rejectsMalformedClaim() {
        assertThatThrownBy(() -> TenantClaims.requireTenantId(jwtWith("not-a-uuid")))
                .isInstanceOf(MissingTenantClaimException.class);
    }
}