package com.ems.academic;

import com.ems.academic.domain.TenantSnapshot;
import com.ems.academic.repository.TenantSnapshotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import com.ems.academic.event.AttendanceRecorded;
import org.springframework.context.event.EventListener;
import com.ems.academic.domain.GuardianLogin;
import com.ems.academic.repository.GuardianLoginRepository;
import com.ems.academic.event.FeeReminderEvent;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.time.Clock;
import java.time.LocalDate;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"spring.kafka.listener.auto-startup=false", "ems.kafka.publish=false", "ems.scheduling.enabled=false"})
class AcademicApiIntegrationTest {

    @Container
    @ServiceConnection
    @SuppressWarnings({"rawtypes", "resource"})
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16");

    @TestConfiguration
    static class TestJwtConfig {
        static class EventCollector {
            final List<AttendanceRecorded> events = new CopyOnWriteArrayList<>();
            final List<FeeReminderEvent> feeEvents = new CopyOnWriteArrayList<>();

            @EventListener
            void on(AttendanceRecorded event) { events.add(event); }

            @EventListener
            void on(FeeReminderEvent event) { feeEvents.add(event); }
        }

        @Bean
        JwtDecoder jwtDecoder() {
            return AcademicApiIntegrationTest::decode;
        }

        @Bean
        EventCollector eventCollector() { return new EventCollector(); }
    }

