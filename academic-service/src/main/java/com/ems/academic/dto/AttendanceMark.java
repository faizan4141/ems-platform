package com.ems.academic.dto;

import com.ems.academic.domain.AttendanceStatus;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AttendanceMark(@NotNull UUID studentId, @NotNull AttendanceStatus status) {}