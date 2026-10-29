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

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.kafka.listener.auto-startup=false")
class AcademicApiIntegrationTest {

    @Container
    @ServiceConnection
    @SuppressWarnings({"rawtypes", "resource"})
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16");

    @TestConfiguration
    static class TestJwtConfig {
        @Bean
        JwtDecoder jwtDecoder() {
            return AcademicApiIntegrationTest::decode;
        }
    }

    /** Test tokens look like "ROLE" or "ROLE.tenantId" (bearer tokens may only use letters, digits and -._~+/). */
    private static Jwt decode(String token) {
        String[] parts = token.split("\\.");
        Jwt.Builder builder = Jwt.withTokenValue(token).header("alg", "none").subject("user-" + parts[0])
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
}