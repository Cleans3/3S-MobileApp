package shelftap.server.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.util.regex.Pattern;

/** Per-IP rate limit on every unauthenticated credential endpoint: password login, refresh, staff login, card challenge and auth. */
public class RateLimitFilter extends OncePerRequestFilter {
    private static final Pattern LIMITED = Pattern.compile(
            "/api/auth/(login|refresh)|/api/staff/login|/api/cards/[^/]+/(challenge|auth)");

    private final RateLimiter limiter;
    private final JsonMapper mapper;

    public RateLimitFilter(RateLimiter limiter, JsonMapper mapper) {
        this.limiter = limiter;
        this.mapper = mapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // The path is decoded, so /api/auth/lo%67in cannot slip past the match.
        return !"POST".equals(request.getMethod()) || !LIMITED.matcher(Paths.of(request)).matches();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        int retryAfter = limiter.hit(request.getRemoteAddr());
        if (retryAfter > 0) {
            Problems.write(response, mapper, ErrorCode.RATE_LIMITED, ErrorCode.RATE_LIMITED.status,
                    "too many requests", retryAfter);
            return;
        }
        chain.doFilter(request, response);
    }
}
