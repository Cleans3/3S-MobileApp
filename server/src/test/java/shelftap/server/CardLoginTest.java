package shelftap.server;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import shelftap.server.member.Staff;
import tools.jackson.databind.JsonNode;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/** C7 end to end: the real applet in jCardSim signs, the server verifies. */
class CardLoginTest extends IntegrationTest {
    Session librarian;

    @BeforeEach
    void librarianSession() throws Exception {
        librarian = staffLogin(newStaff(Staff.Role.LIBRARIAN));
    }

    /** What a desk does at issue: server makes the card, the applet is issued with the server's admin key, the key is registered. */
    record IssuedCard(String cardId, UUID memberId, JCardSimCard applet, String modulus, String exponent) {
    }

    IssuedCard issueCard(UUID memberId, boolean registerKey) throws Exception {
        Res issued = post("/api/cards", Map.of("memberId", memberId.toString()), librarian.access());
        assertEquals(201, issued.status());
        String cardId = issued.body().get("cardId").asString();
        byte[] adminKey = Base64.getDecoder().decode(issued.body().get("adminKey").asString());
        assertArrayEquals(expectedAdminKey(cardId), adminKey);

        JCardSimCard applet = new JCardSimCard(cardId, adminKey);
        byte[][] key = applet.publicKey();
        String modulus = Base64.getEncoder().encodeToString(key[0]);
        String exponent = Base64.getEncoder().encodeToString(key[1]);
        if (registerKey) {
            assertEquals(204, put("/api/cards/" + cardId + "/public-key", Map.of("modulus", modulus, "exponent", exponent), librarian.access()).status());
        }
        return new IssuedCard(cardId, memberId, applet, modulus, exponent);
    }

    UUID registerMember() throws Exception {
        Res res = post("/api/members", Map.of("fullName", "Nguyễn Văn An", "email", "card-" + randomHex(5).toLowerCase() + "@test.example",
                "dateOfBirth", "2004-05-17", "cccd", String.valueOf(200_000_000_000L + Math.abs(RANDOM.nextLong() % 99_999_999_999L)),
                "phone", "0912345678", "memberType", "STUDENT", "initialPassword", "one-time passphrase"), librarian.access());
        assertEquals(201, res.status());
        return UUID.fromString(res.body().get("memberId").asString());
    }

    Res challenge(String cardId) throws Exception {
        return post("/api/cards/" + cardId + "/challenge", null, null);
    }

    Res auth(String cardId, String nonce, byte[] signature, UUID device) throws Exception {
        return post("/api/cards/" + cardId + "/auth", Map.of("nonce", nonce, "signature", Base64.getEncoder().encodeToString(signature),
                "deviceId", device.toString()), null);
    }

    @Test
    void realAppletSignatureLogsTheMemberIn() throws Exception {
        UUID memberId = registerMember();
        IssuedCard card = issueCard(memberId, true);
        UUID device = UUID.randomUUID();

        Res challenge = challenge(card.cardId());
        assertEquals(200, challenge.status());
        String nonce = challenge.body().get("nonce").asString();
        assertEquals(32, Base64.getDecoder().decode(nonce).length);

        Res login = auth(card.cardId(), nonce, card.applet().sign(Base64.getDecoder().decode(nonce)), device);
        assertEquals(200, login.status());
        String access = login.body().get("accessToken").asString();
        JsonNode claims = claims(access);
        assertEquals("card", claims.get("amr").get(0).asString());
        assertEquals(memberId.toString(), claims.get("sub").asString());
        assertEquals(true, login.body().get("mustChangePassword").asBoolean(), "member registered by staff still has the one-time password");
        assertEquals(200, get("/api/me", access).status());
        assertEquals(1, jdbc.sql("select count(*) from audit_log where action = 'CARD_AUTH' and outcome = 'OK' and target = :c")
                .param("c", card.cardId()).query(Long.class).single());
    }

    Session cardSession(IssuedCard card) throws Exception {
        UUID device = UUID.randomUUID();
        String nonce = challenge(card.cardId()).body().get("nonce").asString();
        Res login = auth(card.cardId(), nonce, card.applet().sign(Base64.getDecoder().decode(nonce)), device);
        assertEquals(200, login.status());
        return new Session(login.body().get("accessToken").asString(), login.body().get("refreshToken").asString(), device, login.body());
    }

