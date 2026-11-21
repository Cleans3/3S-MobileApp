package vn.shelftap.app.ui.navigation

sealed class Screen {
    data object SignIn : Screen()
    data object Profile : Screen()
    data class ChangePassword(val isMandatory: Boolean = false) : Screen()
}
