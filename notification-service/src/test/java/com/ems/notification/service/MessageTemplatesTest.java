package com.ems.notification.service;

import com.ems.notification.event.AttendanceAlertEvent;
import com.ems.notification.event.FeeReminderEvent;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class MessageTemplatesTest {

    private AttendanceAlertEvent event(String type, String studentName) {
        return new AttendanceAlertEvent(UUID.randomUUID(), type, UUID.randomUUID(), "Springfield Public School",
                UUID.randomUUID(), studentName, "Class 8", "A", LocalDate.of(2026, 10, 30),
                "Raj Sharma", "+919876543210", "raj@example.com", Instant.now());
    }

    private FeeReminderEvent fee(boolean overdue, int items) {
        return new FeeReminderEvent(UUID.randomUUID(), overdue ? "FEE_OVERDUE" : "FEE_DUE_SOON", UUID.randomUUID(),
                "Springfield Public School", UUID.randomUUID(), "Aarav Sharma", "Class 8", "A",
                new BigDecimal("12500.50"), "INR", items, LocalDate.of(2026, 10, 20), overdue,
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

    @Test
    void overdueFeeReminder() {
        MessageTemplates.Message m = MessageTemplates.feeReminder(fee(true, 1), "UniTent");
        assertThat(m.subject()).isEqualTo("Fee reminder: INR 12,500.50 overdue for Aarav Sharma");
        assertThat(m.body()).contains("was due on 20 Oct 2026").doesNotContain("fee items");
    }

    @Test
    void upcomingFeeReminderMentionsTheNumberOfItems() {
        MessageTemplates.Message m = MessageTemplates.feeReminder(fee(false, 3), "UniTent");
        assertThat(m.subject()).contains("due soon for Aarav Sharma");
        assertThat(m.body()).contains("is due on 20 Oct 2026").contains("(3 fee items)");
    }
}