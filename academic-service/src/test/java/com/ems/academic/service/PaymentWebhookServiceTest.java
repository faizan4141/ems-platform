package com.ems.academic.service;

import com.ems.academic.domain.*;
import com.ems.academic.exception.InvalidSignatureException;
import com.ems.academic.exception.ResourceNotFoundException;
import com.ems.academic.payment.PaymentGateway;
import com.ems.academic.payment.WebhookEvent;
import com.ems.academic.repository.FeeChargeRepository;
import com.ems.academic.repository.PaymentAttemptItemRepository;
import com.ems.academic.repository.PaymentAttemptRepository;
import com.ems.academic.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PaymentWebhookServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-30T05:00:00Z");

    @Mock PaymentAttemptRepository attempts;
    @Mock PaymentAttemptItemRepository items;
    @Mock FeeChargeRepository charges;
    @Mock PaymentRepository payments;
    @Mock ReceiptNumbers receipts;
    @Mock PaymentGateway gateway;
    private PaymentWebhookService service;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID chargeA = UUID.randomUUID();
    private final UUID chargeB = UUID.randomUUID();
    private PaymentAttempt attempt;

    @BeforeEach
    void setUp() {
        service = new PaymentWebhookService(attempts, items, charges, payments, receipts, gateway,
                Clock.fixed(NOW, ZoneOffset.UTC));
        attempt = new PaymentAttempt(tenantId, UUID.randomUUID(), "parent-1", new BigDecimal("150.00"), "INR",
                "simulated", UUID.randomUUID(), NOW);
        attempt.attachCheckout("sim_1", "http://x");

        when(gateway.verifySignature(any(), any())).thenReturn(true);
        when(attempts.lockByProviderRef("sim_1")).thenReturn(Optional.of(attempt));
        when(items.findByAttemptId(any())).thenReturn(List.of(
                new PaymentAttemptItem(attempt.getId(), chargeA, new BigDecimal("100.00")),
                new PaymentAttemptItem(attempt.getId(), chargeB, new BigDecimal("50.00"))));
        for (UUID id : List.of(chargeA, chargeB)) {
            FeeCharge charge = new FeeCharge(tenantId, UUID.randomUUID(), UUID.randomUUID(), "Fee",
                    id.equals(chargeA) ? new BigDecimal("100.00") : new BigDecimal("50.00"), LocalDate.of(2026, 11, 5));
            when(charges.lockById(id, tenantId)).thenReturn(Optional.of(charge));
            when(charges.findById(id)).thenReturn(Optional.of(charge));
        }
        when(payments.findByTenantIdAndIdempotencyKey(any(), any())).thenReturn(Optional.empty());
        when(payments.sumForCharge(any())).thenReturn(null);
        when(receipts.next(tenantId)).thenReturn("RCP-000001", "RCP-000002");
        when(payments.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private WebhookEvent success(String amount) {
        return new WebhookEvent("evt-1", WebhookEvent.SUCCEEDED, "sim_1", "pay_1", amount, "INR", "");
    }

    @Test
    void aGenuineSuccessCreatesOneReceiptPerFeeAndMarksTheAttemptSucceeded() {
        service.handle(success("150.00"), "sig");

        ArgumentCaptor<Payment> saved = ArgumentCaptor.forClass(Payment.class);
        verify(payments, times(2)).save(saved.capture());
        assertThat(saved.getAllValues()).extracting(Payment::getMethod).containsOnly(PaymentMethod.ONLINE);
        assertThat(saved.getAllValues()).extracting(Payment::getAmount)
                .containsExactlyInAnyOrder(new BigDecimal("100.00"), new BigDecimal("50.00"));
        assertThat(saved.getAllValues()).extracting(Payment::getReference).containsOnly("pay_1");
        assertThat(attempt.getStatus()).isEqualTo(AttemptStatus.SUCCEEDED);
    }

    @Test
    void aRepeatedSuccessDeliveryChangesNothing() {
        attempt.succeed(NOW);

        service.handle(success("150.00"), "sig");

        verify(payments, never()).save(any());
        verifyNoInteractions(charges);
    }

    @Test
    void aReceiptAlreadyCreatedForAFeeIsNotDuplicated() {
        when(payments.findByTenantIdAndIdempotencyKey(any(), any())).thenReturn(
                Optional.of(mock(Payment.class)));

        service.handle(success("150.00"), "sig");

        verify(payments, never()).save(any());
        assertThat(attempt.getStatus()).isEqualTo(AttemptStatus.SUCCEEDED);
    }

    @Test
    void aWrongAmountIsSentForReviewAndNothingIsApplied() {
        service.handle(success("99.00"), "sig");

        assertThat(attempt.getStatus()).isEqualTo(AttemptStatus.REVIEW);
        assertThat(attempt.getReviewNote()).contains("99.00").contains("150.00");
        verify(payments, never()).save(any());
    }

    @Test
    void feesPaidInTheMeantimeGoToReviewAndOnlyTheOpenOneIsApplied() {
        // fee A was fully paid in cash while the parent was at the checkout page
        when(payments.sumForCharge(chargeA)).thenReturn(new BigDecimal("100.00"));

        service.handle(success("150.00"), "sig");

        verify(payments, times(1)).save(any(Payment.class));
        assertThat(attempt.getStatus()).isEqualTo(AttemptStatus.REVIEW);
        assertThat(attempt.getReviewNote()).contains("100.00");
    }

    @Test
    void aFailureMarksAPendingAttemptFailed() {
        service.handle(new WebhookEvent("evt-2", WebhookEvent.FAILED, "sim_1", "", "150.00", "INR", "Card declined"), "sig");

        assertThat(attempt.getStatus()).isEqualTo(AttemptStatus.FAILED);
        assertThat(attempt.getFailureReason()).isEqualTo("Card declined");
        verify(payments, never()).save(any());
    }

    @Test
    void aLateFailureCannotUndoASuccess() {
        attempt.succeed(NOW);

        service.handle(new WebhookEvent("evt-3", WebhookEvent.FAILED, "sim_1", "", "150.00", "INR", "late"), "sig");

        assertThat(attempt.getStatus()).isEqualTo(AttemptStatus.SUCCEEDED);
    }

    @Test
    void anInvalidSignatureIsRejectedBeforeAnythingIsLookedUp() {
        when(gateway.verifySignature(any(), any())).thenReturn(false);

        assertThatThrownBy(() -> service.handle(success("150.00"), "forged"))
                .isInstanceOf(InvalidSignatureException.class);
        verifyNoInteractions(attempts, payments);
    }

    @Test
    void anUnknownReferenceIsNotFound() {
        WebhookEvent unknown = new WebhookEvent("evt-4", WebhookEvent.SUCCEEDED, "sim_nope", "pay_9", "1.00", "INR", "");
        when(attempts.lockByProviderRef("sim_nope")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.handle(unknown, "sig")).isInstanceOf(ResourceNotFoundException.class);
    }
}