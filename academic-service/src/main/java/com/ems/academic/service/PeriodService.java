package com.ems.academic.service;

import com.ems.academic.domain.SchoolPeriod;
import com.ems.academic.dto.TimetableDtos.PeriodRequest;
import com.ems.academic.dto.TimetableDtos.PeriodView;
import com.ems.academic.exception.DuplicateResourceException;
import com.ems.academic.exception.InvalidRequestException;
import com.ems.academic.exception.ResourceNotFoundException;
import com.ems.academic.repository.PeriodRepository;
import com.ems.academic.repository.TimetableEntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Service
public class PeriodService {

    private final PeriodRepository periods;
    private final TimetableEntryRepository entries;
    private final TenantGuard guard;

    public PeriodService(PeriodRepository periods, TimetableEntryRepository entries, TenantGuard guard) {
        this.periods = periods;
        this.entries = entries;
        this.guard = guard;
    }

    @Transactional(readOnly = true)
    public List<PeriodView> list(UUID tenantId) {
        guard.requireActive(tenantId);
        return periods.findByTenantIdOrderByNumberAsc(tenantId).stream().map(PeriodView::from).toList();
    }

    @Transactional
    public PeriodView create(UUID tenantId, PeriodRequest req) {
        guard.requireActive(tenantId);
        PeriodRules.validateTimes(req.startTime(), req.endTime());
        if (periods.existsByTenantIdAndNumber(tenantId, req.number())) {
            throw new DuplicateResourceException("There is already a period number " + req.number());
        }
        checkOverlap(tenantId, null, req.startTime(), req.endTime());
        return PeriodView.from(periods.save(new SchoolPeriod(tenantId, req.number(), req.name().trim(),
                req.startTime(), req.endTime(), Boolean.TRUE.equals(req.breakPeriod()))));
    }

    @Transactional
    public PeriodView update(UUID tenantId, UUID id, PeriodRequest req) {
        guard.requireActive(tenantId);
        SchoolPeriod period = require(tenantId, id);
        PeriodRules.validateTimes(req.startTime(), req.endTime());
        boolean makeBreak = Boolean.TRUE.equals(req.breakPeriod());
        if (req.number() != period.getNumber() && periods.existsByTenantIdAndNumber(tenantId, req.number())) {
            throw new DuplicateResourceException("There is already a period number " + req.number());
        }
        if (makeBreak && !period.isBreakPeriod() && entries.existsByPeriodId(id)) {
            throw new InvalidRequestException("Lessons are scheduled in this period, so it cannot become a break");
        }
        checkOverlap(tenantId, id, req.startTime(), req.endTime());
        period.update(req.number(), req.name().trim(), req.startTime(), req.endTime(), makeBreak);
        return PeriodView.from(period);
    }

    @Transactional
    public void delete(UUID tenantId, UUID id) {
        guard.requireActive(tenantId);
        SchoolPeriod period = require(tenantId, id);
        if (entries.existsByPeriodId(id)) {
            throw new DuplicateResourceException("This period is used by the timetable. Clear its lessons first.");
        }
        periods.delete(period);
    }

    private SchoolPeriod require(UUID tenantId, UUID id) {
        return periods.findByIdAndTenantId(id, tenantId).orElseThrow(() -> new ResourceNotFoundException("Period not found"));
    }

    private void checkOverlap(UUID tenantId, UUID selfId, LocalTime start, LocalTime end) {
        for (SchoolPeriod other : periods.findByTenantIdOrderByNumberAsc(tenantId)) {
            if (other.getId().equals(selfId)) continue;
            if (PeriodRules.overlaps(start, end, other.getStartTime(), other.getEndTime())) {
                throw new InvalidRequestException("These times overlap " + other.getName()
                        + " (" + other.getStartTime() + "–" + other.getEndTime() + ")");
            }
        }
    }
}