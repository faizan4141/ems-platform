package com.ems.user.service;

import com.ems.user.domain.*;
import com.ems.user.dto.*;
import com.ems.user.exception.*;
import com.ems.user.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository users;
    private final TenantSnapshotRepository tenants;

    public UserService(UserRepository users, TenantSnapshotRepository tenants) {
        this.users = users;
        this.tenants = tenants;
    }

    @Transactional
    public UserResponse create(UUID tenantId, CreateUserRequest req) {
        TenantSnapshot tenant = tenants.findById(tenantId)
                .orElseThrow(() -> new TenantNotFoundException(tenantId));
        if (!"ACTIVE".equals(tenant.getStatus())) {
            throw new TenantNotActiveException(tenantId);
        }

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
        return UserResponse.from(users.save(u));
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
}