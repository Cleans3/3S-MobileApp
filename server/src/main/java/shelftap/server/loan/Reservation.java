package shelftap.server.loan;

import jakarta.persistence.Column;
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
@Table(name = "reservation")
public class Reservation {
    public enum Status { WAITING, READY, FULFILLED, CANCELLED, EXPIRED }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public UUID id;
    /** Queue position, assigned by the database. */
    @Column(insertable = false, updatable = false)
    public Long seq;
    public UUID titleId;
    public UUID memberId;
    public Instant placedAt;
    public Instant readyAt;
    /** The returned copy held for this member while the reservation is READY. */
    public UUID heldCopyId;
    @Enumerated(EnumType.STRING)
    public Status status;

    public boolean isOpen() {
        return status == Status.WAITING || status == Status.READY;
    }
}
