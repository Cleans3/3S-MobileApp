package shelftap.server.web;

/** A business error that maps to one problem+json response. No stack trace: it is control flow, not a fault. */
public class ApiException extends RuntimeException {
    public final ErrorCode code;
    public final Integer retryAfterSeconds;

    public ApiException(ErrorCode code, String detail) {
        this(code, detail, null);
    }

    public ApiException(ErrorCode code, String detail, Integer retryAfterSeconds) {
        super(detail, null, false, false);
        this.code = code;
        this.retryAfterSeconds = retryAfterSeconds;
    }
}
