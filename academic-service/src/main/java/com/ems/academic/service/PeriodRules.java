package com.ems.academic.service;

import com.ems.academic.exception.InvalidRequestException;

import java.time.LocalTime;

public final class PeriodRules {

    private PeriodRules() {}

    public static void validateTimes(LocalTime start, LocalTime end) {
        if (!end.isAfter(start)) {
            throw new InvalidRequestException("A period must end after it starts");
        }
    }

    /** Periods that only touch (one ends exactly when the next starts) do not overlap. */
    public static boolean overlaps(LocalTime startA, LocalTime endA, LocalTime startB, LocalTime endB) {
        return startA.isBefore(endB) && startB.isBefore(endA);
    }
}