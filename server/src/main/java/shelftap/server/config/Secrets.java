package shelftap.server.config;

import java.util.Base64;

/** Startup check for env-supplied keys: Base64, at least 32 bytes. Messages name the variable, never the value. */
final class Secrets {
    static final int MIN_BYTES = 32;

    private Secrets() {
    }

    static void check(String envName, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(envName + " is not set");
        }
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(value.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(envName + " is not valid Base64");
        }
        if (bytes.length < MIN_BYTES) {
            throw new IllegalStateException(envName + " must decode to at least " + MIN_BYTES + " bytes");
        }
    }

    static byte[] decode(String value) {
        return Base64.getDecoder().decode(value.trim());
    }
}
