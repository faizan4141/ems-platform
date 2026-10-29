package com.ems.academic.web;

import com.ems.academic.dto.OnboardingResponse;
import com.ems.academic.security.TenantClaims;
import com.ems.academic.service.OnboardingService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/onboarding")
public class OnboardingController {

    private final OnboardingService service;

    public OnboardingController(OnboardingService service) { this.service = service; }

    @GetMapping
    public OnboardingResponse status(@AuthenticationPrincipal Jwt jwt) {
        return service.status(TenantClaims.requireTenantId(jwt));
    }
}