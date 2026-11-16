package shelftap.server.loan;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReservationRepository extends JpaRepository<Reservation, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Reservation r where r.id = :id")
    Optional<Reservation> lockById(UUID id);

    @Query("select count(r) > 0 from Reservation r where r.titleId = :titleId and r.memberId = :memberId "
            + "and r.status in (shelftap.server.loan.Reservation.Status.WAITING, shelftap.server.loan.Reservation.Status.READY)")
    boolean hasOpen(UUID titleId, UUID memberId);

    /** Anyone other than `memberId` waiting for, or holding a copy of, the title. */
    @Query("select count(r) > 0 from Reservation r where r.titleId = :titleId and r.memberId <> :memberId "
            + "and r.status in (shelftap.server.loan.Reservation.Status.WAITING, shelftap.server.loan.Reservation.Status.READY)")
    boolean othersOpen(UUID titleId, UUID memberId);

    Optional<Reservation> findFirstByTitleIdAndStatusOrderBySeqAsc(UUID titleId, Reservation.Status status);

    Optional<Reservation> findByHeldCopyIdAndStatus(UUID heldCopyId, Reservation.Status status);

    Optional<Reservation> findFirstByTitleIdAndMemberIdAndStatus(UUID titleId, UUID memberId, Reservation.Status status);

    List<Reservation> findByStatusAndReadyAtBefore(Reservation.Status status, Instant cutoff);
}
