package com.ems.academic.dto;

import com.ems.academic.domain.Gender;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record StudentRequest(
        @NotBlank @Size(max = 30) String admissionNo,
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @NotNull LocalDate dateOfBirth,
        Gender gender,
        LocalDate admissionDate,
        UUID sectionId,
        @NotEmpty @Size(max = 5) List<@Valid GuardianRequest> guardians) {}