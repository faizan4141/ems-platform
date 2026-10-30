package com.ems.user.event;

import com.ems.user.domain.User;

import java.time.Instant;
import java.util.UUID;

public record UserLoginEvent(UUID eventId, String eventType, UUID tenantId, String email,
                             UUID keycloakId, Instant occurredAt) {

    public static final String PARENT_LOGIN_CREATED = "PARENT_LOGIN_CREATED";

    public static UserLoginEvent parentLogin(User u) {
        return new UserLoginEvent(UUID.randomUUID(), PARENT_LOGIN_CREATED, u.getTenantId(),
                u.getEmail(), u.getKeycloakId(), Instant.now());
    }
}