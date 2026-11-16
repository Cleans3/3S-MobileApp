package shelftap.server.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Service;
import shelftap.server.config.ShelfTapProperties;
import shelftap.server.web.ApiException;
import shelftap.server.web.ErrorCode;

import javax.crypto.spec.SecretKeySpec;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Access tokens: HS256 JWT, 15 minutes. The decoder accepts HS256 only, so `alg: none` and key-confusion
 * tokens are rejected. Claims: sub, role, amr (pwd|card), sid (refresh family), did (device).
 */
@Service
public class JwtService {
    private final JwtEncoder encoder;
    private final JwtDecoder decoder;
    private final Clock clock;
    private final ShelfTapProperties props;

    JwtService(ShelfTapProperties props, Clock clock) {
        this.props = props;
        this.clock = clock;
        SecretKeySpec key = new SecretKeySpec(props.jwt().keyBytes(), "HmacSHA256");
        this.encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
        NimbusJwtDecoder nimbus = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        JwtTimestampValidator timestamps = new JwtTimestampValidator(java.time.Duration.ZERO);
        timestamps.setClock(clock);
        nimbus.setJwtValidator(new DelegatingOAuth2TokenValidator<>(timestamps, new JwtIssuerValidator(props.jwt().issuer())));
        this.decoder = nimbus;
    }

    public JwtDecoder decoder() {
        return decoder;
    }

    public String issue(UUID subject, String role, String amr, UUID familyId, UUID deviceId) {
        Instant now = clock.instant();
        JwtClaimsSet.Builder builder = JwtClaimsSet.builder()
                .issuer(props.jwt().issuer())
                .subject(subject.toString())
                .issuedAt(now)
                .expiresAt(now.plus(props.jwt().accessTtl()))
                .id(UUID.randomUUID().toString())
                .claim("role", role)
                .claim("amr", List.of(amr))
                .claim("sid", familyId.toString());
        if (deviceId != null) {
            builder.claim("did", deviceId.toString());
        }
        JwtClaimsSet claims = builder.build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }

    /** Who is calling, read from a verified token. */
    public record Caller(UUID id, String role, UUID familyId, boolean passwordLogin) {
        public static Caller from(Jwt jwt) {
            List<String> amr = jwt.getClaimAsStringList("amr");
            return new Caller(UUID.fromString(jwt.getSubject()), jwt.getClaimAsString("role"),
                    UUID.fromString(jwt.getClaimAsString("sid")), amr != null && amr.contains("pwd"));
        }

        /** A card tap proves possession only. Endpoints on personal data call this and fail closed unless the session came from a password. */
        public Caller requirePasswordLogin() {
            if (!passwordLogin) {
                throw new ApiException(ErrorCode.STEP_UP_REQUIRED, "sign in with your password for this action");
            }
            return this;
        }
    }
}
