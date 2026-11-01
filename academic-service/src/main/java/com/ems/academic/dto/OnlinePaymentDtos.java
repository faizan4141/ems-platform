package com.ems.academic.dto;

import com.ems.academic.domain.AttemptStatus;
import com.ems.academic.domain.PaymentAttempt;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class OnlinePaymentDtos {

    private OnlinePaymentDtos() {}

    public record PayRequest(@NotEmpty @Size(max = 20) List<UUID> chargeIds, @NotNull UUID idempotencyKey) {}

    public record CheckoutResponse(UUID attemptId, String checkoutUrl) {}

    public record AttemptView(UUID id, AttemptStatus status, BigDecimal amount, String currency,
                              Instant createdAt, Instant completedAt, String failureReason, String reviewNote) {
        public static AttemptView from(PaymentAttempt a) {
            return new AttemptView(a.getId(), a.getStatus(), a.getAmount(), a.getCurrency(), a.getCreatedAt(),
                    a.getCompletedAt(), a.getFailureReason(), a.getReviewNote());
        }
    }

    public record AttemptRow(UUID id, UUID studentId, String studentName, String admissionNo, AttemptStatus status,
                             BigDecimal amount, String currency, Instant createdAt, String note) {}
}