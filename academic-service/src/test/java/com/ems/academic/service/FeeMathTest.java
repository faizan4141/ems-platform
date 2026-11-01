package com.ems.academic.service;

import com.ems.academic.domain.FeeStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class FeeMathTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 30);
    private static final BigDecimal AMOUNT = new BigDecimal("12000.00");

    private static BigDecimal money(String v) { return new BigDecimal(v); }

    @Test
    void statusFollowsPaymentsAndDueDate() {
        assertThat(FeeMath.status(AMOUNT, money("0.00"), TODAY.plusDays(5), TODAY)).isEqualTo(FeeStatus.DUE);
        assertThat(FeeMath.status(AMOUNT, money("5000.00"), TODAY.plusDays(5), TODAY)).isEqualTo(FeeStatus.PARTIAL);
        assertThat(FeeMath.status(AMOUNT, money("12000.00"), TODAY.minusDays(5), TODAY)).isEqualTo(FeeStatus.PAID);
        assertThat(FeeMath.status(AMOUNT, money("5000.00"), TODAY.minusDays(1), TODAY)).isEqualTo(FeeStatus.OVERDUE);
    }

    @Test
    void aFeeDueTodayIsNotYetOverdue() {
        assertThat(FeeMath.status(AMOUNT, money("0.00"), TODAY, TODAY)).isEqualTo(FeeStatus.DUE);
    }

    @Test
    void balanceNeverGoesNegative() {
        assertThat(FeeMath.balance(AMOUNT, money("12000.00"))).isEqualByComparingTo("0");
        assertThat(FeeMath.balance(AMOUNT, money("12500.00"))).isEqualByComparingTo("0");
        assertThat(FeeMath.balance(AMOUNT, money("4999.99"))).isEqualByComparingTo("7000.01");
    }

    @Test
    void scalesToTwoDecimals() {
        assertThat(FeeMath.scale(new BigDecimal("10")).toPlainString()).isEqualTo("10.00");
    }

    @Test
    void buildsAPaidMapFromQueryRows() {
        UUID id = UUID.randomUUID();
        Map<UUID, BigDecimal> map = FeeMath.toPaidMap(List.<Object[]>of(new Object[]{id, money("5.50")}));
        assertThat(FeeMath.paidOf(map, id)).isEqualByComparingTo("5.50");
        assertThat(FeeMath.paidOf(map, UUID.randomUUID())).isEqualByComparingTo("0");
    }

    @Test
    void formatsReceiptNumbers() {
        assertThat(ReceiptNumbers.format(42)).isEqualTo("RCP-000042");
    }
}