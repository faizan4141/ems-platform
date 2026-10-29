package com.ems.academic.service;

import com.ems.academic.domain.Gender;
import com.ems.academic.domain.Relationship;
import com.ems.academic.exception.InvalidRequestException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StudentFieldsTest {

    @Test
    void parsesThreeDateFormats() {
        LocalDate expected = LocalDate.of(2014, 5, 10);
        assertThat(StudentFields.parseDate("2014-05-10", "dob")).isEqualTo(expected);
        assertThat(StudentFields.parseDate("10/05/2014", "dob")).isEqualTo(expected);
        assertThat(StudentFields.parseDate("10-05-2014", "dob")).isEqualTo(expected);
    }

    @Test
    void rejectsImpossibleOrUnknownDates() {
        assertThatThrownBy(() -> StudentFields.parseDate("31/02/2014", "dob")).isInstanceOf(InvalidRequestException.class);
        assertThatThrownBy(() -> StudentFields.parseDate("May 10", "dob")).isInstanceOf(InvalidRequestException.class);
        assertThatThrownBy(() -> StudentFields.parseDate("", "dob")).isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void dateOfBirthMustBePastAndPlausible() {
        LocalDate today = LocalDate.of(2026, 10, 29);
        assertThat(StudentFields.checkDob(LocalDate.of(2014, 5, 10), today)).isNotNull();
        assertThatThrownBy(() -> StudentFields.checkDob(today, today)).isInstanceOf(InvalidRequestException.class);
        assertThatThrownBy(() -> StudentFields.checkDob(LocalDate.of(1980, 1, 1), today)).isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void normalizesPhoneNumbers() {
        assertThat(StudentFields.normalizePhone("+91 98765-43210")).isEqualTo("+919876543210");
        assertThat(StudentFields.normalizePhone("(011) 2345 6789")).isEqualTo("01123456789");
        assertThatThrownBy(() -> StudentFields.normalizePhone("12345")).isInstanceOf(InvalidRequestException.class);
        assertThatThrownBy(() -> StudentFields.normalizePhone("abc")).isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void mapsGenderAndRelationship() {
        assertThat(StudentFields.parseGender("m")).isEqualTo(Gender.MALE);
        assertThat(StudentFields.parseGender("")).isNull();
        assertThatThrownBy(() -> StudentFields.parseGender("x")).isInstanceOf(InvalidRequestException.class);
        assertThat(StudentFields.parseRelationship("mother")).isEqualTo(Relationship.MOTHER);
        assertThat(StudentFields.parseRelationship("")).isEqualTo(Relationship.GUARDIAN);
    }

    @Test
    void emailIsOptionalButMustBeValidWhenGiven() {
        assertThat(StudentFields.cleanEmail("")).isNull();
        assertThat(StudentFields.cleanEmail(" a@b.com ")).isEqualTo("a@b.com");
        assertThatThrownBy(() -> StudentFields.cleanEmail("not-an-email")).isInstanceOf(InvalidRequestException.class);
    }
}