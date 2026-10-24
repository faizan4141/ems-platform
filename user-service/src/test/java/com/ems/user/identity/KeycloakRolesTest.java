package com.ems.user.identity;

import com.ems.user.domain.Role;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class KeycloakRolesTest {

    @Test
    void adminBecomesTenantAdmin() {
        assertThat(KeycloakRoles.forRole(Role.ADMIN)).isEqualTo("TENANT_ADMIN");
    }

    @Test
    void noUserRoleCanMapToPlatformAdmin() {
        assertThat(Arrays.stream(Role.values()).map(KeycloakRoles::forRole))
                .doesNotContain("PLATFORM_ADMIN");
    }
}