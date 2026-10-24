package com.ems.tenant.web;

import com.ems.tenant.dto.*;
import com.ems.tenant.security.TenantAccess;
import com.ems.tenant.service.TenantService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tenants")
public class TenantController {

    private final TenantService service;

    public TenantController(TenantService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public TenantResponse create(@Valid @RequestBody CreateTenantRequest request) {
        return service.create(request);
    }

    @GetMapping
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public List<TenantResponse> list() {
        return service.list();
    }

    @GetMapping("/{id}")
    public TenantResponse get(@PathVariable UUID id,
                              Authentication authentication,
                              @AuthenticationPrincipal Jwt jwt) {
        if (!TenantAccess.canView(authentication, jwt, id)) {
            throw new AccessDeniedException("Not allowed to view this tenant");
        }
        return service.get(id);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public TenantResponse updateStatus(@PathVariable UUID id,
                                       @Valid @RequestBody UpdateStatusRequest request) {
        return service.updateStatus(id, request.status());
    }
}