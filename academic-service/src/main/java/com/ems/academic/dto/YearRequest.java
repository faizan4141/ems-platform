package com.ems.academic.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record YearRequest(
        @NotBlank @Size(max = 50) String name,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        Boolean makeCurrent) {}