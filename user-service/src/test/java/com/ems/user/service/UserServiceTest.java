package com.ems.user.service;

import com.ems.user.domain.*;
import com.ems.user.dto.*;
import com.ems.user.exception.*;
import com.ems.user.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    UserRepository users;
    @Mock
    TenantSnapshotRepository tenants;
    @InjectMocks
    UserService service;

    private final UUID tenantId = UUID.randomUUID();
    private final CreateUserRequest request =
            new CreateUserRequest("Teacher@School.com", "Asha", "Verma", Role.TEACHER);

    private TenantSnapshot snapshot(String status) {
        TenantSnapshot s = new TenantSnapshot(tenantId);
        s.update("dps", "DPS", status, Instant.now());
        return s;
    }

    @Test
    void create_normalizesEmail_andActivatesUser() {
        when(tenants.findById(tenantId)).thenReturn(Optional.of(snapshot("ACTIVE")));
        when(users.existsByTenantIdAndEmail(tenantId, "teacher@school.com")).thenReturn(false);
        when(users.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponse res = service.create(tenantId, request);

        assertThat(res.email()).isEqualTo("teacher@school.com");
        assertThat(res.status()).isEqualTo(UserStatus.ACTIVE);
        assertThat(res.tenantId()).isEqualTo(tenantId);
    }

    @Test
    void create_failsWhenTenantUnknown() {
        when(tenants.findById(tenantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(tenantId, request))
                .isInstanceOf(TenantNotFoundException.class);
    }

    @Test
    void create_failsWhenTenantSuspended() {
        when(tenants.findById(tenantId)).thenReturn(Optional.of(snapshot("SUSPENDED")));

        assertThatThrownBy(() -> service.create(tenantId, request))
                .isInstanceOf(TenantNotActiveException.class);
        verify(users, never()).save(any());
    }

    @Test
    void create_failsOnDuplicateEmailInSameTenant() {
        when(tenants.findById(tenantId)).thenReturn(Optional.of(snapshot("ACTIVE")));
        when(users.existsByTenantIdAndEmail(tenantId, "teacher@school.com")).thenReturn(true);

        assertThatThrownBy(() -> service.create(tenantId, request))
                .isInstanceOf(DuplicateUserException.class);
    }
}