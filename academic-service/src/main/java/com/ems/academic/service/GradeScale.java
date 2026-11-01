package com.ems.academic.service;

import com.ems.academic.exception.InvalidRequestException;

import java.math.BigDecimal;
import java.util.*;

public final class GradeScale {

    public record Band(BigDecimal min, String grade, String label) {}

    private static Band band(String min, String grade, String label) {
        return new Band(new BigDecimal(min).setScale(2), grade, label);
    }

    /** Used until a school saves its own scale. */
    public static final List<Band> DEFAULT = List.of(
            band("90", "A+", "Outstanding"), band("80", "A", "Excellent"), band("70", "B+", "Very good"),
            band("60", "B", "Good"), band("50", "C", "Satisfactory"), band("40", "D", "Pass"),
            band("0", "F", "Needs improvement"));

    private GradeScale() {}

    public static String gradeFor(BigDecimal percent, List<Band> bands) {
        return bands.stream().sorted(Comparator.comparing(Band::min).reversed())
                .filter(b -> percent.compareTo(b.min()) >= 0)
                .map(Band::grade).findFirst().orElse(null);
    }

    public static void validate(List<Band> bands) {
        if (bands == null || bands.isEmpty() || bands.size() > 12) {
            throw new InvalidRequestException("A grading scale needs between 1 and 12 grades");
        }
        Set<BigDecimal> mins = new HashSet<>();
        Set<String> names = new HashSet<>();
        boolean hasZero = false;
        for (Band b : bands) {
            if (b.grade() == null || b.grade().isBlank()) {
                throw new InvalidRequestException("Every grade needs a name");
            }
            if (!mins.add(b.min().stripTrailingZeros())) {
                throw new InvalidRequestException("Two grades start at the same percentage");
            }
            if (!names.add(b.grade().trim().toLowerCase(Locale.ROOT))) {
                throw new InvalidRequestException("Grade names must be different from each other");
            }
            if (b.min().signum() == 0) hasZero = true;
        }
        if (!hasZero) {
            throw new InvalidRequestException("The lowest grade must start at 0%, so every score gets a grade");
        }
    }
}