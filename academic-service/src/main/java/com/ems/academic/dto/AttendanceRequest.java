package com.ems.academic.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/** Students not listed are treated as present. */
public record AttendanceRequest(@NotNull @Size(max = 500) List<@Valid AttendanceMark> marks) {}