package com.ems.academic.service;

import com.ems.academic.service.ReportCardCalculator.Computation;
import com.ems.academic.service.ReportCardCalculator.Entry;
import com.ems.academic.service.ReportCardCalculator.SubjectInput;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ReportCardCalculatorTest {

    private final UUID english = UUID.randomUUID();
    private final UUID maths = UUID.randomUUID();
    private final List<SubjectInput> subjects = List.of(
            new SubjectInput(english, "English", new BigDecimal("50.00"), new BigDecimal("18.00")),
            new SubjectInput(maths, "Mathematics", new BigDecimal("100.00"), new BigDecimal("35.00")));

    private static Entry marks(String v) { return new Entry(new BigDecimal(v), false); }

    private Computation run(Map<UUID, Entry> entries) {
        return ReportCardCalculator.compute(subjects, entries, GradeScale.DEFAULT);
    }

    @Test
    void passesWhenEverySubjectMeetsItsPassMark() {
        Computation c = run(Map.of(english, marks("40"), maths, marks("80")));

        assertThat(c.result()).isEqualTo("PASS");
        assertThat(c.totalMarks()).isEqualByComparingTo("120");
        assertThat(c.totalMax()).isEqualByComparingTo("150");
        assertThat(c.percent()).isEqualByComparingTo("80.00");
        assertThat(c.grade()).isEqualTo("A");
        assertThat(c.complete()).isTrue();
        assertThat(c.lines()).extracting("grade").containsExactly("A", "A");
    }

    @Test
    void oneFailedSubjectFailsTheExamEvenWithAGoodTotal() {
        Computation c = run(Map.of(english, marks("10"), maths, marks("90")));

        assertThat(c.percent()).isEqualByComparingTo("66.67");   // 100 / 150, rounded half up
        assertThat(c.grade()).isEqualTo("B");
        assertThat(c.result()).isEqualTo("FAIL");
        assertThat(c.lines().get(0).passed()).isFalse();
    }

    @Test
    void aPassMarkExactlyMetIsAPass() {
        assertThat(run(Map.of(english, marks("18"), maths, marks("35"))).result()).isEqualTo("PASS");
    }

    @Test
    void absentCountsAsZeroAndAsNotPassed() {
        Computation c = run(Map.of(english, new Entry(null, true), maths, marks("90")));

        assertThat(c.totalMarks()).isEqualByComparingTo("90");
        assertThat(c.result()).isEqualTo("FAIL");
        assertThat(c.lines().get(0).absent()).isTrue();
        assertThat(c.lines().get(0).marks()).isNull();
    }

    @Test
    void aMissingEntryMakesTheResultIncompleteWithNoGrade() {
        Computation c = run(Map.of(maths, marks("90")));

        assertThat(c.complete()).isFalse();
        assertThat(c.result()).isEqualTo("INCOMPLETE");
        assertThat(c.percent()).isNull();
        assertThat(c.grade()).isNull();
        assertThat(c.lines().get(0).marks()).isNull();
    }
}