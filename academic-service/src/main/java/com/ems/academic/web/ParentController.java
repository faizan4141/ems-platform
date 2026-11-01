package com.ems.academic.web;

import com.ems.academic.dto.ChildSummary;
import com.ems.academic.dto.ChildToday;
import com.ems.academic.dto.FeeDtos.Ledger;
import com.ems.academic.dto.OnlinePaymentDtos.*;
import com.ems.academic.security.TenantClaims;
import com.ems.academic.service.FeeLedgerService;
import com.ems.academic.service.OnlinePaymentService;
import com.ems.academic.service.ParentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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
    private final OnlinePaymentService online;

    public ParentController(ParentService service, FeeLedgerService feeLedger, OnlinePaymentService online) {
        this.service = service;
        this.feeLedger = feeLedger;
        this.online = online;
    }

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

    @PostMapping("/children/{studentId}/pay")
    @ResponseStatus(HttpStatus.CREATED)
    public CheckoutResponse pay(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID studentId,
                                @Valid @RequestBody PayRequest request) {
        return online.start(TenantClaims.requireTenantId(jwt), jwt.getSubject(), studentId, request);
    }

    @GetMapping("/payment-attempts/{attemptId}")
    public AttemptView attempt(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID attemptId) {
        return online.status(TenantClaims.requireTenantId(jwt), jwt.getSubject(), attemptId);
    }
}