package com.ems.academic.event;

import com.ems.academic.domain.AttendanceStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AttendanceEventPublisherTest {

    @Test
    void createsOneMessagePerAlert_withDistinctIdsAndMatchingTypes() {
        UUID tenant = UUID.randomUUID();
        AttendanceRecorded recorded = new AttendanceRecorded(tenant, UUID.randomUUID(), LocalDate.of(2026, 10, 30),
                "Springfield Public School", "Class 8", "A", List.of(
                new AttendanceRecorded.Alert(UUID.randomUUID(), "Aarav Sharma", AttendanceStatus.ABSENT,
                        "Raj Sharma", "+919876543210", "raj@example.com"),
                new AttendanceRecorded.Alert(UUID.randomUUID(), "Diya Patel", AttendanceStatus.LATE,
                        "Mohan Patel", "+919800000001", null)));

        List<AttendanceAlertEvent> messages = AttendanceEventPublisher.toMessages(recorded, Instant.parse("2026-10-30T04:00:00Z"));

        assertThat(messages).hasSize(2);
        assertThat(messages).extracting(AttendanceAlertEvent::eventType).containsExactly("STUDENT_ABSENT", "STUDENT_LATE");
        assertThat(messages.get(0).eventId()).isNotEqualTo(messages.get(1).eventId());
        assertThat(messages.get(0)).satisfies(m -> {
            assertThat(m.tenantId()).isEqualTo(tenant);
            assertThat(m.guardianPhone()).isEqualTo("+919876543210");
            assertThat(m.className()).isEqualTo("Class 8");
        });
    }
}