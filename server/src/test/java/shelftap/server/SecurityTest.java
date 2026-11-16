package shelftap.server;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import shelftap.server.member.Staff;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Transport, headers, error format, RBAC and what is stored. */
class SecurityTest extends IntegrationTest {

    @Test
    void anUnauthenticatedCallGetsProblemJsonWithAStableCode() throws Exception {
        Res res = get("/api/me/loans", null);
        assertEquals(401, res.status());
        assertEquals("application/problem+json", res.raw().getContentType().split(";")[0]);
        for (String field : new String[]{"type", "title", "status", "detail", "code"}) {
            assertTrue(res.body().has(field), field);
        }
        assertEquals("TOKEN_INVALID", res.code());
        assertEquals(401, res.body().get("status").asInt());
        assertEquals("Bearer", res.header("WWW-Authenticate"));
    }

    @Test
    void securityHeadersAreOnSuccessAndErrorResponses() throws Exception {
        for (String path : new String[]{"/api/titles", "/api/me"}) {
            Res res = call(MockMvcRequestBuilders.get(path).secure(true));
            assertTrue(res.header("Strict-Transport-Security").contains("max-age=31536000"), path);
            assertTrue(res.header("Strict-Transport-Security").contains("includeSubDomains"), path);
            assertEquals("nosniff", res.header("X-Content-Type-Options"), path);
            assertEquals("DENY", res.header("X-Frame-Options"), path);
            assertTrue(res.header("Content-Security-Policy").startsWith("default-src 'none'"), path);
            assertEquals("no-referrer", res.header("Referrer-Policy"), path);
            assertTrue(res.header("Cache-Control").contains("no-store"), path);
        }
    }

    @Test
    void thereIsNoCorsAndNoCsrfCookie() throws Exception {
        Res preflight = call(MockMvcRequestBuilders.options("/api/titles").header("Origin", "https://evil.example")
                .header("Access-Control-Request-Method", "GET"));
        assertEquals(null, preflight.header("Access-Control-Allow-Origin"));
        Res simple = call(MockMvcRequestBuilders.get("/api/titles").header("Origin", "https://evil.example"));
        assertEquals(null, simple.header("Access-Control-Allow-Origin"));
        assertEquals(0, simple.raw().getCookies().length, "stateless: no session or CSRF cookie");
    }

