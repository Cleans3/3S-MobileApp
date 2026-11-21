package vn.shelftap.app.ui.navigation

sealed class Screen(val route: String) {
    object Loans : Screen("loans")
    object History : Screen("history")
    object BookDetail : Screen("book_detail/{titleId}") {
        fun createRoute(titleId: String) = "book_detail/$titleId"
    }
}
