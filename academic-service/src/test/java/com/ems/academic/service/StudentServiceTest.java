package com.ems.academic.service;

import com.ems.academic.domain.Guardian;
import com.ems.academic.domain.Relationship;
import com.ems.academic.domain.Student;
import com.ems.academic.dto.GuardianRequest;
import com.ems.academic.dto.StudentDetail;
import com.ems.academic.dto.StudentRequest;
import com.ems.academic.exception.DuplicateResourceException;
import com.ems.academic.exception.InvalidRequestException;
import com.ems.academic.exception.ResourceNotFoundException;
import com.ems.academic.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StudentServiceTest {

    @Mock StudentRepository students;
    @Mock GuardianRepository guardians;
    @Mock SectionRepository sections;
    @Mock ClassRepository classes;
    @Mock TenantGuard guard;
    @Mock GuardianLoginRepository logins;
    @InjectMocks StudentService service;

    private final UUID tenantId = UUID.randomUUID();

    private GuardianRequest guardian(String name, Boolean primary, String phone) {
        return new GuardianRequest(name, Relationship.FATHER, phone, null, primary);
    }

    private StudentRequest request(UUID sectionId, GuardianRequest... gs) {
        return new StudentRequest("A-1", "Aarav", "Sharma", LocalDate.of(2014, 5, 10), null, null, sectionId, List.of(gs));
    }

    private void stubSaves() {
        when(students.existsByTenantIdAndAdmissionNoIgnoreCase(tenantId, "A-1")).thenReturn(false);
        when(students.save(any(Student.class))).thenAnswer(inv -> inv.getArgument(0));
        when(guardians.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void firstGuardianBecomesPrimaryWhenNoneIsMarked() {
        stubSaves();

        StudentDetail res = service.create(tenantId,
                request(null, guardian("Raj", null, "+919876543210"), guardian("Sita", null, "+919876543211")));

        assertThat(res.guardians()).extracting("primary").containsExactly(true, false);
        assertThat(res.guardians().get(0).phone()).isEqualTo("+919876543210");
    }

    @Test
    void respectsAnExplicitPrimaryGuardian() {
        stubSaves();

        StudentDetail res = service.create(tenantId,
                request(null, guardian("Raj", false, "+919876543210"), guardian("Sita", true, "+919876543211")));

        assertThat(res.guardians()).extracting("primary").containsExactly(false, true);
    }

    @Test
    void rejectsTwoPrimaryGuardians() {
        when(students.existsByTenantIdAndAdmissionNoIgnoreCase(tenantId, "A-1")).thenReturn(false);

        assertThatThrownBy(() -> service.create(tenantId,
                request(null, guardian("Raj", true, "+919876543210"), guardian("Sita", true, "+919876543211"))))
                .isInstanceOf(InvalidRequestException.class);
        verify(students, never()).save(any());
    }

    @Test
    void rejectsAnInvalidGuardianPhone() {
        when(students.existsByTenantIdAndAdmissionNoIgnoreCase(tenantId, "A-1")).thenReturn(false);
        when(students.save(any(Student.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThatThrownBy(() -> service.create(tenantId, request(null, guardian("Raj", null, "12"))))
                .isInstanceOf(InvalidRequestException.class);
        verify(guardians, never()).saveAll(any());
    }

    @Test
    void rejectsADuplicateAdmissionNumber() {
        when(students.existsByTenantIdAndAdmissionNoIgnoreCase(tenantId, "A-1")).thenReturn(true);

        assertThatThrownBy(() -> service.create(tenantId, request(null, guardian("Raj", null, "+919876543210"))))
                .isInstanceOf(DuplicateResourceException.class);
        verify(guardians, never()).saveAll(any());
    }

    @Test
    void rejectsASectionOfAnotherSchool() {
        UUID foreignSection = UUID.randomUUID();
        when(students.existsByTenantIdAndAdmissionNoIgnoreCase(tenantId, "A-1")).thenReturn(false);
        when(sections.findByIdAndTenantId(foreignSection, tenantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(tenantId,
                request(foreignSection, guardian("Raj", null, "+919876543210"))))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}