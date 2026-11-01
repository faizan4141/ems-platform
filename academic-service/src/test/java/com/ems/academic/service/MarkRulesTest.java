package com.ems.academic.service;

import com.ems.academic.exception.InvalidRequestException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MarkRulesTest {

    private static final BigDecimal MAX = new BigDecimal("100.00");

    @Test
    void storesTwoDecimals() {
        assertThat(MarkRules.normalize(new BigDecimal("85"), false, MAX).toPlainString()).isEqualTo("85.00");
        assertThat(MarkRules.normalize(new BigDecimal("85.5"), false, MAX).toPlainString()).isEqualTo("85.50");
        assertThat(MarkRules.normalize(new BigDecimal("100"), false, MAX)).isNotNull();
        assertThat(MarkRules.normalize(BigDecimal.ZERO, false, MAX).toPlainString()).isEqualTo("0.00");
    }

    @Test
    void rejectsOutOfRangeAndOverPreciseMarks() {
        assertThatThrownBy(() -> MarkRules.normalize(new BigDecimal("100.01"), false, MAX)).isInstanceOf(InvalidRequestException.class);
        assertThatThrownBy(() -> MarkRules.normalize(new BigDecimal("-1"), false, MAX)).isInstanceOf(InvalidRequestException.class);
        assertThatThrownBy(() -> MarkRules.normalize(new BigDecimal("85.555"), false, MAX)).isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void trailingZerosBeyondTwoDecimalsAreFine() {
        assertThat(MarkRules.normalize(new BigDecimal("85.500"), false, MAX).toPlainString()).isEqualTo("85.50");
    }

    @Test
    void absentAndClearedEntriesStoreNoMarks() {
        assertThat(MarkRules.normalize(null, true, MAX)).isNull();
        assertThat(MarkRules.normalize(null, false, MAX)).isNull();
        assertThatThrownBy(() -> MarkRules.normalize(new BigDecimal("5"), true, MAX)).isInstanceOf(InvalidRequestException.class);
    }
}