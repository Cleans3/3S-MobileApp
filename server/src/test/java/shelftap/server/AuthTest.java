package shelftap.server;

import org.junit.jupiter.api.Test;
import shelftap.server.member.Member;
import shelftap.server.member.Staff;
import tools.jackson.databind.JsonNode;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthTest extends IntegrationTest {

    @Test
    void loginReturnsTokenPairWithPwdAmr() throws Exception {
        Account member = newMember();
        Session session = login(member);

        assertEquals(900, session.pair().get("accessExpiresIn").asInt());
        assertEquals(false, session.pair().get("mustChangePassword").asBoolean());
        JsonNode claims = claims(session.access());
        assertEquals("pwd", claims.get("amr").get(0).asString());
        assertEquals("MEMBER", claims.get("role").asString());
        assertEquals(member.id().toString(), claims.get("sub").asString());
        assertEquals(900, claims.get("exp").asLong() - claims.get("iat").asLong());
        assertEquals(200, get("/api/me", session.access()).status());
    }

    @Test
    void unknownEmailAndWrongPasswordLookTheSame() throws Exception {
        Account member = newMember();
        UUID device = UUID.randomUUID();
        Res wrong = post("/api/auth/login", Map.of("email", member.email(), "password", "not the password", "deviceId", device.toString()), null);
        Res unknown = post("/api/auth/login", Map.of("email", "nobody-" + randomHex(4) + "@test.example", "password", "not the password", "deviceId", device.toString()), null);

        assertEquals(401, wrong.status());
        assertEquals("INVALID_CREDENTIALS", wrong.code());
        assertEquals(wrong.status(), unknown.status());
        assertEquals(wrong.body().toString(), unknown.body().toString());
    }

    @Test
    void refreshRotatesAndReuseRevokesTheFamily() throws Exception {
        Session first = login(newMember());
        Res second = post("/api/auth/refresh", Map.of("refreshToken", first.refresh(), "deviceId", first.device().toString()), null);
        assertEquals(200, second.status());
        String refresh2 = second.body().get("refreshToken").asString();
        assertNotEquals(first.refresh(), refresh2);

        Res replay = post("/api/auth/refresh", Map.of("refreshToken", first.refresh(), "deviceId", first.device().toString()), null);
        assertEquals(401, replay.status());
        assertEquals("TOKEN_REUSED", replay.code());

        Res afterRevoke = post("/api/auth/refresh", Map.of("refreshToken", refresh2, "deviceId", first.device().toString()), null);
        assertEquals("TOKEN_INVALID", afterRevoke.code());
        assertEquals(1, jdbc.sql("select count(*) from audit_log where action = 'REFRESH' and outcome = 'REUSE_FAMILY_REVOKED' and actor_id = :id")
                .param("id", claims(first.access()).get("sub").asString()).query(Long.class).single());
    }

    @Test
    void refreshTokenIsBoundToItsDevice() throws Exception {
        Session session = login(newMember());
        Res stolen = post("/api/auth/refresh", Map.of("refreshToken", session.refresh(), "deviceId", UUID.randomUUID().toString()), null);
        assertEquals("TOKEN_INVALID", stolen.code());
        Res owner = post("/api/auth/refresh", Map.of("refreshToken", session.refresh(), "deviceId", session.device().toString()), null);
        assertEquals(401, owner.status(), "the family is revoked once a wrong device presented the token");
    }

    @Test
    void logoutRevokesTheFamilyAndNeverLeaksTokenValidity() throws Exception {
        Session session = login(newMember());
        assertEquals(204, post("/api/auth/logout", Map.of("refreshToken", session.refresh()), null).status());
        assertEquals("TOKEN_INVALID", post("/api/auth/refresh", Map.of("refreshToken", session.refresh(), "deviceId", session.device().toString()), null).code());
        assertEquals(204, post("/api/auth/logout", Map.of("refreshToken", "garbage"), null).status());
    }

    @Test
    void fiveFailuresLockTheAccountForTheWindow() throws Exception {
        Account member = newMember();
        UUID device = UUID.randomUUID();
        for (int i = 0; i < 5; i++) {
            assertEquals(401, post("/api/auth/login", Map.of("email", member.email(), "password", "wrong-" + i, "deviceId", device.toString()), null).status());
        }
        Res locked = post("/api/auth/login", Map.of("email", member.email(), "password", member.password(), "deviceId", device.toString()), null);
        assertEquals(423, locked.status());
        assertEquals("ACCOUNT_LOCKED", locked.code());
        assertTrue(Integer.parseInt(locked.header("Retry-After")) > 0);

        clock.advance(Duration.ofMinutes(16));
        assertEquals(200, post("/api/auth/login", Map.of("email", member.email(), "password", member.password(), "deviceId", device.toString()), null).status());
    }

    @Test
    void unknownEmailLocksExactlyLikeAKnownOne() throws Exception {
        String email = "ghost-" + randomHex(4) + "@test.example";
        UUID device = UUID.randomUUID();
        for (int i = 0; i < 5; i++) {
            assertEquals("INVALID_CREDENTIALS", post("/api/auth/login", Map.of("email", email, "password", "x" + i, "deviceId", device.toString()), null).code());
        }
        assertEquals("ACCOUNT_LOCKED", post("/api/auth/login", Map.of("email", email, "password", "x", "deviceId", device.toString()), null).code());
    }

    @Test
    void passwordChangeEndsOtherSessionsButKeepsTheCurrentOne() throws Exception {
        Account member = newMember();
        Session phone = login(member);
        Session tablet = login(member);

        assertEquals("INVALID_CREDENTIALS", post("/api/me/password", Map.of("currentPassword", "nope", "newPassword", "a brand new passphrase"), phone.access()).code());
        assertEquals("VALIDATION_FAILED", post("/api/me/password", Map.of("currentPassword", member.password(), "newPassword", member.password()), phone.access()).code());
        assertEquals("VALIDATION_FAILED", post("/api/me/password", Map.of("currentPassword", member.password(), "newPassword", "short"), phone.access()).code());

        assertEquals(204, post("/api/me/password", Map.of("currentPassword", member.password(), "newPassword", "a brand new passphrase"), phone.access()).status());

        assertEquals("TOKEN_INVALID", post("/api/auth/refresh", Map.of("refreshToken", tablet.refresh(), "deviceId", tablet.device().toString()), null).code());
        assertEquals(200, post("/api/auth/refresh", Map.of("refreshToken", phone.refresh(), "deviceId", phone.device().toString()), null).status());
        assertEquals(401, post("/api/auth/login", Map.of("email", member.email(), "password", member.password(), "deviceId", UUID.randomUUID().toString()), null).status());
        assertEquals(200, post("/api/auth/login", Map.of("email", member.email(), "password", "a brand new passphrase", "deviceId", UUID.randomUUID().toString()), null).status());
    }

    @Test
    void mustChangePasswordGatesEveryMemberEndpointButTheThreeAllowed() throws Exception {
        Account member = newMember(true, Member.Status.ACTIVE);
        Session session = login(member);
        assertEquals(true, session.pair().get("mustChangePassword").asBoolean());

        assertEquals(200, get("/api/me", session.access()).status());
        for (String path : new String[]{"/api/me/loans", "/api/me/history", "/api/me/reservations", "/api/me/fines"}) {
            Res res = get(path, session.access());
            assertEquals(403, res.status(), path);
            assertEquals("PASSWORD_CHANGE_REQUIRED", res.code(), path);
        }
        assertEquals("PASSWORD_CHANGE_REQUIRED", post("/api/loans", Map.of("tagId", "0000000000000000"), session.access()).code());
        assertEquals("PASSWORD_CHANGE_REQUIRED", post("/api/reservations", Map.of("titleId", UUID.randomUUID().toString()), session.access()).code());
        assertEquals("PASSWORD_CHANGE_REQUIRED", put("/api/me/privacy", Map.of("historyEnabled", false), session.access()).code());
        assertEquals(200, get("/api/titles", session.access()).status(), "public catalog stays open");

        assertEquals(204, post("/api/me/password", Map.of("currentPassword", member.password(), "newPassword", "a brand new passphrase"), session.access()).status());
        assertEquals(200, get("/api/me/loans", session.access()).status(), "the gate lifts at once, same access token");
        assertEquals(false, get("/api/me", session.access()).body().get("mustChangePassword").asBoolean());
    }

    @Test
    void expiredTamperedAndForgedAccessTokensAreRejected() throws Exception {
        Account member = newMember();
        Session session = login(member);
        assertEquals(200, get("/api/me", session.access()).status());

        String tampered = session.access().substring(0, session.access().length() - 2) + "AA";
        assertEquals("TOKEN_INVALID", get("/api/me", tampered).code());

        String payload = session.access().split("\\.")[1];
        String none = b64("{\"alg\":\"none\",\"typ\":\"JWT\"}") + "." + payload + ".";
        assertEquals("TOKEN_INVALID", get("/api/me", none).code());

        String signedWithOtherKey = hs256(randomKey().getBytes(StandardCharsets.UTF_8), session.access().split("\\.")[0], payload);
        assertEquals("TOKEN_INVALID", get("/api/me", signedWithOtherKey).code());

        assertEquals("TOKEN_INVALID", get("/api/me", null).code());
        clock.advance(Duration.ofMinutes(16));
        assertEquals("TOKEN_INVALID", get("/api/me", session.access()).code());
    }

    @Test
    void staffLoginRolesAndSeparation() throws Exception {
        Account librarian = newStaff(Staff.Role.LIBRARIAN);
        Session staff = staffLogin(librarian);
        assertEquals("LIBRARIAN", claims(staff.access()).get("role").asString());

        assertEquals(403, get("/api/me", staff.access()).status(), "staff token is not a member token");
        Session member = login(newMember());
        Res asMember = post("/api/members", Map.of("fullName", "X"), member.access());
        assertEquals(403, asMember.status());
        assertEquals("FORBIDDEN", asMember.code());

        String device = UUID.randomUUID().toString();
        assertEquals("INVALID_CREDENTIALS", post("/api/staff/login", Map.of("username", librarian.email(), "password", "wrong", "deviceId", device), null).code());
        assertEquals("INVALID_CREDENTIALS", post("/api/staff/login", Map.of("username", "no-such-" + randomHex(3), "password", "wrong", "deviceId", device), null).code());

        Res refreshed = post("/api/auth/refresh", Map.of("refreshToken", staff.refresh(), "deviceId", staff.device().toString()), null);
        assertEquals(200, refreshed.status());
        assertEquals("LIBRARIAN", claims(refreshed.body().get("accessToken").asString()).get("role").asString());
        assertEquals(staff.device().toString(), claims(refreshed.body().get("accessToken").asString()).get("did").asString());
    }

    @Test
    void staffLoginNeedsADeviceAndItsRefreshTokenIsBoundToIt() throws Exception {
        Account librarian = newStaff(Staff.Role.LIBRARIAN);
        Res missing = post("/api/staff/login", Map.of("username", librarian.email(), "password", librarian.password()), null);
        assertEquals(400, missing.status());
        assertEquals("VALIDATION_FAILED", missing.code());
        assertEquals("VALIDATION_FAILED", post("/api/staff/login", Map.of("username", librarian.email(), "password", librarian.password(),
                "deviceId", "not-a-uuid"), null).code());
        assertEquals(0, jdbc.sql("select count(*) from refresh_token where subject_id = :id").param("id", librarian.id()).query(Long.class).single(),
                "a refused login issues no token");

        Session staff = staffLogin(librarian);
        Res stolen = post("/api/auth/refresh", Map.of("refreshToken", staff.refresh(), "deviceId", UUID.randomUUID().toString()), null);
        assertEquals("TOKEN_INVALID", stolen.code());
        assertEquals("TOKEN_INVALID", post("/api/auth/refresh", Map.of("refreshToken", staff.refresh(), "deviceId", staff.device().toString()), null).code(),
                "the mismatch revoked the whole family");
    }

    private static String b64(String text) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(text.getBytes(StandardCharsets.UTF_8));
    }

    private static String hs256(byte[] key, String header, String payload) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        byte[] sig = mac.doFinal((header + "." + payload).getBytes(StandardCharsets.US_ASCII));
        return header + "." + payload + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(sig);
    }
}
