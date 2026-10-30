package com.ems.academic.event;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record AttendanceAlertEvent(UUID eventId, String eventType, UUID tenantId, String schoolName,
                                   UUID studentId, String studentName, String className, String sectionName,
                                   LocalDate date, String guardianName, String guardianPhone,
                                   String guardianEmail, Instant occurredAt) {}