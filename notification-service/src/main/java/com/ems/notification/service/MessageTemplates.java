package com.ems.notification.service;

import com.ems.notification.event.AttendanceAlertEvent;

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

    /** Names come from user input, so strip line breaks to prevent mail-header injection. */
    private static String clean(String value) {
        return value == null ? "" : value.replaceAll("[\\r\\n]+", " ").trim();
    }
}