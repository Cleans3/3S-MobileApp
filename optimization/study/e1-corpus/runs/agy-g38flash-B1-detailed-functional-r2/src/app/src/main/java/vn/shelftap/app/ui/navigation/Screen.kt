package vn.shelftap.app.ui.navigation

sealed class Screen(val route: String) {
    object Catalogue : Screen("catalogue")
    object TitleDetail : Screen("title_detail/{titleId}") {
        fun createRoute(titleId: String): String = "title_detail/$titleId"
    }
}
