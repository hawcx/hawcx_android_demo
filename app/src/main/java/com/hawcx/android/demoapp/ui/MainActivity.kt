package com.hawcx.android.demoapp.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.hawcx.android.demoapp.ui.theme.HawcxDemoAppTheme
import com.hawcx.android.demoapp.viewmodel.AppViewModel
import com.hawcx.android.demoapp.viewmodel.AuthenticationState
import com.hawcx.android.demoapp.viewmodel.SharedAuthManager
import com.hawcx.android.demoapp.ui.composables.AppAlertDialogs
import com.hawcx.utils.SDKLogger

/**
 * Main entry point for the app
 * Extends FragmentActivity to support BiometricPrompt
 */
class MainActivity : androidx.fragment.app.FragmentActivity() {  // Changed from ComponentActivity to FragmentActivity

    private val appViewModel: AppViewModel by viewModels()
    private val sharedAuthManager = SharedAuthManager()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen
        installSplashScreen()

        super.onCreate(savedInstanceState)
        sharedAuthManager.appViewModel = appViewModel

        // Log app startup for debugging
        SDKLogger.i("MainActivity onCreate called. App starting up.", tag = "MainActivity")

        // Store a reference to the Activity in AppViewModel for biometric operations
        appViewModel.setActivityContext(this)

        setContent {
            HawcxDemoAppTheme(darkTheme = false) {
                val navController = rememberNavController()
                val authState by appViewModel.authenticationState.collectAsStateWithLifecycle()

                // Ensure we have a proper activity context for biometric operations
                DisposableEffect(this) {
                    SDKLogger.d("DisposableEffect: Setting activity context", tag = "MainActivity")
                    appViewModel.setActivityContext(this@MainActivity)
                    onDispose {
                        SDKLogger.d("DisposableEffect: Clearing activity context", tag = "MainActivity")
                        appViewModel.clearActivityContext()
                    }
                }

                // Use CompositionLocalProvider to make sharedAuthManager available down the tree
                CompositionLocalProvider(LocalSharedAuthManager provides sharedAuthManager) {
                    AppNavigation(
                        navController = navController,
                        appViewModel = appViewModel,
                        authState = authState
                    )
                }

                // Handle Alerts globally
                AppAlertDialogs(appViewModel = appViewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        SDKLogger.i("MainActivity onResume called", tag = "MainActivity")
        // Ensure the activity context is set when app comes to foreground
        appViewModel.setActivityContext(this)
    }

    override fun onPause() {
        super.onPause()
        SDKLogger.i("MainActivity onPause called", tag = "MainActivity")
    }
}

// Define CompositionLocal for SharedAuthManager
val LocalSharedAuthManager = staticCompositionLocalOf<SharedAuthManager> {
    error("No SharedAuthManager provided") // Error if not provided
}