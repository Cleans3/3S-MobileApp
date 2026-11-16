package shelftap.server.security;

import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** Argon2id with the OWASP minimum (19 MiB, 2 passes, 1 lane). Salt is random per hash and stored inside it. */
@Component
public class Passwords {
    private final Argon2PasswordEncoder encoder = new Argon2PasswordEncoder(16, 32, 1, 19_456, 2);
    /** Verified against when the account does not exist, so an unknown email costs the same time as a wrong password. */
    private final String dummyHash = encoder.encode(UUID.randomUUID().toString());

    public String hash(String raw) {
        return encoder.encode(raw);
    }

    /** `storedHash` may be null (no such account): still runs one full verification and returns false. */
    public boolean matches(String raw, String storedHash) {
        boolean match = encoder.matches(raw, storedHash != null ? storedHash : dummyHash);
        return match && storedHash != null;
    }
}