    @Test
    void errorsNeverLeakInternals() throws Exception {
        Res badJson = call(MockMvcRequestBuilders.post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{not json"));
        assertEquals(400, badJson.status());
        assertEquals("VALIDATION_FAILED", badJson.code());

        Res missing = post("/api/auth/login", Map.of("email", "a@b.c"), null);
        assertEquals("VALIDATION_FAILED", missing.code());
        assertTrue(missing.body().get("detail").asString().contains("password"));

        Session member = login(newMember());
        for (Res res : new Res[]{badJson, missing, get("/api/nothing-here", member.access()), call(MockMvcRequestBuilders.delete("/api/titles")),
                post("/api/loans", Map.of("tagId", "x".repeat(20)), member.access())}) {
            String body = res.body().toString();
            for (String leak : new String[]{"Exception", "at shelftap", "org.springframework", "SQL", "jdbc", "postgres"}) {
                assertFalse(body.contains(leak), leak + " in " + body);
            }
            assertTrue(res.body().has("code"));
        }
        assertEquals(403, get("/api/nothing-here", member.access()).status(), "unknown routes are denied, not served");
        assertEquals(401, get("/api/nothing-here", null).status());
    }

    @Test
    void anOversizedBodyIsRefusedBeforeItIsRead() throws Exception {
        Res res = call(MockMvcRequestBuilders.post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"a@b.c\",\"password\":\"" + "x".repeat(20_000) + "\",\"deviceId\":\"" + UUID.randomUUID() + "\"}"));
        assertEquals(413, res.status());
        assertEquals("VALIDATION_FAILED", res.code());
    }

    @Test
    void onlyHealthIsExposed() throws Exception {
        Res health = get("/actuator/health", null);
        assertEquals(200, health.status());
        assertEquals("UP", health.body().get("status").asString());
        assertFalse(health.body().has("components"), "no details");
        assertEquals(401, get("/actuator/env", null).status());
        assertEquals(403, get("/actuator/env", login(newMember()).access()).status());
    }

    @Test
    void theAuditLogCannotBeChangedOrDeleted() throws Exception {
        login(newMember());
        long rows = jdbc.sql("select count(*) from audit_log").query(Long.class).single();
        assertTrue(rows > 0);
        DataAccessException deleted = assertThrows(DataAccessException.class, () -> jdbc.sql("delete from audit_log").update());
        assertTrue(deleted.getMessage().contains("append-only"));
        assertThrows(DataAccessException.class, () -> jdbc.sql("update audit_log set outcome = 'OK'").update());
    }

    @Test
    void secretsAreStoredOnlyAsHashes() throws Exception {
        Account account = newMember();
        Session session = login(account);
        String passwordHash = jdbc.sql("select password_hash from member where id = :id").param("id", account.id()).query(String.class).single();
        assertTrue(passwordHash.startsWith("$argon2id$"), passwordHash);
        assertFalse(passwordHash.contains(PASSWORD));

        String stored = jdbc.sql("select token_hash from refresh_token where subject_id = :id").param("id", account.id()).query(String.class).single();
        assertEquals(64, stored.length());
        assertTrue(!stored.equals(session.refresh()), "the refresh token itself is never stored");
        assertEquals(0, jdbc.sql("select count(*) from refresh_token where token_hash = :t").param("t", session.refresh()).query(Long.class).single());
    }

    @Test
    void staffRegistersAMemberWhoMustChangeTheOneTimePassword() throws Exception {
        Session staff = staffLogin(newStaff(Staff.Role.LIBRARIAN));
        String email = "reg-" + randomHex(4).toLowerCase() + "@test.example";
        Map<String, Object> body = Map.of("fullName", "Trần Thị Bình", "email", email, "dateOfBirth", "2001-02-03",
                "cccd", String.valueOf(300_000_000_000L + Math.abs(RANDOM.nextLong() % 99_999_999_999L)), "phone", "+84912345678",
                "memberType", "STAFF", "initialPassword", "one-time passphrase");
        Res created = post("/api/members", body, staff.access());
        assertEquals(201, created.status());
        assertNotNull(created.body().get("memberId"));

        Res login = post("/api/auth/login", Map.of("email", email, "password", "one-time passphrase", "deviceId", UUID.randomUUID().toString()), null);
        assertEquals(true, login.body().get("mustChangePassword").asBoolean());

        assertEquals("VALIDATION_FAILED", post("/api/members", body, staff.access()).code(), "duplicate email and cccd");
        Res bad = post("/api/members", Map.of("fullName", "", "email", "not-an-email", "dateOfBirth", "2999-01-01", "cccd", "12",
                "phone", "abc", "memberType", "STAFF", "initialPassword", "short"), staff.access());
        assertEquals("VALIDATION_FAILED", bad.code());
        String detail = bad.body().get("detail").asString();
        for (String field : new String[]{"fullName", "email", "dateOfBirth", "cccd", "phone", "initialPassword"}) {
            assertTrue(detail.contains(field), field + " missing from " + detail);
        }
    }

    @Test
    void injectionAttemptsInCredentialsJustFail() throws Exception {
        UUID device = UUID.randomUUID();
        assertEquals("INVALID_CREDENTIALS", post("/api/auth/login", Map.of("email", "' or 1=1 --", "password", "' or '1'='1", "deviceId", device.toString()), null).code());
        assertEquals("INVALID_CREDENTIALS", post("/api/staff/login", Map.of("username", "admin'--", "password", "x", "deviceId", device.toString()), null).code());
    }
}
