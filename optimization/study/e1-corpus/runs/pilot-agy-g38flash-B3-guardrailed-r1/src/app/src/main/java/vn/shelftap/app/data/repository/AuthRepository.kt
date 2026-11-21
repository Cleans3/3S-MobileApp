package vn.shelftap.app.data.repository

interface AuthRepository {
    suspend fun accessToken(): String
}
