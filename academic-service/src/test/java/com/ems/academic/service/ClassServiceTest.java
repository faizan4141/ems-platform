package com.ems.academic.service;

import com.ems.academic.domain.AcademicYear;
import com.ems.academic.domain.SchoolClass;
import com.ems.academic.dto.ClassRequest;
import com.ems.academic.dto.ClassResponse;
import com.ems.academic.exception.InvalidRequestException;
import com.ems.academic.exception.ResourceNotFoundException;
import com.ems.academic.repository.AcademicYearRepository;
import com.ems.academic.repository.ClassRepository;
import com.ems.academic.repository.SectionRepository;
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
class ClassServiceTest {

    @Mock ClassRepository classes;
    @Mock SectionRepository sections;
    @Mock AcademicYearRepository years;
    @Mock TenantGuard guard;
    @InjectMocks ClassService service;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID yearId = UUID.randomUUID();

    private AcademicYear year() {
        return new AcademicYear(tenantId, "2026-27", LocalDate.of(2026, 4, 1), LocalDate.of(2027, 3, 31), true);
    }

    @Test
    void createsClassWithItsSections() {
        when(years.findByIdAndTenantId(yearId, tenantId)).thenReturn(Optional.of(year()));
        when(classes.existsByAcademicYearIdAndName(any(), any())).thenReturn(false);
        when(classes.save(any(SchoolClass.class))).thenAnswer(inv -> inv.getArgument(0));
        when(sections.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ClassResponse res = service.create(tenantId, new ClassRequest(yearId, "Class 8", 8, List.of("B", "A")));

        assertThat(res.sections()).extracting("name").containsExactly("A", "B");   // sorted
        verify(sections, times(2)).save(any());
    }

    @Test
    void rejectsDuplicateSectionNamesIgnoringCase() {
        when(years.findByIdAndTenantId(yearId, tenantId)).thenReturn(Optional.of(year()));
        when(classes.existsByAcademicYearIdAndName(any(), any())).thenReturn(false);

        assertThatThrownBy(() -> service.create(tenantId, new ClassRequest(yearId, "Class 8", 8, List.of("A", "a"))))
                .isInstanceOf(InvalidRequestException.class);
        verify(classes, never()).save(any());
    }

    @Test
    void failsForAYearOfAnotherSchool() {
        when(years.findByIdAndTenantId(yearId, tenantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(tenantId, new ClassRequest(yearId, "Class 8", 8, List.of())))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}