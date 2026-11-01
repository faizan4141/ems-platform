package com.ems.academic.web;

import com.ems.academic.dto.ChildSummary;
import com.ems.academic.dto.ChildToday;
import com.ems.academic.security.TenantClaims;
import com.ems.academic.service.FeeLedgerService;
import com.ems.academic.dto.FeeDtos.Ledger;
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
    private final FeeLedgerService feeLedger;

    public ParentController(ParentService service, FeeLedgerService feeLedger) { this.service = service; this.feeLedger = feeLedger; }

    @GetMapping("/children")
    public List<ChildSummary> children(@AuthenticationPrincipal Jwt jwt) {
        return service.children(TenantClaims.requireTenantId(jwt), jwt.getSubject());
    }

    @GetMapping("/children/{studentId}/today")
    public ChildToday today(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID studentId) {
        return service.today(TenantClaims.requireTenantId(jwt), jwt.getSubject(), studentId);
    }

    @GetMapping("/children/{studentId}/fees")
    public Ledger fees(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID studentId) {
        UUID tenantId = TenantClaims.requireTenantId(jwt);
        service.requireChild(tenantId, jwt.getSubject(), studentId);   // only the parent's own children
        return feeLedger.ledger(tenantId, studentId);
    }
}