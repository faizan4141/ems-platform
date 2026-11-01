package com.ems.academic.service;

import com.ems.academic.domain.*;
import com.ems.academic.dto.FeeDtos.*;
import com.ems.academic.exception.DuplicateResourceException;
import com.ems.academic.exception.InvalidRequestException;
import com.ems.academic.exception.ResourceNotFoundException;
import com.ems.academic.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class FeeItemService {

    private final FeeItemRepository feeItems;
    private final FeeChargeRepository charges;
    private final ClassRepository classes;
    private final SectionRepository sections;
    private final StudentRepository students;
    private final TenantGuard guard;

    public FeeItemService(FeeItemRepository feeItems, FeeChargeRepository charges, ClassRepository classes,
                          SectionRepository sections, StudentRepository students, TenantGuard guard) {
        this.feeItems = feeItems;
        this.charges = charges;
        this.classes = classes;
        this.sections = sections;
        this.students = students;
        this.guard = guard;
    }

    @Transactional(readOnly = true)
    public List<FeeItemResponse> list(UUID tenantId, UUID classId) {
        guard.requireActive(tenantId);
        requireClass(tenantId, classId);
        Map<UUID, Integer> counts = new HashMap<>();
        for (Object[] row : charges.countsByItem(tenantId)) {
            counts.put((UUID) row[0], ((Number) row[1]).intValue());
        }
        return feeItems.findByTenantIdAndClassIdOrderByDueDateAscNameAsc(tenantId, classId).stream()
                .map(i -> FeeItemResponse.from(i, counts.getOrDefault(i.getId(), 0))).toList();
    }

    @Transactional
    public FeeItemResponse create(UUID tenantId, FeeItemRequest req) {
        guard.requireActive(tenantId);
        requireClass(tenantId, req.classId());
        String name = req.name().trim();
        if (feeItems.existsByClassIdAndNameIgnoreCase(req.classId(), name)) {
            throw new DuplicateResourceException("This class already has a fee called " + name);
        }
        FeeItem saved = feeItems.save(new FeeItem(tenantId, req.classId(), name,
                FeeMath.scale(req.amount()), req.dueDate()));
        return FeeItemResponse.from(saved, 0);
    }

    @Transactional
    public void delete(UUID tenantId, UUID itemId) {
        guard.requireActive(tenantId);
        FeeItem item = feeItems.findByIdAndTenantId(itemId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Fee not found"));
        if (charges.existsByFeeItemId(itemId)) {
            throw new InvalidRequestException("Charges were already generated for this fee, so it cannot be deleted");
        }
        feeItems.delete(item);
    }

    /** Creates the missing charges for every active student of the class. Safe to run repeatedly. */
    @Transactional
    public GenerateResponse generate(UUID tenantId, UUID classId) {
        guard.requireActive(tenantId);
        requireClass(tenantId, classId);
        List<FeeItem> items = feeItems.findByTenantIdAndClassIdOrderByDueDateAscNameAsc(tenantId, classId);
        if (items.isEmpty()) {
            throw new InvalidRequestException("Add fees for this class first");
        }
        Set<UUID> sectionIds = sections.findByClassId(classId).stream().map(Section::getId).collect(Collectors.toSet());
        if (sectionIds.isEmpty()) {
            throw new InvalidRequestException("This class has no sections yet");
        }
        List<Student> roster = students.findByTenantIdAndSectionIdInAndStatus(tenantId, sectionIds, StudentStatus.ACTIVE);
        Set<String> existing = charges.findExisting(tenantId, items.stream().map(FeeItem::getId).toList()).stream()
                .map(row -> row[0] + "|" + row[1]).collect(Collectors.toSet());

        List<FeeCharge> missing = new ArrayList<>();
        for (Student s : roster) {
            for (FeeItem i : items) {
                if (!existing.contains(s.getId() + "|" + i.getId())) {
                    missing.add(new FeeCharge(tenantId, s.getId(), i.getId(), i.getName(), i.getAmount(), i.getDueDate()));
                }
            }
        }
        charges.saveAll(missing);
        return new GenerateResponse(missing.size(), roster.size(), items.size());
    }

    private void requireClass(UUID tenantId, UUID classId) {
        classes.findByIdAndTenantId(classId, tenantId).orElseThrow(() -> new ResourceNotFoundException("Class not found"));
    }
}