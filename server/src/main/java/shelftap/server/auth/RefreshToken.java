package shelftap.server.auth;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** Only the SHA-256 of the token is stored: a database dump cannot be replayed as a session. */
@Entity
@Table(name = "refresh_token")
public class RefreshToken {
    public enum SubjectType { MEMBER, STAFF }

    public enum Method {
        PWD("pwd"), CARD("card");

        public final String amr;

        Method(String amr) {
            this.amr = amr;
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public UUID id;
    public UUID familyId;
    @Enumerated(EnumType.STRING)
    public SubjectType subjectType;
    public UUID subjectId;
    public String tokenHash;
    public UUID deviceId;
    @Enumerated(EnumType.STRING)
    public Method method;
    public String cardId;
    public Instant issuedAt;
    public Instant expiresAt;
    public Instant usedAt;
    public Instant revokedAt;
}
