package com.ems.user.web;

import com.ems.user.dto.*;
import com.ems.user.security.TenantClaims;
import com.ems.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService service;

    public UserController(UserService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    public UserResponse create(@AuthenticationPrincipal Jwt jwt,
                               @Valid @RequestBody CreateUserRequest request) {
        return service.create(TenantClaims.requireTenantId(jwt), request);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','TEACHER')")
    public List<UserResponse> list(@AuthenticationPrincipal Jwt jwt) {
        return service.list(TenantClaims.requireTenantId(jwt));
    }

    @GetMapping("/{userId}")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','TEACHER')")
    public UserResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID userId) {
        return service.get(TenantClaims.requireTenantId(jwt), userId);
    }
}