package com.ems.user.dto;

import jakarta.validation.constraints.*;

import java.util.UUID;

public record CreateTenantAdminRequest(
        @NotNull UUID tenantId,
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName) {
}