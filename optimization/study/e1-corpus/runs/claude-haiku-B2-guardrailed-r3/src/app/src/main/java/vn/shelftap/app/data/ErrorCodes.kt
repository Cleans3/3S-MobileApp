package vn.shelftap.app.data

/** Error codes returned by the API in the "code" field, plus app-side codes. */
object ErrorCodes {
    const val INVALID_CREDENTIALS = "INVALID_CREDENTIALS"
    const val ACCOUNT_LOCKED = "ACCOUNT_LOCKED"
    const val RATE_LIMITED = "RATE_LIMITED"
    const val TOKEN_INVALID = "TOKEN_INVALID"
    const val PASSWORD_CHANGE_REQUIRED = "PASSWORD_CHANGE_REQUIRED"

    const val UNKNOWN = "UNKNOWN"
    const val INVALID_RESPONSE = "INVALID_RESPONSE"
    const val NETWORK = "NETWORK"
    const val FIELDS_REQUIRED = "FIELDS_REQUIRED"
    const val PASSWORD_MISMATCH = "PASSWORD_MISMATCH"
}
