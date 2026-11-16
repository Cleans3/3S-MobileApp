package shelftap.server.catalog;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface CopyRepository extends JpaRepository<Copy, UUID> {
    /** Row lock: two taps on the same tag run one after the other, so only the first can win. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Copy c where c.tagId = :tagId")
    Optional<Copy> lockByTagId(String tagId);

    Optional<Copy> findByTagId(String tagId);
}
