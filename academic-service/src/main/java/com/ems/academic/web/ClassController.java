package com.ems.academic.web;

import com.ems.academic.dto.ClassRequest;
import com.ems.academic.dto.ClassResponse;
import com.ems.academic.dto.SectionRequest;
import com.ems.academic.security.TenantClaims;
import com.ems.academic.service.ClassService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/classes")
public class ClassController {

    private final ClassService service;

    public ClassController(ClassService service) { this.service = service; }

    @GetMapping
    public List<ClassResponse> list(@AuthenticationPrincipal Jwt jwt,
                                    @RequestParam(required = false) UUID academicYearId) {
        return service.list(TenantClaims.requireTenantId(jwt), academicYearId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ClassResponse create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ClassRequest request) {
        return service.create(TenantClaims.requireTenantId(jwt), request);
    }

    @PostMapping("/{classId}/sections")
    @ResponseStatus(HttpStatus.CREATED)
    public ClassResponse addSection(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID classId,
                                    @Valid @RequestBody SectionRequest request) {
        return service.addSection(TenantClaims.requireTenantId(jwt), classId, request);
    }
}