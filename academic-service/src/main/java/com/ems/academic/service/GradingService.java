package com.ems.academic.service;

import com.ems.academic.domain.GradeBand;
import com.ems.academic.dto.ExamDtos.BandView;
import com.ems.academic.dto.ExamDtos.ScaleRequest;
import com.ems.academic.repository.GradeBandRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class GradingService {

    private final GradeBandRepository bands;
    private final TenantGuard guard;

    public GradingService(GradeBandRepository bands, TenantGuard guard) {
        this.bands = bands;
        this.guard = guard;
    }

    @Transactional(readOnly = true)
    public List<BandView> scale(UUID tenantId) {
        guard.requireActive(tenantId);
        return bandsFor(tenantId).stream().map(b -> new BandView(b.min(), b.grade(), b.label())).toList();
    }

    /** The school's own scale, or the default one until it saves its own. */
    @Transactional(readOnly = true)
    public List<GradeScale.Band> bandsFor(UUID tenantId) {
        List<GradeBand> saved = bands.findByTenantIdOrderByMinPercentDesc(tenantId);
        if (saved.isEmpty()) return GradeScale.DEFAULT;
        return saved.stream().map(b -> new GradeScale.Band(b.getMinPercent(), b.getGrade(), b.getLabel())).toList();
    }

    @Transactional
    public List<BandView> save(UUID tenantId, ScaleRequest req) {
        guard.requireActive(tenantId);
        List<GradeScale.Band> parsed = req.bands().stream()
                .map(b -> new GradeScale.Band(b.minPercent().setScale(2), b.grade().trim(),
                        b.label() == null || b.label().isBlank() ? null : b.label().trim()))
                .toList();
        GradeScale.validate(parsed);

        bands.deleteAllForTenant(tenantId);
        bands.saveAll(parsed.stream().map(b -> new GradeBand(tenantId, b.min(), b.grade(), b.label())).toList());
        return parsed.stream().sorted(Comparator.comparing(GradeScale.Band::min).reversed())
                .map(b -> new BandView(b.min(), b.grade(), b.label())).toList();
    }
}