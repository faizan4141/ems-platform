package com.ems.academic.dto;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class AttendanceSummaryTest {

    private static final LocalDate DAY = LocalDate.of(2026, 10, 30);

    @Test
    void lateStudentsCountAsAttending() {
        assertThat(AttendanceSummary.of(DAY, 3, 2, 17, 2, 1).percentPresent()).isEqualTo(90);
    }

    @Test
    void hasNoPercentageUntilSomeoneIsMarked() {
        assertThat(AttendanceSummary.of(DAY, 3, 0, 0, 0, 0).percentPresent()).isNull();
    }
}