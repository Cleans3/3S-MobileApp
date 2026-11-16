package shelftap.server.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.util.unit.DataSize;

import java.time.Duration;

/** Every tunable of the server. Policy defaults are SPEC.md §6.2; secrets have no default and fail startup when missing. */
@ConfigurationProperties("shelftap")
public record ShelfTapProperties(
        @DefaultValue Jwt jwt,
        @DefaultValue Master master,
        @DefaultValue Gate gate,
        @DefaultValue Loan loan,
        @DefaultValue Auth auth,
        @DefaultValue RateLimit rateLimit,
        @DefaultValue Privacy privacy,
        @DefaultValue("16KB") DataSize maxRequestBody) {

    /** HS256 signing key for access tokens. */
    public record Jwt(String key,
                      @DefaultValue("PT15M") Duration accessTtl,
                      @DefaultValue("P30D") Duration refreshTtl,
                      @DefaultValue("shelftap") String issuer) {
        public Jwt {
            Secrets.check("SHELFTAP_JWT_KEY", key);
        }

        public byte[] keyBytes() {
            return Secrets.decode(key);
        }
    }

    /** K_master: derives every card's admin key (card-spec §2). */
    public record Master(String key) {
        public Master {
            Secrets.check("SHELFTAP_MASTER_KEY", key);
        }

        public byte[] keyBytes() {
            return Secrets.decode(key);
        }
    }

    /** Shared secret of the gate device, sent as X-Gate-Key. */
    public record Gate(String key) {
        public Gate {
            Secrets.check("SHELFTAP_GATE_KEY", key);
        }
    }

    public record Loan(@DefaultValue("14") int days,
                       @DefaultValue("5") int maxLoans,
                       @DefaultValue("1") int maxRenewals,
                       @DefaultValue("1000") int finePerDayVnd,
                       @DefaultValue("P3D") Duration reservationHold) {
    }

    public record Auth(@DefaultValue("5") int maxFailedLogins,
                       @DefaultValue("PT15M") Duration lockoutWindow) {
    }

    public record RateLimit(@DefaultValue("20") int maxRequests,
                            @DefaultValue("PT1M") Duration window) {
    }

    /** Decree 13/2023: returned loans lose their member link after this long. */
    public record Privacy(@DefaultValue("P365D") Duration historyRetention) {
    }
}
