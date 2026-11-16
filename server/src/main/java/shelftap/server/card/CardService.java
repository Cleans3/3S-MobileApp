package shelftap.server.card;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import shelftap.server.audit.AuditService;
import shelftap.server.audit.AuditService.Actor;
import shelftap.server.auth.AuthService;
import shelftap.server.auth.RefreshToken;
import shelftap.server.auth.RefreshTokenRepository;
import shelftap.server.auth.TokenPair;
import shelftap.server.auth.TokenService;
import shelftap.server.member.Member;
import shelftap.server.member.MemberRepository;
import shelftap.server.web.ApiException;
import shelftap.server.web.ErrorCode;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

/** Card login (C7) and card issuing. Denials commit their audit row (noRollbackFor); no write precedes a denial. */
@Service
public class CardService {
    static final Duration CHALLENGE_TTL = Duration.ofSeconds(60);
    private static final Pattern CARD_ID = Pattern.compile("[0-9A-F]{16}");
    private static final BigInteger EXPONENT = BigInteger.valueOf(65537);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final CardRepository cards;
    private final CardChallengeRepository challenges;
    private final MemberRepository members;
    private final RefreshTokenRepository refreshTokens;
    private final TokenService tokens;
    private final CardCrypto crypto;
    private final AuditService audit;
    private final Clock clock;

    CardService(CardRepository cards, CardChallengeRepository challenges, MemberRepository members,
                RefreshTokenRepository refreshTokens, TokenService tokens, CardCrypto crypto, AuditService audit, Clock clock) {
        this.cards = cards;
        this.challenges = challenges;
        this.members = members;
        this.refreshTokens = refreshTokens;
        this.tokens = tokens;
        this.crypto = crypto;
        this.audit = audit;
        this.clock = clock;
    }

    /** A fresh single-use nonce, bound to this card and valid for 60 s. */
    @Transactional(noRollbackFor = ApiException.class)
    public String challenge(String cardId) {
        Card card = find(cardId);
        if (card.status != Card.Status.ACTIVE) {
            audit.log(Actor.ANON, null, "CARD_CHALLENGE", cardId, "CARD_" + card.status);
            throw new ApiException(ErrorCode.CARD_NOT_ACTIVE, "card is " + card.status.name().toLowerCase());
        }
        byte[] nonce = new byte[32];
        RANDOM.nextBytes(nonce);
        CardChallenge challenge = new CardChallenge();
        challenge.cardId = cardId;
        challenge.nonce = Base64.getEncoder().encodeToString(nonce);
        challenge.expiresAt = clock.instant().plus(CHALLENGE_TTL);
        challenges.save(challenge);
        audit.log(Actor.ANON, null, "CARD_CHALLENGE", cardId, "OK");
        return challenge.nonce;
    }

