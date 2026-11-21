package vn.shelftap.app.ui

import vn.shelftap.app.data.AppError
import vn.shelftap.app.data.Member

enum class Stage { Starting, SignedOut, ChangePassword, Profile }

data class AppUiState(
    val stage: Stage = Stage.Starting,
    val member: Member? = null,
    val profileLoading: Boolean = false,
    val profileError: AppError? = null,
    val signInLoading: Boolean = false,
    val signInError: AppError? = null,
    /** True when the member has to change the password before using the app. */
    val passwordForced: Boolean = false,
    val passwordLoading: Boolean = false,
    val passwordError: AppError? = null,
)
