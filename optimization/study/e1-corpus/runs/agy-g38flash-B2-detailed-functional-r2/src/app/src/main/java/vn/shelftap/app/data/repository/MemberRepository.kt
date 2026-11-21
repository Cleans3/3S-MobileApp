package vn.shelftap.app.data.repository

import vn.shelftap.app.data.local.SessionManager
import vn.shelftap.app.data.model.ChangePasswordRequest
import vn.shelftap.app.data.model.MemberProfile
import vn.shelftap.app.data.remote.ShelfTapApiService
import vn.shelftap.app.data.remote.unwrapBody

class MemberRepository(
    private val apiService: ShelfTapApiService,
    private val sessionManager: SessionManager
) {
    suspend fun getProfile(): MemberProfile {
        val profile = apiService.getProfile().unwrapBody()
        if (profile.mustChangePassword) {
            sessionManager.updateMustChangePassword(true)
        }
        return profile
    }

    suspend fun changePassword(currentPassword: String, newPassword: String) {
        val request = ChangePasswordRequest(
            currentPassword = currentPassword,
            newPassword = newPassword
        )
        apiService.changePassword(request).unwrapBody()
        sessionManager.updateMustChangePassword(false)
    }
}
