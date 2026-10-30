package com.ems.academic.dto;

import com.ems.academic.domain.NoticeAudience;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record NoticeRequest(@NotBlank @Size(max = 200) String title,
                            @NotBlank @Size(max = 4000) String body,
                            @NotNull NoticeAudience audience) {}