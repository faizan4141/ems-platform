package com.ems.academic.web;

import com.ems.academic.dto.FeeDtos.*;
import com.ems.academic.security.TenantClaims;
import com.ems.academic.service.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/fees")
public class FeeController {

    private final FeeItemService items;
    private final FeeLedgerService ledger;
    private final FeePaymentService paymentService;
    private final FeeReminderService reminders;

    public FeeController(FeeItemService items, FeeLedgerService ledger, FeePaymentService paymentService,
                         FeeReminderService reminders) {
        this.items = items;
        this.ledger = ledger;
        this.paymentService = paymentService;
        this.reminders = reminders;
    }

    @GetMapping("/summary")
    public FeeSummary summary(@AuthenticationPrincipal Jwt jwt) {
        return ledger.summary(TenantClaims.requireTenantId(jwt));
    }

    @GetMapping("/dues")
    public DuesResponse dues(@AuthenticationPrincipal Jwt jwt, @RequestParam(required = false) UUID classId) {
        return ledger.dues(TenantClaims.requireTenantId(jwt), classId);
    }

    @GetMapping("/items")
    public List<FeeItemResponse> listItems(@AuthenticationPrincipal Jwt jwt, @RequestParam UUID classId) {
        return items.list(TenantClaims.requireTenantId(jwt), classId);
    }

    @PostMapping("/items")
    @ResponseStatus(HttpStatus.CREATED)
    public FeeItemResponse createItem(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody FeeItemRequest request) {
        return items.create(TenantClaims.requireTenantId(jwt), request);
    }

    @DeleteMapping("/items/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteItem(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        items.delete(TenantClaims.requireTenantId(jwt), id);
    }

    @PostMapping("/classes/{classId}/generate")
    public GenerateResponse generate(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID classId) {
        return items.generate(TenantClaims.requireTenantId(jwt), classId);
    }

    @GetMapping("/students/{studentId}")
    public Ledger studentLedger(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID studentId) {
        return ledger.ledger(TenantClaims.requireTenantId(jwt), studentId);
    }

    @PostMapping("/charges/{chargeId}/payments")
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentView pay(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID chargeId,
                           @Valid @RequestBody PaymentRequest request) {
        return paymentService.record(TenantClaims.requireTenantId(jwt), chargeId, request, jwt.getSubject());
    }

    @PostMapping("/payments/{paymentId}/void")
    public PaymentView voidPayment(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID paymentId,
                                   @Valid @RequestBody VoidRequest request) {
        return paymentService.voidPayment(TenantClaims.requireTenantId(jwt), paymentId, request.reason(), jwt.getSubject());
    }

    @PostMapping("/reminders/run")
    public ReminderRunResponse runReminders(@AuthenticationPrincipal Jwt jwt) {
        return new ReminderRunResponse(reminders.remindTenant(TenantClaims.requireTenantId(jwt)));
    }
}