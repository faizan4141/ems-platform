package com.ems.academic.dto;

import com.ems.academic.domain.SchoolClass;
import com.ems.academic.domain.Section;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public record ClassResponse(UUID id, UUID academicYearId, String name, int gradeLevel,
                            List<SectionResponse> sections) {

    public static ClassResponse from(SchoolClass c, List<Section> sections) {
        return new ClassResponse(c.getId(), c.getAcademicYearId(), c.getName(), c.getGradeLevel(),
                sections.stream().sorted(Comparator.comparing(Section::getName))
                        .map(SectionResponse::from).toList());
    }
}