package com.ems.user.dto;

import com.ems.user.domain.Role;
import jakarta.validation.constraints.*;

public record CreateUserRequest(
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @NotNull Role role) {
}