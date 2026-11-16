package shelftap.server.auth;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import shelftap.server.audit.AuditService;
import shelftap.server.audit.AuditService.Actor;
import shelftap.server.card.Card;
import shelftap.server.card.CardRepository;
import shelftap.server.member.Member;
import shelftap.server.member.MemberRepository;
import shelftap.server.member.Staff;
import shelftap.server.member.StaffRepository;
import shelftap.server.security.Hashing;
import shelftap.server.security.Passwords;
import shelftap.server.web.ApiException;
import shelftap.server.web.ErrorCode;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import static shelftap.server.auth.RefreshToken.Method.PWD;
import static shelftap.server.auth.RefreshToken.SubjectType.MEMBER;
import static shelftap.server.auth.RefreshToken.SubjectType.STAFF;

/**
 * Password login, staff login, refresh rotation, logout and password change.
 * Every method commits its failure bookkeeping (attempt counters, family revocation, audit rows) even when it ends in an ApiException.
 */
@Service
public class AuthService {
    private final MemberRepository members;
    private final StaffRepository staff;
    private final CardRepository cards;
    private final RefreshTokenRepository refreshTokens;
    private final TokenService tokens;
    private final LockoutService lockout;
    private final Passwords passwords;
    private final AuditService audit;
    private final Clock clock;

    AuthService(MemberRepository members, StaffRepository staff, CardRepository cards, RefreshTokenRepository refreshTokens,
                TokenService tokens, LockoutService lockout, Passwords passwords, AuditService audit, Clock clock) {
        this.members = members;
        this.staff = staff;
        this.cards = cards;
        this.refreshTokens = refreshTokens;
        this.tokens = tokens;
        this.lockout = lockout;
        this.passwords = passwords;
        this.audit = audit;
        this.clock = clock;
    }

    @Transactional(noRollbackFor = ApiException.class)
    public TokenPair login(String email, String password, UUID deviceId) {
        String key = "m:" + normalise(email);
        lockedCheck(key, Actor.ANON, deviceId, "LOGIN");
        Optional<Member> found = members.findByEmail(email.strip());
        // Same work and same answer whether the email exists or not (no user enumeration).
        if (!passwords.matches(password, found.map(m -> m.passwordHash).orElse(null))) {
            lockout.recordFailure(key);
            audit.log(Actor.ANON, deviceId, "LOGIN", null, "FAILED");
            throw new ApiException(ErrorCode.INVALID_CREDENTIALS, "invalid email or password");
        }
        Member member = found.get();
        lockout.clear(key);
        audit.log(Actor.member(member.id), deviceId, "LOGIN", member.id.toString(), "OK");
        return tokens.issue(memberSubject(member), deviceId, PWD, null, null);
    }

    @Transactional(noRollbackFor = ApiException.class)
    public TokenPair staffLogin(String username, String password, UUID deviceId) {
        String key = "s:" + normalise(username);
        lockedCheck(key, Actor.ANON, deviceId, "STAFF_LOGIN");
        Optional<Staff> found = staff.findByUsername(username.strip()).filter(s -> s.active);
        if (!passwords.matches(password, found.map(s -> s.passwordHash).orElse(null))) {
            lockout.recordFailure(key);
            audit.log(Actor.ANON, deviceId, "STAFF_LOGIN", null, "FAILED");
            throw new ApiException(ErrorCode.INVALID_CREDENTIALS, "invalid username or password");
        }
        Staff account = found.get();
        lockout.clear(key);
        audit.log(Actor.staff(account.id), deviceId, "STAFF_LOGIN", account.id.toString(), "OK");
        return tokens.issue(new TokenService.Subject(STAFF, account.id, account.role.name(), false), deviceId, PWD, null, null);
    }

