package com.hawcx.android.demoapp.viewmodel

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hawcx.utils.SDKLogger
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Coordinates automatic login attempts after SignUp or AddDevice completion.
 * Needs access to the LoginViewModel instance to trigger the internal login flow.
 */
class SharedAuthManager {

    private val TAG = "SharedAuthManager"

    // Allow setting AppViewModel if needed for global state access
    var appViewModel: AppViewModel? = null

    // Hold a reference to the LoginViewModel when the LoginScreen is active
    private var loginViewModel: LoginViewModel? = null

    fun registerLoginViewModel(viewModel: LoginViewModel) {
        SDKLogger.d("LoginViewModel registered with SharedAuthManager", tag = TAG)
        this.loginViewModel = viewModel
    }

    fun unregisterLoginViewModel() {
        SDKLogger.d("LoginViewModel unregistered from SharedAuthManager", tag = TAG)
        this.loginViewModel = null
    }

    /**
     * Initiates an automatic login attempt.
     * Assumes the LoginScreen is currently active or will become active soon.
     *
     * @param email The email address to log in with.
     */
    fun loginWithEmail(email: String) {
        val currentLoginViewModel = loginViewModel // Capture current ref

        if (currentLoginViewModel != null) {
            SDKLogger.i("Triggering internal login via registered LoginViewModel for $email", tag = TAG)
            // Call the internal login function which handles state and SDK calls
            // The completion handler is now managed within LoginViewModel itself (onAutoLoginResult)
            currentLoginViewModel.loginWithEmailInternal(email, isAutomatic = true) { success ->
                SDKLogger.i("Internal login completion reported to SharedAuthManager. Success: $success", tag = TAG)
                // SharedAuthManager doesn't need to do anything with the result directly,
                // as LoginViewModel handles UI/State updates and navigation triggers.
            }
        } else {
            SDKLogger.e("Cannot perform automatic login: LoginViewModel is not registered.", tag = TAG)
            // Optionally notify AppViewModel about the failure if needed
            // appViewModel?.showAlert("Auto-Login Failed", "Could not initiate automatic login.")
        }
    }
}

// Define CompositionLocal for SharedAuthManager (can be placed here or in MainActivity)
val LocalSharedAuthManager = staticCompositionLocalOf<SharedAuthManager> {
    error("No SharedAuthManager provided")
}