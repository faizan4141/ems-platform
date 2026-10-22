package com.ems.tenant.dto;

import com.ems.tenant.domain.OrgType;
import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record CreateTenantRequest(
        @NotBlank
        @Pattern(regexp = "^[a-z0-9-]{3,50}$",
                message = "code must be 3-50 chars: lowercase letters, digits, hyphens")
        String code,

        @NotBlank @Size(max = 200)
        String name,

        @NotNull OrgType orgType,
        @NotNull LocalDate contractStartDate,
        @NotNull LocalDate contractEndDate
) {}