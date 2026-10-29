package com.ems.academic.dto;

import java.util.List;

public record ImportResult(boolean dryRun, int totalRows, int validRows, int imported,
                           List<ImportRowError> errors) {}