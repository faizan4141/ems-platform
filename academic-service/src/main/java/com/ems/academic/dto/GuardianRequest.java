package com.ems.academic.dto;

import com.ems.academic.domain.Relationship;
import jakarta.validation.constraints.*;

public record GuardianRequest(
        @NotBlank @Size(max = 200) String name,
        @NotNull Relationship relationship,
        @NotBlank @Size(max = 30) String phone,
        @Size(max = 254) String email,
        Boolean primary) {}