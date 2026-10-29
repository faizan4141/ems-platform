package com.ems.academic.dto;

import com.ems.academic.domain.Gender;
import com.ems.academic.domain.StudentStatus;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.util.UUID;

public record StudentUpdateRequest(
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @NotNull LocalDate dateOfBirth,
        Gender gender,
        UUID sectionId,
        @NotNull StudentStatus status) {}