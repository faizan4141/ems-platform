package com.ems.user.dto;

import com.ems.user.domain.*;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(UUID id, UUID tenantId, String email, String firstName,
                           String lastName, Role role, UserStatus status, Instant createdAt) {

    public static UserResponse from(User u) {
        return new UserResponse(u.getId(), u.getTenantId(), u.getEmail(), u.getFirstName(),
                u.getLastName(), u.getRole(), u.getStatus(), u.getCreatedAt());
    }
}