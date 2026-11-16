package shelftap.server.loan;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import shelftap.server.auth.LoginAttemptRepository;
import shelftap.server.auth.RefreshTokenRepository;
import shelftap.server.card.CardChallengeRepository;
import shelftap.server.config.ShelfTapProperties;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Data minimisation (Decree 13/2023). A returned loan loses its member link after the retention period, or at once when the
 * member turns history off. Loans with an unpaid fine keep the link. Also expires reservation holds and purges spent auth rows.
 */
@Component
public class Retention {
    /** A returned loan, still linked to a member, with no unpaid fine hanging on it. */
    private static final String ELIGIBLE = """
            returned_at is not null and returned_at <= :cutoff and member_id is not null
            and not exists (select 1 from fine f where f.loan_id = loan.id and f.paid_at is null)""";

    private final JdbcClient jdbc;
    private final ReservationService reservations;
    private final CardChallengeRepository challenges;
    private final LoginAttemptRepository attempts;
    private final RefreshTokenRepository refreshTokens;
    private final Clock clock;
    private final ShelfTapProperties props;

    Retention(JdbcClient jdbc, ReservationService reservations, CardChallengeRepository challenges,
              LoginAttemptRepository attempts, RefreshTokenRepository refreshTokens, Clock clock, ShelfTapProperties props) {
        this.jdbc = jdbc;
        this.reservations = reservations;
        this.challenges = challenges;
        this.attempts = attempts;
        this.refreshTokens = refreshTokens;
        this.clock = clock;
        this.props = props;
    }

    /** `memberId` null means every member. Paid fines lose their loan link too, so the book cannot be recovered through them. */
    @Transactional
    public int anonymise(UUID memberId, Instant cutoff) {
        String scope = memberId == null ? "" : " and member_id = :member";
        JdbcClient.StatementSpec detach = jdbc.sql("update fine set loan_id = null where loan_id in (select loan.id from loan where "
                + ELIGIBLE + scope + ")").param("cutoff", LoanViews.odt(cutoff));
        JdbcClient.StatementSpec unlink = jdbc.sql("update loan set member_id = null where " + ELIGIBLE + scope)
                .param("cutoff", LoanViews.odt(cutoff));
        if (memberId != null) {
            detach = detach.param("member", memberId);
            unlink = unlink.param("member", memberId);
        }
        detach.update();
        return unlink.update();
    }

    @Scheduled(fixedDelayString = "${shelftap.maintenance-interval:PT1H}", initialDelayString = "PT5M")
    @Transactional
    public void run() {
        Instant now = clock.instant();
        anonymise(null, now.minus(props.privacy().historyRetention()));
        reservations.expireHolds();
        purgeSpentAuthRows(now);
    }

    private void purgeSpentAuthRows(Instant now) {
        challenges.purgeExpired(now.minus(Duration.ofDays(1)));
        attempts.purgeBefore(now.minus(props.auth().lockoutWindow()));
        refreshTokens.purgeExpired(now.minus(Duration.ofDays(7)));
    }
}