    @Test
    void aCardSessionLendsButCannotReadHistoryOrChangePrivacy() throws Exception {
        Account member = newMember();
        Session viaCard = cardSession(issueCard(member.id(), true));
        Session viaPassword = login(member);

        for (Res refused : new Res[] {get("/api/me/history", viaCard.access()),
                put("/api/me/privacy", Map.of("historyEnabled", false), viaCard.access())}) {
            assertEquals(403, refused.status());
            assertEquals("STEP_UP_REQUIRED", refused.code());
        }
        assertEquals(true, get("/api/me", viaPassword.access()).body().get("historyEnabled").asBoolean(), "the refused PUT changed nothing");

        // A refreshed card session is still a card session.
        Res refreshed = post("/api/auth/refresh", Map.of("refreshToken", viaCard.refresh(), "deviceId", viaCard.device().toString()), null);
        assertEquals(200, refreshed.status());
        assertEquals("STEP_UP_REQUIRED", get("/api/me/history", refreshed.body().get("accessToken").asString()).code());

        // What a tap is for stays allowed: read loans, borrow, renew, reserve.
        String card = refreshed.body().get("accessToken").asString();
        assertEquals(200, get("/api/me/loans", card).status());
        TitleFixture book = newTitle(1);
        Res loan = post("/api/loans", Map.of("tagId", book.tagIds().get(0)), card);
        assertEquals(201, loan.status());
        assertEquals(200, post("/api/loans/" + loan.body().get("loanId").asString() + "/renew", null, card).status());
        TitleFixture taken = newTitle(1);
        assertEquals(201, post("/api/loans", Map.of("tagId", taken.tagIds().get(0)), login(newMember()).access()).status());
        assertEquals(201, post("/api/reservations", Map.of("titleId", taken.titleId().toString()), card).status());

        // The same member with a password session reads and changes both.
        assertEquals(200, get("/api/me/history", viaPassword.access()).status());
        Res off = put("/api/me/privacy", Map.of("historyEnabled", false), viaPassword.access());
        assertEquals(200, off.status());
        assertEquals(false, off.body().get("historyEnabled").asBoolean());
    }

    /** The Android step-up: password login, then log the card session out. Logout must end that one session and no other. */
    @Test
    void logoutEndsOnlyThePresentedSessionNotTheMembersOthers() throws Exception {
        Account member = newMember();
        Session viaCard = cardSession(issueCard(member.id(), true));
        Res sameDevice = post("/api/auth/login", Map.of("email", member.email(), "password", member.password(),
                "deviceId", viaCard.device().toString()), null);
        assertEquals(200, sameDevice.status());
        String passwordRefresh = sameDevice.body().get("refreshToken").asString();
        Session otherDevice = login(member);

        assertEquals(204, post("/api/auth/logout", Map.of("refreshToken", viaCard.refresh()), null).status());

        assertEquals("TOKEN_INVALID", post("/api/auth/refresh", Map.of("refreshToken", viaCard.refresh(), "deviceId", viaCard.device().toString()), null).code(),
                "the card session is gone");
        Res kept = post("/api/auth/refresh", Map.of("refreshToken", passwordRefresh, "deviceId", viaCard.device().toString()), null);
        assertEquals(200, kept.status(), "the password session opened just before survives");
        assertEquals(200, get("/api/me/history", kept.body().get("accessToken").asString()).status());
        assertEquals(200, post("/api/auth/refresh", Map.of("refreshToken", otherDevice.refresh(), "deviceId", otherDevice.device().toString()), null).status());
        assertEquals(2, jdbc.sql("select count(distinct family_id) from refresh_token where subject_id = :id and revoked_at is null")
                .param("id", member.id()).query(Long.class).single(), "exactly the two other families are still live");
    }

    @Test
    void aNonceWorksOnceOnly() throws Exception {
        IssuedCard card = issueCard(registerMember(), true);
        String nonce = challenge(card.cardId()).body().get("nonce").asString();
        byte[] signature = card.applet().sign(Base64.getDecoder().decode(nonce));

        assertEquals(200, auth(card.cardId(), nonce, signature, UUID.randomUUID()).status());
        Res replay = auth(card.cardId(), nonce, signature, UUID.randomUUID());
        assertEquals(401, replay.status());
        assertEquals("CHALLENGE_EXPIRED", replay.code());
    }

