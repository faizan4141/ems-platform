package com.ems.academic.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record RosterResponse(UUID sectionId, String className, String sectionName, LocalDate date,
                             boolean taken, List<RosterEntry> students) {}