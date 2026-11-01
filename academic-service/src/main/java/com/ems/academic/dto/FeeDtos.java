package com.ems.academic.dto;

import com.ems.academic.domain.FeeItem;
import com.ems.academic.domain.FeeStatus;
import com.ems.academic.domain.Payment;
import com.ems.academic.domain.PaymentMethod;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class FeeDtos {

    private FeeDtos() {}

    public record FeeItemRequest(
            @NotNull UUID classId,
            @NotBlank @Size(max = 100) String name,
            @NotNull @DecimalMin("0.01") @Digits(integer = 9, fraction = 2) BigDecimal amount,
            @NotNull LocalDate dueDate) {}

    public record FeeItemResponse(UUID id, UUID classId, String name, BigDecimal amount, LocalDate dueDate,
                                  int chargesGenerated) {
        public static FeeItemResponse from(FeeItem i, int generated) {
            return new FeeItemResponse(i.getId(), i.getClassId(), i.getName(), i.getAmount(), i.getDueDate(), generated);
        }
    }

    public record GenerateResponse(int created, int students, int items) {}

    public record PaymentRequest(
            @NotNull @DecimalMin("0.01") @Digits(integer = 9, fraction = 2) BigDecimal amount,
            @NotNull PaymentMethod method,
            @Size(max = 100) String reference,
            LocalDate paidOn,
            @NotNull UUID idempotencyKey) {}

    public record VoidRequest(@NotBlank @Size(max = 200) String reason) {}

    public record PaymentView(UUID id, UUID chargeId, String chargeName, BigDecimal amount, PaymentMethod method,
                              String reference, LocalDate paidOn, String receiptNo, boolean voided, String voidReason) {
        public static PaymentView from(Payment p, String chargeName) {
            return new PaymentView(p.getId(), p.getChargeId(), chargeName, p.getAmount(), p.getMethod(),
                    p.getReference(), p.getPaidOn(), p.getReceiptNo(), p.isVoided(), p.getVoidReason());
        }
    }

    public record ChargeView(UUID id, String name, BigDecimal amount, BigDecimal paid, BigDecimal balance,
                             LocalDate dueDate, FeeStatus status) {}

    public record Ledger(String currency, UUID studentId, String studentName, String admissionNo, String className,
                         String sectionName, String schoolName, BigDecimal billed, BigDecimal paid, BigDecimal balance,
                         List<ChargeView> charges, List<PaymentView> payments) {}

    public record FeeSummary(String currency, BigDecimal billed, BigDecimal collected, BigDecimal outstanding,
                             BigDecimal overdue, int studentsWithOverdue) {}

    public record DueRow(UUID studentId, String studentName, String admissionNo, String className, String sectionName,
                         BigDecimal balance, BigDecimal overdue, LocalDate earliestDue) {}

    public record DuesResponse(String currency, List<DueRow> rows) {}

    public record ReminderRunResponse(int reminded) {}
}