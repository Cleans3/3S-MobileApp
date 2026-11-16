package shelftap.server.loan;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "fine")
public class Fine {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public UUID id;
    /** Null once the loan it came from has been anonymised. */
    public UUID loanId;
    public UUID memberId;
    public int amountVnd;
    public Instant assessedAt;
    public Instant paidAt;
}
