package shelftap.server.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class Hashing {
    private Hashing() {
    }

    public static byte[] sha256(byte[] data) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(data);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public static String sha256Hex(String text) {
        return HexFormat.of().formatHex(sha256(text.getBytes(StandardCharsets.UTF_8)));
    }

    /** Compares the digests, so the comparison takes the same time whatever the lengths or the first differing byte. */
    public static boolean constantTimeEquals(String a, String b) {
        return MessageDigest.isEqual(sha256(a.getBytes(StandardCharsets.UTF_8)), sha256(b.getBytes(StandardCharsets.UTF_8)));
    }
}
