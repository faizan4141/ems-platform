package com.ems.academic.service;

import com.ems.academic.config.FeeConfig.FeeSettings;
import com.ems.academic.domain.*;
import com.ems.academic.event.FeeReminderEvent;
import com.ems.academic.repository.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FeeReminderService {

    private final FeeChargeRepository charges;
    private final PaymentRepository payments;
    private final StudentRepository students;
    private final GuardianRepository guardians;
    private final SchoolProfileRepository profiles;
    private final StudentService studentService;
    private final TenantGuard guard;
    private final ApplicationEventPublisher events;
    private final Clock clock;
    private final FeeSettings settings;

    public FeeReminderService(FeeChargeRepository charges, PaymentRepository payments, StudentRepository students,
                              GuardianRepository guardians, SchoolProfileRepository profiles,
                              StudentService studentService, TenantGuard guard, ApplicationEventPublisher events,
                              Clock clock, FeeSettings settings) {
        this.charges = charges;
        this.payments = payments;
        this.students = students;
        this.guardians = guardians;
        this.profiles = profiles;
        this.studentService = studentService;
        this.guard = guard;
        this.events = events;
        this.clock = clock;
        this.settings = settings;
    }

    /** One reminder per student with fees due soon or overdue that were not reminded recently. Returns how many. */
    @Transactional
    public int remindTenant(UUID tenantId) {
        TenantSnapshot tenant = guard.requireActive(tenantId);
        LocalDate today = LocalDate.now(clock);
        Instant now = clock.instant();

        List<FeeCharge> candidates = charges.findReminderCandidates(tenantId,
                today.plusDays(settings.dueSoonDays()), now.minus(Duration.ofDays(settings.reminderIntervalDays())));
        if (candidates.isEmpty()) return 0;

        Map<UUID, BigDecimal> paid = FeeMath.toPaidMap(payments.sumByChargeForTenant(tenantId));
        Map<UUID, List<FeeCharge>> open = candidates.stream()
                .filter(c -> FeeMath.balance(c.getAmount(), FeeMath.paidOf(paid, c.getId())).signum() > 0)
                .collect(Collectors.groupingBy(FeeCharge::getStudentId));
        if (open.isEmpty()) return 0;

        Map<UUID, Student> active = students.findAllById(open.keySet()).stream()
                .filter(s -> s.getStatus() == StudentStatus.ACTIVE).collect(Collectors.toMap(Student::getId, s -> s));
        Map<UUID, List<Guardian>> contacts = guardians.findByStudentIdIn(active.keySet()).stream()
                .collect(Collectors.groupingBy(Guardian::getStudentId));
        Map<UUID, String[]> places = studentService.placements(active.values().stream()
                .map(Student::getSectionId).filter(Objects::nonNull).collect(Collectors.toSet()));
        String school = profiles.findById(tenantId).map(SchoolProfile::getDisplayName).orElse(tenant.getName());

        int sent = 0;
        for (Student s : active.values()) {
            List<Guardian> list = contacts.getOrDefault(s.getId(), List.of());
            Guardian contact = list.stream().filter(Guardian::isPrimary).findFirst()
                    .orElse(list.isEmpty() ? null : list.get(0));
            if (contact == null) continue;   // nobody to tell; try again at the next run

            List<FeeCharge> mine = open.get(s.getId());
            BigDecimal total = mine.stream()
                    .map(c -> FeeMath.balance(c.getAmount(), FeeMath.paidOf(paid, c.getId())))
                    .reduce(FeeMath.ZERO, BigDecimal::add);
            LocalDate earliest = mine.stream().map(FeeCharge::getDueDate).min(Comparator.naturalOrder()).orElse(today);
            String[] place = StudentService.placementOf(places, s.getSectionId());

            events.publishEvent(new FeeReminderEvent(UUID.randomUUID(),
                    earliest.isBefore(today) ? "FEE_OVERDUE" : "FEE_DUE_SOON", tenantId, school, s.getId(),
                    s.getFirstName() + " " + s.getLastName(), place == null ? "" : place[0], place == null ? "" : place[1],
                    total, settings.currency(), mine.size(), earliest, earliest.isBefore(today),
                    contact.getName(), contact.getPhone(), contact.getEmail(), now));
            mine.forEach(c -> c.markReminded(now));
            sent++;
        }
        return sent;
    }
}