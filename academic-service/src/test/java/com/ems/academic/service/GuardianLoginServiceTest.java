package com.ems.academic.service;

import com.ems.academic.domain.GuardianLogin;
import com.ems.academic.event.UserLoginEvent;
import com.ems.academic.repository.GuardianLoginRepository;
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
class GuardianLoginServiceTest {

    @Mock GuardianLoginRepository logins;
    @InjectMocks GuardianLoginService service;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID keycloakId = UUID.randomUUID();

    private UserLoginEvent event(String type, String email) {
        return new UserLoginEvent(UUID.randomUUID(), type, tenantId, email, keycloakId, Instant.now());
    }

    @Test
    void storesTheLinkWithALowerCasedEmail() {
        when(logins.findById(keycloakId)).thenReturn(Optional.empty());

        service.apply(event(UserLoginEvent.PARENT_LOGIN_CREATED, "Raj@Example.com"));

        ArgumentCaptor<GuardianLogin> captor = ArgumentCaptor.forClass(GuardianLogin.class);
        verify(logins).save(captor.capture());
        assertThat(captor.getValue().getEmailLower()).isEqualTo("raj@example.com");
    }

    @Test
    void aRepeatedEventRewritesTheSameLoginInsteadOfAddingAnother() {
        GuardianLogin existing = new GuardianLogin(keycloakId, tenantId, "old@example.com");
        when(logins.findById(keycloakId)).thenReturn(Optional.of(existing));

        service.apply(event(UserLoginEvent.PARENT_LOGIN_CREATED, "new@example.com"));

        assertThat(existing.getEmailLower()).isEqualTo("new@example.com");
        verify(logins).save(existing);
    }

    @Test
    void ignoresEventsItDoesNotUnderstand() {
        service.apply(event("SOMETHING_ELSE", "raj@example.com"));
        service.apply(event(UserLoginEvent.PARENT_LOGIN_CREATED, " "));

        verifyNoInteractions(logins);
    }
}