package shelftap.server.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneOffset;

@Configuration
class AppConfig {
    /** Whole seconds, so every stored and returned instant reads like `2026-11-16T10:00:00Z`. Tests replace this bean. */
    @Bean
    Clock clock() {
        return Clock.tickSeconds(ZoneOffset.UTC);
    }
}
