package com.ems.academic.web;

import com.ems.academic.dto.TimetableDtos.*;
import com.ems.academic.security.Caller;
import com.ems.academic.security.TenantClaims;
import com.ems.academic.service.PeriodService;
import com.ems.academic.service.TimetableService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class TimetableController {

    private final PeriodService periods;
    private final TimetableService timetable;

    public TimetableController(PeriodService periods, TimetableService timetable) {
        this.periods = periods;
        this.timetable = timetable;
    }

    @GetMapping("/periods")
    public List<PeriodView> listPeriods(@AuthenticationPrincipal Jwt jwt) {
        return periods.list(TenantClaims.requireTenantId(jwt));
    }

    @PostMapping("/periods")
    @ResponseStatus(HttpStatus.CREATED)
    public PeriodView createPeriod(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody PeriodRequest request) {
        return periods.create(TenantClaims.requireTenantId(jwt), request);
    }

    @PutMapping("/periods/{id}")
    public PeriodView updatePeriod(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id, @Valid @RequestBody PeriodRequest request) {
        return periods.update(TenantClaims.requireTenantId(jwt), id, request);
    }

    @DeleteMapping("/periods/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePeriod(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        periods.delete(TenantClaims.requireTenantId(jwt), id);
    }

    @GetMapping("/timetable/sections/{sectionId}")
    public SectionTimetable sectionTimetable(@AuthenticationPrincipal Jwt jwt, Authentication authentication, @PathVariable UUID sectionId) {
        return timetable.sectionTimetable(Caller.of(jwt, authentication), sectionId);
    }

    @PutMapping("/timetable/sections/{sectionId}/slots/{day}/{periodId}")
    public EntryView setSlot(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID sectionId, @PathVariable int day,
                             @PathVariable UUID periodId, @Valid @RequestBody SlotRequest request) {
        return timetable.setSlot(TenantClaims.requireTenantId(jwt), sectionId, day, periodId, request);
    }

    @DeleteMapping("/timetable/sections/{sectionId}/slots/{day}/{periodId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clearSlot(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID sectionId, @PathVariable int day,
                          @PathVariable UUID periodId) {
        timetable.clearSlot(TenantClaims.requireTenantId(jwt), sectionId, day, periodId);
    }

    @GetMapping("/me/timetable")
    public TeacherTimetable myTimetable(@AuthenticationPrincipal Jwt jwt, Authentication authentication) {
        return timetable.myTimetable(Caller.of(jwt, authentication));
    }

    @GetMapping("/me/today")
    public TodayView myToday(@AuthenticationPrincipal Jwt jwt, Authentication authentication) {
        return timetable.myToday(Caller.of(jwt, authentication));
    }
}