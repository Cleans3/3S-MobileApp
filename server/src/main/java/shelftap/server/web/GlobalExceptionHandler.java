package shelftap.server.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.stream.Collectors;

/** Every error leaves as problem+json with a stable `code`. Unexpected faults are logged here and never shown to the client. */
@RestControllerAdvice
class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    ResponseEntity<Object> api(ApiException e) {
        return Problems.response(e.code, e.code.status, e.getMessage(), e.retryAfterSeconds);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Object> unexpected(Exception e) {
        log.error("unhandled exception", e);
        return Problems.response(ErrorCode.INTERNAL_ERROR, 500, "internal error", null);
    }

    /** All Spring MVC errors (bad JSON, bad types, validation, 404, 405, 415) funnel through here. */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
                                                             HttpStatusCode status, WebRequest request) {
        if (status.is5xxServerError()) {
            log.error("server error", ex);
            return Problems.response(ErrorCode.INTERNAL_ERROR, status.value(), "internal error", null);
        }
        ErrorCode code = status.value() == 404 ? ErrorCode.NOT_FOUND : ErrorCode.VALIDATION_FAILED;
        String detail = ex instanceof MethodArgumentNotValidException invalid
                ? invalid.getBindingResult().getFieldErrors().stream()
                        .map(f -> f.getField() + ": " + f.getDefaultMessage()).sorted().collect(Collectors.joining("; "))
                : "invalid request";
        return Problems.response(code, status.value(), detail, null);
    }
}
