package com.ems.user.service;

import com.ems.user.domain.*;
import com.ems.user.dto.*;
import com.ems.user.exception.*;
import com.ems.user.identity.IdentityProvisioner;
import com.ems.user.identity.KeycloakRoles;
import com.ems.user.identity.NewLogin;
import com.ems.user.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import com.ems.user.event.UserLoginEvent;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.UUID;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository users;
    private final TenantSnapshotRepository tenants;
    private final IdentityProvisioner identity;
    private final ApplicationEventPublisher events;

    public UserService(UserRepository users, TenantSnapshotRepository tenants,
                       IdentityProvisioner identity, ApplicationEventPublisher events) {
        this.users = users;
        this.tenants = tenants;
        this.identity = identity;
        this.events = events;
    }

    @Transactional
    public CreateUserResponse create(UUID tenantId, CreateUserRequest req) {
        requireActiveTenant(tenantId);

        String email = req.email().trim().toLowerCase();
        if (users.existsByTenantIdAndEmail(tenantId, email)) {
            throw new DuplicateUserException(email);
        }

        User u = new User();
        u.setTenantId(tenantId);
        u.setEmail(email);
        u.setFirstName(req.firstName().trim());
        u.setLastName(req.lastName().trim());
        u.setRole(req.role());
        u.setStatus(UserStatus.ACTIVE);
        u = users.saveAndFlush(u);

        provisionLogin(u);
        announceParentLogin(u);
        boolean invitationSent = trySendInvitation(u);
        return new CreateUserResponse(UserResponse.from(u), invitationSent);
    }

    /**
     * Platform admin: invite the first admin of a school.
     */
    @Transactional
    public CreateUserResponse createTenantAdmin(CreateTenantAdminRequest req) {
        return create(req.tenantId(),
                new CreateUserRequest(req.email(), req.firstName(), req.lastName(), Role.ADMIN));
    }

    @Transactional
    public void resendInvitation(UUID tenantId, UUID userId) {
        requireActiveTenant(tenantId);
        User u = users.findByIdAndTenantId(userId, tenantId)
                .orElseThrow(() -> new UserNotFoundException(userId));
        if (u.getKeycloakId() == null) {
            provisionLogin(u);   // users created before Step 9 get their login now
        }
        announceParentLogin(u);
        identity.sendSetPasswordEmail(u.getKeycloakId());
    }

    @Transactional(readOnly = true)
    public List<UserResponse> list(UUID tenantId) {
        return users.findByTenantId(tenantId).stream().map(UserResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public UserResponse get(UUID tenantId, UUID userId) {
        return users.findByIdAndTenantId(userId, tenantId)
                .map(UserResponse::from)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }

    // ---- helpers ----

    private void requireActiveTenant(UUID tenantId) {
        TenantSnapshot tenant = tenants.findById(tenantId)
                .orElseThrow(() -> new TenantNotFoundException(tenantId));
        if (!"ACTIVE".equals(tenant.getStatus())) {
            throw new TenantNotActiveException(tenantId);
        }
    }

    private void provisionLogin(User u) {
        UUID keycloakId = identity.createUser(new NewLogin(
                u.getTenantId(), u.getEmail(), u.getFirstName(), u.getLastName(),
                KeycloakRoles.forRole(u.getRole())));
        u.setKeycloakId(keycloakId);
        deleteLoginIfTransactionRollsBack(keycloakId);
    }

    private boolean trySendInvitation(User u) {
        try {
            identity.sendSetPasswordEmail(u.getKeycloakId());
            return true;
        } catch (IdentityProviderException e) {
            log.warn("User {} created but invitation email failed: {}", u.getEmail(), e.getMessage());
            return false;
        }
    }

    /**
     * If the database transaction fails after Keycloak succeeded, remove the orphaned login.
     */
    private void deleteLoginIfTransactionRollsBack(UUID keycloakId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    try {
                        identity.deleteUser(keycloakId);
                    } catch (RuntimeException e) {
                        log.error("Orphaned Keycloak user {} could not be deleted; remove it manually",
                                keycloakId, e);
                    }
                }
            }
        });
    }

    /** Tells other services that a parent login exists, so children can be linked to it. */
    private void announceParentLogin(User u) {
        if (u.getRole() == Role.PARENT && u.getKeycloakId() != null) {
            events.publishEvent(UserLoginEvent.parentLogin(u));
        }
    }
}