package com.ems.academic.service;

import com.ems.academic.exception.InvalidRequestException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CsvParserTest {

    @Test
    void parsesPlainRowsWithWindowsLineEndings() {
        List<String[]> rows = CsvParser.parse("a,b,c\r\n1,2,3\r\n");
        assertThat(rows).hasSize(2);
        assertThat(rows.get(1)).containsExactly("1", "2", "3");
    }

    @Test
    void handlesQuotedCommasQuotesAndLineBreaks() {
        List<String[]> rows = CsvParser.parse("name,note\n\"Sharma, Aarav\",\"said \"\"hi\"\"\nthen left\"\n");
        assertThat(rows.get(1)).containsExactly("Sharma, Aarav", "said \"hi\"\nthen left");
    }

    @Test
    void keepsEmptyCellsAndLastRowWithoutNewline() {
        List<String[]> rows = CsvParser.parse("a,b,c\n1,,3");
        assertThat(rows.get(1)).containsExactly("1", "", "3");
    }

    @Test
    void rejectsAnUnclosedQuote() {
        assertThatThrownBy(() -> CsvParser.parse("a,b\n\"oops,2\n"))
                .isInstanceOf(InvalidRequestException.class);
    }
}