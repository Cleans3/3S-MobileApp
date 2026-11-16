package shelftap.server;

import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.core.NestedExceptionUtils;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The server refuses to start without its three secrets. */
class StartupTest {
    static String startupFailure(Map<String, String> overrides) {
        Map<String, String> props = new HashMap<>();
        props.put("spring.datasource.url", IntegrationTest.POSTGRES.getJdbcUrl());
        props.put("spring.datasource.username", IntegrationTest.POSTGRES.getUsername());
        props.put("spring.datasource.password", IntegrationTest.POSTGRES.getPassword());
        props.put("shelftap.jwt.key", IntegrationTest.JWT_KEY);
        props.put("shelftap.master.key", IntegrationTest.MASTER_KEY);
        props.put("shelftap.gate.key", IntegrationTest.GATE_KEY);
        props.putAll(overrides);
        // Command-line arguments beat application.yml; SpringApplicationBuilder.properties() would only set defaults.
        String[] args = props.entrySet().stream().map(e -> "--" + e.getKey() + "=" + e.getValue()).toArray(String[]::new);
        Exception failure = assertThrows(Exception.class, () -> new SpringApplicationBuilder(ServerApplication.class)
                .web(WebApplicationType.NONE).run(args).close());
        return NestedExceptionUtils.getMostSpecificCause(failure).getMessage();
    }

    @Test
    void missingKeyStopsStartup() {
        assertEquals("SHELFTAP_JWT_KEY is not set", startupFailure(Map.of("shelftap.jwt.key", "")));
        assertEquals("SHELFTAP_MASTER_KEY is not set", startupFailure(Map.of("shelftap.master.key", "  ")));
        assertEquals("SHELFTAP_GATE_KEY is not set", startupFailure(Map.of("shelftap.gate.key", "")));
    }

    @Test
    void shortOrMalformedKeyStopsStartup() {
        assertEquals("SHELFTAP_JWT_KEY must decode to at least 32 bytes", startupFailure(Map.of("shelftap.jwt.key", "c2hvcnQ=")));
        assertEquals("SHELFTAP_MASTER_KEY is not valid Base64", startupFailure(Map.of("shelftap.master.key", "%%%not base64%%%")));
        assertTrue(startupFailure(Map.of("shelftap.gate.key", "AAAA")).contains("SHELFTAP_GATE_KEY"));
    }
}
