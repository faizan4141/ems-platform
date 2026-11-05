package com.ems.academic.service;

import com.ems.academic.domain.HomeworkKind;
import com.ems.academic.exception.InvalidRequestException;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

public final class HomeworkRules {

    public static final int MAX_LINKS = 5;
    public static final int MAX_LINK_LENGTH = 500;
    static final int MAX_DAYS_AHEAD = 366;

    private HomeworkRules() {}

    /** Blank entries are ignored and repeats removed. Anything that is not a plain web link is rejected. */
    public static List<String> normalizeLinks(List<String> raw) {
        if (raw == null) return List.of();
        LinkedHashSet<String> links = new LinkedHashSet<>();
        for (String candidate : raw) {
            if (candidate == null || candidate.isBlank()) continue;
            String link = candidate.trim();
            if (link.length() > MAX_LINK_LENGTH) {
                throw new InvalidRequestException("A link is longer than " + MAX_LINK_LENGTH + " characters");
            }
            if (!isWebLink(link)) {
                throw new InvalidRequestException("\"" + shorten(link) + "\" is not a valid web link. Links must start with http:// or https://");
            }
            links.add(link);
        }
        if (links.size() > MAX_LINKS) {
            throw new InvalidRequestException("Add at most " + MAX_LINKS + " links");
        }
        return List.copyOf(links);
    }

    static boolean isWebLink(String link) {
        try {
            URI uri = new URI(link);
            String scheme = uri.getScheme();
            return scheme != null && (scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))
                    && uri.getHost() != null && !uri.getHost().isBlank()
                    && uri.getUserInfo() == null;   // no embedded passwords
        } catch (URISyntaxException e) {
            return false;
        }
    }

    public static String joinLinks(List<String> links) {
        return links.isEmpty() ? null : String.join("\n", links);
    }

    public static List<String> splitLinks(String stored) {
        if (stored == null || stored.isBlank()) return List.of();
        return Arrays.stream(stored.split("\n")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    /** Returns the due date to store: required for homework, forbidden for notes. */
    public static LocalDate checkDue(HomeworkKind kind, LocalDate due, LocalDate today, boolean mustNotBePast) {
        if (kind == HomeworkKind.NOTE) {
            if (due != null) throw new InvalidRequestException("Class notes do not have a due date");
            return null;
        }
        if (due == null) throw new InvalidRequestException("Homework needs a due date");
        if (mustNotBePast && due.isBefore(today)) throw new InvalidRequestException("The due date cannot be in the past");
        if (due.isAfter(today.plusDays(MAX_DAYS_AHEAD))) throw new InvalidRequestException("The due date is more than a year away");
        return due;
    }

    private static String shorten(String value) { return value.length() > 60 ? value.substring(0, 60) + "…" : value; }
}