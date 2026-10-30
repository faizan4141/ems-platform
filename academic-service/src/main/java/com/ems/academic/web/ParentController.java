package com.ems.academic.web;

import com.ems.academic.dto.ChildSummary;
import com.ems.academic.dto.ChildToday;
import com.ems.academic.security.TenantClaims;
import com.ems.academic.service.ParentService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/parent")
public class ParentController {

    private final ParentService service;

    public ParentController(ParentService service) { this.service = service; }

    @GetMapping("/children")
    public List<ChildSummary> children(@AuthenticationPrincipal Jwt jwt) {
        return service.children(TenantClaims.requireTenantId(jwt), jwt.getSubject());
    }

    @GetMapping("/children/{studentId}/today")
    public ChildToday today(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID studentId) {
        return service.today(TenantClaims.requireTenantId(jwt), jwt.getSubject(), studentId);
    }
}