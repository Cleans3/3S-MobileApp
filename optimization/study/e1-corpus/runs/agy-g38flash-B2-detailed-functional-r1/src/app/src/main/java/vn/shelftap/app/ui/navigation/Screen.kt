package vn.shelftap.app.ui.navigation

sealed class Screen(val route: String) {
    object SignIn : Screen("sign_in")
    object Profile : Screen("profile")
    object ChangePassword : Screen("change_password?forced={forced}") {
        fun createRoute(forced: Boolean = false) = "change_password?forced=$forced"
    }
}
