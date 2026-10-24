package com.ems.user.service;

import com.ems.user.domain.*;
import com.ems.user.dto.*;
import com.ems.user.exception.*;
import com.ems.user.identity.IdentityProvisioner;
import com.ems.user.identity.NewLogin;
import com.ems.user.repository.*;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    UserRepository users;
    @Mock
    TenantSnapshotRepository tenants;
    @Mock
    IdentityProvisioner identity;
    @InjectMocks
    UserService service;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID keycloakId = UUID.randomUUID();
    private final CreateUserRequest request =
            new CreateUserRequest("Teacher@School.com", "Asha", "Verma", Role.TEACHER);

    private TenantSnapshot snapshot(String status) {
        TenantSnapshot s = new TenantSnapshot(tenantId);
        s.update("dps", "DPS", status, Instant.now());
        return s;
    }

    private void activeTenant() {
        when(tenants.findById(tenantId)).thenReturn(Optional.of(snapshot("ACTIVE")));
    }

    private void freshEmail(String email) {
        when(users.existsByTenantIdAndEmail(tenantId, email)).thenReturn(false);
        when(users.saveAndFlush(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void create_createsLoginWithTenantAndRole_andSendsInvitation() {
        activeTenant();
        freshEmail("teacher@school.com");
        when(identity.createUser(any(NewLogin.class))).thenReturn(keycloakId);

        CreateUserResponse res = service.create(tenantId, request);

        assertThat(res.user().email()).isEqualTo("teacher@school.com");
        assertThat(res.user().hasLogin()).isTrue();
        assertThat(res.invitationSent()).isTrue();

        ArgumentCaptor<NewLogin> captor = ArgumentCaptor.forClass(NewLogin.class);
        verify(identity).createUser(captor.capture());
        assertThat(captor.getValue().tenantId()).isEqualTo(tenantId);
        assertThat(captor.getValue().realmRole()).isEqualTo("TEACHER");
        verify(identity).sendSetPasswordEmail(keycloakId);
    }

    @Test
    void create_stillSucceedsWhenInvitationEmailFails() {
        activeTenant();
        freshEmail("teacher@school.com");
        when(identity.createUser(any(NewLogin.class))).thenReturn(keycloakId);
        doThrow(new IdentityProviderException("smtp down", null))
                .when(identity).sendSetPasswordEmail(keycloakId);

        CreateUserResponse res = service.create(tenantId, request);

        assertThat(res.user().hasLogin()).isTrue();
        assertThat(res.invitationSent()).isFalse();
    }

    @Test
    void create_mapsAdminRoleToTenantAdmin() {
        activeTenant();
        freshEmail("boss@school.com");
        when(identity.createUser(any(NewLogin.class))).thenReturn(keycloakId);

        service.create(tenantId, new CreateUserRequest("boss@school.com", "Ravi", "Singh", Role.ADMIN));

        ArgumentCaptor<NewLogin> captor = ArgumentCaptor.forClass(NewLogin.class);
        verify(identity).createUser(captor.capture());
        assertThat(captor.getValue().realmRole()).isEqualTo("TENANT_ADMIN");
    }

    @Test
    void create_failsWhenTenantUnknown() {
        when(tenants.findById(tenantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(tenantId, request))
                .isInstanceOf(TenantNotFoundException.class);
    }

    @Test
    void create_failsWhenTenantSuspended_andTouchesNothing() {
        when(tenants.findById(tenantId)).thenReturn(Optional.of(snapshot("SUSPENDED")));

        assertThatThrownBy(() -> service.create(tenantId, request))
                .isInstanceOf(TenantNotActiveException.class);
        verifyNoInteractions(identity);
        verify(users, never()).saveAndFlush(any());
    }

    @Test
    void create_failsOnDuplicateEmailInSameTenant() {
        activeTenant();
        when(users.existsByTenantIdAndEmail(tenantId, "teacher@school.com")).thenReturn(true);

        assertThatThrownBy(() -> service.create(tenantId, request))
                .isInstanceOf(DuplicateUserException.class);
        verify(users, never()).saveAndFlush(any());
        verifyNoInteractions(identity);
    }

    @Test
    void create_propagatesLoginConflict_andNeverInvites() {
        activeTenant();
        freshEmail("teacher@school.com");
        when(identity.createUser(any(NewLogin.class)))
                .thenThrow(new LoginAccountExistsException("teacher@school.com"));

        assertThatThrownBy(() -> service.create(tenantId, request))
                .isInstanceOf(LoginAccountExistsException.class);
        verify(identity, never()).sendSetPasswordEmail(any());
    }

    @Test
    void resendInvitation_provisionsMissingLogin_thenInvites() {
        UUID userId = UUID.randomUUID();
        User legacy = new User();
        legacy.setTenantId(tenantId);
        legacy.setEmail("meena@dps.edu");
        legacy.setFirstName("Meena");
        legacy.setLastName("Gupta");
        legacy.setRole(Role.TEACHER);
        legacy.setStatus(UserStatus.ACTIVE);

        activeTenant();
        when(users.findByIdAndTenantId(userId, tenantId)).thenReturn(Optional.of(legacy));
        when(identity.createUser(any(NewLogin.class))).thenReturn(keycloakId);

        service.resendInvitation(tenantId, userId);

        assertThat(legacy.getKeycloakId()).isEqualTo(keycloakId);
        verify(identity).sendSetPasswordEmail(keycloakId);
    }

    @Test
    void createTenantAdmin_alwaysCreatesATenantAdminLogin() {
        activeTenant();
        freshEmail("head@school.com");
        when(identity.createUser(any(NewLogin.class))).thenReturn(keycloakId);

        service.createTenantAdmin(new CreateTenantAdminRequest(tenantId, "head@school.com", "Ravi", "Singh"));

        ArgumentCaptor<NewLogin> captor = ArgumentCaptor.forClass(NewLogin.class);
        verify(identity).createUser(captor.capture());
        assertThat(captor.getValue().tenantId()).isEqualTo(tenantId);
        assertThat(captor.getValue().realmRole()).isEqualTo("TENANT_ADMIN");
    }
}