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
}