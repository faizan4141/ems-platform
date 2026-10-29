package com.ems.academic.dto;

public record OnboardingResponse(boolean profileDone, boolean academicYearDone, boolean classesDone,
                                 boolean studentsDone) {}