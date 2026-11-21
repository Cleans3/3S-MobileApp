package com.example.libraryapp.ui.navigation

sealed class Screen(val route: String) {
    data object Login : Screen("login")
    data object Profile : Screen("profile")
}
