package com.ems.academic.service;

import com.ems.academic.domain.FeeStatus;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class FeeMath {

    public static final BigDecimal ZERO = new BigDecimal("0.00");

    private FeeMath() {}

    public static BigDecimal scale(BigDecimal value) { return value.setScale(2, RoundingMode.HALF_UP); }

    public static BigDecimal balance(BigDecimal amount, BigDecimal paid) { return amount.subtract(paid).max(ZERO); }

    /** Overdue wins over part-paid: a late balance is what the school needs to see. */
    public static FeeStatus status(BigDecimal amount, BigDecimal paid, LocalDate dueDate, LocalDate today) {
        if (paid.compareTo(amount) >= 0) return FeeStatus.PAID;
        if (dueDate.isBefore(today)) return FeeStatus.OVERDUE;
        return paid.signum() > 0 ? FeeStatus.PARTIAL : FeeStatus.DUE;
    }

    public static Map<UUID, BigDecimal> toPaidMap(List<Object[]> rows) {
        Map<UUID, BigDecimal> map = new HashMap<>();
        for (Object[] row : rows) {
            map.put((UUID) row[0], (BigDecimal) row[1]);
        }
        return map;
    }

    public static BigDecimal paidOf(Map<UUID, BigDecimal> paid, UUID chargeId) {
        return paid.getOrDefault(chargeId, ZERO);
    }
}