    @Test
    void aBadSignatureFailsAndBurnsTheNonce() throws Exception {
        IssuedCard card = issueCard(registerMember(), true);
        String nonce = challenge(card.cardId()).body().get("nonce").asString();
        byte[] signature = card.applet().sign(Base64.getDecoder().decode(nonce));
        byte[] bad = Arrays.copyOf(signature, signature.length);
        bad[10] ^= 0x01;

        Res failed = auth(card.cardId(), nonce, bad, UUID.randomUUID());
        assertEquals(401, failed.status());
        assertEquals("CARD_AUTH_FAILED", failed.code());
        assertEquals("CHALLENGE_EXPIRED", auth(card.cardId(), nonce, signature, UUID.randomUUID()).code(), "no second attempt with the same nonce");
    }

    @Test
    void aSignatureOverTheWrongNonceFails() throws Exception {
        IssuedCard card = issueCard(registerMember(), true);
        String issued = challenge(card.cardId()).body().get("nonce").asString();
        byte[] other = new byte[32];
        RANDOM.nextBytes(other);
        assertEquals("CARD_AUTH_FAILED", auth(card.cardId(), issued, card.applet().sign(other), UUID.randomUUID()).code());
    }

    @Test
    void aNonceIsBoundToItsCard() throws Exception {
        UUID member = registerMember();
        IssuedCard a = issueCard(member, true);
        IssuedCard b = issueCard(registerMember(), true);
        String nonceForB = challenge(b.cardId()).body().get("nonce").asString();
        Res crossed = auth(a.cardId(), nonceForB, a.applet().sign(Base64.getDecoder().decode(nonceForB)), UUID.randomUUID());
        assertEquals("CHALLENGE_EXPIRED", crossed.code());
    }

    @Test
    void aNonceExpiresAfterSixtySeconds() throws Exception {
        IssuedCard card = issueCard(registerMember(), true);
        String nonce = challenge(card.cardId()).body().get("nonce").asString();
        byte[] signature = card.applet().sign(Base64.getDecoder().decode(nonce));
        clock.advance(Duration.ofSeconds(61));
        assertEquals("CHALLENGE_EXPIRED", auth(card.cardId(), nonce, signature, UUID.randomUUID()).code());
    }

    @Test
    void aCardWithoutARegisteredKeyCannotLogIn() throws Exception {
        IssuedCard card = issueCard(registerMember(), false);
        String nonce = challenge(card.cardId()).body().get("nonce").asString();
        assertEquals("CARD_AUTH_FAILED", auth(card.cardId(), nonce, card.applet().sign(Base64.getDecoder().decode(nonce)), UUID.randomUUID()).code());
    }

    @Test
    void aBlockedCardIsRefusedAndItsSessionsEnd() throws Exception {
        IssuedCard card = issueCard(registerMember(), true);
        UUID device = UUID.randomUUID();
        String nonce = challenge(card.cardId()).body().get("nonce").asString();
        Res login = auth(card.cardId(), nonce, card.applet().sign(Base64.getDecoder().decode(nonce)), device);
        assertEquals(200, login.status());
        String refresh = login.body().get("refreshToken").asString();

        assertEquals(204, put("/api/cards/" + card.cardId() + "/status", Map.of("status", "BLOCKED"), librarian.access()).status());

        Res refused = challenge(card.cardId());
        assertEquals(403, refused.status());
        assertEquals("CARD_NOT_ACTIVE", refused.code());
        assertEquals("TOKEN_INVALID", post("/api/auth/refresh", Map.of("refreshToken", refresh, "deviceId", device.toString()), null).code());

        assertEquals(204, put("/api/cards/" + card.cardId() + "/status", Map.of("status", "ACTIVE"), librarian.access()).status());
        assertEquals(200, challenge(card.cardId()).status());
    }

