package com.ems.notification.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record FeeReminderEvent(UUID eventId, String eventType, UUID tenantId, String schoolName,
                               UUID studentId, String studentName, String className, String sectionName,
                               BigDecimal totalDue, String currency, int itemCount, LocalDate earliestDueDate,
                               boolean overdue, String guardianName, String guardianPhone, String guardianEmail,
                               Instant occurredAt) {}