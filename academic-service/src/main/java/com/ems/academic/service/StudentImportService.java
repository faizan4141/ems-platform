package com.ems.academic.service;

import com.ems.academic.domain.*;
import com.ems.academic.dto.ImportResult;
import com.ems.academic.dto.ImportRowError;
import com.ems.academic.exception.InvalidRequestException;
import com.ems.academic.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class StudentImportService {

    static final int MAX_ROWS = 2000;
    private static final int MAX_REPORTED_ERRORS = 200;
    private static final List<String> REQUIRED = List.of("admission_no", "first_name", "last_name",
            "date_of_birth", "guardian_name", "guardian_phone");

    private record Draft(Student student, String guardianName, Relationship relationship, String phone, String email) {}

    private final StudentRepository students;
    private final GuardianRepository guardians;
    private final AcademicYearRepository years;
    private final ClassRepository classes;
    private final SectionRepository sections;
    private final TenantGuard guard;

    public StudentImportService(StudentRepository students, GuardianRepository guardians, AcademicYearRepository years,
                                ClassRepository classes, SectionRepository sections, TenantGuard guard) {
        this.students = students;
        this.guardians = guardians;
        this.years = years;
        this.classes = classes;
        this.sections = sections;
        this.guard = guard;
    }

    @Transactional
    public ImportResult importCsv(UUID tenantId, byte[] data, boolean dryRun) {
        guard.requireActive(tenantId);

        String text = new String(data, StandardCharsets.UTF_8);
        if (text.startsWith("\uFEFF")) text = text.substring(1);   // Excel's byte-order mark
        List<String[]> rows = CsvParser.parse(text);
        if (rows.isEmpty()) throw new InvalidRequestException("The file is empty");

        Map<String, Integer> columns = indexColumns(rows.get(0));
        List<String> missing = REQUIRED.stream().filter(c -> !columns.containsKey(c)).toList();
        if (!missing.isEmpty()) {
            throw new InvalidRequestException("Missing columns: " + String.join(", ", missing));
        }
        if (rows.size() - 1 > MAX_ROWS) {
            throw new InvalidRequestException("Too many rows: the limit is " + MAX_ROWS + " per file");
        }

        Set<String> existing = new HashSet<>(students.findAdmissionNosLower(tenantId));
        Map<String, UUID> sectionLookup = sectionLookup(tenantId);
        Set<String> seenInFile = new HashSet<>();
        List<Draft> valid = new ArrayList<>();
        List<ImportRowError> errors = new ArrayList<>();
        int total = 0;
        LocalDate today = LocalDate.now();

        for (int i = 1; i < rows.size(); i++) {
            String[] row = rows.get(i);
            if (Arrays.stream(row).allMatch(c -> c.isBlank())) continue;   // skip empty lines
            total++;
            try {
                valid.add(buildDraft(tenantId, row, columns, existing, seenInFile, sectionLookup, today));
            } catch (InvalidRequestException e) {
                errors.add(new ImportRowError(i + 1, e.getMessage()));
            }
        }

        int imported = 0;
        if (!dryRun && !valid.isEmpty()) {
            List<Student> saved = students.saveAll(valid.stream().map(Draft::student).toList());
            List<Guardian> toSave = new ArrayList<>();
            for (int i = 0; i < saved.size(); i++) {
                Draft d = valid.get(i);
                toSave.add(new Guardian(tenantId, saved.get(i).getId(), d.guardianName(), d.relationship(),
                        d.phone(), d.email(), true));
            }
            guardians.saveAll(toSave);
            imported = saved.size();
        }
        return new ImportResult(dryRun, total, valid.size(), imported,
                errors.size() > MAX_REPORTED_ERRORS ? errors.subList(0, MAX_REPORTED_ERRORS) : errors);
    }

    private Draft buildDraft(UUID tenantId, String[] row, Map<String, Integer> col, Set<String> existing,
                             Set<String> seenInFile, Map<String, UUID> sectionLookup, LocalDate today) {
        String admissionNo = StudentFields.requireText(cell(row, col, "admission_no"), "admission_no", 30);
        String key = admissionNo.toLowerCase(Locale.ROOT);
        if (seenInFile.contains(key)) throw new InvalidRequestException("admission_no appears twice in this file: " + admissionNo);
        if (existing.contains(key)) throw new InvalidRequestException("admission_no already exists: " + admissionNo);

        String first = StudentFields.requireText(cell(row, col, "first_name"), "first_name", 100);
        String last = StudentFields.requireText(cell(row, col, "last_name"), "last_name", 100);
        LocalDate dob = StudentFields.checkDob(StudentFields.parseDate(cell(row, col, "date_of_birth"), "date_of_birth"), today);
        Gender gender = StudentFields.parseGender(cell(row, col, "gender"));
        String guardianName = StudentFields.requireText(cell(row, col, "guardian_name"), "guardian_name", 200);
        Relationship relationship = StudentFields.parseRelationship(cell(row, col, "guardian_relationship"));
        String phone = StudentFields.normalizePhone(cell(row, col, "guardian_phone"));
        String email = StudentFields.cleanEmail(cell(row, col, "guardian_email"));
        UUID sectionId = resolveSection(cell(row, col, "class"), cell(row, col, "section"), sectionLookup);

        seenInFile.add(key);
        return new Draft(new Student(tenantId, admissionNo, first, last, dob, gender, today, sectionId),
                guardianName, relationship, phone, email);
    }

    private UUID resolveSection(String className, String sectionName, Map<String, UUID> lookup) {
        if (className.isEmpty() && sectionName.isEmpty()) return null;
        if (className.isEmpty()) throw new InvalidRequestException("class is required when a section is given");
        if (sectionName.isEmpty()) throw new InvalidRequestException("section is required when a class is given");
        UUID id = lookup.get(className.toLowerCase(Locale.ROOT) + "|" + sectionName.toLowerCase(Locale.ROOT));
        if (id == null) {
            throw new InvalidRequestException("No section '" + sectionName + "' in class '" + className
                    + "' for the current academic year");
        }
        return id;
    }

    /** "class|section" (lower case) to section id, for the current academic year. */
    private Map<String, UUID> sectionLookup(UUID tenantId) {
        Map<String, UUID> lookup = new HashMap<>();
        years.findByTenantIdAndCurrentYearTrue(tenantId).ifPresent(year -> {
            List<SchoolClass> yearClasses = classes
                    .findByTenantIdAndAcademicYearIdOrderByGradeLevelAscNameAsc(tenantId, year.getId());
            if (yearClasses.isEmpty()) return;
            Map<UUID, String> classNames = yearClasses.stream()
                    .collect(Collectors.toMap(SchoolClass::getId, SchoolClass::getName));
            for (Section s : sections.findByClassIdIn(classNames.keySet())) {
                lookup.put(classNames.get(s.getClassId()).toLowerCase(Locale.ROOT) + "|"
                        + s.getName().toLowerCase(Locale.ROOT), s.getId());
            }
        });
        return lookup;
    }

    private static Map<String, Integer> indexColumns(String[] header) {
        Map<String, Integer> index = new HashMap<>();
        for (int i = 0; i < header.length; i++) {
            index.putIfAbsent(header[i].trim().toLowerCase(Locale.ROOT).replace(' ', '_').replace('-', '_'), i);
        }
        return index;
    }

    private static String cell(String[] row, Map<String, Integer> columns, String name) {
        Integer i = columns.get(name);
        return i == null || i >= row.length ? "" : row[i].trim();
    }
}