package shelftap.server.web;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import shelftap.server.config.ShelfTapProperties;

import java.time.Clock;
import java.util.concurrent.ConcurrentHashMap;

/** Fixed-window counter per client IP, in memory. One instance only; a second node needs a shared store. */
@Component
public class RateLimiter {
    private record Window(long startMs, int count) {
    }

    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();
    private final Clock clock;
    private final int max;
    private final long windowMs;

    RateLimiter(Clock clock, ShelfTapProperties props) {
        this.clock = clock;
        this.max = props.rateLimit().maxRequests();
        this.windowMs = props.rateLimit().window().toMillis();
    }

    /** Counts one request. Returns 0 when allowed, else the seconds until the window resets. */
    public int hit(String key) {
        long now = clock.millis();
        int[] retryAfter = {0};
        windows.compute(key, (k, cur) -> {
            if (cur == null || now - cur.startMs() >= windowMs) {
                return new Window(now, 1);
            }
            if (cur.count() >= max) {
                retryAfter[0] = (int) Math.max(1, (cur.startMs() + windowMs - now + 999) / 1000);
                return cur;
            }
            return new Window(cur.startMs(), cur.count() + 1);
        });
        return retryAfter[0];
    }

    @Scheduled(fixedDelayString = "PT1M", initialDelayString = "PT1M")
    void evictStale() {
        long now = clock.millis();
        windows.values().removeIf(w -> now - w.startMs() >= windowMs);
    }
}
