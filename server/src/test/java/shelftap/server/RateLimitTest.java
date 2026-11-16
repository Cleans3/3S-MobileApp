package shelftap.server;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestPropertySource(properties = "shelftap.rate-limit.max-requests=5")
class RateLimitTest extends IntegrationTest {

    Res loginAttempt(String ip) throws Exception {
        // A fresh email each time, so only the IP budget (not the per-account lockout) can answer 429/423.
        return from("/api/auth/login", Map.of("email", "nobody-" + randomHex(5) + "@test.example", "password", "x",
                "deviceId", UUID.randomUUID().toString()), ip);
    }

    Res from(String path, Object body, String ip) throws Exception {
        return call(MockMvcRequestBuilders.post(path).contentType(MediaType.APPLICATION_JSON)
                .content(body == null ? "" : json.writeValueAsString(body))
                .with(r -> {
                    r.setRemoteAddr(ip);
                    return r;
                }));
    }

    @Test
    void theSixthCredentialRequestFromOneIpIsRefusedWithRetryAfter() throws Exception {
        String ip = "10.1.1.1";
        for (int i = 0; i < 5; i++) {
            assertEquals(401, loginAttempt(ip).status());
        }
        Res limited = loginAttempt(ip);
        assertEquals(429, limited.status());
        assertEquals("RATE_LIMITED", limited.code());
        int retryAfter = Integer.parseInt(limited.header("Retry-After"));
        assertTrue(retryAfter >= 1 && retryAfter <= 60);
        assertEquals("application/problem+json", limited.raw().getContentType().split(";")[0]);

        assertEquals(401, loginAttempt("10.1.1.2").status(), "another IP has its own budget");
        assertEquals(200, call(MockMvcRequestBuilders.get("/api/titles").with(r -> {
            r.setRemoteAddr(ip);
            return r;
        })).status(), "the public catalog is not rate limited");
    }

    @Test
    void refreshStaffLoginAndCardEndpointsShareTheSameBudget() throws Exception {
        String ip = "10.2.2.2";
        assertEquals(401, from("/api/auth/refresh", Map.of("refreshToken", "x", "deviceId", UUID.randomUUID().toString()), ip).status());
        assertEquals(401, from("/api/staff/login", Map.of("username", "x", "password", "x", "deviceId", UUID.randomUUID().toString()), ip).status());
        assertEquals(404, from("/api/cards/0123456789ABCDEF/challenge", null, ip).status());
        assertEquals(401, from("/api/cards/0123456789ABCDEF/auth", Map.of("nonce", "AAAA", "signature", "AAAA", "deviceId", UUID.randomUUID().toString()), ip).status());
        assertEquals(401, loginAttempt(ip).status());
        assertEquals(429, from("/api/cards/0123456789ABCDEF/challenge", null, ip).status());
    }

    @Test
    void theBudgetComesBackWhenTheWindowPasses() throws Exception {
        String ip = "10.3.3.3";
        for (int i = 0; i < 6; i++) {
            loginAttempt(ip);
        }
        assertEquals(429, loginAttempt(ip).status());
        clock.advance(Duration.ofSeconds(61));
        assertEquals(401, loginAttempt(ip).status());
    }
}
