package com.hawcx.android.demoapp.ui

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.*
import androidx.navigation.compose.*
import com.hawcx.android.demoapp.model.AppScreen
import com.hawcx.android.demoapp.ui.screens.*
import com.hawcx.android.demoapp.viewmodel.*
import com.hawcx.utils.SDKLogger

@Composable
fun AppNavigation(
    navController: NavHostController,
    appViewModel: AppViewModel,
    authState: AuthenticationState
) {
    // Navigation Logic based on authState (remains the same)
    LaunchedEffect(authState) {
        when (authState) {
            AuthenticationState.LOGGED_IN -> {
                navController.navigate(AppScreen.Home.route) {
                    popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                    launchSingleTop = true
                }
            }
            AuthenticationState.LOGGED_OUT -> {
                if (navController.currentBackStackEntry?.destination?.route != AppScreen.Login.route) {
                    navController.navigate(AppScreen.Login.route) {
                        popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }
            AuthenticationState.CHECKING -> { /* Handled by initial UI */ }
        }
    }

    val application = LocalContext.current.applicationContext as Application
    val factory = ViewModelFactory(application, appViewModel)

    NavHost(
        navController = navController,
        startDestination = AppScreen.Login.route // Ensure Login is start destination
    ) {
        composable(AppScreen.Login.route) { backStackEntry -> // <<< Get backStackEntry
            val loginViewModel: LoginViewModel = viewModel(factory = factory)

            // --- Observe result from SignUpScreen ---
            val loginEmailResult = backStackEntry.savedStateHandle.get<String>("loginEmail")
            LaunchedEffect(loginEmailResult) {
                if (loginEmailResult != null) {
                    SDKLogger.i("LoginScreen received email '$loginEmailResult' to auto-login.", tag = "AppNavigation")
                    // Call internal function which now handles completion itself
                    loginViewModel.loginWithEmailInternal(loginEmailResult, isAutomatic = true) { success ->
                        SDKLogger.d("Auto-login completion reported back to NavEffect (Success: $success). ViewModel handles state.", tag = "AppNavigation")
                    }
                    // Clear the result from the SavedStateHandle so it doesn't trigger again on config change etc.
                    backStackEntry.savedStateHandle.remove<String>("loginEmail")
                    SDKLogger.d("Cleared 'loginEmail' result from SavedStateHandle.", tag = "AppNavigation")
                }
            }
            // --- End Observation ---

            LoginScreen(navController = navController, viewModel = loginViewModel)
        }

        composable(
            route = AppScreen.SignUp.route,
            arguments = listOf(navArgument(AppScreen.emailArg) { nullable = true; type = NavType.StringType })
        ) { navBackStackEntry ->
            val signUpViewModel: SignUpViewModel = viewModel {
                SignUpViewModel(appViewModel = appViewModel, savedStateHandle = navBackStackEntry.savedStateHandle)
            }
            SignUpScreen(navController = navController, viewModel = signUpViewModel)
        }

        // UPDATED: Simplified Add Device route without parameters
        composable(AppScreen.AddDevice.route) {
            // Get email from AppViewModel instead of from navigation args
            val addDeviceViewModel: AddDeviceViewModel = viewModel {
                AddDeviceViewModel(
                    appViewModel = appViewModel,
                    email = appViewModel.addDeviceEmail.value // Get email directly from AppViewModel
                )
            }
            AddDeviceScreen(navController = navController, viewModel = addDeviceViewModel)
        }

        composable(AppScreen.Home.route) {
            if (authState == AuthenticationState.LOGGED_IN) {
                val homeViewModel: HomeViewModel = viewModel(factory = factory)
                HomeScreen(viewModel = homeViewModel)
            } else {
                // Guard redirection
                LaunchedEffect(Unit) {
                    navController.navigate(AppScreen.Login.route) {
                        popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }
        }
    }
}

// Keep the helper extension function
fun NavGraph.findStartDestination(): NavDestination {
    var startDestination: NavDestination = this
    while (startDestination is NavGraph) {
        val graph = startDestination as NavGraph
        startDestination = graph.findNode(graph.startDestinationId)!!
    }
    return startDestination
}