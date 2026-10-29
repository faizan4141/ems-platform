package com.ems.academic.service;

import com.ems.academic.domain.AcademicYear;
import com.ems.academic.dto.YearRequest;
import com.ems.academic.dto.YearResponse;
import com.ems.academic.exception.DuplicateResourceException;
import com.ems.academic.exception.InvalidRequestException;
import com.ems.academic.exception.ResourceNotFoundException;
import com.ems.academic.repository.AcademicYearRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class AcademicYearService {

    private final AcademicYearRepository years;
    private final TenantGuard guard;

    public AcademicYearService(AcademicYearRepository years, TenantGuard guard) {
        this.years = years;
        this.guard = guard;
    }

    @Transactional(readOnly = true)
    public List<YearResponse> list(UUID tenantId) {
        guard.requireActive(tenantId);
        return years.findByTenantIdOrderByStartDateDesc(tenantId).stream().map(YearResponse::from).toList();
    }

    @Transactional
    public YearResponse create(UUID tenantId, YearRequest req) {
        guard.requireActive(tenantId);
        if (!req.endDate().isAfter(req.startDate())) {
            throw new InvalidRequestException("The end date must be after the start date");
        }
        String name = req.name().trim();
        if (years.existsByTenantIdAndName(tenantId, name)) {
            throw new DuplicateResourceException("Academic year already exists: " + name);
        }
        // The first year is always current; later ones only if asked.
        boolean makeCurrent = !years.existsByTenantIdAndCurrentYearTrue(tenantId)
                || Boolean.TRUE.equals(req.makeCurrent());
        if (makeCurrent) {
            years.clearCurrent(tenantId);
        }
        return YearResponse.from(years.save(
                new AcademicYear(tenantId, name, req.startDate(), req.endDate(), makeCurrent)));
    }

    @Transactional
    public YearResponse setCurrent(UUID tenantId, UUID yearId) {
        guard.requireActive(tenantId);
        years.findByIdAndTenantId(yearId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Academic year not found"));
        years.clearCurrent(tenantId);   // also detaches loaded entities, so load again
        AcademicYear year = years.findByIdAndTenantId(yearId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Academic year not found"));
        year.setCurrentYear(true);
        return YearResponse.from(year);
    }
}