package shelftap.server.auth;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import shelftap.server.config.ShelfTapProperties;
import shelftap.server.security.Hashing;
import shelftap.server.web.ApiException;
import shelftap.server.web.ErrorCode;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * Account lockout: N failed logins inside the window lock the login name, known or not, so a locked
 * account and an unknown one answer alike. Call from a transaction that commits despite the ApiException.
 */
@Service
public class LockoutService {
    private final LoginAttemptRepository attempts;
    private final JdbcClient jdbc;
    private final Clock clock;
    private final int max;
    private final Duration window;

    LockoutService(LoginAttemptRepository attempts, JdbcClient jdbc, Clock clock, ShelfTapProperties props) {
        this.attempts = attempts;
        this.jdbc = jdbc;
        this.clock = clock;
        this.max = props.auth().maxFailedLogins();
        this.window = props.auth().lockoutWindow();
    }

    /** Throws ACCOUNT_LOCKED when the key has used up its attempts. Keys are namespaced ("m:" member, "s:" staff). */
    public void assertNotLocked(String key) {
        String hash = Hashing.sha256Hex(key.toLowerCase());
        // Per-key advisory lock: parallel guesses queue up here, so they cannot all pass the count before one is recorded.
        jdbc.sql("select pg_advisory_xact_lock(hashtext(:k))").param("k", hash).query((rs, n) -> 1).list();
        Instant now = clock.instant();
        Instant since = now.minus(window);
        if (attempts.countByKeyHashAndAtAfter(hash, since) >= max) {
            Instant oldest = attempts.findFirstByKeyHashAndAtAfterOrderByAtAsc(hash, since).orElseThrow().at;
            int retryAfter = (int) Math.max(1, Duration.between(now, oldest.plus(window)).toSeconds());
            throw new ApiException(ErrorCode.ACCOUNT_LOCKED, "too many failed logins, try again later", retryAfter);
        }
    }

    public void recordFailure(String key) {
        LoginAttempt attempt = new LoginAttempt();
        attempt.keyHash = Hashing.sha256Hex(key.toLowerCase());
        attempt.at = clock.instant();
        attempts.save(attempt);
    }

    public void clear(String key) {
        attempts.clear(Hashing.sha256Hex(key.toLowerCase()));
    }
}
