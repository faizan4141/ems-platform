package com.ems.academic.dto;

import java.util.UUID;

public record ChildSummary(UUID studentId, String name, String className, String sectionName) {}