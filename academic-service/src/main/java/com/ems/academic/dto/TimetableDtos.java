package com.ems.academic.dto;

import com.ems.academic.domain.SchoolPeriod;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public final class TimetableDtos {

    private TimetableDtos() {}

    public record PeriodRequest(@NotNull @Min(1) @Max(20) Integer number, @NotBlank @Size(max = 30) String name,
                                @NotNull LocalTime startTime, @NotNull LocalTime endTime, Boolean breakPeriod) {}

    public record PeriodView(UUID id, int number, String name, LocalTime startTime, LocalTime endTime, boolean breakPeriod) {
        public static PeriodView from(SchoolPeriod p) {
            return new PeriodView(p.getId(), p.getNumber(), p.getName(), p.getStartTime(), p.getEndTime(), p.isBreakPeriod());
        }
    }

    public record SlotRequest(@NotNull UUID subjectId, @NotNull UUID staffId, @Size(max = 30) String room) {}

    public record EntryView(UUID id, int day, UUID periodId, UUID subjectId, String subjectName,
                            UUID staffId, String teacherName, String room) {}

    /** sectionId is null for a child who has not been placed in a class yet. */
    public record SectionTimetable(UUID sectionId, String className, String sectionName,
                                   List<PeriodView> periods, List<EntryView> entries) {}

    public record LessonView(int day, UUID periodId, int periodNumber, String periodName, LocalTime startTime,
                             LocalTime endTime, UUID sectionId, String className, String sectionName,
                             UUID subjectId, String subjectName, String room) {}

    public record TeacherTimetable(List<PeriodView> periods, List<LessonView> lessons) {}

    public record TodayView(LocalDate date, int day, List<LessonView> lessons) {}
}