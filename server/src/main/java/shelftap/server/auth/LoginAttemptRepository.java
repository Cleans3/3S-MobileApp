package shelftap.server.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface LoginAttemptRepository extends JpaRepository<LoginAttempt, UUID> {
    long countByKeyHashAndAtAfter(String keyHash, Instant since);

    Optional<LoginAttempt> findFirstByKeyHashAndAtAfterOrderByAtAsc(String keyHash, Instant since);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from LoginAttempt a where a.keyHash = :keyHash")
    int clear(String keyHash);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from LoginAttempt a where a.at < :cutoff")
    int purgeBefore(Instant cutoff);
}
