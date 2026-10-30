package com.ems.academic.dto;

import com.ems.academic.domain.Gender;
import com.ems.academic.domain.Guardian;
import com.ems.academic.domain.Student;
import com.ems.academic.domain.StudentStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public record StudentDetail(UUID id, String admissionNo, String firstName, String lastName,
                            LocalDate dateOfBirth, Gender gender, LocalDate admissionDate, StudentStatus status,
                            UUID sectionId, String className, String sectionName,
                            List<GuardianResponse> guardians) {

    public static StudentDetail from(Student s, List<Guardian> guardians, Set<String> portalEmails,
                                     String className, String sectionName) {
        return new StudentDetail(s.getId(), s.getAdmissionNo(), s.getFirstName(), s.getLastName(),
                s.getDateOfBirth(), s.getGender(), s.getAdmissionDate(), s.getStatus(), s.getSectionId(),
                className, sectionName,
                guardians.stream().map(g -> GuardianResponse.from(g,
                        g.getEmail() != null && portalEmails.contains(g.getEmail().toLowerCase(Locale.ROOT)))).toList());
    }
}