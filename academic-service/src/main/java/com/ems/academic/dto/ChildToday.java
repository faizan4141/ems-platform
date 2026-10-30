package com.ems.academic.dto;

import com.ems.academic.domain.AttendanceStatus;
import java.util.List;
import java.util.UUID;

/** today is null until attendance has been taken for the child's section. */
public record ChildToday(UUID studentId, String name, String className, String sectionName,
                         AttendanceStatus today, Integer percentLast30, List<AttendanceDay> recent) {}