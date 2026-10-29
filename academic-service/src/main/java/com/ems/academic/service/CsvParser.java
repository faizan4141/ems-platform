package com.ems.academic.service;

import com.ems.academic.exception.InvalidRequestException;

import java.util.ArrayList;
import java.util.List;

/** Minimal RFC 4180 parser: quoted cells, doubled quotes, commas and line breaks inside quotes. */
public final class CsvParser {

    private CsvParser() {}

    public static List<String[]> parse(String text) {
        List<String[]> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean quoted = false;
        int n = text.length();

        for (int i = 0; i < n; i++) {
            char c = text.charAt(i);
            if (quoted) {
                if (c == '"') {
                    if (i + 1 < n && text.charAt(i + 1) == '"') { cell.append('"'); i++; }
                    else quoted = false;
                } else {
                    cell.append(c);
                }
            } else if (c == '"' && cell.length() == 0) {
                quoted = true;
            } else if (c == ',') {
                row.add(cell.toString());
                cell.setLength(0);
            } else if (c == '\n') {
                row.add(cell.toString());
                cell.setLength(0);
                rows.add(row.toArray(new String[0]));
                row = new ArrayList<>();
            } else if (c != '\r') {
                cell.append(c);
            }
        }
        if (quoted) throw new InvalidRequestException("The file has a quote that is never closed");
        if (!cell.isEmpty() || !row.isEmpty()) {
            row.add(cell.toString());
            rows.add(row.toArray(new String[0]));
        }
        return rows;
    }
}