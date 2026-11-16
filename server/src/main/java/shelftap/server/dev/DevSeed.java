package shelftap.server.dev;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import shelftap.server.member.Member;
import shelftap.server.member.MemberRepository;
import shelftap.server.member.Staff;
import shelftap.server.member.StaffRepository;
import shelftap.server.security.Passwords;

import java.time.Clock;
import java.time.LocalDate;

/** Dev profile only: one librarian and one demo member, passwords from env. The demo catalog is db/dev/V100. */
@Component
@Profile("dev")
class DevSeed implements ApplicationRunner {
    static final String LIBRARIAN = "librarian";
    static final String MEMBER_EMAIL = "member@shelftap.test";
    private static final Logger log = LoggerFactory.getLogger(DevSeed.class);

    private final StaffRepository staff;
    private final MemberRepository members;
    private final Passwords passwords;
    private final Clock clock;
    private final String librarianPassword;
    private final String memberPassword;

    DevSeed(StaffRepository staff, MemberRepository members, Passwords passwords, Clock clock,
            @Value("${SHELFTAP_DEV_LIBRARIAN_PASSWORD:}") String librarianPassword,
            @Value("${SHELFTAP_DEV_MEMBER_PASSWORD:}") String memberPassword) {
        this.staff = staff;
        this.members = members;
        this.passwords = passwords;
        this.clock = clock;
        this.librarianPassword = librarianPassword;
        this.memberPassword = memberPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (librarianPassword.isBlank() || memberPassword.isBlank()) {
            log.warn("SHELFTAP_DEV_LIBRARIAN_PASSWORD / SHELFTAP_DEV_MEMBER_PASSWORD not set: no dev accounts created");
            return;
        }
        if (staff.findByUsername(LIBRARIAN).isEmpty()) {
            Staff librarian = new Staff();
            librarian.username = LIBRARIAN;
            librarian.passwordHash = passwords.hash(librarianPassword);
            librarian.role = Staff.Role.LIBRARIAN;
            librarian.active = true;
            librarian.createdAt = clock.instant();
            staff.save(librarian);
            log.info("dev librarian '{}' created", LIBRARIAN);
        }
        if (members.findByEmail(MEMBER_EMAIL).isEmpty()) {
            Member member = new Member();
            member.fullName = "Demo Member";
            member.email = MEMBER_EMAIL;
            member.dateOfBirth = LocalDate.of(2000, 1, 1);
            member.cccd = "000000000001";
            member.phone = "0900000001";
            member.memberType = Member.Type.PUBLIC;
            member.status = Member.Status.ACTIVE;
            member.passwordHash = passwords.hash(memberPassword);
            member.mustChangePassword = false;
            member.historyEnabled = true;
            member.createdAt = clock.instant();
            members.save(member);
            log.info("dev member '{}' created", MEMBER_EMAIL);
        }
    }
}
