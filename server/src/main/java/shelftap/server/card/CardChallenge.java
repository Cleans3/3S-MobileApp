package shelftap.server.card;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "card_challenge")
public class CardChallenge {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public UUID id;
    public String cardId;
    /** Base64 of the 32 random bytes. Not secret: the signature is what proves the card. */
    public String nonce;
    public Instant expiresAt;
    public Instant usedAt;
}
