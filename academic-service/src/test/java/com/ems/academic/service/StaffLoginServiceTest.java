package com.ems.academic.service;

import com.ems.academic.domain.StaffMember;
import com.ems.academic.event.UserLoginEvent;
import com.ems.academic.repository.StaffMemberRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StaffLoginServiceTest {

    @Mock StaffMemberRepository staff;
    @InjectMocks StaffLoginService service;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID login = UUID.randomUUID();

    private UserLoginEvent event(String type, UUID tenant, String email) {
        return new UserLoginEvent(UUID.randomUUID(), type, tenant, email, login, "Meena", "Gupta", Instant.now());
    }

    @Test
    void aTeacherEventCreatesAStaffMember() {
        when(staff.findByKeycloakId(login)).thenReturn(Optional.empty());

        service.apply(event(UserLoginEvent.TEACHER_LOGIN_CREATED, tenantId, "Meena@School.com"));

        ArgumentCaptor<StaffMember> captor = ArgumentCaptor.forClass(StaffMember.class);
        verify(staff).save(captor.capture());
        assertThat(captor.getValue().getEmailLower()).isEqualTo("meena@school.com");
        assertThat(captor.getValue().isActive()).isTrue();
    }

    @Test
    void aRepeatedEventRefreshesTheSameRecordWithoutTouchingTheirStatus() {
        StaffMember existing = new StaffMember(tenantId, login, "old@school.com", "Old", "Name");
        existing.update("Head of Maths", false);
        when(staff.findByKeycloakId(login)).thenReturn(Optional.of(existing));

        service.apply(event(UserLoginEvent.TEACHER_LOGIN_CREATED, tenantId, "new@school.com"));

        assertThat(existing.getEmailLower()).isEqualTo("new@school.com");
        assertThat(existing.getFirstName()).isEqualTo("Meena");
        assertThat(existing.isActive()).isFalse();
        assertThat(existing.getDesignation()).isEqualTo("Head of Maths");
        verify(staff, never()).save(any());
    }

    @Test
    void aLoginNeverMovesToAnotherSchool() {
        StaffMember existing = new StaffMember(tenantId, login, "meena@school.com", "Meena", "Gupta");
        when(staff.findByKeycloakId(login)).thenReturn(Optional.of(existing));

        service.apply(event(UserLoginEvent.TEACHER_LOGIN_CREATED, UUID.randomUUID(), "meena@elsewhere.com"));

        assertThat(existing.getEmailLower()).isEqualTo("meena@school.com");
        verify(staff, never()).save(any());
    }

    @Test
    void otherEventTypesAndUnusableEventsAreIgnored() {
        service.apply(event(UserLoginEvent.PARENT_LOGIN_CREATED, tenantId, "raj@example.com"));
        service.apply(event(UserLoginEvent.TEACHER_LOGIN_CREATED, tenantId, " "));

        verifyNoInteractions(staff);
    }
}