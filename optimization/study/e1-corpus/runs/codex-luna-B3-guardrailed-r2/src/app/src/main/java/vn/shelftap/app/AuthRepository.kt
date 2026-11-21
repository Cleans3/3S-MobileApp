package vn.shelftap.app

interface AuthRepository { suspend fun accessToken(): String }

/** Authentication is owned by the host sign-in flow; this app only consumes its token. */
class SignedInAuthRepository : AuthRepository {
    override suspend fun accessToken(): String = ""
}
