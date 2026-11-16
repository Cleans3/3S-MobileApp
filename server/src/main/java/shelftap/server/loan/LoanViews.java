package shelftap.server.loan;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import shelftap.server.config.ShelfTapProperties;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Read side of loans, history and reservations: the joined shapes of API.md. Callers always pass the member from the token. */
@Component
public class LoanViews {
    public record LoanDto(UUID loanId, UUID copyId, UUID titleId, String title, String barcode, Instant outAt,
                          Instant dueAt, Instant returnedAt, int renewals, boolean renewable) {
    }

    public record HistoryItem(UUID titleId, String title, Instant outAt, Instant returnedAt) {
    }

    public record ReservationDto(UUID reservationId, UUID titleId, String title, Instant placedAt, Instant readyAt, String status) {
    }

    /** `renewable` mirrors LoanService.renew: still out, renewal left, not overdue, nobody else waiting for the title. */
    private static final String LOAN_SELECT = """
            select l.id, l.copy_id, t.id as title_id, t.title, c.barcode, l.out_at, l.due_at, l.returned_at, l.renewals,
                   (l.returned_at is null and l.renewals < :maxRenewals and l.due_at >= :now and not exists (
                       select 1 from reservation r where r.title_id = t.id and r.member_id is distinct from l.member_id
                          and r.status in ('WAITING', 'READY'))) as renewable
            from loan l join copy c on c.id = l.copy_id join title t on t.id = c.title_id\s""";

    private static final String RESERVATION_SELECT = """
            select r.id, r.title_id, t.title, r.placed_at, r.ready_at, r.status
            from reservation r join title t on t.id = r.title_id\s""";

    private final JdbcClient jdbc;
    private final Clock clock;
    private final int maxRenewals;

    LoanViews(JdbcClient jdbc, Clock clock, ShelfTapProperties props) {
        this.jdbc = jdbc;
        this.clock = clock;
        this.maxRenewals = props.loan().maxRenewals();
    }

    public List<LoanDto> current(UUID memberId) {
        return jdbc.sql(LOAN_SELECT + "where l.member_id = :member and l.returned_at is null order by l.due_at, l.id")
                .param("maxRenewals", maxRenewals).param("now", odt(clock.instant())).param("member", memberId)
                .query(LoanViews::loan).list();
    }

    public Optional<LoanDto> loan(UUID loanId) {
        return jdbc.sql(LOAN_SELECT + "where l.id = :id")
                .param("maxRenewals", maxRenewals).param("now", odt(clock.instant())).param("id", loanId)
                .query(LoanViews::loan).optional();
    }

    public List<HistoryItem> history(UUID memberId) {
        return jdbc.sql("""
                        select t.id as title_id, t.title, l.out_at, l.returned_at
                        from loan l join copy c on c.id = l.copy_id join title t on t.id = c.title_id
                        where l.member_id = :member and l.returned_at is not null
                        order by l.returned_at desc, l.id limit 500""")
                .param("member", memberId)
                .query((rs, n) -> new HistoryItem(rs.getObject("title_id", UUID.class), rs.getString("title"),
                        instant(rs, "out_at"), instant(rs, "returned_at")))
                .list();
    }

    public List<ReservationDto> reservations(UUID memberId) {
        return jdbc.sql(RESERVATION_SELECT + "where r.member_id = :member order by r.seq desc limit 200")
                .param("member", memberId).query(LoanViews::reservation).list();
    }

    public Optional<ReservationDto> reservation(UUID reservationId) {
        return jdbc.sql(RESERVATION_SELECT + "where r.id = :id").param("id", reservationId)
                .query(LoanViews::reservation).optional();
    }

    public static OffsetDateTime odt(Instant instant) {
        return OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    private static LoanDto loan(ResultSet rs, int n) throws SQLException {
        return new LoanDto(rs.getObject("id", UUID.class), rs.getObject("copy_id", UUID.class),
                rs.getObject("title_id", UUID.class), rs.getString("title"), rs.getString("barcode"),
                instant(rs, "out_at"), instant(rs, "due_at"), instant(rs, "returned_at"),
                rs.getInt("renewals"), rs.getBoolean("renewable"));
    }

    private static ReservationDto reservation(ResultSet rs, int n) throws SQLException {
        return new ReservationDto(rs.getObject("id", UUID.class), rs.getObject("title_id", UUID.class),
                rs.getString("title"), instant(rs, "placed_at"), instant(rs, "ready_at"), rs.getString("status"));
    }

    private static Instant instant(ResultSet rs, String column) throws SQLException {
        java.sql.Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }
}
