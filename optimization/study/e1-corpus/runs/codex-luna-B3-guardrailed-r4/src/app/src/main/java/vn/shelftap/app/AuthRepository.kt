package vn.shelftap.app

interface AuthRepository { suspend fun accessToken(): String }

/** The host sign-in flow supplies the real implementation at app composition time. */
class SignedInAuthRepository(private val tokenProvider: suspend () -> String) : AuthRepository {
    override suspend fun accessToken(): String = tokenProvider()
}