    /** Test tokens look like "ROLE", "ROLE.tenantId" or "ROLE.tenantId.subject". */
    private static Jwt decode(String token) {
        String[] parts = token.split("\\.");
        String subject = parts.length > 2 ? parts[2] : "user-" + parts[0];
        Jwt.Builder builder = Jwt.withTokenValue(token).header("alg", "none").subject(subject)
                .claim("realm_access", Map.of("roles", List.of(parts[0])))
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(600));
        if (parts.length > 1) {
            builder.claim("tenant_id", parts[1]);
        }
        return builder.build();
    }

    private static final HttpClient CLIENT = HttpClient.newHttpClient();
    private static final Pattern ID = Pattern.compile("\"id\":\"([0-9a-f-]{36})\"");

    @Value("${local.server.port}") int port;
    @Autowired TenantSnapshotRepository snapshots;
    @Autowired TestJwtConfig.EventCollector collector;
    @Autowired Clock clock;
    @Autowired GuardianLoginRepository logins;

    private final UUID tenantA = UUID.randomUUID();
    private final UUID tenantB = UUID.randomUUID();
    private final UUID suspended = UUID.randomUUID();

    @BeforeEach
    void seedTenants() {
        snapshot(tenantA, "ACTIVE");
        snapshot(tenantB, "ACTIVE");
        snapshot(suspended, "SUSPENDED");
    }

    private void snapshot(UUID id, String status) {
        TenantSnapshot s = new TenantSnapshot(id);
        s.update("t-" + id.toString().substring(0, 8), "School " + id.toString().substring(0, 4), status, Instant.now());
        snapshots.save(s);
    }

    private String admin(UUID tenant) { return "TENANT_ADMIN." + tenant; }
    private String teacher(UUID tenant) { return "TEACHER." + tenant; }

    private HttpResponse<String> send(String method, String path, String token, String json) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json");
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        request.method(method, json == null ? BodyPublishers.noBody() : BodyPublishers.ofString(json));
        return CLIENT.send(request.build(), BodyHandlers.ofString());
    }

    private static String year(String name, String start, String end) {
        return "{\"name\":\"" + name + "\",\"startDate\":\"" + start + "\",\"endDate\":\"" + end + "\"}";
    }

    private static String idOf(String body) {
        Matcher m = ID.matcher(body);
        assertThat(m.find()).as("response has an id: " + body).isTrue();
        return m.group(1);
    }

    private static long count(String text, String needle) {
        return text.split(java.util.regex.Pattern.quote(needle), -1).length - 1;
    }

    private HttpResponse<String> upload(String path, String token, String csv) throws Exception {
        String boundary = "----ems" + UUID.randomUUID();
        String body = "--" + boundary + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\"students.csv\"\r\n"
                + "Content-Type: text/csv\r\n\r\n" + csv + "\r\n--" + boundary + "--\r\n";
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(BodyPublishers.ofString(body)).build();
        return CLIENT.send(request, BodyHandlers.ofString());
    }

    /** The n-th "id" value (0-based) in a JSON response. */
    private static String nthId(String body, int n) {
        Matcher m = ID.matcher(body);
        for (int i = 0; i <= n; i++) {
            assertThat(m.find()).as("id #" + n + " in: " + body).isTrue();
        }
        return m.group(1);
    }

    private static String studentJson(String admissionNo, String sectionId, String guardians) {
        return "{\"admissionNo\":\"" + admissionNo + "\",\"firstName\":\"Aarav\",\"lastName\":\"Sharma\","
                + "\"dateOfBirth\":\"2014-05-10\"" + (sectionId == null ? "" : ",\"sectionId\":\"" + sectionId + "\"")
                + ",\"guardians\":" + guardians + "}";
    }

    private static final String ONE_GUARDIAN =
            "[{\"name\":\"Raj Sharma\",\"relationship\":\"FATHER\",\"phone\":\"+91 98765 43210\"}]";

    /** Creates a current year with "Class 8" and sections A and B; returns the id of section A. */
    private String classWithSections(String token) throws Exception {
        String yearId = idOf(send("POST", "/api/v1/academic-years", token, year("2026-27", "2026-04-01", "2027-03-31")).body());
        String body = "{\"academicYearId\":\"" + yearId + "\",\"name\":\"Class 8\",\"gradeLevel\":8,\"sections\":[\"A\",\"B\"]}";
        return nthId(send("POST", "/api/v1/classes", token, body).body(), 1);   // 0 = class id, 1 = first section id
    }

    private String studentInSection(String token, String admissionNo, String sectionId) throws Exception {
        return idOf(send("POST", "/api/v1/students", token, studentJson(admissionNo, sectionId, ONE_GUARDIAN)).body());
    }

    /** marks(id1, "ABSENT", id2, "LATE"): students not listed are present. */
    private static String marks(String... idAndStatus) {
        StringBuilder sb = new StringBuilder("{\"marks\":[");
        for (int i = 0; i < idAndStatus.length; i += 2) {
            if (i > 0) sb.append(',');
            sb.append("{\"studentId\":\"").append(idAndStatus[i]).append("\",\"status\":\"")
                    .append(idAndStatus[i + 1]).append("\"}");
        }
        return sb.append("]}").toString();
    }

    private String parent(UUID tenant, UUID subject) { return "PARENT." + tenant + "." + subject; }

    private static String guardianWithEmail(String email) {
        return "[{\"name\":\"Raj Sharma\",\"relationship\":\"FATHER\",\"phone\":\"+919876543210\",\"email\":\"" + email + "\"}]";
    }

    private static String notice(String title, String audience) {
        return "{\"title\":\"" + title + "\",\"body\":\"Details\",\"audience\":\"" + audience + "\"}";
    }

    private String[] classAndSection(String token) throws Exception {
        String yearId = idOf(send("POST", "/api/v1/academic-years", token, year("2026-27", "2026-04-01", "2027-03-31")).body());
        String body = "{\"academicYearId\":\"" + yearId + "\",\"name\":\"Class 8\",\"gradeLevel\":8,\"sections\":[\"A\"]}";
        String created = send("POST", "/api/v1/classes", token, body).body();
        return new String[]{nthId(created, 0), nthId(created, 1)};   // class id, then section A's id
    }

    private static String feeItem(String classId, String name, String amount, LocalDate due) {
        return "{\"classId\":\"" + classId + "\",\"name\":\"" + name + "\",\"amount\":\"" + amount + "\",\"dueDate\":\"" + due + "\"}";
    }

    private static String payment(String amount, String key) {
        return "{\"amount\":\"" + amount + "\",\"method\":\"CASH\",\"idempotencyKey\":\"" + key + "\"}";
    }

    private String ledger(String token, String studentId) throws Exception {
        return send("GET", "/api/v1/fees/students/" + studentId, token, null).body();
    }

    @Test
    void rejectsRequestsWithoutToken() throws Exception {
        assertThat(send("GET", "/api/v1/academic-years", null, null).statusCode()).isEqualTo(401);
    }

    @Test
    void rolesAreEnforcedBeforeValidation() throws Exception {
        assertThat(send("POST", "/api/v1/academic-years", teacher(tenantA), "{}").statusCode()).isEqualTo(403);
        assertThat(send("GET", "/api/v1/academic-years", "PLATFORM_ADMIN", null).statusCode()).isEqualTo(403);
    }

    @Test
    void firstYearIsCurrent_andSwitchingKeepsExactlyOneCurrent() throws Exception {
        String token = admin(tenantA);

        HttpResponse<String> first = send("POST", "/api/v1/academic-years", token, year("2026-27", "2026-04-01", "2027-03-31"));
        assertThat(first.statusCode()).isEqualTo(201);
        assertThat(first.body()).contains("\"current\":true");

        HttpResponse<String> second = send("POST", "/api/v1/academic-years", token, year("2027-28", "2027-04-01", "2028-03-31"));
        assertThat(second.body()).contains("\"current\":false");

        assertThat(send("PUT", "/api/v1/academic-years/" + idOf(second.body()) + "/current", token, null).statusCode())
                .isEqualTo(200);

        String list = send("GET", "/api/v1/academic-years", token, null).body();
        assertThat(count(list, "\"current\":true")).isEqualTo(1);
    }

    @Test
    void rejectsInvalidDatesAndDuplicateYears() throws Exception {
        String token = admin(tenantA);
        assertThat(send("POST", "/api/v1/academic-years", token, year("bad", "2027-03-31", "2026-04-01")).statusCode())
                .isEqualTo(400);

        send("POST", "/api/v1/academic-years", token, year("2026-27", "2026-04-01", "2027-03-31"));
        assertThat(send("POST", "/api/v1/academic-years", token, year("2026-27", "2026-04-01", "2027-03-31")).statusCode())
                .isEqualTo(409);
    }

    @Test
    void createsClassWithSections_andRejectsDuplicates() throws Exception {
        String token = admin(tenantA);
        String yearId = idOf(send("POST", "/api/v1/academic-years", token, year("2026-27", "2026-04-01", "2027-03-31")).body());
        String body = "{\"academicYearId\":\"" + yearId + "\",\"name\":\"Class 8\",\"gradeLevel\":8,\"sections\":[\"A\",\"B\"]}";

        HttpResponse<String> created = send("POST", "/api/v1/classes", token, body);
        assertThat(created.statusCode()).isEqualTo(201);
        assertThat(created.body()).contains("\"name\":\"A\"").contains("\"name\":\"B\"");

        assertThat(send("POST", "/api/v1/classes", token, body).statusCode()).isEqualTo(409);

        String dupSections = "{\"academicYearId\":\"" + yearId + "\",\"name\":\"Class 9\",\"gradeLevel\":9,\"sections\":[\"A\",\"a\"]}";
        assertThat(send("POST", "/api/v1/classes", token, dupSections).statusCode()).isEqualTo(400);

        // default listing uses the current year
        assertThat(send("GET", "/api/v1/classes", teacher(tenantA), null).body()).contains("Class 8");
    }

    @Test
    void schoolsCannotSeeOrTouchEachOther() throws Exception {
        String yearId = idOf(send("POST", "/api/v1/academic-years", admin(tenantA),
                year("2026-27", "2026-04-01", "2027-03-31")).body());

        assertThat(send("GET", "/api/v1/academic-years", admin(tenantB), null).body()).isEqualTo("[]");

        String body = "{\"academicYearId\":\"" + yearId + "\",\"name\":\"Class 1\",\"gradeLevel\":1}";
        assertThat(send("POST", "/api/v1/classes", admin(tenantB), body).statusCode()).isEqualTo(404);
        assertThat(send("GET", "/api/v1/classes?academicYearId=" + yearId, admin(tenantB), null).statusCode())
                .isEqualTo(404);
    }

    @Test
    void suspendedSchoolsCannotWrite() throws Exception {
        assertThat(send("POST", "/api/v1/academic-years", admin(suspended),
                year("2026-27", "2026-04-01", "2027-03-31")).statusCode()).isEqualTo(403);
    }

    @Test
    void profileAndOnboardingProgress() throws Exception {
        String token = admin(tenantA);

        assertThat(send("GET", "/api/v1/school/profile", token, null).body()).contains("\"configured\":false");
        assertThat(send("GET", "/api/v1/onboarding", token, null).body()).contains("\"profileDone\":false");

        String profile = "{\"displayName\":\"Springfield Public School\",\"board\":\"CBSE\",\"city\":\"Springfield\"}";
        assertThat(send("PUT", "/api/v1/school/profile", token, profile).statusCode()).isEqualTo(200);

        assertThat(send("GET", "/api/v1/school/profile", token, null).body())
                .contains("\"configured\":true").contains("Springfield Public School");
        String onboarding = send("GET", "/api/v1/onboarding", token, null).body();
        assertThat(onboarding).contains("\"profileDone\":true").contains("\"academicYearDone\":false");
    }

    @Test
    void createsAStudentWithGuardians_andFindsThemByFilterAndSearch() throws Exception {
        String token = admin(tenantA);
        String sectionA = classWithSections(token);

        HttpResponse<String> created = send("POST", "/api/v1/students", token, studentJson("A-1", sectionA, ONE_GUARDIAN));
        assertThat(created.statusCode()).isEqualTo(201);
        assertThat(created.body()).contains("+919876543210").contains("\"primary\":true").contains("\"className\":\"Class 8\"");

        send("POST", "/api/v1/students", token, studentJson("A-2", null, ONE_GUARDIAN));

        assertThat(send("GET", "/api/v1/students?sectionId=" + sectionA, teacher(tenantA), null).body())
                .contains("A-1").doesNotContain("A-2");
        assertThat(send("GET", "/api/v1/students?unassigned=true", token, null).body())
                .contains("A-2").doesNotContain("A-1");
        assertThat(send("GET", "/api/v1/students?search=aarav%20sharma", token, null).body())
                .contains("\"totalElements\":2");
        assertThat(send("GET", "/api/v1/students?search=zzz", token, null).body()).contains("\"totalElements\":0");
    }

    @Test
    void validatesStudentsAndGuardians() throws Exception {
        String token = admin(tenantA);
        assertThat(send("POST", "/api/v1/students", token, studentJson("A-1", null, ONE_GUARDIAN)).statusCode()).isEqualTo(201);
        assertThat(send("POST", "/api/v1/students", token, studentJson("a-1", null, ONE_GUARDIAN)).statusCode())
                .as("admission numbers are unique ignoring case").isEqualTo(409);
        assertThat(send("POST", "/api/v1/students", token, studentJson("A-3", null, "[]")).statusCode()).isEqualTo(400);
        assertThat(send("POST", "/api/v1/students", token, studentJson("A-4", null,
                "[{\"name\":\"Raj\",\"relationship\":\"FATHER\",\"phone\":\"12\"}]")).statusCode()).isEqualTo(400);
        assertThat(send("POST", "/api/v1/students", token, studentJson("A-5", null,
                "[{\"name\":\"Raj\",\"relationship\":\"FATHER\",\"phone\":\"+919876543210\",\"primary\":true},"
                        + "{\"name\":\"Sita\",\"relationship\":\"MOTHER\",\"phone\":\"+919876543211\",\"primary\":true}]"))
                .statusCode()).isEqualTo(400);
    }

    @Test
    void studentsAreIsolatedBetweenSchools_andOnlyAdminsCanWrite() throws Exception {
        String studentId = idOf(send("POST", "/api/v1/students", admin(tenantA), studentJson("A-1", null, ONE_GUARDIAN)).body());

        assertThat(send("GET", "/api/v1/students", admin(tenantB), null).body()).contains("\"totalElements\":0");
        assertThat(send("GET", "/api/v1/students/" + studentId, admin(tenantB), null).statusCode()).isEqualTo(404);
        assertThat(send("POST", "/api/v1/students", teacher(tenantA), studentJson("A-9", null, ONE_GUARDIAN)).statusCode()).isEqualTo(403);
        assertThat(send("GET", "/api/v1/students/" + studentId, teacher(tenantA), null).statusCode()).isEqualTo(200);
    }

    @Test
    void movesAStudentAndReplacesGuardians() throws Exception {
        String token = admin(tenantA);
        String sectionA = classWithSections(token);
        String studentId = idOf(send("POST", "/api/v1/students", token, studentJson("A-1", null, ONE_GUARDIAN)).body());

        String update = "{\"firstName\":\"Aarav\",\"lastName\":\"Sharma\",\"dateOfBirth\":\"2014-05-10\","
                + "\"sectionId\":\"" + sectionA + "\",\"status\":\"ACTIVE\"}";
        assertThat(send("PUT", "/api/v1/students/" + studentId, token, update).body()).contains("\"sectionName\":\"A\"");

        String replacement = "[{\"name\":\"Sita Sharma\",\"relationship\":\"MOTHER\",\"phone\":\"+919876500000\"}]";
        HttpResponse<String> replaced = send("PUT", "/api/v1/students/" + studentId + "/guardians", token, replacement);
        assertThat(replaced.statusCode()).isEqualTo(200);
        assertThat(replaced.body()).contains("Sita Sharma").doesNotContain("Raj Sharma");
    }

    @Test
    void csvImport_previewsFirst_thenImportsValidRowsOnly() throws Exception {
        String token = admin(tenantA);
        classWithSections(token);
        String csv = "admission_no,first_name,last_name,date_of_birth,gender,class,section,guardian_name,guardian_relationship,guardian_phone,guardian_email\n"
                + "S-1,Diya,Patel,2014-02-11,F,Class 8,A,Mohan Patel,FATHER,+919800000001,mohan@example.com\n"
                + "S-2,Kabir,Singh,31/02/2014,M,Class 8,B,Anil Singh,FATHER,+919800000002,\n"
                + "S-3,Meera,Nair,12/03/2014,F,Class 8,C,Latha Nair,MOTHER,+919800000003,\n"
                + "S-1,Rohan,Gupta,2014-07-01,M,,,Vijay Gupta,FATHER,+919800000004,\n"
                + "S-5,Sana,Khan,2014-09-09,F,,,Imran Khan,FATHER,+919800000005,\n";

        HttpResponse<String> preview = upload("/api/v1/students/import?dryRun=true", token, csv);
        assertThat(preview.statusCode()).isEqualTo(200);
        assertThat(preview.body()).contains("\"totalRows\":5").contains("\"validRows\":2").contains("\"imported\":0");
        assertThat(send("GET", "/api/v1/students", token, null).body()).contains("\"totalElements\":0");

        HttpResponse<String> done = upload("/api/v1/students/import?dryRun=false", token, csv);
        assertThat(done.body()).contains("\"imported\":2");
        assertThat(send("GET", "/api/v1/students", token, null).body()).contains("\"totalElements\":2");

        // importing the same file again must flag every previously imported admission number
        assertThat(upload("/api/v1/students/import?dryRun=false", token, csv).body()).contains("\"imported\":0");
    }

    @Test
    void csvImport_rejectsFilesWithMissingColumns_andNonAdmins() throws Exception {
        assertThat(upload("/api/v1/students/import", admin(tenantA), "admission_no,first_name\nS-1,Diya\n").statusCode())
                .isEqualTo(400);
        assertThat(upload("/api/v1/students/import", teacher(tenantA), "a,b\n").statusCode()).isEqualTo(403);
    }

    @Test
    void takesAttendance_notifiesOnlyOnChanges_andShowsItInTheRoster() throws Exception {
        String token = admin(tenantA);
        String section = classWithSections(token);
        String aarav = studentInSection(token, "A-1", section);
        studentInSection(token, "A-2", section);
        String path = "/api/v1/attendance/sections/" + section;

        String before = send("GET", path, teacher(tenantA), null).body();
        assertThat(before).contains("\"taken\":false").contains("\"status\":\"PRESENT\"");

        int eventsBefore = collector.events.size();
        HttpResponse<String> saved = send("PUT", path, teacher(tenantA), marks(aarav, "ABSENT"));
        assertThat(saved.statusCode()).isEqualTo(200);
        assertThat(saved.body()).contains("\"absent\":1").contains("\"present\":1").contains("\"notified\":1");
        AttendanceRecorded event = collector.events.get(eventsBefore);
        assertThat(event.alerts()).singleElement().satisfies(a -> {
            assertThat(a.guardianPhone()).isEqualTo("+919876543210");
            assertThat(a.status().name()).isEqualTo("ABSENT");
        });

        assertThat(send("PUT", path, teacher(tenantA), marks(aarav, "ABSENT")).body())
                .as("saving the same marks again sends nothing").contains("\"notified\":0");
        assertThat(send("PUT", path, teacher(tenantA), marks(aarav, "LATE")).body())
                .contains("\"notified\":1").contains("\"late\":1");
        assertThat(send("PUT", path, teacher(tenantA), marks()).body())
                .as("a correction back to present sends nothing").contains("\"notified\":0").contains("\"present\":2");
        assertThat(send("GET", path, teacher(tenantA), null).body()).contains("\"taken\":true");
    }

    @Test
    void teachersCanOnlyEditToday_adminsCanCorrectThePast_futureIsRejected() throws Exception {
        String token = admin(tenantA);
        String section = classWithSections(token);
        String student = studentInSection(token, "A-1", section);
        String path = "/api/v1/attendance/sections/" + section;
        LocalDate today = LocalDate.now(clock);

        assertThat(send("PUT", path + "?date=" + today.minusDays(1), teacher(tenantA), marks(student, "ABSENT")).statusCode())
                .isEqualTo(403);
        assertThat(send("PUT", path + "?date=" + today.minusDays(1), token, marks(student, "ABSENT")).statusCode())
                .isEqualTo(200);
        assertThat(send("PUT", path + "?date=" + today.plusDays(1), token, marks()).statusCode()).isEqualTo(400);
    }

    @Test
    void rejectsForeignStudents_keepsSchoolsApart_andSummarizesTheDay() throws Exception {
        String token = admin(tenantA);
        String section = classWithSections(token);
        String student = studentInSection(token, "A-1", section);
        studentInSection(token, "A-2", section);
        String path = "/api/v1/attendance/sections/" + section;

        assertThat(send("PUT", path, token, marks(UUID.randomUUID().toString(), "ABSENT")).statusCode()).isEqualTo(400);
        assertThat(send("GET", path, admin(tenantB), null).statusCode()).isEqualTo(404);

        send("PUT", path, token, marks(student, "ABSENT"));
        String summary = send("GET", "/api/v1/attendance/summary", token, null).body();
        assertThat(summary).contains("\"sectionsTaken\":1").contains("\"absent\":1").contains("\"percentPresent\":50");
        assertThat(send("GET", "/api/v1/attendance/summary", admin(tenantB), null).body())
                .contains("\"sectionsTaken\":0");
    }

    @Test
    void parentsSeeOnlyTheirOwnChildren() throws Exception {
        String token = admin(tenantA);
        String section = classWithSections(token);
        String mine = idOf(send("POST", "/api/v1/students", token,
                studentJson("P-1", section, guardianWithEmail("raj@example.com"))).body());
        String other = idOf(send("POST", "/api/v1/students", token,
                studentJson("P-2", section, guardianWithEmail("someone@example.com"))).body());
        UUID parentSub = UUID.randomUUID();
        logins.save(new GuardianLogin(parentSub, tenantA, "Raj@Example.com"));   // matching ignores case

        send("PUT", "/api/v1/attendance/sections/" + section, teacher(tenantA), marks(mine, "ABSENT"));

        String children = send("GET", "/api/v1/parent/children", parent(tenantA, parentSub), null).body();
        assertThat(children).contains(mine).doesNotContain(other);
        assertThat(send("GET", "/api/v1/parent/children/" + mine + "/today", parent(tenantA, parentSub), null).body())
                .contains("\"today\":\"ABSENT\"");
        assertThat(send("GET", "/api/v1/parent/children/" + other + "/today", parent(tenantA, parentSub), null).statusCode())
                .isEqualTo(404);
        assertThat(send("GET", "/api/v1/parent/children", parent(tenantB, parentSub), null).body()).isEqualTo("[]");
    }

    @Test
    void parentsAreLockedOutOfSchoolData_andStaffOutOfParentData() throws Exception {
        UUID sub = UUID.randomUUID();
        assertThat(send("GET", "/api/v1/students", parent(tenantA, sub), null).statusCode()).isEqualTo(403);
        assertThat(send("GET", "/api/v1/school/profile", parent(tenantA, sub), null).statusCode()).isEqualTo(403);
        assertThat(send("GET", "/api/v1/parent/children", teacher(tenantA), null).statusCode()).isEqualTo(403);
        assertThat(send("GET", "/api/v1/parent/children", admin(tenantA), null).statusCode()).isEqualTo(403);
    }

    @Test
    void guardiansShowWhetherTheyHavePortalAccess() throws Exception {
        String token = admin(tenantA);
        String linked = idOf(send("POST", "/api/v1/students", token,
                studentJson("L-1", null, guardianWithEmail("raj@example.com"))).body());
        String unlinked = idOf(send("POST", "/api/v1/students", token,
                studentJson("L-2", null, guardianWithEmail("other@example.com"))).body());
        logins.save(new GuardianLogin(UUID.randomUUID(), tenantA, "raj@example.com"));

        assertThat(send("GET", "/api/v1/students/" + linked, token, null).body()).contains("\"portalAccess\":true");
        assertThat(send("GET", "/api/v1/students/" + unlinked, token, null).body()).contains("\"portalAccess\":false");
    }

    @Test
    void noticesAreFilteredByAudience_andOnlyAdminsPublish() throws Exception {
        String token = admin(tenantA);
        UUID sub = UUID.randomUUID();

        assertThat(send("POST", "/api/v1/notices", token, notice("Sports day", "PARENTS")).statusCode()).isEqualTo(201);
        String staffNotice = idOf(send("POST", "/api/v1/notices", token, notice("Staff meeting", "STAFF")).body());
        assertThat(send("POST", "/api/v1/notices", token, "{\"title\":\"\",\"body\":\"x\",\"audience\":\"ALL\"}").statusCode())
                .isEqualTo(400);

        String parentView = send("GET", "/api/v1/notices", parent(tenantA, sub), null).body();
        assertThat(parentView).contains("Sports day").doesNotContain("Staff meeting");
        assertThat(send("GET", "/api/v1/notices", teacher(tenantA), null).body())
                .contains("Sports day").contains("Staff meeting");

        assertThat(send("POST", "/api/v1/notices", teacher(tenantA), notice("Nope", "ALL")).statusCode()).isEqualTo(403);
        assertThat(send("POST", "/api/v1/notices", parent(tenantA, sub), notice("Nope", "ALL")).statusCode()).isEqualTo(403);

        assertThat(send("DELETE", "/api/v1/notices/" + staffNotice, token, null).statusCode()).isEqualTo(204);
        assertThat(send("GET", "/api/v1/notices", teacher(tenantA), null).body()).doesNotContain("Staff meeting");
        assertThat(send("GET", "/api/v1/notices", admin(tenantB), null).body()).isEqualTo("[]");
    }

    @Test
    void feeLifecycle_generate_pay_receipt_void() throws Exception {
        String token = admin(tenantA);
        String[] cs = classAndSection(token);
        String student = studentInSection(token, "F-1", cs[1]);
        studentInSection(token, "F-2", cs[1]);
        LocalDate due = LocalDate.now(clock).plusDays(10);
        String generatePath = "/api/v1/fees/classes/" + cs[0] + "/generate";

        assertThat(send("POST", generatePath, token, null).statusCode()).as("no fees defined yet").isEqualTo(400);
        assertThat(send("POST", "/api/v1/fees/items", token, feeItem(cs[0], "Tuition", "12000.00", due)).statusCode()).isEqualTo(201);
        assertThat(send("POST", "/api/v1/fees/items", token, feeItem(cs[0], "tuition", "1.00", due)).statusCode())
                .as("fee names are unique per class, ignoring case").isEqualTo(409);

        assertThat(send("POST", generatePath, token, null).body()).contains("\"created\":2");
        assertThat(send("POST", generatePath, token, null).body()).as("generating again adds nothing").contains("\"created\":0");

        String before = ledger(token, student);
        assertThat(before).contains("\"balance\":12000.00").contains("\"status\":\"DUE\"");
        String chargeId = idOf(before);

        String key1 = UUID.randomUUID().toString();
        HttpResponse<String> first = send("POST", "/api/v1/fees/charges/" + chargeId + "/payments", token, payment("5000.00", key1));
        assertThat(first.statusCode()).isEqualTo(201);
        assertThat(first.body()).contains("RCP-000001");
        assertThat(send("POST", "/api/v1/fees/charges/" + chargeId + "/payments", token, payment("5000.00", key1)).body())
                .as("a retry with the same key returns the same receipt").contains("RCP-000001");
        assertThat(ledger(token, student)).contains("\"paid\":5000.00").contains("\"balance\":7000.00").contains("\"status\":\"PARTIAL\"");

        assertThat(send("POST", "/api/v1/fees/charges/" + chargeId + "/payments", token,
                payment("7000.01", UUID.randomUUID().toString())).statusCode()).as("cannot overpay").isEqualTo(400);

        HttpResponse<String> second = send("POST", "/api/v1/fees/charges/" + chargeId + "/payments", token,
                payment("7000.00", UUID.randomUUID().toString()));
        assertThat(second.body()).contains("RCP-000002");
        assertThat(ledger(token, student)).contains("\"status\":\"PAID\"");

        String paymentId = idOf(first.body());
        assertThat(send("POST", "/api/v1/fees/payments/" + paymentId + "/void", token, "{\"reason\":\"Entered by mistake\"}").body())
                .contains("\"voided\":true").contains("RCP-000001");
        assertThat(send("POST", "/api/v1/fees/payments/" + paymentId + "/void", token, "{\"reason\":\"again\"}").statusCode())
                .isEqualTo(409);
        assertThat(ledger(token, student)).contains("\"balance\":5000.00").contains("\"status\":\"PARTIAL\"");
    }

    @Test
    void validatesPayments() throws Exception {
        String token = admin(tenantA);
        String[] cs = classAndSection(token);
        String student = studentInSection(token, "F-1", cs[1]);
        send("POST", "/api/v1/fees/items", token, feeItem(cs[0], "Tuition", "100.00", LocalDate.now(clock).plusDays(5)));
        send("POST", "/api/v1/fees/classes/" + cs[0] + "/generate", token, null);
        String chargeId = idOf(ledger(token, student));
        String path = "/api/v1/fees/charges/" + chargeId + "/payments";

        String upiWithoutReference = "{\"amount\":\"10.00\",\"method\":\"UPI\",\"idempotencyKey\":\"" + UUID.randomUUID() + "\"}";
        assertThat(send("POST", path, token, upiWithoutReference).statusCode()).isEqualTo(400);
        String future = "{\"amount\":\"10.00\",\"method\":\"CASH\",\"paidOn\":\"" + LocalDate.now(clock).plusDays(1)
                + "\",\"idempotencyKey\":\"" + UUID.randomUUID() + "\"}";
        assertThat(send("POST", path, token, future).statusCode()).isEqualTo(400);
        assertThat(send("POST", path, token, payment("0.00", UUID.randomUUID().toString())).statusCode()).isEqualTo(400);
    }

    @Test
    void duesSummaryAndReminders() throws Exception {
        String token = admin(tenantA);
        String[] cs = classAndSection(token);
        String paying = studentInSection(token, "F-1", cs[1]);
        studentInSection(token, "F-2", cs[1]);
        send("POST", "/api/v1/fees/items", token, feeItem(cs[0], "Term 1", "10000.00", LocalDate.now(clock).minusDays(1)));
        send("POST", "/api/v1/fees/classes/" + cs[0] + "/generate", token, null);

        String summary = send("GET", "/api/v1/fees/summary", token, null).body();
        assertThat(summary).contains("\"billed\":20000.00").contains("\"outstanding\":20000.00")
                .contains("\"overdue\":20000.00").contains("\"studentsWithOverdue\":2");
        assertThat(send("GET", "/api/v1/fees/dues?classId=" + cs[0], token, null).body()).contains("F-1").contains("F-2");

        String chargeId = idOf(ledger(token, paying));
        send("POST", "/api/v1/fees/charges/" + chargeId + "/payments", token, payment("10000.00", UUID.randomUUID().toString()));

        assertThat(send("GET", "/api/v1/fees/dues", token, null).body()).doesNotContain("F-1").contains("F-2");
        assertThat(send("GET", "/api/v1/fees/summary", token, null).body())
                .contains("\"collected\":10000.00").contains("\"overdue\":10000.00").contains("\"studentsWithOverdue\":1");

        int before = collector.feeEvents.size();
        assertThat(send("POST", "/api/v1/fees/reminders/run", token, null).body()).contains("\"reminded\":1");
        FeeReminderEvent reminder = collector.feeEvents.get(before);
        assertThat(reminder.overdue()).isTrue();
        assertThat(reminder.totalDue()).isEqualByComparingTo("10000");
        assertThat(reminder.guardianPhone()).isEqualTo("+919876543210");
        assertThat(send("POST", "/api/v1/fees/reminders/run", token, null).body())
                .as("not reminded again within the interval").contains("\"reminded\":0");
    }

    @Test
    void feesAreAdminOnly_parentsSeeOnlyTheirOwnChild_andSchoolsStayApart() throws Exception {
        String token = admin(tenantA);
        String[] cs = classAndSection(token);
        String mine = idOf(send("POST", "/api/v1/students", token,
                studentJson("F-1", cs[1], guardianWithEmail("raj@example.com"))).body());
        String other = idOf(send("POST", "/api/v1/students", token,
                studentJson("F-2", cs[1], guardianWithEmail("someone@example.com"))).body());
        send("POST", "/api/v1/fees/items", token, feeItem(cs[0], "Tuition", "100.00", LocalDate.now(clock).plusDays(5)));
        send("POST", "/api/v1/fees/classes/" + cs[0] + "/generate", token, null);
        String chargeId = idOf(ledger(token, mine));
        UUID parentSub = UUID.randomUUID();
        logins.save(new GuardianLogin(parentSub, tenantA, "raj@example.com"));

        assertThat(send("GET", "/api/v1/fees/summary", teacher(tenantA), null).statusCode()).isEqualTo(403);
        assertThat(send("GET", "/api/v1/fees/summary", parent(tenantA, parentSub), null).statusCode()).isEqualTo(403);
        assertThat(send("POST", "/api/v1/fees/charges/" + chargeId + "/payments", parent(tenantA, parentSub),
                payment("10.00", UUID.randomUUID().toString())).statusCode()).isEqualTo(403);

        assertThat(send("GET", "/api/v1/parent/children/" + mine + "/fees", parent(tenantA, parentSub), null).body())
                .contains("Tuition").contains("\"balance\":100.00");
        assertThat(send("GET", "/api/v1/parent/children/" + other + "/fees", parent(tenantA, parentSub), null).statusCode())
                .isEqualTo(404);

        assertThat(send("POST", "/api/v1/fees/charges/" + chargeId + "/payments", admin(tenantB),
                payment("10.00", UUID.randomUUID().toString())).statusCode()).isEqualTo(404);
        assertThat(send("GET", "/api/v1/fees/students/" + mine, admin(tenantB), null).statusCode()).isEqualTo(404);
    }
}