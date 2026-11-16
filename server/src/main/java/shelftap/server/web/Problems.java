package shelftap.server.web;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/** Builds the `application/problem+json` body of API.md, for controllers and for filters alike. */
public final class Problems {
    private Problems() {
    }

    public static Map<String, Object> body(ErrorCode code, int status, String detail) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", "urn:shelftap:error:" + code.name());
        body.put("title", code.title);
        body.put("status", status);
        body.put("detail", detail);
        body.put("code", code.name());
        return body;
    }

    public static ResponseEntity<Object> response(ErrorCode code, int status, String detail, Integer retryAfterSeconds) {
        ResponseEntity.BodyBuilder builder = ResponseEntity.status(status).contentType(MediaType.APPLICATION_PROBLEM_JSON);
        if (retryAfterSeconds != null) {
            builder.header(HttpHeaders.RETRY_AFTER, retryAfterSeconds.toString());
        }
        return builder.body(body(code, status, detail));
    }

    /** For servlet filters and security handlers, which run before Spring MVC's exception handling. */
    public static void write(HttpServletResponse response, JsonMapper mapper, ErrorCode code, int status,
                             String detail, Integer retryAfterSeconds) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        if (retryAfterSeconds != null) {
            response.setHeader(HttpHeaders.RETRY_AFTER, retryAfterSeconds.toString());
        }
        mapper.writeValue(response.getOutputStream(), body(code, status, detail));
    }
}
