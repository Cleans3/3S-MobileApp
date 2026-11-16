package shelftap.server;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.testcontainers.postgresql.PostgreSQLContainer;
import shelftap.server.member.Member;
import shelftap.server.member.MemberRepository;
import shelftap.server.member.Staff;
import shelftap.server.member.StaffRepository;
import shelftap.server.security.Passwords;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;


/**
 * Full application against a real PostgreSQL (Testcontainers, one container for the whole test JVM) driven through MockMvc.
 * Keys are random per run, so no secret is committed.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(IntegrationTest.ClockConfig.class)
abstract class IntegrationTest {
    static final SecureRandom RANDOM = new SecureRandom();
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");
    static final String JWT_KEY = randomKey();
    static final String MASTER_KEY = randomKey();
    static final String GATE_KEY = randomKey();
    static final String PASSWORD = "correct horse battery";

    static {
        POSTGRES.start();
    }

    @TestConfiguration
    static class ClockConfig {
        @Bean
        @Primary
        MutableClock testClock() {
            return new MutableClock();
        }
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("shelftap.jwt.key", () -> JWT_KEY);
        registry.add("shelftap.master.key", () -> MASTER_KEY);
        registry.add("shelftap.gate.key", () -> GATE_KEY);
    }

    @Autowired
    MockMvc mvc;
    @Autowired
    JsonMapper json;
    @Autowired
    JdbcClient jdbc;
    @Autowired
    Passwords passwords;
    @Autowired
    MemberRepository members;
    @Autowired
    StaffRepository staffRepository;
    @Autowired
    MutableClock clock;

    static String randomKey() {
        byte[] key = new byte[32];
        RANDOM.nextBytes(key);
        return Base64.getEncoder().encodeToString(key);
    }

    static String randomHex(int bytes) {
        byte[] b = new byte[bytes];
        RANDOM.nextBytes(b);
        return HexFormat.of().withUpperCase().formatHex(b);
    }

    // --- http ---

    record Res(int status, JsonNode body, MockHttpServletResponse raw) {
        String code() {
            return body.path("code").asText();
        }

        String header(String name) {
            return raw.getHeader(name);
        }
    }

    Res call(MockHttpServletRequestBuilder request) throws Exception {
        MockHttpServletResponse response = mvc.perform(request).andReturn().getResponse();
        String content = response.getContentAsString(StandardCharsets.UTF_8);
        return new Res(response.getStatus(), content.isBlank() ? json.createObjectNode() : json.readTree(content), response);
    }

    static MockHttpServletRequestBuilder withToken(MockHttpServletRequestBuilder request, String token) {
        return token == null ? request : request.header("Authorization", "Bearer " + token);
    }

    Res get(String path, String token) throws Exception {
        return call(withToken(MockMvcRequestBuilders.get(path), token));
    }

    Res post(String path, Object body, String token) throws Exception {
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders.post(path).contentType(MediaType.APPLICATION_JSON);
        if (body != null) {
            request.content(json.writeValueAsString(body));
        }
        return call(withToken(request, token));
    }

    Res put(String path, Object body, String token) throws Exception {
        return call(withToken(MockMvcRequestBuilders.put(path).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(body)), token));
    }

    Res delete(String path, String token) throws Exception {
        return call(withToken(MockMvcRequestBuilders.delete(path), token));
    }

    // --- fixtures ---

    record Account(UUID id, String email, String password) {
    }

    record Session(String access, String refresh, UUID device, JsonNode pair) {
    }

    Account newMember() {
        return newMember(false, Member.Status.ACTIVE);
    }

    Account newMember(boolean mustChangePassword, Member.Status status) {
        Member m = new Member();
        m.fullName = "Test Member";
        m.email = "m-" + randomHex(6).toLowerCase() + "@test.example";
        m.dateOfBirth = LocalDate.of(2000, 1, 1);
        m.cccd = String.valueOf(100_000_000_000L + Math.abs(RANDOM.nextLong() % 899_999_999_999L));
        m.phone = "0900000000";
        m.memberType = Member.Type.PUBLIC;
        m.status = status;
        m.passwordHash = passwords.hash(PASSWORD);
        m.mustChangePassword = mustChangePassword;
        m.historyEnabled = true;
        m.createdAt = clock.instant();
        members.save(m);
        return new Account(m.id, m.email, PASSWORD);
    }

    Account newStaff(Staff.Role role) {
        Staff s = new Staff();
        s.username = "staff-" + randomHex(6).toLowerCase();
        s.passwordHash = passwords.hash(PASSWORD);
        s.role = role;
        s.active = true;
        s.createdAt = clock.instant();
        staffRepository.save(s);
        return new Account(s.id, s.username, PASSWORD);
    }

    Session login(Account member) throws Exception {
        UUID device = UUID.randomUUID();
        Res res = post("/api/auth/login", java.util.Map.of("email", member.email(), "password", member.password(),
                "deviceId", device.toString()), null);
        if (res.status() != 200) {
            throw new AssertionError("login failed: " + res.status() + " " + res.body());
        }
        return new Session(res.body().get("accessToken").asString(), res.body().get("refreshToken").asString(), device, res.body());
    }

    Session staffLogin(Account staff) throws Exception {
        UUID device = UUID.randomUUID();
        Res res = post("/api/staff/login", java.util.Map.of("username", staff.email(), "password", staff.password(),
                "deviceId", device.toString()), null);
        if (res.status() != 200) {
            throw new AssertionError("staff login failed: " + res.status() + " " + res.body());
        }
        return new Session(res.body().get("accessToken").asString(), res.body().get("refreshToken").asString(), device, res.body());
    }

    record TitleFixture(UUID titleId, String title, List<String> tagIds, List<UUID> copyIds) {
    }

    TitleFixture newTitle(int copies) {
        return newTitle("Title " + randomHex(4), copies);
    }

    TitleFixture newTitle(String name, int copies) {
        UUID titleId = UUID.randomUUID();
        String isbn = "978" + String.format("%010d", Math.abs(RANDOM.nextLong() % 10_000_000_000L));
        jdbc.sql("insert into title (id, isbn, title, authors, subjects, cover_url) "
                        + "values (:id, :isbn, :title, array['An Author'], array['A Subject'], null)")
                .param("id", titleId).param("isbn", isbn).param("title", name).update();
        List<String> tags = new ArrayList<>();
        List<UUID> ids = new ArrayList<>();
        for (int i = 0; i < copies; i++) {
            UUID copyId = UUID.randomUUID();
            String tag = randomHex(8);
            jdbc.sql("insert into copy (id, title_id, barcode, tag_id, shelf, status) values (:id, :title, :barcode, :tag, 'T1', 'LENDABLE')")
                    .param("id", copyId).param("title", titleId).param("barcode", "BC-" + randomHex(6)).param("tag", tag).update();
            tags.add(tag);
            ids.add(copyId);
        }
        return new TitleFixture(titleId, name, tags, ids);
    }

    static JsonNode claims(JsonMapper json, String jwt) {
        return json.readTree(Base64.getUrlDecoder().decode(jwt.split("\\.")[1]));
    }

    JsonNode claims(String jwt) {
        return claims(json, jwt);
    }
}
