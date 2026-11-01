package com.ems.academic.service;

import com.ems.academic.domain.FeeCharge;
import com.ems.academic.domain.Payment;
import com.ems.academic.domain.PaymentMethod;
import com.ems.academic.dto.FeeDtos.PaymentRequest;
import com.ems.academic.dto.FeeDtos.PaymentView;
import com.ems.academic.exception.DuplicateResourceException;
import com.ems.academic.exception.InvalidRequestException;
import com.ems.academic.repository.FeeChargeRepository;
import com.ems.academic.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FeePaymentServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-30T05:00:00Z");
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 30);

    @Mock FeeChargeRepository charges;
    @Mock PaymentRepository payments;
    @Mock ReceiptNumbers receipts;
    @Mock TenantGuard guard;
    private FeePaymentService service;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID chargeId = UUID.randomUUID();
    private final UUID key = UUID.randomUUID();
    private final FeeCharge charge = new FeeCharge(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
            "Tuition", new BigDecimal("12000.00"), TODAY.plusDays(10));

    @BeforeEach
    void setUp() {
        service = new FeePaymentService(charges, payments, receipts, guard, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private PaymentRequest request(String amount, PaymentMethod method, String reference, LocalDate paidOn) {
        return new PaymentRequest(new BigDecimal(amount), method, reference, paidOn, key);
    }

    private Payment existing(UUID forCharge) {
        return new Payment(tenantId, forCharge, new BigDecimal("5000.00"), PaymentMethod.CASH, null, TODAY,
                "RCP-000001", "admin", NOW, key);
    }

    @Test
    void recordsAPartialPaymentAndIssuesAReceipt() {
        when(charges.lockById(chargeId, tenantId)).thenReturn(Optional.of(charge));
        when(payments.findByTenantIdAndIdempotencyKey(tenantId, key)).thenReturn(Optional.empty());
        when(payments.sumForCharge(chargeId)).thenReturn(null);
        when(receipts.next(tenantId)).thenReturn("RCP-000001");
        when(payments.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));

        PaymentView view = service.record(tenantId, chargeId, request("5000", PaymentMethod.CASH, null, null), "admin-1");

        assertThat(view.receiptNo()).isEqualTo("RCP-000001");
        assertThat(view.amount()).isEqualByComparingTo("5000.00");
        assertThat(view.paidOn()).isEqualTo(TODAY);
        assertThat(view.chargeName()).isEqualTo("Tuition");
    }

    @Test
    void rejectsMoreThanTheRemainingBalance() {
        when(charges.lockById(chargeId, tenantId)).thenReturn(Optional.of(charge));
        when(payments.findByTenantIdAndIdempotencyKey(tenantId, key)).thenReturn(Optional.empty());
        when(payments.sumForCharge(chargeId)).thenReturn(new BigDecimal("5000.00"));

        assertThatThrownBy(() -> service.record(tenantId, chargeId, request("7000.01", PaymentMethod.CASH, null, null), "a"))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("7000.00");
        verify(payments, never()).save(any());
        verifyNoInteractions(receipts);
    }

    @Test
    void aRetriedSubmissionReturnsTheOriginalReceipt() {
        when(charges.lockById(chargeId, tenantId)).thenReturn(Optional.of(charge));
        when(payments.findByTenantIdAndIdempotencyKey(tenantId, key)).thenReturn(Optional.of(existing(chargeId)));

        PaymentView view = service.record(tenantId, chargeId, request("5000", PaymentMethod.CASH, null, null), "a");

        assertThat(view.receiptNo()).isEqualTo("RCP-000001");
        verify(payments, never()).save(any());
        verifyNoInteractions(receipts);
    }

    @Test
    void theSameKeyCannotBeReusedForAnotherFee() {
        when(charges.lockById(chargeId, tenantId)).thenReturn(Optional.of(charge));
        when(payments.findByTenantIdAndIdempotencyKey(tenantId, key)).thenReturn(Optional.of(existing(UUID.randomUUID())));

        assertThatThrownBy(() -> service.record(tenantId, chargeId, request("100", PaymentMethod.CASH, null, null), "a"))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void chequeUpiAndBankTransfersNeedAReference() {
        assertThatThrownBy(() -> service.record(tenantId, chargeId, request("100", PaymentMethod.UPI, " ", null), "a"))
                .isInstanceOf(InvalidRequestException.class).hasMessageContaining("reference");
        assertThatThrownBy(() -> service.record(tenantId, chargeId, request("100", PaymentMethod.BANK_TRANSFER, null, null), "a"))
                .isInstanceOf(InvalidRequestException.class);
        verifyNoInteractions(charges, payments);
    }

    @Test
    void rejectsAFuturePaymentDate() {
        assertThatThrownBy(() -> service.record(tenantId, chargeId,
                request("100", PaymentMethod.CASH, null, TODAY.plusDays(1)), "a"))
                .isInstanceOf(InvalidRequestException.class);
        verifyNoInteractions(charges, payments);
    }

    @Test
    void voidingMarksThePaymentAndKeepsItsReceipt() {
        Payment payment = existing(chargeId);
        UUID paymentId = UUID.randomUUID();
        when(payments.findByIdAndTenantId(paymentId, tenantId)).thenReturn(Optional.of(payment));
        when(charges.lockById(chargeId, tenantId)).thenReturn(Optional.of(charge));

        PaymentView view = service.voidPayment(tenantId, paymentId, "  Entered twice ", "admin-1");

        assertThat(view.voided()).isTrue();
        assertThat(view.voidReason()).isEqualTo("Entered twice");
        assertThat(view.receiptNo()).isEqualTo("RCP-000001");
    }

    @Test
    void aPaymentCanOnlyBeVoidedOnce() {
        Payment payment = existing(chargeId);
        payment.voidIt("first", "admin", NOW);
        UUID paymentId = UUID.randomUUID();
        when(payments.findByIdAndTenantId(paymentId, tenantId)).thenReturn(Optional.of(payment));
        when(charges.lockById(chargeId, tenantId)).thenReturn(Optional.of(charge));

        assertThatThrownBy(() -> service.voidPayment(tenantId, paymentId, "again", "admin"))
                .isInstanceOf(DuplicateResourceException.class);
    }
}