package vn.shelftap.app.auth

/**
 * PLACEHOLDER. The signed-in app already has an AuthRepository; this file only
 * provides the contract this project uses. Replace this file with the real
 * implementation. Only [accessToken] is used by ShelfTap.
 */
class AuthRepository {

    suspend fun accessToken(): String =
        throw IllegalStateException("AuthRepository placeholder: replace with the real implementation")
}
