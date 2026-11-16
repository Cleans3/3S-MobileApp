package shelftap.server.web;

/** The `code` values of server/API.md. Clients branch on these, never on `detail`. */
public enum ErrorCode {
    VALIDATION_FAILED(400, "Validation failed"),
    INVALID_CREDENTIALS(401, "Invalid credentials"),
    ACCOUNT_LOCKED(423, "Account temporarily locked"),
    TOKEN_INVALID(401, "Token invalid"),
    TOKEN_REUSED(401, "Refresh token reused"),
    PASSWORD_CHANGE_REQUIRED(403, "Password change required"),
    STEP_UP_REQUIRED(403, "Password login required"),
    FORBIDDEN(403, "Forbidden"),
    NOT_FOUND(404, "Not found"),
    CARD_NOT_ACTIVE(403, "Card not active"),
    CARD_AUTH_FAILED(401, "Card authentication failed"),
    CHALLENGE_EXPIRED(401, "Challenge expired"),
    COPY_NOT_AVAILABLE(409, "Copy not available"),
    LOAN_LIMIT_REACHED(409, "Loan limit reached"),
    UNPAID_FINE(409, "Unpaid fine"),
    RENEWAL_NOT_ALLOWED(409, "Renewal not allowed"),
    RESERVED_BY_OTHER(409, "Reserved by another member"),
    ALREADY_RESERVED(409, "Already reserved"),
    KEY_ALREADY_REGISTERED(409, "Card key already registered"),
    RATE_LIMITED(429, "Too many requests"),
    INTERNAL_ERROR(500, "Internal error");

    public final int status;
    public final String title;

    ErrorCode(int status, String title) {
        this.status = status;
        this.title = title;
    }
}
