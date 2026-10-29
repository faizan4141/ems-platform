package com.ems.academic.dto;

import com.ems.academic.domain.AcademicYear;
import java.time.LocalDate;
import java.util.UUID;

public record YearResponse(UUID id, String name, LocalDate startDate, LocalDate endDate, boolean current) {

    public static YearResponse from(AcademicYear y) {
        return new YearResponse(y.getId(), y.getName(), y.getStartDate(), y.getEndDate(), y.isCurrentYear());
    }
}