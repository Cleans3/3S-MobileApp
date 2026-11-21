package vn.shelftap.app.ui.navigation

sealed class Screen(val route: String) {
    data object Loans : Screen("loans")
    data object History : Screen("history")
    data object BookDetail : Screen("book_detail/{titleId}") {
        fun createRoute(titleId: String) = "book_detail/$titleId"
    }
}
