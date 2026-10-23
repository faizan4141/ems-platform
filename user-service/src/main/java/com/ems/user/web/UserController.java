package com.ems.user.web;

import com.ems.user.dto.*;
import com.ems.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tenants/{tenantId}/users")
public class UserController {

    private final UserService service;

    public UserController(UserService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(@PathVariable UUID tenantId,
                               @Valid @RequestBody CreateUserRequest request) {
        return service.create(tenantId, request);
    }

    @GetMapping
    public List<UserResponse> list(@PathVariable UUID tenantId) {
        return service.list(tenantId);
    }

    @GetMapping("/{userId}")
    public UserResponse get(@PathVariable UUID tenantId, @PathVariable UUID userId) {
        return service.get(tenantId, userId);
    }
}