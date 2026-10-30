package com.ems.academic.dto;

import com.ems.academic.domain.AttendanceStatus;
import java.time.LocalDate;

public record AttendanceDay(LocalDate date, AttendanceStatus status) {}