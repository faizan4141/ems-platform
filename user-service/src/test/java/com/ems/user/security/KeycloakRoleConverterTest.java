package com.ems.user.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class KeycloakRoleConverterTest {

    private final KeycloakRoleConverter converter = new KeycloakRoleConverter();

    @Test
    void mapsRealmRolesToSpringAuthorities() {
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "none")
                .claim("realm_access", Map.of("roles", List.of("TENANT_ADMIN", "TEACHER")))
                .build();

        assertThat(converter.convert(jwt))
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_TENANT_ADMIN", "ROLE_TEACHER");
    }

    @Test
    void returnsNoAuthoritiesWhenClaimMissing() {
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "none").subject("u1").build();

        assertThat(converter.convert(jwt)).isEmpty();
    }
}