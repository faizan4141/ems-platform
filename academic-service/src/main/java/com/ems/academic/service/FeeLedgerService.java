package com.ems.academic.service;

import com.ems.academic.config.FeeConfig.FeeSettings;
import com.ems.academic.domain.*;
import com.ems.academic.dto.FeeDtos.*;
import com.ems.academic.exception.ResourceNotFoundException;
import com.ems.academic.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FeeLedgerService {

    private static final int MAX_DUE_ROWS = 500;

    private final FeeChargeRepository charges;
    private final PaymentRepository payments;
    private final StudentRepository students;
    private final SectionRepository sections;
    private final SchoolProfileRepository profiles;
    private final StudentService studentService;
    private final TenantGuard guard;
    private final Clock clock;
    private final FeeSettings settings;

    public FeeLedgerService(FeeChargeRepository charges, PaymentRepository payments, StudentRepository students,
                            SectionRepository sections, SchoolProfileRepository profiles, StudentService studentService,
                            TenantGuard guard, Clock clock, FeeSettings settings) {
        this.charges = charges;
        this.payments = payments;
        this.students = students;
        this.sections = sections;
        this.profiles = profiles;
        this.studentService = studentService;
        this.guard = guard;
        this.clock = clock;
        this.settings = settings;
    }

    @Transactional(readOnly = true)
    public Ledger ledger(UUID tenantId, UUID studentId) {
        TenantSnapshot tenant = guard.requireActive(tenantId);
        Student student = students.findByIdAndTenantId(studentId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));
        LocalDate today = LocalDate.now(clock);

        List<FeeCharge> list = charges.findByTenantIdAndStudentIdOrderByDueDateAscNameAsc(tenantId, studentId);
        List<UUID> ids = list.stream().map(FeeCharge::getId).toList();
        Map<UUID, BigDecimal> paid = ids.isEmpty() ? Map.of() : FeeMath.toPaidMap(payments.sumByCharge(ids));
        List<Payment> made = ids.isEmpty() ? List.of() : payments.findByChargeIdInOrderByRecordedAtDesc(ids);
        Map<UUID, String> names = list.stream().collect(Collectors.toMap(FeeCharge::getId, FeeCharge::getName));

        BigDecimal billed = FeeMath.ZERO;
        BigDecimal totalPaid = FeeMath.ZERO;
        List<ChargeView> views = new ArrayList<>();
        for (FeeCharge c : list) {
            BigDecimal p = FeeMath.paidOf(paid, c.getId());
            billed = billed.add(c.getAmount());
            totalPaid = totalPaid.add(p);
            views.add(new ChargeView(c.getId(), c.getName(), c.getAmount(), p, FeeMath.balance(c.getAmount(), p),
                    c.getDueDate(), FeeMath.status(c.getAmount(), p, c.getDueDate(), today)));
        }

        UUID sectionId = student.getSectionId();
        String[] place = StudentService.placementOf(
                studentService.placements(sectionId == null ? Set.of() : Set.of(sectionId)), sectionId);
        String school = profiles.findById(tenantId).map(SchoolProfile::getDisplayName).orElse(tenant.getName());

        return new Ledger(settings.currency(), student.getId(), student.getFirstName() + " " + student.getLastName(),
                student.getAdmissionNo(), place == null ? null : place[0], place == null ? null : place[1], school,
                billed, totalPaid, FeeMath.balance(billed, totalPaid), views,
                made.stream().map(p -> PaymentView.from(p, names.get(p.getChargeId()))).toList());
    }

    @Transactional(readOnly = true)
    public FeeSummary summary(UUID tenantId) {
        guard.requireActive(tenantId);
        LocalDate today = LocalDate.now(clock);
        Map<UUID, BigDecimal> paid = FeeMath.toPaidMap(payments.sumByChargeForTenant(tenantId));

        BigDecimal billed = FeeMath.ZERO, collected = FeeMath.ZERO, outstanding = FeeMath.ZERO, overdue = FeeMath.ZERO;
        Set<UUID> lateStudents = new HashSet<>();
        for (FeeCharge c : charges.findByTenantId(tenantId)) {
            BigDecimal p = FeeMath.paidOf(paid, c.getId());
            BigDecimal balance = FeeMath.balance(c.getAmount(), p);
            billed = billed.add(c.getAmount());
            collected = collected.add(p);
            outstanding = outstanding.add(balance);
            if (balance.signum() > 0 && c.getDueDate().isBefore(today)) {
                overdue = overdue.add(balance);
                lateStudents.add(c.getStudentId());
            }
        }
        return new FeeSummary(settings.currency(), billed, collected, outstanding, overdue, lateStudents.size());
    }

    /** Students with money outstanding, most overdue first. Optionally limited to one class. */
    @Transactional(readOnly = true)
    public DuesResponse dues(UUID tenantId, UUID classId) {
        guard.requireActive(tenantId);
        LocalDate today = LocalDate.now(clock);
        Set<UUID> sectionFilter = classId == null ? null
                : sections.findByClassId(classId).stream().map(Section::getId).collect(Collectors.toSet());

        Map<UUID, BigDecimal> paid = FeeMath.toPaidMap(payments.sumByChargeForTenant(tenantId));
        Map<UUID, List<FeeCharge>> byStudent = charges.findByTenantId(tenantId).stream()
                .collect(Collectors.groupingBy(FeeCharge::getStudentId));
        Map<UUID, Student> known = students.findAllById(byStudent.keySet()).stream()
                .collect(Collectors.toMap(Student::getId, s -> s));
        Map<UUID, String[]> places = studentService.placements(known.values().stream()
                .map(Student::getSectionId).filter(Objects::nonNull).collect(Collectors.toSet()));

        List<DueRow> rows = new ArrayList<>();
        for (Map.Entry<UUID, List<FeeCharge>> entry : byStudent.entrySet()) {
            Student s = known.get(entry.getKey());
            if (s == null || s.getStatus() != StudentStatus.ACTIVE) continue;
            if (sectionFilter != null && (s.getSectionId() == null || !sectionFilter.contains(s.getSectionId()))) continue;

            BigDecimal balance = FeeMath.ZERO, overdue = FeeMath.ZERO;
            LocalDate earliest = null;
            for (FeeCharge c : entry.getValue()) {
                BigDecimal b = FeeMath.balance(c.getAmount(), FeeMath.paidOf(paid, c.getId()));
                if (b.signum() == 0) continue;
                balance = balance.add(b);
                if (c.getDueDate().isBefore(today)) overdue = overdue.add(b);
                if (earliest == null || c.getDueDate().isBefore(earliest)) earliest = c.getDueDate();
            }
            if (balance.signum() > 0) {
                String[] p = StudentService.placementOf(places, s.getSectionId());
                rows.add(new DueRow(s.getId(), s.getFirstName() + " " + s.getLastName(), s.getAdmissionNo(),
                        p == null ? null : p[0], p == null ? null : p[1], balance, overdue, earliest));
            }
        }
        rows.sort(Comparator.comparing(DueRow::overdue).reversed()
                .thenComparing(DueRow::balance, Comparator.reverseOrder()));
        return new DuesResponse(settings.currency(), rows.stream().limit(MAX_DUE_ROWS).toList());
    }
}