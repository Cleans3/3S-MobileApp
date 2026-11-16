package shelftap.server.loan;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FineRepository extends JpaRepository<Fine, UUID> {
    boolean existsByMemberIdAndPaidAtIsNull(UUID memberId);

    List<Fine> findByMemberIdOrderByAssessedAtDesc(UUID memberId);
}
