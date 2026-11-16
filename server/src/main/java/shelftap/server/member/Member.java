package shelftap.server.member;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Persistence row, public fields on purpose: no logic lives here, services own the rules. */
@Entity
@Table(name = "member")
public class Member {
    public enum Type { STUDENT, STAFF, PUBLIC }

    public enum Status { ACTIVE, SUSPENDED }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public UUID id;
    public String fullName;
    public String email;
    public LocalDate dateOfBirth;
    public String cccd;
    public String phone;
    @Enumerated(EnumType.STRING)
    public Type memberType;
    @Enumerated(EnumType.STRING)
    public Status status;
    public String passwordHash;
    public boolean mustChangePassword;
    public boolean historyEnabled;
    public Instant createdAt;
}
