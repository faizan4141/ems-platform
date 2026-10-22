package com.ems.tenant.web;

import com.ems.tenant.dto.*;
import com.ems.tenant.service.TenantService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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
    public TenantResponse create(@Valid @RequestBody CreateTenantRequest request) {
        return service.create(request);
    }

    @GetMapping("/{id}")
    public TenantResponse get(@PathVariable UUID id) {
        return service.get(id);
    }

    @GetMapping
    public List<TenantResponse> list() {
        return service.list();
    }

    @PatchMapping("/{id}/status")
    public TenantResponse updateStatus(@PathVariable UUID id,
                                       @Valid @RequestBody UpdateStatusRequest request) {
        return service.updateStatus(id, request.status());
    }
}