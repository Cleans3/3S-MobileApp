package shelftap.server.card;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.UUID;

public interface CardChallengeRepository extends JpaRepository<CardChallenge, UUID> {
    /** Atomic single use: exactly one caller flips `usedAt`, so a nonce can never be redeemed twice. Returns 1 or 0. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update CardChallenge c set c.usedAt = :now where c.cardId = :cardId and c.nonce = :nonce "
            + "and c.usedAt is null and c.expiresAt > :now")
    int consume(String cardId, String nonce, Instant now);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from CardChallenge c where c.cardId = :cardId")
    int deleteForCard(String cardId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from CardChallenge c where c.expiresAt < :cutoff")
    int purgeExpired(Instant cutoff);
}
