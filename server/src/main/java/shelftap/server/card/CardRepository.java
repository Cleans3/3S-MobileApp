package shelftap.server.card;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CardRepository extends JpaRepository<Card, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Card c where c.cardId = :cardId")
    Optional<Card> lockById(String cardId);

    List<Card> findByMemberIdAndStatusIn(UUID memberId, Collection<Card.Status> statuses);
}
