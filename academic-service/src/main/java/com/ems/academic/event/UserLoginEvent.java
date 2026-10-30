package com.ems.academic.event;

import java.time.Instant;
import java.util.UUID;

public record UserLoginEvent(UUID eventId, String eventType, UUID tenantId, String email,
                             UUID keycloakId, Instant occurredAt) {

    public static final String PARENT_LOGIN_CREATED = "PARENT_LOGIN_CREATED";
}