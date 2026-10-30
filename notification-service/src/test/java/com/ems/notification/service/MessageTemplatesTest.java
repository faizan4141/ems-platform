package com.ems.notification.service;

import com.ems.notification.event.AttendanceAlertEvent;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MessageTemplatesTest {

    private AttendanceAlertEvent event(String type, String studentName) {
        return new AttendanceAlertEvent(UUID.randomUUID(), type, UUID.randomUUID(), "Springfield Public School",
                UUID.randomUUID(), studentName, "Class 8", "A", LocalDate.of(2026, 10, 30),
                "Raj Sharma", "+919876543210", "raj@example.com", Instant.now());
    }

    @Test
    void absentMessage() {
        MessageTemplates.Message m = MessageTemplates.attendance(event("STUDENT_ABSENT", "Aarav Sharma"), "UniTent");
        assertThat(m.subject()).isEqualTo("Aarav Sharma was marked absent on 30 Oct 2026");
        assertThat(m.body()).contains("Dear Raj Sharma").contains("Class 8, section A").contains("Springfield Public School");
    }

    @Test
    void lateMessage() {
        assertThat(MessageTemplates.attendance(event("STUDENT_LATE", "Diya Patel"), "UniTent").subject())
                .contains("marked late");
    }

    @Test
    void lineBreaksInNamesCannotInjectMailHeaders() {
        MessageTemplates.Message m = MessageTemplates.attendance(
                event("STUDENT_ABSENT", "Evil\r\nBcc: someone@example.com"), "UniTent");
        assertThat(m.subject()).doesNotContain("\n").doesNotContain("\r");
    }
}