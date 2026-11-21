package vn.shelftap.app.data

interface AuthRepository { suspend fun accessToken(): String }

object AuthRepositoryProvider {
    var repository: AuthRepository = object : AuthRepository {
        override suspend fun accessToken(): String = error("Provide the signed-in AuthRepository")
    }
}
