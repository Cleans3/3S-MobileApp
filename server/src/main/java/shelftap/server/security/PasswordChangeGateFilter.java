package shelftap.server.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.filter.OncePerRequestFilter;
import shelftap.server.member.MemberRepository;
import shelftap.server.web.ErrorCode;
import shelftap.server.web.Paths;
import shelftap.server.web.Problems;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * While a member must change their password, every member endpoint except GET /api/me and POST /api/me/password answers 403.
 * The flag is read from the database on each call, not from the token, so the gate lifts the moment the password changes.
 */
public class PasswordChangeGateFilter extends OncePerRequestFilter {
    private static final Pattern MEMBER_PATHS = Pattern.compile("/api/(me|loans|reservations)(/.*)?");

    private final MemberRepository members;
    private final JsonMapper mapper;

    public PasswordChangeGateFilter(MemberRepository members, JsonMapper mapper) {
        this.members = members;
        this.mapper = mapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = Paths.of(request);
        boolean allowed = ("GET".equals(request.getMethod()) && path.equals("/api/me"))
                || ("POST".equals(request.getMethod()) && path.equals("/api/me/password"));
        return allowed || !MEMBER_PATHS.matcher(path).matches();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Jwt jwt && "MEMBER".equals(jwt.getClaimAsString("role"))
                && members.findById(UUID.fromString(jwt.getSubject())).map(m -> m.mustChangePassword).orElse(false)) {
            Problems.write(response, mapper, ErrorCode.PASSWORD_CHANGE_REQUIRED, ErrorCode.PASSWORD_CHANGE_REQUIRED.status,
                    "change your password first", null);
            return;
        }
        chain.doFilter(request, response);
    }
}
