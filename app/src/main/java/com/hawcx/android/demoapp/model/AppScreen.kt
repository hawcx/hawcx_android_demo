package com.hawcx.android.demoapp.model

// Defines the different screens/destinations in the app
sealed class AppScreen(val route: String) {
    object Login : AppScreen("login")
    object SignUp : AppScreen("signup?email={email}") { // Optional email arg
        fun createRoute(email: String? = null) = "signup".let { base ->
            if (email != null) "$base?email=$email" else base
        }
    }
    // Simplified AddDevice route without email parameter
    object AddDevice : AppScreen("add_device") {
        fun createRoute() = route
    }
    object Home : AppScreen("home")

    // Helper to extract argument (example for AddDevice)
    companion object {
        const val emailArg = "email"
    }
}