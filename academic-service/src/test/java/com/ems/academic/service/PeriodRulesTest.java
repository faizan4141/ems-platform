package com.ems.academic.service;

import com.ems.academic.exception.InvalidRequestException;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PeriodRulesTest {

    private static LocalTime t(String value) { return LocalTime.parse(value); }

    @Test
    void periodsThatOnlyTouchDoNotOverlap() {
        assertThat(PeriodRules.overlaps(t("08:00"), t("08:40"), t("08:40"), t("09:20"))).isFalse();
        assertThat(PeriodRules.overlaps(t("08:40"), t("09:20"), t("08:00"), t("08:40"))).isFalse();
    }

    @Test
    void partialContainedAndIdenticalPeriodsOverlap() {
        assertThat(PeriodRules.overlaps(t("08:00"), t("08:40"), t("08:30"), t("09:10"))).isTrue();
        assertThat(PeriodRules.overlaps(t("08:00"), t("09:00"), t("08:10"), t("08:20"))).isTrue();
        assertThat(PeriodRules.overlaps(t("08:00"), t("08:40"), t("08:00"), t("08:40"))).isTrue();
    }

    @Test
    void distantPeriodsDoNotOverlap() {
        assertThat(PeriodRules.overlaps(t("08:00"), t("08:40"), t("13:00"), t("13:40"))).isFalse();
    }

    @Test
    void aPeriodMustEndAfterItStarts() {
        PeriodRules.validateTimes(t("08:00"), t("08:01"));
        assertThatThrownBy(() -> PeriodRules.validateTimes(t("08:00"), t("08:00"))).isInstanceOf(InvalidRequestException.class);
        assertThatThrownBy(() -> PeriodRules.validateTimes(t("09:00"), t("08:00"))).isInstanceOf(InvalidRequestException.class);
    }
}