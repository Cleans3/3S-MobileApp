package vn.shelftap.app.ui.navigation

sealed interface Screen {
    data object Loading : Screen
    data object SignIn : Screen
    data object Profile : Screen
    data object ChangePassword : Screen
}