    /** Rotation: every use of a refresh token retires it and issues the next one in the same family. */
    @Transactional(noRollbackFor = ApiException.class)
    public TokenPair refresh(String rawToken, UUID deviceId) {
        Instant now = clock.instant();
        RefreshToken token = refreshTokens.lockByHash(Hashing.sha256Hex(rawToken)).orElse(null);
        if (token == null || token.revokedAt != null || !token.expiresAt.isAfter(now)) {
            audit.log(Actor.ANON, deviceId, "REFRESH", null, "INVALID");
            throw new ApiException(ErrorCode.TOKEN_INVALID, "refresh token invalid");
        }
        Actor actor = new Actor(token.subjectType.name(), token.subjectId.toString());
        if (token.usedAt != null) {
            // A retired token came back: someone holds a copy. Kill the whole family, the thief and the owner both.
            refreshTokens.revokeFamily(token.familyId, now);
            audit.log(actor, deviceId, "REFRESH", token.familyId.toString(), "REUSE_FAMILY_REVOKED");
            throw new ApiException(ErrorCode.TOKEN_REUSED, "refresh token was already used; sign in again");
        }
        if (!token.deviceId.equals(deviceId)) {
            refreshTokens.revokeFamily(token.familyId, now);
            audit.log(actor, deviceId, "REFRESH", token.familyId.toString(), "DEVICE_MISMATCH_FAMILY_REVOKED");
            throw new ApiException(ErrorCode.TOKEN_INVALID, "refresh token invalid");
        }
        TokenService.Subject subject = resubject(token);
        if (subject == null) {
            refreshTokens.revokeFamily(token.familyId, now);
            audit.log(actor, deviceId, "REFRESH", token.familyId.toString(), "SUBJECT_GONE");
            throw new ApiException(ErrorCode.TOKEN_INVALID, "refresh token invalid");
        }
        token.usedAt = now;
        audit.log(actor, deviceId, "REFRESH", token.familyId.toString(), "OK");
        return tokens.issue(subject, token.deviceId, token.method, token.cardId, token.familyId);
    }

    /** Always 204 for the caller: a probe learns nothing about which tokens exist. */
    @Transactional
    public void logout(String rawToken) {
        refreshTokens.lockByHash(Hashing.sha256Hex(rawToken)).ifPresent(t -> {
            refreshTokens.revokeFamily(t.familyId, clock.instant());
            audit.log(new Actor(t.subjectType.name(), t.subjectId.toString()), t.deviceId, "LOGOUT", t.familyId.toString(), "OK");
        });
    }

    /** Needs the current password (counted by the lockout), then ends every other session of the member. */
    @Transactional(noRollbackFor = ApiException.class)
    public void changePassword(UUID memberId, UUID currentFamily, String current, String next) {
        Member member = members.findById(memberId).orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "member not found"));
        String key = "m:" + normalise(member.email);
        lockedCheck(key, Actor.member(memberId), null, "PASSWORD_CHANGE");
        if (!passwords.matches(current, member.passwordHash)) {
            lockout.recordFailure(key);
            audit.log("PASSWORD_CHANGE", memberId.toString(), "WRONG_CURRENT");
            throw new ApiException(ErrorCode.INVALID_CREDENTIALS, "current password is wrong");
        }
        if (passwords.matches(next, member.passwordHash)) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "newPassword: must differ from the current password");
        }
        lockout.clear(key);
        member.passwordHash = passwords.hash(next);
        member.mustChangePassword = false;
        members.save(member);
        int ended = refreshTokens.revokeAllExcept(memberId, currentFamily, clock.instant());
        audit.log("PASSWORD_CHANGE", memberId.toString(), "OK_SESSIONS_ENDED_" + ended);
    }

    private void lockedCheck(String key, Actor actor, UUID deviceId, String action) {
        try {
            lockout.assertNotLocked(key);
        } catch (ApiException locked) {
            audit.log(actor, deviceId, action, null, "LOCKED");
            throw locked;
        }
    }

    private TokenService.Subject resubject(RefreshToken token) {
        if (token.subjectType == MEMBER) {
            Optional<Member> member = members.findById(token.subjectId);
            if (member.isEmpty() || (token.cardId != null && !cardActive(token.cardId))) {
                return null;
            }
            return memberSubject(member.get());
        }
        return staff.findById(token.subjectId).filter(s -> s.active)
                .map(s -> new TokenService.Subject(STAFF, s.id, s.role.name(), false)).orElse(null);
    }

    private boolean cardActive(String cardId) {
        return cards.findById(cardId).map(c -> c.status == Card.Status.ACTIVE).orElse(false);
    }

    public static TokenService.Subject memberSubject(Member member) {
        return new TokenService.Subject(MEMBER, member.id, "MEMBER", member.mustChangePassword);
    }

    private static String normalise(String name) {
        return name.strip().toLowerCase(Locale.ROOT);
    }
}
