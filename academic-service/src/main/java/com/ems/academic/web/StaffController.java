package com.ems.academic.web;

import com.ems.academic.dto.StaffDtos.*;
import com.ems.academic.security.Caller;
import com.ems.academic.security.TenantClaims;
import com.ems.academic.service.StaffService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class StaffController {

    private final StaffService service;

    public StaffController(StaffService service) { this.service = service; }

    @GetMapping("/staff")
    public List<StaffView> list(@AuthenticationPrincipal Jwt jwt) {
        return service.list(TenantClaims.requireTenantId(jwt));
    }

    @PutMapping("/staff/{id}")
    public StaffView update(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id, @Valid @RequestBody StaffUpdate request) {
        return service.update(TenantClaims.requireTenantId(jwt), id, request);
    }

    @PostMapping("/assignments")
    @ResponseStatus(HttpStatus.CREATED)
    public AssignmentView assign(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody AssignmentRequest request) {
        return service.assign(TenantClaims.requireTenantId(jwt), request);
    }

    @DeleteMapping("/assignments/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unassign(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        service.unassign(TenantClaims.requireTenantId(jwt), id);
    }

    @GetMapping("/me/teaching")
    public MyTeaching myTeaching(@AuthenticationPrincipal Jwt jwt, Authentication authentication) {
        return service.myTeaching(Caller.of(jwt, authentication));
    }
}