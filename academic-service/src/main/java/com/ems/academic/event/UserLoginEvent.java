package com.ems.academic.event;

import java.time.Instant;
import java.util.UUID;

public record UserLoginEvent(UUID eventId, String eventType, UUID tenantId, String email, UUID keycloakId,
                             String firstName, String lastName, Instant occurredAt) {

    public static final String PARENT_LOGIN_CREATED = "PARENT_LOGIN_CREATED";
    public static final String TEACHER_LOGIN_CREATED = "TEACHER_LOGIN_CREATED";
}