package com.ems.academic.dto;

import com.ems.academic.domain.AttendanceStatus;
import java.util.UUID;

public record RosterEntry(UUID studentId, String admissionNo, String name, AttendanceStatus status) {}