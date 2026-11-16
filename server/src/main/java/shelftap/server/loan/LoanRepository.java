package shelftap.server.loan;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface LoanRepository extends JpaRepository<Loan, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from Loan l where l.id = :id")
    Optional<Loan> lockById(UUID id);

    boolean existsByCopyIdAndReturnedAtIsNull(UUID copyId);

    long countByMemberIdAndReturnedAtIsNull(UUID memberId);

    Optional<Loan> findFirstByCopyIdAndReturnedAtIsNull(UUID copyId);

    @Query("select count(l) > 0 from Loan l, Copy c where c.id = l.copyId and l.memberId = :memberId "
            + "and c.titleId = :titleId and l.returnedAt is null")
    boolean hasOpenLoanOnTitle(UUID memberId, UUID titleId);
}
