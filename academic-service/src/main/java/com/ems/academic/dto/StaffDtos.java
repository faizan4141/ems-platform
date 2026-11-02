package com.ems.academic.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public final class StaffDtos {

    private StaffDtos() {}

    public record AssignmentView(UUID id, UUID sectionId, String className, String sectionName,
                                 UUID subjectId, String subjectName, boolean classTeacher) {}

    public record StaffView(UUID id, String name, String email, String designation, boolean active,
                            List<AssignmentView> assignments) {}

    public record StaffUpdate(@Size(max = 100) String designation, @NotNull Boolean active) {}

    public record AssignmentRequest(@NotNull UUID staffId, @NotNull UUID sectionId, UUID subjectId, Boolean classTeacher) {}

    public record SubjectRef(UUID id, String name) {}

    public record SectionAccess(UUID sectionId, UUID classId, String className, String sectionName,
                                boolean classTeacher, List<SubjectRef> subjects) {}

    public record MyTeaching(boolean admin, List<SectionAccess> sections) {}
}