    /** C7. The nonce is spent first and whatever follows, so a replayed or retried nonce never gets a second attempt. */
    @Transactional(noRollbackFor = ApiException.class)
    public TokenPair authenticate(String cardId, String nonceB64, String signatureB64, UUID deviceId) {
        checkFormat(cardId);
        byte[] nonce = decode(nonceB64);
        byte[] signature = decode(signatureB64);
        if (challenges.consume(cardId, Base64.getEncoder().encodeToString(nonce), clock.instant()) == 0) {
            audit.log(Actor.ANON, deviceId, "CARD_AUTH", cardId, "CHALLENGE_EXPIRED");
            throw new ApiException(ErrorCode.CHALLENGE_EXPIRED, "challenge unknown, used or expired");
        }
        Card card = find(cardId);
        if (card.status != Card.Status.ACTIVE) {
            audit.log(Actor.ANON, deviceId, "CARD_AUTH", cardId, "CARD_" + card.status);
            throw new ApiException(ErrorCode.CARD_NOT_ACTIVE, "card is " + card.status.name().toLowerCase());
        }
        if (nonce.length != 32 || !crypto.verify(card, nonce, signature)) {
            audit.log(Actor.ANON, deviceId, "CARD_AUTH", cardId, "BAD_SIGNATURE");
            throw new ApiException(ErrorCode.CARD_AUTH_FAILED, "signature does not verify");
        }
        Member member = members.findById(card.memberId).orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "member not found"));
        audit.log(Actor.member(member.id), deviceId, "CARD_AUTH", cardId, "OK");
        return tokens.issue(AuthService.memberSubject(member), deviceId, RefreshToken.Method.CARD, cardId, null);
    }

    /** New card for a member. The member's earlier live cards are revoked (one live card per member, card-spec §5.1). */
    @Transactional
    public CardIssue issue(UUID staffId, UUID memberId) {
        Member member = members.findById(memberId).orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "member not found"));
        for (Card old : cards.findByMemberIdAndStatusIn(member.id, List.of(Card.Status.ACTIVE, Card.Status.BLOCKED))) {
            applyStatus(old.cardId, old, Card.Status.REVOKED);
        }
        String cardId;
        do {
            byte[] id = new byte[8];
            RANDOM.nextBytes(id);
            cardId = HexFormat.of().withUpperCase().formatHex(id);
        } while (cards.existsById(cardId));
        Card card = new Card();
        card.cardId = cardId;
        card.memberId = member.id;
        card.status = Card.Status.ACTIVE;
        card.issuedAt = clock.instant();
        cards.save(card);
        audit.log(Actor.staff(staffId), null, "CARD_ISSUE", cardId, "OK");
        return new CardIssue(cardId, Base64.getEncoder().encodeToString(crypto.adminKey(cardId)));
    }

    /** Write-once. A second, different key would let a staff account forge card logins for any member. */
    @Transactional(noRollbackFor = ApiException.class)
    public void registerKey(UUID staffId, String cardId, String modulusB64, String exponentB64) {
        checkFormat(cardId);
        byte[] modulus = decode(modulusB64);
        byte[] exponent = decode(exponentB64);
        boolean wellFormed = modulus.length == 256 && (modulus[0] & 0x80) != 0 && (modulus[255] & 1) == 1
                && new BigInteger(1, exponent).equals(EXPONENT);
        if (!wellFormed) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "key must be RSA-2048 with exponent 65537");
        }
        Card card = cards.lockById(cardId).orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "card not found"));
        if (card.status == Card.Status.REVOKED) {
            throw new ApiException(ErrorCode.CARD_NOT_ACTIVE, "card is revoked");
        }
        if (card.publicKeyModulus != null) {
            boolean same = Arrays.equals(card.publicKeyModulus, modulus) && Arrays.equals(card.publicKeyExponent, trimmed(exponent));
            audit.log(Actor.staff(staffId), null, "CARD_KEY", cardId, same ? "UNCHANGED" : "REFUSED_OVERWRITE");
            if (!same) {
                throw new ApiException(ErrorCode.KEY_ALREADY_REGISTERED, "a different key is already registered for this card");
            }
            return;
        }
        card.publicKeyModulus = modulus;
        card.publicKeyExponent = trimmed(exponent);
        cards.save(card);
        audit.log(Actor.staff(staffId), null, "CARD_KEY", cardId, "REGISTERED");
    }

    /** REVOKED is final. Leaving ACTIVE ends every session that was opened by tapping this card. */
    @Transactional
    public void setStatus(UUID staffId, String cardId, Card.Status status) {
        checkFormat(cardId);
        Card card = cards.lockById(cardId).orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "card not found"));
        if (card.status == Card.Status.REVOKED && status != Card.Status.REVOKED) {
            throw new ApiException(ErrorCode.CARD_NOT_ACTIVE, "a revoked card cannot be reinstated");
        }
        applyStatus(cardId, card, status);
        audit.log(Actor.staff(staffId), null, "CARD_STATUS", cardId, status.name());
    }

    private void applyStatus(String cardId, Card card, Card.Status status) {
        card.status = status;
        cards.save(card);
        if (status != Card.Status.ACTIVE) {
            refreshTokens.revokeForCard(cardId, clock.instant());
            challenges.deleteForCard(cardId);
        }
    }

    private Card find(String cardId) {
        checkFormat(cardId);
        return cards.findById(cardId).orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "card not found"));
    }

    private static void checkFormat(String cardId) {
        if (!CARD_ID.matcher(cardId).matches()) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "cardId must be 16 uppercase hex characters");
        }
    }

    private static byte[] decode(String base64) {
        try {
            return Base64.getDecoder().decode(base64);
        } catch (IllegalArgumentException e) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "value is not valid Base64");
        }
    }

    /** Exponent without leading zero bytes, so equal keys compare equal however the client padded them. */
    private static byte[] trimmed(byte[] exponent) {
        byte[] unsigned = new BigInteger(1, exponent).toByteArray();
        return unsigned[0] == 0 && unsigned.length > 1 ? Arrays.copyOfRange(unsigned, 1, unsigned.length) : unsigned;
    }

    public record CardIssue(String cardId, String adminKey) {
    }
}
