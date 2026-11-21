package vn.shelftap.app.ui.navigation

/**
 * Screen routes and navigation arguments.
 */
sealed class Screen(val route: String) {
    object Catalogue : Screen("catalogue")

    object TitleDetail : Screen("title_detail/{titleId}") {
        const val ARG_TITLE_ID = "titleId"

        fun createRoute(titleId: String): String {
            return "title_detail/$titleId"
        }
    }
}
