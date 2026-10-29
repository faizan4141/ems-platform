package com.ems.academic.dto;

import jakarta.validation.constraints.*;

public record ProfileRequest(
        @NotBlank @Size(max = 200) String displayName,
        @Size(max = 50) String board,
        @Size(max = 50) String affiliationNo,
        @Size(max = 300) String addressLine,
        @Size(max = 100) String city,
        @Size(max = 100) String state,
        @Size(max = 20) String postalCode,
        @Size(max = 30) String phone,
        @Email @Size(max = 254) String email,
        @Size(max = 300) String website) {}