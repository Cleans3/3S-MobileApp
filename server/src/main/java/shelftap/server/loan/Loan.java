package shelftap.server.loan;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "loan")
public class Loan {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public UUID id;
    public UUID copyId;
    /** Null once the returned loan has been anonymised. */
    public UUID memberId;
    public Instant outAt;
    public Instant dueAt;
    public Instant returnedAt;
    public int renewals;
}
