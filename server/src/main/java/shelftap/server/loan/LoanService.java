package shelftap.server.loan;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import shelftap.server.audit.AuditService;
import shelftap.server.audit.AuditService.Actor;
import shelftap.server.catalog.Copy;
import shelftap.server.catalog.CopyRepository;
import shelftap.server.config.ShelfTapProperties;
import shelftap.server.member.Member;
import shelftap.server.member.MemberRepository;
import shelftap.server.web.ApiException;
import shelftap.server.web.ErrorCode;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Circulation. The server decides every loan: the phone only sends a tag ID. Checks run before any write,
 * so a denial (which commits its audit row) leaves nothing else behind.
 */
@Service
public class LoanService {
    private static final Pattern TAG_ID = Pattern.compile("[0-9A-F]{16}");

    private final MemberRepository members;
    private final CopyRepository copies;
    private final LoanRepository loans;
    private final FineRepository fines;
    private final ReservationRepository reservations;
    private final ReservationService reservationService;
    private final AuditService audit;
    private final Clock clock;
    private final ShelfTapProperties.Loan policy;

    LoanService(MemberRepository members, CopyRepository copies, LoanRepository loans, FineRepository fines,
                ReservationRepository reservations, ReservationService reservationService, AuditService audit,
                Clock clock, ShelfTapProperties props) {
        this.members = members;
        this.copies = copies;
        this.loans = loans;
        this.fines = fines;
        this.reservations = reservations;
        this.reservationService = reservationService;
        this.audit = audit;
        this.clock = clock;
        this.policy = props.loan();
    }

    /**
     * Self-checkout. Locks the member row, then the copy row (always in that order, so no deadlock): the limit
     * check cannot be raced by two taps of one member, and two taps on one tag cannot both win.
     */
    @Transactional(noRollbackFor = ApiException.class)
    public UUID checkout(UUID memberId, String tagId) {
        if (!TAG_ID.matcher(tagId).matches()) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "tagId must be 16 uppercase hex characters");
        }
        Member member = members.lockById(memberId).orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "member not found"));
        Copy copy = copies.lockByTagId(tagId).orElseThrow(() -> deny("LOAN_OUT", tagId, ErrorCode.NOT_FOUND, "no book with this tag"));
        if (copy.status != Copy.Status.LENDABLE || loans.existsByCopyIdAndReturnedAtIsNull(copy.id)) {
            throw deny("LOAN_OUT", tagId, ErrorCode.COPY_NOT_AVAILABLE, "this copy is not available");
        }
        if (member.status != Member.Status.ACTIVE) {
            throw deny("LOAN_OUT", tagId, ErrorCode.FORBIDDEN, "membership suspended");
        }
        if (fines.existsByMemberIdAndPaidAtIsNull(memberId)) {
            throw deny("LOAN_OUT", tagId, ErrorCode.UNPAID_FINE, "pay your fine at the desk first");
        }
        if (loans.countByMemberIdAndReturnedAtIsNull(memberId) >= policy.maxLoans()) {
            throw deny("LOAN_OUT", tagId, ErrorCode.LOAN_LIMIT_REACHED, "you have reached the loan limit");
        }
        Reservation held = reservations.findByHeldCopyIdAndStatus(copy.id, Reservation.Status.READY).orElse(null);
        if (held != null && !held.memberId.equals(memberId)) {
            throw deny("LOAN_OUT", tagId, ErrorCode.RESERVED_BY_OTHER, "this copy is held for another member");
        }

        Instant now = clock.instant();
        Loan loan = new Loan();
        loan.copyId = copy.id;
        loan.memberId = memberId;
        loan.outAt = now;
        loan.dueAt = now.plus(Duration.ofDays(policy.days()));
        loans.save(loan);

        Reservation own = held != null ? held
                : reservations.findFirstByTitleIdAndMemberIdAndStatus(copy.titleId, memberId, Reservation.Status.WAITING).orElse(null);
        if (own != null) {
            own.status = Reservation.Status.FULFILLED;
            own.heldCopyId = null;
            reservations.save(own);
        }
        audit.log("LOAN_OUT", loan.id.toString(), "OK");
        return loan.id;
    }

    /** One renewal if the loan is still out, not overdue, and nobody else is waiting for the title. */
    @Transactional(noRollbackFor = ApiException.class)
    public UUID renew(UUID memberId, UUID loanId) {
        Loan loan = loans.lockById(loanId).filter(l -> memberId.equals(l.memberId))
                .orElseThrow(() -> deny("LOAN_RENEW", loanId.toString(), ErrorCode.NOT_FOUND, "loan not found"));
        Member member = members.findById(memberId).orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "member not found"));
        Instant now = clock.instant();
        String refusal = null;
        if (loan.returnedAt != null) {
            refusal = "this loan is already returned";
        } else if (member.status != Member.Status.ACTIVE) {
            refusal = "membership suspended";
        } else if (loan.renewals >= policy.maxRenewals()) {
            refusal = "no renewals left";
        } else if (now.isAfter(loan.dueAt)) {
            refusal = "overdue loans cannot be renewed";
        } else if (reservations.othersOpen(copies.findById(loan.copyId).orElseThrow().titleId, memberId)) {
            refusal = "another member is waiting for this title";
        }
        if (refusal != null) {
            throw deny("LOAN_RENEW", loanId.toString(), ErrorCode.RENEWAL_NOT_ALLOWED, refusal);
        }
        loan.dueAt = loan.dueAt.plus(Duration.ofDays(policy.days()));
        loan.renewals++;
        loans.save(loan);
        audit.log("LOAN_RENEW", loanId.toString(), "OK");
        return loan.id;
    }

    /**
     * Desk return. No HTTP route yet (API.md has none; the desktop slice adds it). Charges the overdue fine,
     * hands the copy to the oldest waiting reservation, and drops the member link at once if history is off.
     */
    @Transactional
    public void checkIn(UUID loanId) {
        Loan loan = loans.lockById(loanId).orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "loan not found"));
        if (loan.returnedAt != null) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "loan already returned");
        }
        Instant now = clock.instant();
        loan.returnedAt = now;
        boolean fined = false;
        if (now.isAfter(loan.dueAt)) {
            long overdueDays = (Duration.between(loan.dueAt, now).toSeconds() + 86_399) / 86_400;
            Fine fine = new Fine();
            fine.loanId = loan.id;
            fine.memberId = loan.memberId;
            fine.amountVnd = Math.toIntExact(overdueDays * policy.finePerDayVnd());
            fine.assessedAt = now;
            fines.save(fine);
            fined = true;
        }
        UUID memberId = loan.memberId;
        if (!fined && !members.findById(memberId).orElseThrow().historyEnabled) {
            loan.memberId = null;
        }
        loans.save(loan);
        reservationService.promoteNext(copies.findById(loan.copyId).orElseThrow());
        audit.log(Actor.SYSTEM, null, "LOAN_RETURN", loanId.toString(), fined ? "OK_FINED" : "OK");
    }

    private ApiException deny(String action, String target, ErrorCode code, String detail) {
        audit.log(action, target, "DENIED_" + code);
        return new ApiException(code, detail);
    }
}
