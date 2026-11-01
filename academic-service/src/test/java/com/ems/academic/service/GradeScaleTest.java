package com.ems.academic.service;

import com.ems.academic.exception.InvalidRequestException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GradeScaleTest {

    private static BigDecimal pct(String v) { return new BigDecimal(v); }

    @Test
    void theDefaultScaleGradesBoundariesInclusively() {
        assertThat(GradeScale.gradeFor(pct("90.00"), GradeScale.DEFAULT)).isEqualTo("A+");
        assertThat(GradeScale.gradeFor(pct("89.99"), GradeScale.DEFAULT)).isEqualTo("A");
        assertThat(GradeScale.gradeFor(pct("40.00"), GradeScale.DEFAULT)).isEqualTo("D");
        assertThat(GradeScale.gradeFor(pct("39.99"), GradeScale.DEFAULT)).isEqualTo("F");
        assertThat(GradeScale.gradeFor(pct("0.00"), GradeScale.DEFAULT)).isEqualTo("F");
        assertThat(GradeScale.gradeFor(pct("100.00"), GradeScale.DEFAULT)).isEqualTo("A+");
    }

    @Test
    void worksWithABandListInAnyOrder() {
        List<GradeScale.Band> bands = List.of(new GradeScale.Band(pct("0"), "Fail", null), new GradeScale.Band(pct("50"), "Pass", null));
        assertThat(GradeScale.gradeFor(pct("50"), bands)).isEqualTo("Pass");
        assertThat(GradeScale.gradeFor(pct("49.99"), bands)).isEqualTo("Fail");
    }

    @Test
    void theDefaultScaleIsValid() {
        GradeScale.validate(GradeScale.DEFAULT);
    }

    @Test
    void rejectsAScaleWithoutAZeroBand() {
        assertThatThrownBy(() -> GradeScale.validate(List.of(new GradeScale.Band(pct("50"), "Pass", null))))
                .isInstanceOf(InvalidRequestException.class).hasMessageContaining("0%");
    }

    @Test
    void rejectsDuplicateStartsAndDuplicateNames() {
        assertThatThrownBy(() -> GradeScale.validate(List.of(
                new GradeScale.Band(pct("0"), "F", null), new GradeScale.Band(pct("0.00"), "E", null))))
                .isInstanceOf(InvalidRequestException.class);
        assertThatThrownBy(() -> GradeScale.validate(List.of(
                new GradeScale.Band(pct("0"), "A", null), new GradeScale.Band(pct("50"), "a", null))))
                .isInstanceOf(InvalidRequestException.class);
    }
}