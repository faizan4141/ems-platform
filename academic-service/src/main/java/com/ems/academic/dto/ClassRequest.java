package com.ems.academic.dto;

import jakarta.validation.constraints.*;
import java.util.List;
import java.util.UUID;

public record ClassRequest(
        @NotNull UUID academicYearId,
        @NotBlank @Size(max = 50) String name,
        @NotNull @Min(0) @Max(12) Integer gradeLevel,
        List<@NotBlank @Size(max = 20) String> sections) {}