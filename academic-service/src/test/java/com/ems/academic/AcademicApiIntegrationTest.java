package com.ems.academic;

import com.ems.academic.domain.TenantSnapshot;
import com.ems.academic.repository.StaffMemberRepository;
import com.ems.academic.repository.TenantSnapshotRepository;
import com.ems.academic.service.StaffLoginService;
import com.ems.academic.domain.StaffMember;
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
import com.ems.academic.payment.WebhookEvent;
import com.ems.academic.payment.WebhookSigner;
import com.ems.academic.event.UserLoginEvent;

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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"spring.kafka.listener.auto-startup=false", "ems.kafka.publish=false", "ems.scheduling.enabled=false", "ems.payments.webhook-secret=test-secret"})
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
    @Autowired StaffMemberRepository staff;
    @Autowired StaffLoginService staffLogins;

    private final UUID tenantA = UUID.randomUUID();
    private final UUID tenantB = UUID.randomUUID();
    private final UUID suspended = UUID.randomUUID();
    private final UUID teacherSub = UUID.randomUUID();

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
    private String teacher(UUID tenant) { return "TEACHER." + tenant + "." + teacherSub; }

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

    private static final Pattern PROVIDER_REF = Pattern.compile("/pay/simulated/(sim_[a-z0-9]+)");
    private static final Pattern ATTEMPT_ID = Pattern.compile("\"attemptId\":\"([0-9a-f-]{36})\"");

    private record Family(String admin, String parent, String studentId, String chargeA, String chargeB) {}

    /** A student with two fees (A: 100.00 due first, B: 50.00 due later) and a parent who can see them. */
    private Family onlineFamily() throws Exception {
        String admin = admin(tenantA);
        String[] cs = classAndSection(admin);
        String student = idOf(send("POST", "/api/v1/students", admin,
                studentJson("O-1", cs[1], guardianWithEmail("raj@example.com"))).body());
        UUID sub = UUID.randomUUID();
        logins.save(new GuardianLogin(sub, tenantA, "raj@example.com"));
        LocalDate today = LocalDate.now(clock);
        send("POST", "/api/v1/fees/items", admin, feeItem(cs[0], "Tuition", "100.00", today.plusDays(5)));
        send("POST", "/api/v1/fees/items", admin, feeItem(cs[0], "Transport", "50.00", today.plusDays(6)));
        send("POST", "/api/v1/fees/classes/" + cs[0] + "/generate", admin, null);
        String parent = parent(tenantA, sub);
        String ledger = send("GET", "/api/v1/parent/children/" + student + "/fees", parent, null).body();
        return new Family(admin, parent, student, nthId(ledger, 0), nthId(ledger, 1));
    }

    private HttpResponse<String> pay(Family f, String key, String... chargeIds) throws Exception {
        String ids = "\"" + String.join("\",\"", chargeIds) + "\"";
        return send("POST", "/api/v1/parent/children/" + f.studentId() + "/pay", f.parent(),
                "{\"chargeIds\":[" + ids + "],\"idempotencyKey\":\"" + key + "\"}");
    }

    private static String providerRef(String body) {
        Matcher m = PROVIDER_REF.matcher(body);
        assertThat(m.find()).as("checkout url in: " + body).isTrue();
        return m.group(1);
    }

    private static String attemptIdOf(String body) {
        Matcher m = ATTEMPT_ID.matcher(body);
        assertThat(m.find()).as("attemptId in: " + body).isTrue();
        return m.group(1);
    }

    private String attemptStatus(Family f, String attemptId) throws Exception {
        return send("GET", "/api/v1/parent/payment-attempts/" + attemptId, f.parent(), null).body();
    }

    private HttpResponse<String> webhook(String type, String ref, String amount, String signatureOverride) throws Exception {
        WebhookEvent event = new WebhookEvent(UUID.randomUUID().toString(), type, ref, "pay_test", amount, "INR", "");
        String signature = signatureOverride != null ? signatureOverride : WebhookSigner.sign("test-secret", event);
        String form = "eventId=" + enc(event.eventId()) + "&type=" + enc(type) + "&providerRef=" + enc(ref)
                + "&providerPaymentId=pay_test&amount=" + enc(amount) + "&currency=INR&failureReason=";
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/payments/webhook"))
                .header("Content-Type", "application/x-www-form-urlencoded").header("X-Signature", signature)
                .POST(BodyPublishers.ofString(form)).build();
        return CLIENT.send(request, BodyHandlers.ofString());
    }

    private static String enc(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8); }

    private static List<String> valuesOf(String body, String key) {
        Matcher m = Pattern.compile("\"" + key + "\":\"([0-9a-f-]{36})\"").matcher(body);
        List<String> found = new ArrayList<>();
        while (m.find()) found.add(m.group(1));
        return found;
    }

    private String subject(String token, String name) throws Exception {
        return idOf(send("POST", "/api/v1/subjects", token, "{\"name\":\"" + name + "\"}").body());
    }

    /** triples of studentId, examSubjectId, value where value is "85.5", "AB" (absent) or "" (clear). */
    private static String marksBody(String... t) {
        StringBuilder sb = new StringBuilder("{\"entries\":[");
        for (int i = 0; i < t.length; i += 3) {
            if (i > 0) sb.append(',');
            sb.append("{\"studentId\":\"").append(t[i]).append("\",\"examSubjectId\":\"").append(t[i + 1]).append("\",");
            if (t[i + 2].equals("AB")) sb.append("\"absent\":true");
            else if (t[i + 2].isEmpty()) sb.append("\"marks\":null,\"absent\":false");
            else sb.append("\"marks\":\"").append(t[i + 2]).append("\"");
            sb.append('}');
        }
        return sb.append("]}").toString();
    }

    private record ExamSetup(String admin, String classId, String sectionId, String examId, String english, String maths,
                             String englishSubject, String mathsSubject) {}

    private ExamSetup examSetup(UUID tenant) throws Exception {
        String token = admin(tenant);
        String[] cs = classAndSection(token);
        String eng = subject(token, "English");
        String math = subject(token, "Mathematics");
        String body = "{\"classId\":\"" + cs[0] + "\",\"name\":\"Term 1\",\"subjects\":["
                + "{\"subjectId\":\"" + math + "\",\"maxMarks\":\"100\",\"passMarks\":\"35\"},"
                + "{\"subjectId\":\"" + eng + "\",\"maxMarks\":\"50\",\"passMarks\":\"18\"}]}";
        HttpResponse<String> created = send("POST", "/api/v1/exams", token, body);
        assertThat(created.statusCode()).isEqualTo(201);
        List<String> examSubjects = valuesOf(created.body(), "examSubjectId");   // English first, then Mathematics
        return new ExamSetup(token, cs[0], cs[1], idOf(created.body()), examSubjects.get(0), examSubjects.get(1), eng, math);
    }

    /** Makes the test teacher a staff member of tenant A and returns their staff id. */
    private UUID teacherStaffId() {
        return staff.findByKeycloakId(teacherSub).orElseGet(() -> staff.save(new StaffMember(tenantA, teacherSub,
                "teacher-" + teacherSub.toString().substring(0, 8) + "@example.com", "Test", "Teacher"))).getId();
    }

    private void assign(UUID staffId, String sectionId, String subjectId, boolean classTeacher, int expectedStatus) throws Exception {
        String body = "{\"staffId\":\"" + staffId + "\",\"sectionId\":\"" + sectionId + "\","
                + (subjectId == null ? "" : "\"subjectId\":\"" + subjectId + "\",") + "\"classTeacher\":" + classTeacher + "}";
        assertThat(send("POST", "/api/v1/assignments", admin(tenantA), body).statusCode()).isEqualTo(expectedStatus);
    }

    private void classTeacherOf(String sectionId) throws Exception { assign(teacherStaffId(), sectionId, null, true, 201); }

    private void teachesSubject(String sectionId, String subjectId) throws Exception {
        assign(teacherStaffId(), sectionId, subjectId, false, 201);
    }

    private String[] classWithTwoSections(String token) throws Exception {
        String yearId = idOf(send("POST", "/api/v1/academic-years", token, year("2026-27", "2026-04-01", "2027-03-31")).body());
        String body = "{\"academicYearId\":\"" + yearId + "\",\"name\":\"Class 8\",\"gradeLevel\":8,\"sections\":[\"A\",\"B\"]}";
        String created = send("POST", "/api/v1/classes", token, body).body();
        return new String[]{nthId(created, 0), nthId(created, 1), nthId(created, 2)};   // class, section A, section B
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
        classTeacherOf(sectionA);

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
        String section = classWithSections(admin(tenantA));
        classTeacherOf(section);
        String studentId = studentInSection(admin(tenantA), "A-1", section);

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
        classTeacherOf(section);
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
        classTeacherOf(section);
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
        classTeacherOf(section);
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

    @Test
    void onlinePayment_success_createsOneReceiptPerFee_andIsReplaySafe() throws Exception {
        Family f = onlineFamily();
        String key = UUID.randomUUID().toString();

        HttpResponse<String> started = pay(f, key, f.chargeA(), f.chargeB());
        assertThat(started.statusCode()).isEqualTo(201);
        String ref = providerRef(started.body());
        String attemptId = attemptIdOf(started.body());
        assertThat(attemptIdOf(pay(f, key, f.chargeA(), f.chargeB()).body()))
                .as("the same request key returns the same checkout").isEqualTo(attemptId);
        assertThat(attemptStatus(f, attemptId)).contains("\"status\":\"PENDING\"").contains("\"amount\":150.00");
        assertThat(pay(f, UUID.randomUUID().toString(), f.chargeA()).statusCode())
                .as("one payment in flight per fee").isEqualTo(409);

        assertThat(send("POST", "/api/v1/payments/simulated/" + ref + "/complete?outcome=success", f.parent(), null)
                .statusCode()).isEqualTo(200);
        assertThat(attemptStatus(f, attemptId)).contains("\"status\":\"SUCCEEDED\"");

        String ledger = send("GET", "/api/v1/fees/students/" + f.studentId(), f.admin(), null).body();
        assertThat(ledger).contains("\"balance\":0.00");
        assertThat(count(ledger, "\"method\":\"ONLINE\"")).isEqualTo(2);

        assertThat(webhook(WebhookEvent.SUCCEEDED, ref, "150.00", null).statusCode()).isEqualTo(200);
        assertThat(count(send("GET", "/api/v1/fees/students/" + f.studentId(), f.admin(), null).body(),
                "\"method\":\"ONLINE\"")).as("a replayed webhook adds nothing").isEqualTo(2);
    }

    @Test
    void aFailedPaymentChangesNothing_andCanBeRetried() throws Exception {
        Family f = onlineFamily();
        HttpResponse<String> started = pay(f, UUID.randomUUID().toString(), f.chargeA(), f.chargeB());
        String ref = providerRef(started.body());

        send("POST", "/api/v1/payments/simulated/" + ref + "/complete?outcome=failure", f.parent(), null);

        assertThat(attemptStatus(f, attemptIdOf(started.body()))).contains("\"status\":\"FAILED\"");
        assertThat(send("GET", "/api/v1/fees/students/" + f.studentId(), f.admin(), null).body())
                .contains("\"balance\":150.00").doesNotContain("ONLINE");
        assertThat(pay(f, UUID.randomUUID().toString(), f.chargeA(), f.chargeB()).statusCode())
                .as("a failed attempt no longer blocks a new one").isEqualTo(201);
    }

    @Test
    void theServerGuardsWhoCanPayForWhat() throws Exception {
        Family f = onlineFamily();
        String body = "{\"chargeIds\":[\"" + f.chargeA() + "\"],\"idempotencyKey\":\"" + UUID.randomUUID() + "\"}";

        assertThat(send("POST", "/api/v1/parent/children/" + f.studentId() + "/pay", f.admin(), body).statusCode())
                .as("only parents start payments").isEqualTo(403);
        assertThat(send("POST", "/api/v1/parent/children/" + UUID.randomUUID() + "/pay", f.parent(), body).statusCode())
                .as("not their child").isEqualTo(404);

        HttpResponse<String> started = pay(f, UUID.randomUUID().toString(), f.chargeA());
        String ref = providerRef(started.body());
        String stranger = parent(tenantA, UUID.randomUUID());
        assertThat(send("POST", "/api/v1/payments/simulated/" + ref + "/complete?outcome=success", stranger, null)
                .statusCode()).as("another parent cannot complete or read it").isEqualTo(404);
        assertThat(send("GET", "/api/v1/parent/payment-attempts/" + attemptIdOf(started.body()), stranger, null)
                .statusCode()).isEqualTo(404);

        send("POST", "/api/v1/payments/simulated/" + ref + "/complete?outcome=success", f.parent(), null);
        assertThat(pay(f, UUID.randomUUID().toString(), f.chargeA()).statusCode())
                .as("an already-paid fee cannot be paid again").isEqualTo(400);
    }

    @Test
    void theWebhookTrustsOnlyGenuinelySignedEvents() throws Exception {
        Family f = onlineFamily();
        HttpResponse<String> started = pay(f, UUID.randomUUID().toString(), f.chargeA(), f.chargeB());
        String ref = providerRef(started.body());
        String attemptId = attemptIdOf(started.body());

        assertThat(webhook(WebhookEvent.SUCCEEDED, ref, "150.00", "deadbeef").statusCode()).isEqualTo(401);
        assertThat(webhook(WebhookEvent.SUCCEEDED, "sim_unknown", "150.00", null).statusCode()).isEqualTo(404);
        assertThat(attemptStatus(f, attemptId)).contains("\"status\":\"PENDING\"");

        // genuinely signed, but not the amount we expected: held for review, nothing applied
        assertThat(webhook(WebhookEvent.SUCCEEDED, ref, "149.00", null).statusCode()).isEqualTo(200);
        assertThat(attemptStatus(f, attemptId)).contains("\"status\":\"REVIEW\"");
        assertThat(send("GET", "/api/v1/fees/students/" + f.studentId(), f.admin(), null).body())
                .contains("\"balance\":150.00").doesNotContain("ONLINE");
    }

    @Test
    void aPaymentForAFeePaidInCashMeanwhileIsHeldForReview() throws Exception {
        Family f = onlineFamily();
        HttpResponse<String> started = pay(f, UUID.randomUUID().toString(), f.chargeA());   // fee A, 100.00
        String ref = providerRef(started.body());

        send("POST", "/api/v1/fees/charges/" + f.chargeA() + "/payments", f.admin(), payment("100.00", UUID.randomUUID().toString()));
        assertThat(webhook(WebhookEvent.SUCCEEDED, ref, "100.00", null).statusCode()).isEqualTo(200);

        assertThat(attemptStatus(f, attemptIdOf(started.body()))).contains("\"status\":\"REVIEW\"");
        assertThat(send("GET", "/api/v1/fees/students/" + f.studentId(), f.admin(), null).body()).doesNotContain("ONLINE");

        String list = send("GET", "/api/v1/fees/online-payments", f.admin(), null).body();
        assertThat(list).contains("REVIEW").contains("O-1").contains("refund");
        assertThat(send("GET", "/api/v1/fees/online-payments", teacher(tenantA), null).statusCode()).isEqualTo(403);
        assertThat(send("GET", "/api/v1/fees/online-payments", f.parent(), null).statusCode()).isEqualTo(403);
        assertThat(send("GET", "/api/v1/fees/online-payments", admin(tenantB), null).body()).isEqualTo("[]");
    }

    @Test
    void gradingScale_defaultsUntilTheSchoolSavesItsOwn() throws Exception {
        String token = admin(tenantA);

        String defaults = send("GET", "/api/v1/grading-scale", teacher(tenantA), null).body();
        assertThat(count(defaults, "\"grade\":\"")).isEqualTo(7);
        assertThat(defaults).contains("\"grade\":\"A+\"");

        String custom = "{\"bands\":[{\"minPercent\":\"0\",\"grade\":\"Fail\",\"label\":\"Below 50\"},{\"minPercent\":\"50\",\"grade\":\"Pass\"}]}";
        assertThat(send("PUT", "/api/v1/grading-scale", token, custom).statusCode()).isEqualTo(200);
        String saved = send("GET", "/api/v1/grading-scale", token, null).body();
        assertThat(saved).contains("\"grade\":\"Pass\"").doesNotContain("A+");

        assertThat(send("PUT", "/api/v1/grading-scale", token, "{\"bands\":[{\"minPercent\":\"50\",\"grade\":\"Pass\"}]}").statusCode())
                .as("the lowest grade must start at 0").isEqualTo(400);
        assertThat(send("PUT", "/api/v1/grading-scale", teacher(tenantA), custom).statusCode()).isEqualTo(403);
        assertThat(send("GET", "/api/v1/grading-scale", admin(tenantB), null).body()).as("other schools keep the default").contains("A+");
    }

    @Test
    void subjects_areUniquePerSchool_andProtectedWhileInUse() throws Exception {
        ExamSetup s = examSetup(tenantA);

        assertThat(send("POST", "/api/v1/subjects", s.admin(), "{\"name\":\"english\"}").statusCode()).isEqualTo(409);
        assertThat(send("POST", "/api/v1/subjects", teacher(tenantA), "{\"name\":\"Science\"}").statusCode()).isEqualTo(403);

        String science = subject(s.admin(), "Science");
        assertThat(send("DELETE", "/api/v1/subjects/" + science, s.admin(), null).statusCode()).isEqualTo(204);
        String english = valuesOf(send("GET", "/api/v1/subjects", s.admin(), null).body(), "id").get(0);
        assertThat(send("DELETE", "/api/v1/subjects/" + english, s.admin(), null).statusCode()).as("used by an exam").isEqualTo(409);
    }

    @Test
    void examLifecycle_enter_publish_lock_results_reopen_audit() throws Exception {
        ExamSetup s = examSetup(tenantA);
        classTeacherOf(s.sectionId());
        String one = studentInSection(s.admin(), "E-1", s.sectionId());
        String two = studentInSection(s.admin(), "E-2", s.sectionId());
        String path = "/api/v1/exams/" + s.examId();

        assertThat(send("POST", "/api/v1/exams", s.admin(), "{\"classId\":\"" + s.classId() + "\",\"name\":\"term 1\",\"subjects\":[]}").statusCode())
                .isEqualTo(400);

        String teacher = teacher(tenantA);
        assertThat(send("PUT", path + "/marks", teacher, marksBody(one, s.maths(), "80")).statusCode()).isEqualTo(200);
        assertThat(send("POST", path + "/publish", s.admin(), null).statusCode()).as("marks are missing").isEqualTo(400);

        assertThat(send("PUT", path + "/marks", teacher,
                marksBody(one, s.english(), "40", two, s.english(), "10", two, s.maths(), "90")).body()).contains("\"changed\":3");
        assertThat(send("POST", path + "/publish", teacher, null).statusCode()).as("only admins publish").isEqualTo(403);
        HttpResponse<String> published = send("POST", path + "/publish", s.admin(), null);
        assertThat(published.statusCode()).isEqualTo(200);
        assertThat(published.body()).contains("\"status\":\"PUBLISHED\"");

        assertThat(send("PUT", path + "/marks", teacher, marksBody(one, s.maths(), "81")).statusCode()).as("locked once published").isEqualTo(400);

        String card = send("GET", path + "/report-card/" + one, s.admin(), null).body();
        assertThat(card).contains("\"totalMarks\":120.00").contains("\"totalMax\":150.00").contains("\"percent\":80.00")
                .contains("\"grade\":\"A\"").contains("\"result\":\"PASS\"");
        String failing = send("GET", path + "/report-card/" + two, s.admin(), null).body();
        assertThat(failing).contains("\"percent\":66.67").contains("\"grade\":\"B\"").contains("\"result\":\"FAIL\"");
        assertThat(send("GET", path + "/results", teacher, null).body()).contains("\"result\":\"PASS\"").contains("\"result\":\"FAIL\"");

        assertThat(send("POST", path + "/reopen", s.admin(), "{\"reason\":\"Wrong English mark\"}").body()).contains("\"status\":\"DRAFT\"");
        assertThat(send("PUT", path + "/marks", teacher, marksBody(two, s.english(), "20")).body()).contains("\"changed\":1");
        assertThat(send("PUT", path + "/marks", teacher, marksBody(two, s.english(), "20")).body()).as("no change, no audit").contains("\"changed\":0");

        String audit = send("GET", path + "/audit", s.admin(), null).body();
        assertThat(audit).contains("PUBLISH").contains("REOPEN").contains("Wrong English mark").contains("10.00 → 20.00");
        assertThat(send("GET", path + "/audit", teacher, null).statusCode()).as("the audit trail is for admins").isEqualTo(403);
        assertThat(send("POST", path + "/publish", s.admin(), null).statusCode()).isEqualTo(200);
        assertThat(send("DELETE", path, s.admin(), null).statusCode()).as("published exams cannot be deleted").isEqualTo(400);
    }

    @Test
    void marksAreValidatedStrictly_andCanBeClearedAgain() throws Exception {
        ExamSetup s = examSetup(tenantA);
        classTeacherOf(s.sectionId());
        String student = studentInSection(s.admin(), "E-1", s.sectionId());
        String path = "/api/v1/exams/" + s.examId() + "/marks";
        String teacher = teacher(tenantA);

        assertThat(send("PUT", path, teacher, marksBody(student, s.maths(), "100.01")).statusCode()).as("above the maximum").isEqualTo(400);
        assertThat(send("PUT", path, teacher, marksBody(student, s.maths(), "-1")).statusCode()).isEqualTo(400);
        assertThat(send("PUT", path, teacher, marksBody(student, s.maths(), "85.555")).statusCode()).isEqualTo(400);
        assertThat(send("PUT", path, teacher, "{\"entries\":[{\"studentId\":\"" + student + "\",\"examSubjectId\":\"" + s.maths()
                + "\",\"marks\":\"5\",\"absent\":true}]}").statusCode()).as("absent with marks").isEqualTo(400);
        assertThat(send("PUT", path, teacher, marksBody(UUID.randomUUID().toString(), s.maths(), "50")).statusCode()).as("not in this class").isEqualTo(400);
        assertThat(send("PUT", path, teacher, marksBody(student, UUID.randomUUID().toString(), "50")).statusCode()).as("not in this exam").isEqualTo(400);
        assertThat(send("PUT", path, teacher, marksBody(student, s.maths(), "50", student, s.maths(), "60")).statusCode()).as("duplicate entry").isEqualTo(400);

        assertThat(send("PUT", path, teacher, marksBody(student, s.maths(), "85.5")).statusCode()).isEqualTo(200);
        assertThat(send("GET", path, teacher, null).body()).contains("\"marks\":85.50");
        assertThat(send("PUT", path, teacher, marksBody(student, s.maths(), "AB")).statusCode()).isEqualTo(200);
        assertThat(send("GET", path, teacher, null).body()).contains("\"absent\":true");
        assertThat(send("PUT", path, teacher, marksBody(student, s.maths(), "")).statusCode()).isEqualTo(200);
        assertThat(send("GET", path, teacher, null).body()).doesNotContain("\"marks\":85").doesNotContain("\"absent\":true");
    }

    @Test
    void parentsSeeOnlyPublishedResultsOfTheirOwnChild() throws Exception {
        ExamSetup s = examSetup(tenantA);
        classTeacherOf(s.sectionId());
        String mine = idOf(send("POST", "/api/v1/students", s.admin(),
                studentJson("P-1", s.sectionId(), guardianWithEmail("raj@example.com"))).body());
        String other = idOf(send("POST", "/api/v1/students", s.admin(),
                studentJson("P-2", s.sectionId(), guardianWithEmail("someone@example.com"))).body());
        UUID sub = UUID.randomUUID();
        logins.save(new GuardianLogin(sub, tenantA, "raj@example.com"));
        String parent = parent(tenantA, sub);
        String path = "/api/v1/exams/" + s.examId();

        send("PUT", path + "/marks", teacher(tenantA), marksBody(mine, s.english(), "40", mine, s.maths(), "80",
                other, s.english(), "30", other, s.maths(), "70"));
        assertThat(send("GET", "/api/v1/parent/children/" + mine + "/results", parent, null).body()).as("drafts are hidden").isEqualTo("[]");
        assertThat(send("GET", "/api/v1/parent/children/" + mine + "/results/" + s.examId(), parent, null).statusCode()).isEqualTo(404);

        send("POST", path + "/publish", s.admin(), null);
        String list = send("GET", "/api/v1/parent/children/" + mine + "/results", parent, null).body();
        assertThat(list).contains("Term 1").contains("\"grade\":\"A\"").contains("\"result\":\"PASS\"");
        String card = send("GET", "/api/v1/parent/children/" + mine + "/results/" + s.examId(), parent, null).body();
        assertThat(card).contains("\"percent\":80.00").contains("English").contains("Mathematics");

        assertThat(send("GET", "/api/v1/parent/children/" + other + "/results", parent, null).statusCode()).as("not their child").isEqualTo(404);
        assertThat(send("GET", "/api/v1/exams?classId=" + s.classId(), parent, null).statusCode()).isEqualTo(403);
        assertThat(send("GET", "/api/v1/subjects", parent, null).statusCode()).isEqualTo(403);
        assertThat(send("PUT", path + "/marks", parent, marksBody(mine, s.maths(), "100")).statusCode()).isEqualTo(403);
        assertThat(send("GET", "/api/v1/parent/children/" + mine + "/results", teacher(tenantA), null).statusCode()).isEqualTo(403);
    }

    @Test
    void examsStayWithinTheirSchool() throws Exception {
        ExamSetup s = examSetup(tenantA);
        String student = studentInSection(s.admin(), "E-1", s.sectionId());
        String other = admin(tenantB);
        String path = "/api/v1/exams/" + s.examId();

        assertThat(send("GET", "/api/v1/subjects", other, null).body()).isEqualTo("[]");
        assertThat(send("GET", "/api/v1/exams?classId=" + s.classId(), other, null).statusCode()).isEqualTo(404);
        assertThat(send("GET", path, other, null).statusCode()).isEqualTo(404);
        assertThat(send("PUT", path + "/marks", other, marksBody(student, s.maths(), "50")).statusCode()).isEqualTo(404);
        assertThat(send("GET", path + "/report-card/" + student, other, null).statusCode()).isEqualTo(404);
        assertThat(send("POST", path + "/publish", other, null).statusCode()).isEqualTo(404);
    }

    @Test
    void staffAppearFromLoginEvents_andAssignmentsFollowTheRules() throws Exception {
        String admin = admin(tenantA);
        String[] c = classWithTwoSections(admin);
        UUID second = UUID.randomUUID();
        UserLoginEvent event = new UserLoginEvent(UUID.randomUUID(), UserLoginEvent.TEACHER_LOGIN_CREATED, tenantA,
                "Meena@School.com", teacherSub, "Meena", "Gupta", java.time.Instant.now());
        staffLogins.apply(event);
        staffLogins.apply(event);
        staffLogins.apply(new UserLoginEvent(UUID.randomUUID(), UserLoginEvent.TEACHER_LOGIN_CREATED, tenantA,
                "ravi@school.com", second, "Ravi", "Singh", java.time.Instant.now()));
        staffLogins.apply(new UserLoginEvent(UUID.randomUUID(), UserLoginEvent.TEACHER_LOGIN_CREATED, tenantB,
                "meena@elsewhere.com", teacherSub, "Meena", "Gupta", java.time.Instant.now()));   // never moves schools

        String list = send("GET", "/api/v1/staff", admin, null).body();
        assertThat(count(list, "meena@school.com")).as("a repeated event creates one record").isEqualTo(1);
        assertThat(list).contains("ravi@school.com").doesNotContain("elsewhere");
        assertThat(send("GET", "/api/v1/staff", admin(tenantB), null).body()).isEqualTo("[]");
        assertThat(send("GET", "/api/v1/staff", teacher(tenantA), null).statusCode()).as("admin only").isEqualTo(403);

        UUID meena = staff.findByKeycloakId(teacherSub).orElseThrow().getId();
        UUID ravi = staff.findByKeycloakId(second).orElseThrow().getId();
        String english = subject(admin, "English");

        assign(meena, c[1], null, true, 201);
        assign(ravi, c[1], null, true, 409);                  // one class teacher per section
        assign(ravi, c[2], null, true, 201);
        assign(meena, c[1], null, false, 400);                // a subject teacher needs a subject
        assign(meena, c[1], english, true, 400);              // a class teacher has no subject
        assign(meena, c[1], english, false, 201);
        assign(meena, c[1], english, false, 409);             // no duplicates
        assign(meena, UUID.randomUUID().toString(), null, true, 404);

        String bodyForOtherSchool = "{\"staffId\":\"" + meena + "\",\"sectionId\":\"" + c[2] + "\",\"classTeacher\":true}";
        assertThat(send("POST", "/api/v1/assignments", admin(tenantB), bodyForOtherSchool).statusCode()).isEqualTo(404);
        assertThat(send("POST", "/api/v1/assignments", teacher(tenantA), bodyForOtherSchool).statusCode()).isEqualTo(403);

        String assignmentId = idOf(send("GET", "/api/v1/staff", admin, null).body().split("ravi@school.com")[1]);
        assertThat(send("DELETE", "/api/v1/assignments/" + assignmentId, teacher(tenantA), null).statusCode()).isEqualTo(403);
        assertThat(send("DELETE", "/api/v1/assignments/" + assignmentId, admin, null).statusCode()).isEqualTo(204);
        assign(meena, c[2], null, true, 201);                 // the freed section can have a class teacher again

        assertThat(send("DELETE", "/api/v1/subjects/" + english, admin, null).statusCode())
                .as("a subject that is assigned cannot be deleted").isEqualTo(409);
    }

    @Test
    void aTeacherSeesAndEditsOnlyTheirOwnSections() throws Exception {
        String admin = admin(tenantA);
        String[] c = classWithTwoSections(admin);
        String inA = studentInSection(admin, "T-A", c[1]);
        String inB = studentInSection(admin, "T-B", c[2]);
        classTeacherOf(c[1]);
        String teacher = teacher(tenantA);

        String mine = send("GET", "/api/v1/students", teacher, null).body();
        assertThat(mine).contains("T-A").doesNotContain("T-B");
        assertThat(send("GET", "/api/v1/students?search=T-B", teacher, null).body()).contains("\"totalElements\":0");
        assertThat(send("GET", "/api/v1/students?sectionId=" + c[2], teacher, null).statusCode()).isEqualTo(403);
        assertThat(send("GET", "/api/v1/students?unassigned=true", teacher, null).statusCode()).isEqualTo(403);
        assertThat(send("GET", "/api/v1/students/" + inA, teacher, null).statusCode()).isEqualTo(200);
        assertThat(send("GET", "/api/v1/students/" + inB, teacher, null).statusCode()).as("looks like it does not exist").isEqualTo(404);
        assertThat(send("GET", "/api/v1/students", admin, null).body()).contains("T-A").contains("T-B");

        assertThat(send("GET", "/api/v1/attendance/sections/" + c[1], teacher, null).statusCode()).isEqualTo(200);
        assertThat(send("GET", "/api/v1/attendance/sections/" + c[2], teacher, null).statusCode()).isEqualTo(403);
        assertThat(send("PUT", "/api/v1/attendance/sections/" + c[2], teacher, marks(inB, "ABSENT")).statusCode()).isEqualTo(403);
        assertThat(send("PUT", "/api/v1/attendance/sections/" + c[2], admin, marks(inB, "ABSENT")).statusCode()).isEqualTo(200);

        String teaching = send("GET", "/api/v1/me/teaching", teacher, null).body();
        assertThat(teaching).contains("\"admin\":false").contains("\"sectionName\":\"A\"").doesNotContain("\"sectionName\":\"B\"");
        assertThat(send("GET", "/api/v1/me/teaching", admin, null).body()).contains("\"admin\":true");

        String unassigned = "TEACHER." + tenantA + "." + UUID.randomUUID();
        assertThat(send("GET", "/api/v1/students", unassigned, null).body()).contains("\"totalElements\":0");
        assertThat(send("GET", "/api/v1/attendance/sections/" + c[1], unassigned, null).statusCode()).isEqualTo(403);
        assertThat(send("GET", "/api/v1/me/teaching", unassigned, null).body()).contains("\"sections\":[]");
    }

    @Test
    void aDeactivatedTeacherLosesAccessAtOnce() throws Exception {
        String admin = admin(tenantA);
        String section = classWithSections(admin);
        studentInSection(admin, "D-1", section);
        classTeacherOf(section);
        UUID staffId = teacherStaffId();
        assertThat(send("GET", "/api/v1/students", teacher(tenantA), null).body()).contains("D-1");

        assertThat(send("PUT", "/api/v1/staff/" + staffId, admin, "{\"designation\":\"Maths\",\"active\":false}").body())
                .contains("\"active\":false").contains("Maths");

        assertThat(send("GET", "/api/v1/students", teacher(tenantA), null).body()).contains("\"totalElements\":0");
        assign(staffId, section, null, true, 400);   // an inactive teacher cannot be given new work
    }

    @Test
    void marksFollowSubjectAssignments() throws Exception {
        ExamSetup s = examSetup(tenantA);
        String student = studentInSection(s.admin(), "M-1", s.sectionId());
        String path = "/api/v1/exams/" + s.examId() + "/marks";
        String teacher = teacher(tenantA);

        assertThat(send("PUT", path, teacher, marksBody(student, s.english(), "40")).statusCode()).as("no assignment yet").isEqualTo(403);
        assertThat(send("GET", path, teacher, null).body()).as("sees no students yet").doesNotContain("M-1");

        teachesSubject(s.sectionId(), s.englishSubject());
        assertThat(send("PUT", path, teacher, marksBody(student, s.english(), "40")).statusCode()).isEqualTo(200);
        assertThat(send("PUT", path, teacher, marksBody(student, s.maths(), "80")).statusCode()).as("not their subject").isEqualTo(403);
        assertThat(send("PUT", path, teacher, marksBody(student, s.english(), "41", student, s.maths(), "80")).statusCode())
                .as("one forbidden mark rejects the whole request").isEqualTo(403);
        String grid = send("GET", path, teacher, null).body();
        assertThat(grid).contains("M-1").contains("\"editableSubjectIds\":[\"" + s.english() + "\"]").contains("\"marks\":40.00");
        assertThat(send("GET", "/api/v1/exams/" + s.examId() + "/report-card/" + student, teacher, null).statusCode()).isEqualTo(200);

        classTeacherOf(s.sectionId());
        assertThat(send("PUT", path, teacher, marksBody(student, s.maths(), "80")).statusCode()).as("class teachers may enter any subject").isEqualTo(200);
        assertThat(send("PUT", path, s.admin(), marksBody(student, s.maths(), "81")).statusCode()).isEqualTo(200);
    }

    @Test
    void teachersCannotSeeResultsOrReportCardsOfOtherSections() throws Exception {
        String admin = admin(tenantA);
        String[] c = classWithTwoSections(admin);
        String eng = subject(admin, "English");
        String body = "{\"classId\":\"" + c[0] + "\",\"name\":\"Term 1\",\"subjects\":[{\"subjectId\":\"" + eng + "\",\"maxMarks\":\"50\",\"passMarks\":\"18\"}]}";
        String examId = idOf(send("POST", "/api/v1/exams", admin, body).body());
        String inA = studentInSection(admin, "R-A", c[1]);
        String inB = studentInSection(admin, "R-B", c[2]);
        classTeacherOf(c[1]);
        String teacher = teacher(tenantA);
        String path = "/api/v1/exams/" + examId;

        assertThat(send("GET", path + "/results", teacher, null).body()).contains("R-A").doesNotContain("R-B");
        assertThat(send("GET", path + "/results?sectionId=" + c[2], teacher, null).statusCode()).isEqualTo(403);
        assertThat(send("GET", path + "/report-card/" + inA, teacher, null).statusCode()).isEqualTo(200);
        assertThat(send("GET", path + "/report-card/" + inB, teacher, null).statusCode()).isEqualTo(404);
        assertThat(send("GET", path + "/marks", teacher, null).body()).contains("R-A").doesNotContain("R-B");
    }
}