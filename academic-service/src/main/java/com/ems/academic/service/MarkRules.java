package com.ems.academic.service;

import com.ems.academic.exception.InvalidRequestException;

import java.math.BigDecimal;

public final class MarkRules {

    private MarkRules() {}

    public static BigDecimal scale(BigDecimal value) { return value.setScale(2, java.math.RoundingMode.HALF_UP); }

    /** Returns the marks to store (two decimals), or null when the student is absent or the entry is cleared. */
    public static BigDecimal normalize(BigDecimal marks, boolean absent, BigDecimal max) {
        if (absent) {
            if (marks != null) throw new InvalidRequestException("An absent student cannot also have marks");
            return null;
        }
        if (marks == null) return null;
        if (marks.signum() < 0) throw new InvalidRequestException("Marks cannot be negative");
        if (marks.stripTrailingZeros().scale() > 2) throw new InvalidRequestException("Marks can have at most two decimal places");
        if (marks.compareTo(max) > 0) throw new InvalidRequestException("Marks cannot be more than " + max.toPlainString());
        return marks.setScale(2);
    }
}