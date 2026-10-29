package com.ems.academic.web;

import com.ems.academic.dto.ProfileRequest;
import com.ems.academic.dto.ProfileResponse;
import com.ems.academic.security.TenantClaims;
import com.ems.academic.service.SchoolProfileService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/school/profile")
public class SchoolProfileController {

    private final SchoolProfileService service;

    public SchoolProfileController(SchoolProfileService service) { this.service = service; }

    @GetMapping
    public ProfileResponse get(@AuthenticationPrincipal Jwt jwt) {
        return service.get(TenantClaims.requireTenantId(jwt));
    }

    @PutMapping
    public ProfileResponse save(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ProfileRequest request) {
        return service.save(TenantClaims.requireTenantId(jwt), request);
    }
}