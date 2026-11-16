package shelftap.server.card;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "card")
public class Card {
    public enum Status { ACTIVE, BLOCKED, LOST, REVOKED }

    /** 16 uppercase hex chars, the 8 bytes the applet reports. */
    @Id
    public String cardId;
    public UUID memberId;
    public byte[] publicKeyModulus;
    public byte[] publicKeyExponent;
    @Enumerated(EnumType.STRING)
    public Status status;
    public Instant issuedAt;
}
