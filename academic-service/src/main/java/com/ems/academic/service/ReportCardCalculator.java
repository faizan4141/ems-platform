package com.ems.academic.service;

import com.ems.academic.dto.ExamDtos.ReportLine;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ReportCardCalculator {

    public record SubjectInput(UUID examSubjectId, String name, BigDecimal maxMarks, BigDecimal passMarks) {}
    public record Entry(BigDecimal marks, boolean absent) {}
    public record Computation(List<ReportLine> lines, BigDecimal totalMarks, BigDecimal totalMax,
                              BigDecimal percent, String grade, String result, boolean complete) {}

    private static final BigDecimal ZERO = new BigDecimal("0.00");
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private ReportCardCalculator() {}

    /** A missing entry makes the result INCOMPLETE. An absent subject counts as zero and as not passed. */
    public static Computation compute(List<SubjectInput> subjects, Map<UUID, Entry> entries, List<GradeScale.Band> bands) {
        List<ReportLine> lines = new ArrayList<>();
        BigDecimal total = ZERO;
        BigDecimal totalMax = ZERO;
        boolean complete = true;
        boolean allPassed = true;

        for (SubjectInput s : subjects) {
            totalMax = totalMax.add(s.maxMarks());
            Entry entry = entries.get(s.examSubjectId());
            if (entry == null) {
                complete = false;
                lines.add(new ReportLine(s.name(), s.maxMarks(), s.passMarks(), null, false, null, null, null));
                continue;
            }
            BigDecimal earned = entry.absent() || entry.marks() == null ? ZERO : entry.marks();
            total = total.add(earned);
            boolean passed = !entry.absent() && earned.compareTo(s.passMarks()) >= 0;
            allPassed &= passed;
            BigDecimal percent = percent(earned, s.maxMarks());
            lines.add(new ReportLine(s.name(), s.maxMarks(), s.passMarks(), entry.absent() ? null : earned,
                    entry.absent(), entry.absent() ? null : percent,
                    entry.absent() ? null : GradeScale.gradeFor(percent, bands), passed));
        }
        if (!complete) {
            return new Computation(lines, total, totalMax, null, null, "INCOMPLETE", false);
        }
        BigDecimal percent = percent(total, totalMax);
        return new Computation(lines, total, totalMax, percent, GradeScale.gradeFor(percent, bands),
                allPassed ? "PASS" : "FAIL", true);
    }

    static BigDecimal percent(BigDecimal part, BigDecimal whole) {
        return whole.signum() == 0 ? ZERO : part.multiply(HUNDRED).divide(whole, 2, RoundingMode.HALF_UP);
    }
}