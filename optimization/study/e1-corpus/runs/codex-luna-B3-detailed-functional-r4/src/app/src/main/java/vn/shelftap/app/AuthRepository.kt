package vn.shelftap.app

/** The signed-in host application supplies this implementation. */
interface AuthRepository { suspend fun accessToken(): String }

class SignedInAuthRepository : AuthRepository {
    override suspend fun accessToken(): String = ""
}
