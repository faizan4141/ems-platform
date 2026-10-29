package com.ems.academic.service;

import com.ems.academic.domain.Gender;
import com.ems.academic.domain.Relationship;
import com.ems.academic.exception.InvalidRequestException;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public final class StudentFields {

    private static final List<DateTimeFormatter> DATE_FORMATS = Stream.of("uuuu-MM-dd", "dd/MM/uuuu", "dd-MM-uuuu")
            .map(p -> DateTimeFormatter.ofPattern(p).withResolverStyle(ResolverStyle.STRICT)).toList();
    private static final Pattern PHONE = Pattern.compile("^\\+?[0-9]{7,15}$");
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private StudentFields() {}

    public static String requireText(String raw, String field, int max) {
        String v = raw == null ? "" : raw.trim();
        if (v.isEmpty()) throw new InvalidRequestException(field + " is required");
        if (v.length() > max) throw new InvalidRequestException(field + " is longer than " + max + " characters");
        return v;
    }

    public static LocalDate parseDate(String raw, String field) {
        String v = raw == null ? "" : raw.trim();
        if (v.isEmpty()) throw new InvalidRequestException(field + " is required");
        for (DateTimeFormatter f : DATE_FORMATS) {
            try {
                return LocalDate.parse(v, f);
            } catch (DateTimeParseException ignored) { /* try the next format */ }
        }
        throw new InvalidRequestException(field + ": use YYYY-MM-DD or DD/MM/YYYY");
    }

    public static LocalDate checkDob(LocalDate dob, LocalDate today) {
        if (!dob.isBefore(today)) throw new InvalidRequestException("Date of birth must be in the past");
        if (dob.isBefore(today.minusYears(30))) throw new InvalidRequestException("Date of birth is more than 30 years ago");
        return dob;
    }

    public static Gender parseGender(String raw) {
        String v = raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT);
        return switch (v) {
            case "" -> null;
            case "M", "MALE" -> Gender.MALE;
            case "F", "FEMALE" -> Gender.FEMALE;
            case "O", "OTHER" -> Gender.OTHER;
            default -> throw new InvalidRequestException("gender: use M, F or O");
        };
    }

    public static Relationship parseRelationship(String raw) {
        String v = raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT);
        if (v.isEmpty()) return Relationship.GUARDIAN;
        try {
            return Relationship.valueOf(v);
        } catch (IllegalArgumentException e) {
            throw new InvalidRequestException("guardian_relationship: use FATHER, MOTHER, GUARDIAN or OTHER");
        }
    }

    public static String normalizePhone(String raw) {
        String v = raw == null ? "" : raw.replaceAll("[\\s\\-().]", "");
        if (v.isEmpty()) throw new InvalidRequestException("Guardian phone is required");
        if (!PHONE.matcher(v).matches()) throw new InvalidRequestException("Guardian phone is not a valid number");
        return v;
    }

    public static String cleanEmail(String raw) {
        String v = raw == null ? "" : raw.trim();
        if (v.isEmpty()) return null;
        if (v.length() > 254 || !EMAIL.matcher(v).matches()) throw new InvalidRequestException("Guardian email is not valid");
        return v;
    }
}