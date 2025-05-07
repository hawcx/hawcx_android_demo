package com.hawcx.android.demoapp.viewmodel

import android.app.Application
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hawcx.android.demoapp.model.AlertInfo
import com.hawcx.android.demoapp.model.LoadingAlertInfo
import com.hawcx.android.demoapp.util.Constants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import androidx.core.content.edit
import com.hawcx.utils.SDKLogger
import java.lang.ref.WeakReference

enum class AuthenticationState {
    CHECKING,
    LOGGED_OUT,
    LOGGED_IN
}

// Use AndroidViewModel to easily get Application Context
class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val TAG = "AppViewModel"

    // App's own SharedPreferences for storing app-level state like last user hint
    private val appPrefs = application.getSharedPreferences(
        Constants.PrefsKeys.APP_PREFS_NAME, Context.MODE_PRIVATE
    )

    private val _authenticationState = MutableStateFlow(AuthenticationState.CHECKING)
    val authenticationState: StateFlow<AuthenticationState> = _authenticationState.asStateFlow()

    private val _loggedInUsername = MutableStateFlow<String?>(null)
    val loggedInUsername: StateFlow<String?> = _loggedInUsername.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _alertInfo = MutableStateFlow<AlertInfo?>(null)
    val alertInfo: StateFlow<AlertInfo?> = _alertInfo.asStateFlow()

    private val _loadingAlertInfo = MutableStateFlow<LoadingAlertInfo?>(null)
    val loadingAlertInfo: StateFlow<LoadingAlertInfo?> = _loadingAlertInfo.asStateFlow()

    // For sharing data between screens (like Add Device email)
    private val _addDeviceEmail = MutableStateFlow<String?>(null)
    val addDeviceEmail: StateFlow<String?> = _addDeviceEmail.asStateFlow()

    // Maintain a weak reference to the activity for biometric prompts
    private var activityContextRef = WeakReference<ComponentActivity>(null)

    init {
        SDKLogger.i("AppViewModel initialized", tag = TAG)
        checkInitialAuthenticationState()
    }

    // Store email parameter for AddDeviceViewModel to access
    fun setAddDeviceEmail(email: String?) {
        SDKLogger.d("Setting addDeviceEmail to $email", tag = TAG)
        _addDeviceEmail.value = email
    }

    // Store activity reference for biometric prompts (use weak reference to avoid memory leaks)
    fun setActivityContext(activity: ComponentActivity) {
        activityContextRef = WeakReference(activity)
        SDKLogger.d("Activity context set", tag = TAG)
    }

    fun clearActivityContext() {
        activityContextRef.clear()
        SDKLogger.d("Activity context cleared", tag = TAG)
    }

    // Get activity context for biometric operations
    fun getActivityContext(): ComponentActivity? {
        return activityContextRef.get()
    }

    // Check if an activity context is available
    fun hasActivityContext(): Boolean {
        return activityContextRef.get() != null
    }

    // Mirrors iOS checkInitialAuthenticationState
    private fun checkInitialAuthenticationState() {
        _authenticationState.value = AuthenticationState.CHECKING
        viewModelScope.launch {
            // Read last user hint from APP's SharedPreferences ONLY
            val lastUser = appPrefs.getString(Constants.PrefsKeys.LAST_USER, null)
            SDKLogger.d("Checking initial auth state, last user: $lastUser", tag = TAG)

            // No SDK interaction here
            if (!lastUser.isNullOrEmpty()) {
                _loggedInUsername.value = lastUser
                _authenticationState.value = AuthenticationState.LOGGED_OUT // Start logged out
            } else {
                _loggedInUsername.value = null
                _authenticationState.value = AuthenticationState.LOGGED_OUT
            }
        }
    }

    // Called by LoginViewModel AFTER SDK confirms successful login
    // Mirrors iOS userDidLogin
    fun userDidLogin(username: String) {
        SDKLogger.i("User logged in: $username", tag = TAG)
        _loggedInUsername.value = username
        // Save last user hint in APP's SharedPreferences ONLY
        // SDK handles its own secure state saving internally during its login success path
        appPrefs.edit() { putString(Constants.PrefsKeys.LAST_USER, username) }
        _authenticationState.value = AuthenticationState.LOGGED_IN
        clearAlerts()
    }

    // Mirrors iOS logout
    fun logout(onComplete: () -> Unit) {
        viewModelScope.launch {
            val userToClear = _loggedInUsername.value
            SDKLogger.i("Logging out user: $userToClear", tag = TAG)

            // Clear app state ONLY
            _loggedInUsername.value = null
            appPrefs.edit() { remove(Constants.PrefsKeys.LAST_USER) } // Clear app's last user hint
            if (!userToClear.isNullOrEmpty()) {
                // Clear app-specific biometric preference
                appPrefs.edit() { remove(Constants.PrefsKeys.BIOMETRIC_PREFIX + userToClear) }
                // Clear other app-specific keys if needed (like the demo app's stored device/session details)
                appPrefs.edit() { remove(Constants.PrefsKeys.SDK_DEVICE_DETAILS) }
                appPrefs.edit() { remove(Constants.PrefsKeys.SDK_SESSION_DETAILS) }
                appPrefs.edit() { remove(Constants.PrefsKeys.SDK_WEB_TOKEN) }
            }

            _authenticationState.value = AuthenticationState.LOGGED_OUT
            clearAlerts()
            onComplete()
        }
    }

    fun showAlert(title: String, message: String) {
        clearLoadingAlert()
        _alertInfo.value = AlertInfo(title = title, message = message)
        SDKLogger.d("Showing alert: $title - $message", tag = TAG)
    }

    fun clearAlert() {
        _alertInfo.value = null
    }

    fun showLoadingAlert(title: String, message: String) {
        clearAlert()
        _loadingAlertInfo.value = LoadingAlertInfo(title = title, message = message)
        SDKLogger.d("Showing loading alert: $title - $message", tag = TAG)
    }

    fun clearLoadingAlert() {
        _loadingAlertInfo.value = null
    }

    fun clearAlerts() {
        clearAlert()
        clearLoadingAlert()
    }
}