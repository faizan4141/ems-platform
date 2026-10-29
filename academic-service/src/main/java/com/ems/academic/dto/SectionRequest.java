package com.ems.academic.dto;

import jakarta.validation.constraints.*;

public record SectionRequest(@NotBlank @Size(max = 20) String name, @Min(1) Integer capacity) {}