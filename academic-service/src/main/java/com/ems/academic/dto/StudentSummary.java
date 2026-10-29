package com.ems.academic.dto;

import com.ems.academic.domain.Gender;
import com.ems.academic.domain.Student;
import com.ems.academic.domain.StudentStatus;
import java.util.UUID;

public record StudentSummary(UUID id, String admissionNo, String firstName, String lastName, Gender gender,
                             StudentStatus status, UUID sectionId, String className, String sectionName) {

    public static StudentSummary from(Student s, String className, String sectionName) {
        return new StudentSummary(s.getId(), s.getAdmissionNo(), s.getFirstName(), s.getLastName(),
                s.getGender(), s.getStatus(), s.getSectionId(), className, sectionName);
    }
}