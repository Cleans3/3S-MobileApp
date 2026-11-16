package shelftap.server.audit;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Clock;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Writes the audit row inside the caller's transaction, so a committed action always has its row and a rolled-back one has none.
 * Denials are logged by services that commit despite the ApiException (noRollbackFor), after all their checks and before any write.
 */
@Service
public class AuditService {
    private static final Set<String> INTEGRITY_FLAGS = Set.of("ok", "root", "emulator", "debugger", "tampered");

    public record Actor(String type, String id) {
        public static final Actor ANON = new Actor("ANON", null);
        public static final Actor SYSTEM = new Actor("SYSTEM", null);
        public static final Actor GATE = new Actor("GATE", null);

        public static Actor member(UUID id) {
            return new Actor("MEMBER", id.toString());
        }

        public static Actor staff(UUID id) {
            return new Actor("STAFF", id.toString());
        }
    }

    private final AuditLogRepository repo;
    private final Clock clock;

    AuditService(AuditLogRepository repo, Clock clock) {
        this.repo = repo;
        this.clock = clock;
    }

    /** For an authenticated call: actor and device come from the verified token. */
    public void log(String action, String target, String outcome) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
            String type = "MEMBER".equals(jwt.getClaimAsString("role")) ? "MEMBER" : "STAFF";
            write(new Actor(type, jwt.getSubject()), jwt.getClaimAsString("did"), action, target, outcome);
        } else {
            write(Actor.ANON, null, action, target, outcome);
        }
    }

    public void log(Actor actor, UUID deviceId, String action, String target, String outcome) {
        write(actor, deviceId == null ? null : deviceId.toString(), action, target, outcome);
    }

    private void write(Actor actor, String deviceId, String action, String target, String outcome) {
        AuditLog row = new AuditLog();
        row.at = clock.instant();
        row.actorType = actor.type();
        row.actorId = actor.id();
        row.action = action;
        row.target = target;
        row.outcome = outcome;
        HttpServletRequest request = currentRequest();
        if (request != null) {
            row.ip = request.getRemoteAddr();
            row.integrity = integrity(request.getHeader("X-Device-Integrity"));
        }
        row.deviceId = deviceId;
        repo.save(row);
    }

    /** Only the known flags are stored, so a client cannot write free text into the log. */
    static String integrity(String header) {
        if (header == null) {
            return null;
        }
        List<String> known = Arrays.stream(header.split(",")).map(String::trim)
                .filter(INTEGRITY_FLAGS::contains).distinct().toList();
        return known.isEmpty() ? "malformed" : known.stream().collect(Collectors.joining(","));
    }

    private static HttpServletRequest currentRequest() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        return attributes instanceof ServletRequestAttributes servlet ? servlet.getRequest() : null;
    }
}
