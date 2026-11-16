package shelftap.server.auth;

import org.springframework.stereotype.Service;
import shelftap.server.config.ShelfTapProperties;
import shelftap.server.security.Hashing;
import shelftap.server.security.JwtService;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

/** Mints a session: one opaque refresh token (256 random bits, stored hashed) plus a short access JWT. */
@Service
public class TokenService {
    private static final SecureRandom RANDOM = new SecureRandom();

    /** Whose session this is. `role` is MEMBER, LIBRARIAN or ADMIN. */
    public record Subject(RefreshToken.SubjectType type, UUID id, String role, boolean mustChangePassword) {
    }

    private final RefreshTokenRepository tokens;
    private final JwtService jwt;
    private final Clock clock;
    private final ShelfTapProperties props;

    TokenService(RefreshTokenRepository tokens, JwtService jwt, Clock clock, ShelfTapProperties props) {
        this.tokens = tokens;
        this.jwt = jwt;
        this.clock = clock;
        this.props = props;
    }

    /** `familyId` null starts a new family (a login); a refresh passes the family it rotates inside. */
    public TokenPair issue(Subject subject, UUID deviceId, RefreshToken.Method method, String cardId, UUID familyId) {
        Instant now = clock.instant();
        byte[] raw = new byte[32];
        RANDOM.nextBytes(raw);
        String refresh = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);

        RefreshToken row = new RefreshToken();
        row.familyId = familyId != null ? familyId : UUID.randomUUID();
        row.subjectType = subject.type();
        row.subjectId = subject.id();
        row.tokenHash = Hashing.sha256Hex(refresh);
        row.deviceId = deviceId;
        row.method = method;
        row.cardId = cardId;
        row.issuedAt = now;
        row.expiresAt = now.plus(props.jwt().refreshTtl());
        tokens.save(row);

        String access = jwt.issue(subject.id(), subject.role(), method.amr, row.familyId, deviceId);
        return new TokenPair(access, props.jwt().accessTtl().toSeconds(), refresh, subject.mustChangePassword());
    }
}
