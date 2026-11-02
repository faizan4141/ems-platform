package com.ems.academic.web;

import com.ems.academic.dto.*;
import com.ems.academic.security.Caller;
import com.ems.academic.security.TenantClaims;
import com.ems.academic.service.AttendanceService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/attendance")
public class AttendanceController {

    private final AttendanceService service;

    public AttendanceController(AttendanceService service) { this.service = service; }

    @GetMapping("/sections/{sectionId}")
    public RosterResponse roster(@AuthenticationPrincipal Jwt jwt, Authentication authentication, @PathVariable UUID sectionId,
                                 @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return service.roster(Caller.of(jwt, authentication), sectionId, date);
    }

    @PutMapping("/sections/{sectionId}")
    public SaveResponse save(@AuthenticationPrincipal Jwt jwt, Authentication authentication,
                             @PathVariable UUID sectionId,
                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                             @Valid @RequestBody AttendanceRequest request) {
        return service.save(Caller.of(jwt, authentication), sectionId, date, request);
    }

    @GetMapping("/summary")
    public AttendanceSummary summary(@AuthenticationPrincipal Jwt jwt,
                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return service.summary(TenantClaims.requireTenantId(jwt), date);
    }
}