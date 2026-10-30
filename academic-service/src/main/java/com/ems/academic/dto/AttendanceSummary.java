package com.ems.academic.dto;

import java.time.LocalDate;

public record AttendanceSummary(LocalDate date, long sectionsTotal, long sectionsTaken,
                                int present, int absent, int late, Integer percentPresent) {

    public static AttendanceSummary of(LocalDate date, long sectionsTotal, long sectionsTaken,
                                       int present, int absent, int late) {
        int total = present + absent + late;
        Integer percent = total == 0 ? null : (int) Math.round(100.0 * (present + late) / total);
        return new AttendanceSummary(date, sectionsTotal, sectionsTaken, present, absent, late, percent);
    }
}