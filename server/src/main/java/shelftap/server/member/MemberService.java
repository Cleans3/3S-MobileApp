package shelftap.server.member;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import shelftap.server.audit.AuditService;
import shelftap.server.loan.Retention;
import shelftap.server.security.Passwords;
import shelftap.server.web.ApiException;
import shelftap.server.web.ErrorCode;

import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;

@Service
public class MemberService {
    private final MemberRepository members;
    private final Passwords passwords;
    private final Retention retention;
    private final AuditService audit;
    private final Clock clock;

    MemberService(MemberRepository members, Passwords passwords, Retention retention, AuditService audit, Clock clock) {
        this.members = members;
        this.passwords = passwords;
        this.retention = retention;
        this.audit = audit;
        this.clock = clock;
    }

    public Member get(UUID memberId) {
        return members.findById(memberId).orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "member not found"));
    }

    /** Staff registers a member; the account starts with a one-time password the member must replace at first login. */
    @Transactional
    public UUID register(String fullName, String email, LocalDate dateOfBirth, String cccd, String phone,
                         Member.Type type, String initialPassword) {
        if (members.emailTaken(email) || members.existsByCccd(cccd)) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "email or cccd: already registered");
        }
        Member member = new Member();
        member.fullName = fullName.strip();
        member.email = email.strip();
        member.dateOfBirth = dateOfBirth;
        member.cccd = cccd;
        member.phone = phone;
        member.memberType = type;
        member.status = Member.Status.ACTIVE;
        member.passwordHash = passwords.hash(initialPassword);
        member.mustChangePassword = true;
        member.historyEnabled = true;
        member.createdAt = clock.instant();
        members.save(member);
        audit.log("MEMBER_REGISTER", member.id.toString(), "OK");
        return member.id;
    }

    /** Turning history off is final for what is already stored: returned loans lose their member link now. */
    @Transactional
    public Member setHistoryEnabled(UUID memberId, boolean enabled) {
        Member member = get(memberId);
        member.historyEnabled = enabled;
        members.save(member);
        if (!enabled) {
            retention.anonymise(memberId, clock.instant());
        }
        audit.log("PRIVACY_CHANGE", memberId.toString(), enabled ? "HISTORY_ON" : "HISTORY_OFF");
        return member;
    }
}
