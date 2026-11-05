package com.ems.academic.service;

import com.ems.academic.domain.HomeworkKind;
import com.ems.academic.exception.InvalidRequestException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HomeworkRulesTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 30);

    @Test
    void acceptsWebLinksTrimsThemAndRemovesBlanksAndRepeats() {
        List<String> links = HomeworkRules.normalizeLinks(Arrays.asList(
                " https://example.com/a ", "", null, "http://school.example/b?x=1#y", "https://example.com/a"));
        assertThat(links).containsExactly("https://example.com/a", "http://school.example/b?x=1#y");
    }

    @Test
    void nothingMeansNoLinks() {
        assertThat(HomeworkRules.normalizeLinks(null)).isEmpty();
        assertThat(HomeworkRules.normalizeLinks(List.of())).isEmpty();
    }

    @Test
    void rejectsAnythingThatIsNotAPlainWebLink() {
        for (String bad : List.of("javascript:alert(1)", "ftp://example.com/file", "data:text/html,hi", "example.com",
                "https://", "https:///nohost", "https://exa mple.com", "https://user:pass@example.com/", "mailto:a@b.com")) {
            assertThatThrownBy(() -> HomeworkRules.normalizeLinks(List.of(bad)))
                    .as(bad).isInstanceOf(InvalidRequestException.class);
        }
    }

    @Test
    void limitsTheNumberAndLengthOfLinks() {
        List<String> six = IntStream.rangeClosed(1, 6).mapToObj(i -> "https://example.com/" + i).toList();
        assertThatThrownBy(() -> HomeworkRules.normalizeLinks(six)).hasMessageContaining("at most 5");
        assertThat(HomeworkRules.normalizeLinks(six.subList(0, 5))).hasSize(5);

        String tooLong = "https://example.com/" + "a".repeat(500);
        assertThatThrownBy(() -> HomeworkRules.normalizeLinks(List.of(tooLong))).isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void linksSurviveStorage() {
        List<String> links = List.of("https://example.com/a", "https://example.com/b");
        assertThat(HomeworkRules.splitLinks(HomeworkRules.joinLinks(links))).isEqualTo(links);
        assertThat(HomeworkRules.joinLinks(List.of())).isNull();
        assertThat(HomeworkRules.splitLinks(null)).isEmpty();
        assertThat(HomeworkRules.splitLinks("  ")).isEmpty();
    }

    @Test
    void homeworkNeedsASensibleDueDate() {
        assertThat(HomeworkRules.checkDue(HomeworkKind.HOMEWORK, TODAY, TODAY, true)).isEqualTo(TODAY);
        assertThat(HomeworkRules.checkDue(HomeworkKind.HOMEWORK, TODAY.plusDays(30), TODAY, true)).isNotNull();
        assertThatThrownBy(() -> HomeworkRules.checkDue(HomeworkKind.HOMEWORK, null, TODAY, true)).hasMessageContaining("due date");
        assertThatThrownBy(() -> HomeworkRules.checkDue(HomeworkKind.HOMEWORK, TODAY.minusDays(1), TODAY, true)).hasMessageContaining("past");
        assertThatThrownBy(() -> HomeworkRules.checkDue(HomeworkKind.HOMEWORK, TODAY.plusYears(2), TODAY, true)).hasMessageContaining("year");
    }

    @Test
    void anUnchangedPastDateIsAllowedWhenEditing() {
        LocalDate past = TODAY.minusDays(7);
        assertThat(HomeworkRules.checkDue(HomeworkKind.HOMEWORK, past, TODAY, false)).isEqualTo(past);
    }

    @Test
    void classNotesNeverHaveADueDate() {
        assertThat(HomeworkRules.checkDue(HomeworkKind.NOTE, null, TODAY, true)).isNull();
        assertThatThrownBy(() -> HomeworkRules.checkDue(HomeworkKind.NOTE, TODAY, TODAY, true)).isInstanceOf(InvalidRequestException.class);
    }
}