package shelftap.server.member;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface StaffRepository extends JpaRepository<Staff, UUID> {
    @Query("select s from Staff s where lower(s.username) = lower(:username)")
    Optional<Staff> findByUsername(String username);
}
