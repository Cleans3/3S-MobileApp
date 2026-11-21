package vn.shelftap.app.ui

import java.io.IOException
import vn.shelftap.app.net.ApiException
import vn.shelftap.app.net.RENEWAL_NOT_ALLOWED_CODE

/** A user-facing failure. The UI maps each value to a string resource. */
enum class Problem { NETWORK, RENEWAL_NOT_ALLOWED, GENERIC }

fun Throwable.toProblem(): Problem {
    if (this is IOException) return Problem.NETWORK
    val code = (this as? ApiException)?.errorCode
    return if (code == RENEWAL_NOT_ALLOWED_CODE) Problem.RENEWAL_NOT_ALLOWED else Problem.GENERIC
}
