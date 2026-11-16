package shelftap.server.auth;

/** `TokenPair` of API.md. */
public record TokenPair(String accessToken, long accessExpiresIn, String refreshToken, boolean mustChangePassword) {
}
