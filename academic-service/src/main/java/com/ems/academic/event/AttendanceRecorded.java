package com.ems.academic.event;

import com.ems.academic.domain.AttendanceStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record AttendanceRecorded(UUID tenantId, UUID sessionId, LocalDate date, String schoolName,
                                 String className, String sectionName, List<Alert> alerts) {

    public record Alert(UUID studentId, String studentName, AttendanceStatus status,
                        String guardianName, String guardianPhone, String guardianEmail) {}
}