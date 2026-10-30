package com.ems.academic.service;

import com.ems.academic.domain.AttendanceStatus;
import com.ems.academic.domain.Student;
import com.ems.academic.dto.AttendanceDay;
import com.ems.academic.dto.ChildToday;
import com.ems.academic.exception.ResourceNotFoundException;
import com.ems.academic.repository.AttendanceRecordRepository;
import com.ems.academic.repository.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ParentServiceTest {

    @Mock StudentRepository students;
    @Mock AttendanceRecordRepository records;
    @Mock StudentService studentService;
    @Mock TenantGuard guard;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID parentId = UUID.randomUUID();
    private ParentService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-10-30T05:00:00Z"), ZoneOffset.UTC);
        service = new ParentService(students, records, studentService, guard, clock);
    }

    private Student child(UUID id) {
        Student s = mock(Student.class);
        when(s.getId()).thenReturn(id);
        when(s.getFirstName()).thenReturn("Aarav");
        when(s.getLastName()).thenReturn("Sharma");
        when(s.getSectionId()).thenReturn(null);
        return s;
    }

    @Test
    void lateCountsAsAttending() {
        List<AttendanceDay> days = List.of(
                new AttendanceDay(LocalDate.of(2026, 10, 30), AttendanceStatus.PRESENT),
                new AttendanceDay(LocalDate.of(2026, 10, 29), AttendanceStatus.LATE),
                new AttendanceDay(LocalDate.of(2026, 10, 28), AttendanceStatus.ABSENT),
                new AttendanceDay(LocalDate.of(2026, 10, 27), AttendanceStatus.PRESENT));
        assertThat(ParentService.percent(days)).isEqualTo(75);
    }

    @Test
    void noPercentageWithoutRecords() {
        assertThat(ParentService.percent(List.of())).isNull();
    }

    @Test
    void aSubjectThatIsNotAnIdSeesNoChildren() {
        assertThat(service.children(tenantId, "not-a-uuid")).isEmpty();
        verifyNoInteractions(students);
    }

    @Test
    void aChildThatIsNotLinkedLooksLikeItDoesNotExist() {
        when(students.findChildren(parentId, tenantId)).thenReturn(List.of());

        assertThatThrownBy(() -> service.today(tenantId, parentId.toString(), UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void reportsTodaysStatusAndTheRecentDays() {
        UUID studentId = UUID.randomUUID();
        Student child = child(studentId);
        when(students.findChildren(parentId, tenantId)).thenReturn(List.of(child));
        when(records.findDays(studentId, LocalDate.of(2026, 9, 30))).thenReturn(List.of(
                new AttendanceDay(LocalDate.of(2026, 10, 30), AttendanceStatus.ABSENT),
                new AttendanceDay(LocalDate.of(2026, 10, 29), AttendanceStatus.PRESENT)));

        ChildToday result = service.today(tenantId, parentId.toString(), studentId);

        assertThat(result.today()).isEqualTo(AttendanceStatus.ABSENT);
        assertThat(result.percentLast30()).isEqualTo(50);
        assertThat(result.recent()).hasSize(2);
        assertThat(result.name()).isEqualTo("Aarav Sharma");
    }
}