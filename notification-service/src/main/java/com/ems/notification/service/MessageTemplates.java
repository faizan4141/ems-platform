package com.ems.notification.service;

import com.ems.notification.event.AttendanceAlertEvent;
import com.ems.notification.event.FeeReminderEvent;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class MessageTemplates {

    public record Message(String subject, String body) {}

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMM uuuu", Locale.ENGLISH);

    private MessageTemplates() {}

    public static Message attendance(AttendanceAlertEvent e, String brand) {
        String what = "STUDENT_LATE".equals(e.eventType()) ? "late" : "absent";
        String date = e.date().format(DATE);
        String student = clean(e.studentName());
        String subject = student + " was marked " + what + " on " + date;
        String body = "Dear " + clean(e.guardianName()) + ",\n\n"
                + student + " (" + clean(e.className()) + ", section " + clean(e.sectionName()) + ") was marked "
                + what + " on " + date + ".\n\n"
                + "If this is unexpected, please contact the school.\n\n"
                + clean(e.schoolName()) + "\nSent via " + brand;
        return new Message(subject, body);
    }

    public static Message feeReminder(FeeReminderEvent e, String brand) {
        String amount = money(e.currency(), e.totalDue());
        String student = clean(e.studentName());
        String subject = "Fee reminder: " + amount + (e.overdue() ? " overdue for " : " due soon for ") + student;
        String items = e.itemCount() > 1 ? " (" + e.itemCount() + " fee items)" : "";
        String body = "Dear " + clean(e.guardianName()) + ",\n\n"
                + "This is a reminder that " + amount + " in fees for " + student + " (" + clean(e.className())
                + ", section " + clean(e.sectionName()) + ") " + (e.overdue() ? "was due on " : "is due on ")
                + e.earliestDueDate().format(DATE) + items + ".\n\n"
                + "If you have already paid, please ignore this message. The details are in the parent portal.\n\n"
                + clean(e.schoolName()) + "\nSent via " + brand;
        return new Message(subject, body);
    }

    private static String money(String currency, BigDecimal value) {
        return clean(currency) + " " + String.format(Locale.ENGLISH, "%,.2f", value);
    }

    /** Names come from user input, so strip line breaks to prevent mail-header injection. */
    private static String clean(String value) {
        return value == null ? "" : value.replaceAll("[\\r\\n]+", " ").trim();
    }
}