package com.ems.user.identity;

import com.ems.user.domain.Role;

public final class KeycloakRoles {

    private KeycloakRoles() {
    }

    /**
     * Maps our user roles to Keycloak realm roles. PLATFORM_ADMIN is deliberately unreachable.
     */
    public static String forRole(Role role) {
        return switch (role) {
            case ADMIN -> "TENANT_ADMIN";
            case TEACHER -> "TEACHER";
            case STUDENT -> "STUDENT";
            case PARENT -> "PARENT";
            case STAFF -> "STAFF";
        };
    }
}