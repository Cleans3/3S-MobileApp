package shelftap.server.member;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "staff")
public class Staff {
    public enum Role { LIBRARIAN, ADMIN }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public UUID id;
    public String username;
    public String passwordHash;
    @Enumerated(EnumType.STRING)
    public Role role;
    public boolean active;
    public Instant createdAt;
}
