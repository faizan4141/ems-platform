package com.ems.user.event;

import com.ems.user.domain.Role;
import com.ems.user.domain.User;

import java.time.Instant;
import java.util.UUID;

public record UserLoginEvent(UUID eventId, String eventType, UUID tenantId, String email, UUID keycloakId,
                             String firstName, String lastName, Instant occurredAt) {

    public static final String PARENT_LOGIN_CREATED = "PARENT_LOGIN_CREATED";
    public static final String TEACHER_LOGIN_CREATED = "TEACHER_LOGIN_CREATED";

    /** The event type for roles other services care about, or null for roles they do not. */
    public static String typeFor(Role role) {
        return switch (role) {
            case PARENT -> PARENT_LOGIN_CREATED;
            case TEACHER -> TEACHER_LOGIN_CREATED;
            default -> null;
        };
    }

    public static UserLoginEvent of(User u, String type) {
        return new UserLoginEvent(UUID.randomUUID(), type, u.getTenantId(), u.getEmail(), u.getKeycloakId(),
                u.getFirstName(), u.getLastName(), Instant.now());
    }
}