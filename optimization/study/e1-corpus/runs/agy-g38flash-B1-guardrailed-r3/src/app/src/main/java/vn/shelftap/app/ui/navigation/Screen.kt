package vn.shelftap.app.ui.navigation

sealed class Screen(val route: String) {
    data object Catalogue : Screen("catalogue")
    data object Detail : Screen("detail/{titleId}") {
        const val ARG_TITLE_ID = "titleId"
        fun createRoute(titleId: String): String = "detail/$titleId"
    }
}
