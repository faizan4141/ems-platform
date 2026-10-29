package com.ems.academic.web;

import com.ems.academic.dto.YearRequest;
import com.ems.academic.dto.YearResponse;
import com.ems.academic.security.TenantClaims;
import com.ems.academic.service.AcademicYearService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/academic-years")
public class AcademicYearController {

    private final AcademicYearService service;

    public AcademicYearController(AcademicYearService service) { this.service = service; }

    @GetMapping
    public List<YearResponse> list(@AuthenticationPrincipal Jwt jwt) {
        return service.list(TenantClaims.requireTenantId(jwt));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public YearResponse create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody YearRequest request) {
        return service.create(TenantClaims.requireTenantId(jwt), request);
    }

    @PutMapping("/{id}/current")
    public YearResponse setCurrent(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return service.setCurrent(TenantClaims.requireTenantId(jwt), id);
    }
}