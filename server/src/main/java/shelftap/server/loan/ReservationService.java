package shelftap.server.loan;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import shelftap.server.audit.AuditService;
import shelftap.server.audit.AuditService.Actor;
import shelftap.server.catalog.Catalog;
import shelftap.server.catalog.Copy;
import shelftap.server.catalog.CopyRepository;
import shelftap.server.config.ShelfTapProperties;
import shelftap.server.member.Member;
import shelftap.server.member.MemberRepository;
import shelftap.server.web.ApiException;
import shelftap.server.web.ErrorCode;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import static shelftap.server.loan.Reservation.Status.CANCELLED;
import static shelftap.server.loan.Reservation.Status.EXPIRED;
import static shelftap.server.loan.Reservation.Status.READY;
import static shelftap.server.loan.Reservation.Status.WAITING;

/** Reservation lifecycle: WAITING, READY (a returned copy is held for the member), then FULFILLED / CANCELLED / EXPIRED. */
@Service
public class ReservationService {
    private final ReservationRepository reservations;
    private final MemberRepository members;
    private final LoanRepository loans;
    private final CopyRepository copies;
    private final Catalog catalog;
    private final AuditService audit;
    private final Clock clock;
    private final ShelfTapProperties props;

    ReservationService(ReservationRepository reservations, MemberRepository members, LoanRepository loans, CopyRepository copies,
                       Catalog catalog, AuditService audit, Clock clock, ShelfTapProperties props) {
        this.reservations = reservations;
        this.members = members;
        this.loans = loans;
        this.copies = copies;
        this.catalog = catalog;
        this.audit = audit;
        this.clock = clock;
        this.props = props;
    }

    /** The member row lock serialises one member's concurrent requests, so the open-reservation check cannot race itself. */
    @Transactional(noRollbackFor = ApiException.class)
    public UUID reserve(UUID memberId, UUID titleId) {
        Member member = members.lockById(memberId).orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "member not found"));
        if (!catalog.exists(titleId)) {
            throw deny("RESERVE", titleId.toString(), ErrorCode.NOT_FOUND, "title not found");
        }
        if (member.status != Member.Status.ACTIVE) {
            throw deny("RESERVE", titleId.toString(), ErrorCode.FORBIDDEN, "membership suspended");
        }
        if (reservations.hasOpen(titleId, memberId) || loans.hasOpenLoanOnTitle(memberId, titleId)) {
            throw deny("RESERVE", titleId.toString(), ErrorCode.ALREADY_RESERVED, "you already have this title reserved or on loan");
        }
        Reservation reservation = new Reservation();
        reservation.titleId = titleId;
        reservation.memberId = memberId;
        reservation.placedAt = clock.instant();
        reservation.status = WAITING;
        reservations.save(reservation);
        audit.log("RESERVE", titleId.toString(), "OK");
        return reservation.id;
    }

    /** Someone else's reservation answers NOT_FOUND, exactly like an unknown id. */
    @Transactional(noRollbackFor = ApiException.class)
    public void cancel(UUID memberId, UUID reservationId) {
        Reservation reservation = reservations.lockById(reservationId).filter(r -> r.memberId.equals(memberId)).orElse(null);
        if (reservation == null || (reservation.status != CANCELLED && !reservation.isOpen())) {
            throw deny("RESERVE_CANCEL", reservationId.toString(), ErrorCode.NOT_FOUND, "reservation not found");
        }
        if (reservation.status == CANCELLED) {
            return;
        }
        UUID held = reservation.status == READY ? reservation.heldCopyId : null;
        reservation.status = CANCELLED;
        reservation.heldCopyId = null;
        reservations.save(reservation);
        audit.log("RESERVE_CANCEL", reservationId.toString(), "OK");
        if (held != null) {
            copies.findById(held).ifPresent(this::promoteNext);
        }
    }

    /** A copy just became free (returned, or its hold ended): the oldest waiting reservation for the title gets it. */
    @Transactional
    public void promoteNext(Copy copy) {
        if (copy.status != Copy.Status.LENDABLE) {
            return;
        }
        reservations.findFirstByTitleIdAndStatusOrderBySeqAsc(copy.titleId, WAITING).ifPresent(next -> {
            next.status = READY;
            next.readyAt = clock.instant();
            next.heldCopyId = copy.id;
            reservations.save(next);
            audit.log(Actor.SYSTEM, null, "RESERVE_READY", next.id.toString(), copy.id.toString());
        });
    }

    /** Holds that sat unclaimed past the pickup window expire and pass the copy on. */
    @Transactional
    public int expireHolds() {
        Instant cutoff = clock.instant().minus(props.loan().reservationHold());
        int expired = 0;
        for (Reservation reservation : reservations.findByStatusAndReadyAtBefore(READY, cutoff)) {
            UUID held = reservation.heldCopyId;
            reservation.status = EXPIRED;
            reservation.heldCopyId = null;
            reservations.save(reservation);
            audit.log(Actor.SYSTEM, null, "RESERVE_EXPIRE", reservation.id.toString(), "OK");
            copies.findById(held).ifPresent(this::promoteNext);
            expired++;
        }
        return expired;
    }

    private ApiException deny(String action, String target, ErrorCode code, String detail) {
        audit.log(action, target, "DENIED_" + code);
        return new ApiException(code, detail);
    }
}
