package shelftap.server.member;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface MemberRepository extends JpaRepository<Member, UUID> {
    @Query("select m from Member m where lower(m.email) = lower(:email)")
    Optional<Member> findByEmail(String email);

    /** Row lock: serialises one member's concurrent checkouts and reservations. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Member m where m.id = :id")
    Optional<Member> lockById(UUID id);

    @Query("select count(m) > 0 from Member m where lower(m.email) = lower(:email)")
    boolean emailTaken(String email);

    boolean existsByCccd(String cccd);
}