    @Test
    void aRevokedCardStaysRevoked() throws Exception {
        IssuedCard card = issueCard(registerMember(), true);
        assertEquals(204, put("/api/cards/" + card.cardId() + "/status", Map.of("status", "REVOKED"), librarian.access()).status());
        Res back = put("/api/cards/" + card.cardId() + "/status", Map.of("status", "ACTIVE"), librarian.access());
        assertEquals("CARD_NOT_ACTIVE", back.code());
        assertEquals("CARD_NOT_ACTIVE", challenge(card.cardId()).code());
    }

    @Test
    void theRegisteredKeyIsWriteOnce() throws Exception {
        UUID member = registerMember();
        IssuedCard card = issueCard(member, true);
        assertEquals(204, put("/api/cards/" + card.cardId() + "/public-key", Map.of("modulus", card.modulus(), "exponent", card.exponent()), librarian.access()).status(),
                "same key again is a no-op");

        IssuedCard other = issueCard(registerMember(), false);
        Res overwrite = put("/api/cards/" + card.cardId() + "/public-key", Map.of("modulus", other.modulus(), "exponent", other.exponent()), librarian.access());
        assertEquals(409, overwrite.status());
        assertEquals("KEY_ALREADY_REGISTERED", overwrite.code());
        assertEquals("VALIDATION_FAILED", put("/api/cards/" + other.cardId() + "/public-key", Map.of("modulus", "AAAA", "exponent", other.exponent()), librarian.access()).code());
    }

    @Test
    void issuingANewCardRevokesTheMembersOldOne() throws Exception {
        UUID member = registerMember();
        IssuedCard first = issueCard(member, true);
        IssuedCard second = issueCard(member, true);
        assertNotEquals(first.cardId(), second.cardId());
        assertEquals("CARD_NOT_ACTIVE", challenge(first.cardId()).code());
        assertEquals(200, challenge(second.cardId()).status());
    }

    @Test
    void issuingRevokesActiveAndBlockedCardsButLeavesALostOneAsRecorded() throws Exception {
        UUID member = registerMember();
        IssuedCard blocked = issueCard(member, true);
        assertEquals(204, put("/api/cards/" + blocked.cardId() + "/status", Map.of("status", "BLOCKED"), librarian.access()).status());
        // A lost card is a recorded fact, not a live card: issuing a replacement leaves it LOST.
        IssuedCard lost = issueCard(registerMember(), true);
        assertEquals(204, put("/api/cards/" + lost.cardId() + "/status", Map.of("status", "LOST"), librarian.access()).status());

        issueCard(member, true);
        assertEquals("REVOKED", cardStatus(blocked.cardId()));
        issueCard(UUID.fromString(jdbc.sql("select member_id::text from card where card_id = :c").param("c", lost.cardId()).query(String.class).single()), true);
        assertEquals("LOST", cardStatus(lost.cardId()));
    }

    String cardStatus(String cardId) {
        return jdbc.sql("select status from card where card_id = :c").param("c", cardId).query(String.class).single();
    }

    @Test
    void cardEndpointsRejectBadAndUnknownIds() throws Exception {
        assertEquals("VALIDATION_FAILED", challenge("not-a-card-id").code());
        assertEquals("VALIDATION_FAILED", challenge("abcdef0123456789").code(), "lowercase hex is not a card id");
        Res unknown = challenge("0123456789ABCDEF");
        assertEquals(404, unknown.status());
        assertEquals("NOT_FOUND", unknown.code());
    }

    @Test
    void staffEndpointsNeedAStaffToken() throws Exception {
        Session member = login(newMember());
        assertEquals(403, post("/api/cards", Map.of("memberId", UUID.randomUUID().toString()), member.access()).status());
        assertEquals(401, post("/api/cards", Map.of("memberId", UUID.randomUUID().toString()), null).status());
        assertEquals(403, put("/api/cards/0123456789ABCDEF/status", Map.of("status", "ACTIVE"), member.access()).status());
    }

    static byte[] expectedAdminKey(String cardId) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(Base64.getDecoder().decode(MASTER_KEY), "HmacSHA256"));
        mac.update("ShelfTap-admin".getBytes(StandardCharsets.US_ASCII));
        mac.update(HexFormat.of().parseHex(cardId));
        return Arrays.copyOf(mac.doFinal(), 16);
    }
}
