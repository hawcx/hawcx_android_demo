package com.hawcx.android.demoapp.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.hawcx.android.demoapp.model.AppScreen
import com.hawcx.android.demoapp.util.Constants
import com.hawcx.android.demoapp.util.isValidEmail
import com.hawcx.android.demoapp.util.BiometricAuthHelper
import com.hawcx.internal.HawcxInitializer
import com.hawcx.utils.SignInCallback
import com.hawcx.utils.SignInError
import com.hawcx.utils.SDKLogger
import com.hawcx.utils.getSignInErrorMessage
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import androidx.core.content.edit

// Define events for biometric prompts
sealed class LoginViewEvent {
    data class RequestBiometricPrompt(
        val title: String,
        val subtitle: String,
        val onPromptSuccess: () -> Unit,
        val onPromptFailure: (Int, String) -> Unit
    ) : LoginViewEvent()

    data class RequestEnableBiometricPrompt(
        val currentUser: String,
        val title: String,
        val subtitle: String,
        val onPromptSuccess: () -> Unit,
        val onPromptFailure: (Int, String) -> Unit
    ) : LoginViewEvent()
}

class LoginViewModel(
    application: Application,
    private val appViewModel: AppViewModel
) : AndroidViewModel(application), SignInCallback {

    private val TAG = "LoginViewModel"

    private val context: Context
        get() = getApplication<Application>().applicationContext

    private val appPrefs = context.getSharedPreferences(
        Constants.PrefsKeys.APP_PREFS_NAME, Context.MODE_PRIVATE
    )

    private val signInManager = HawcxInitializer.getInstance().signIn

    var navController: NavController? = null

    // --- State Flows ---
    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email.asStateFlow()

    private val _isLoggingIn = MutableStateFlow(false)
    val isLoggingIn: StateFlow<Boolean> = _isLoggingIn.asStateFlow()

    private val _isBiometricLoggingIn = MutableStateFlow(false)
    val isBiometricLoggingIn: StateFlow<Boolean> = _isBiometricLoggingIn.asStateFlow()

    private val _isLoginButtonDisabled = MutableStateFlow(true)
    val isLoginButtonDisabled: StateFlow<Boolean> = _isLoginButtonDisabled.asStateFlow()

    private val _showBiometricButton = MutableStateFlow(false)
    val showBiometricButton: StateFlow<Boolean> = _showBiometricButton.asStateFlow()

    private val _isBiometricAvailable = MutableStateFlow(false)
    val isBiometricAvailable: StateFlow<Boolean> = _isBiometricAvailable.asStateFlow()

    private val _shouldShowBiometricEnablePrompt = MutableStateFlow(false)
    val shouldShowBiometricEnablePrompt: StateFlow<Boolean> = _shouldShowBiometricEnablePrompt.asStateFlow()

    private val _navigateToSignUp = MutableStateFlow<String?>(null)
    val navigateToSignUp: StateFlow<String?> = _navigateToSignUp.asStateFlow()

    private val _navigateToAddDevice = MutableStateFlow<String?>(null)
    val navigateToAddDevice: StateFlow<String?> = _navigateToAddDevice.asStateFlow()

    // --- Event Channel/Flow for UI Actions ---
    private val _eventChannel = Channel<LoginViewEvent>(Channel.BUFFERED)
    val eventFlow = _eventChannel.receiveAsFlow()

    var onAutoLoginResult: ((Boolean) -> Unit)? = null
    private var isInitialLoad = true  // Flag to track initial load for auto-auth

    init {
        setupBindings()
        loadInitialEmailAndCheckBiometrics()
    }

    // --- Bindings and Initial Load ---
    private fun setupBindings() {
        viewModelScope.launch {
            email.collect { emailValue ->
                _isLoginButtonDisabled.value = !isValidEmail(emailValue)
            }
        }
        viewModelScope.launch {
            combine(_isLoggingIn, _isBiometricLoggingIn, _isLoginButtonDisabled) { logIn, bioLogIn, emailInvalid ->
                logIn || bioLogIn || emailInvalid
            }.collect { isDisabled ->
                _isLoginButtonDisabled.value = isDisabled
            }
        }
    }

    fun onEmailChange(newEmail: String) {
        _email.value = newEmail
    }

    private fun loadInitialEmailAndCheckBiometrics() {
        viewModelScope.launch {
            val lastUser = appPrefs.getString(Constants.PrefsKeys.LAST_USER, null)
            SDKLogger.d("Loading initial email, last user: $lastUser", tag = TAG)

            if (!lastUser.isNullOrEmpty()) {
                _email.value = lastUser
                checkBiometricAvailability(lastUser)

                // Check if biometrics are enabled for this user
                val isBiometricEnabledForUser = appPrefs.getBoolean(Constants.PrefsKeys.BIOMETRIC_PREFIX + lastUser, false)
                SDKLogger.d("Biometrics enabled for $lastUser: $isBiometricEnabledForUser", tag = TAG)

                if (_isBiometricAvailable.value && isBiometricEnabledForUser) {
                    _showBiometricButton.value = true

                    // Only attempt automatic login on initial load (not after config changes)
                    if (isInitialLoad) {
                        SDKLogger.i("Attempting automatic biometric login for $lastUser", tag = TAG)
                        isInitialLoad = false
                        attemptAutomaticBiometricLogin()
                    }
                } else {
                    _showBiometricButton.value = isBiometricEnabledForUser
                }
            } else {
                _isBiometricAvailable.value = false
                _showBiometricButton.value = false
            }
        }
    }

    private fun checkBiometricAvailability(forUser: String?) {
        val isBiometricsAvailable = BiometricAuthHelper.isBiometricAvailable(context)
        _isBiometricAvailable.value = isBiometricsAvailable

        SDKLogger.d("Biometric available: $isBiometricsAvailable for user: $forUser", tag = TAG)

        if (isBiometricsAvailable && !forUser.isNullOrEmpty()) {
            val isEnabled = appPrefs.getBoolean(Constants.PrefsKeys.BIOMETRIC_PREFIX + forUser, false)
            _showBiometricButton.value = isEnabled
            SDKLogger.d("Setting showBiometricButton = $isEnabled", tag = TAG)
        } else {
            _showBiometricButton.value = false
        }
    }

    // --- Actions ---

    fun loginButtonTapped() {
        val currentEmail = _email.value
        if (!isValidEmail(currentEmail) || _isLoggingIn.value || _isBiometricLoggingIn.value) return
        _isLoggingIn.value = true
        signInManager.signIn(userid = currentEmail, callback = this)
    }

    fun loginWithEmailInternal(emailToLogin: String, isAutomatic: Boolean, completion: (Boolean) -> Unit) {
        if (_isLoggingIn.value || _isBiometricLoggingIn.value) {
            completion(false); return
        }
        _email.value = emailToLogin
        _isLoggingIn.value = true
        onAutoLoginResult = completion
        signInManager.signIn(userid = emailToLogin, callback = this)
    }

    /**
     * Attempts to authenticate with biometrics when user taps the button
     */
    fun biometricButtonTapped() {
        if (_isBiometricLoggingIn.value || !_isBiometricAvailable.value) return
        val userToAuth = _email.value
        if (userToAuth.isEmpty()) {
            appViewModel.showAlert("Biometric Error", "No user email entered.")
            return
        }

        // Show biometric prompt via event
        viewModelScope.launch {
            _eventChannel.send(LoginViewEvent.RequestBiometricPrompt(
                title = "Log in",
                subtitle = "Authenticate with biometrics",
                onPromptSuccess = {
                    _isBiometricLoggingIn.value = true
                    signInManager.signIn(userid = userToAuth, callback = this@LoginViewModel)
                },
                onPromptFailure = { errCode, errString ->
                    _isBiometricLoggingIn.value = false
                    if (errCode != androidx.biometric.BiometricPrompt.ERROR_USER_CANCELED &&
                        errCode != androidx.biometric.BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                        appViewModel.showAlert("Biometric Failed", errString)
                    }
                }
            ))
        }
    }

    /**
     * Attempts to authenticate with biometrics on app startup if enabled
     */
    private fun attemptAutomaticBiometricLogin() {
        val lastUser = _email.value
        if (lastUser.isEmpty() || _isBiometricLoggingIn.value) return

        // Only attempt if biometrics are available and enabled for this user
        val isBiometricEnabled = appPrefs.getBoolean(Constants.PrefsKeys.BIOMETRIC_PREFIX + lastUser, false)
        if (!_isBiometricAvailable.value || !isBiometricEnabled) {
            SDKLogger.d("Automatic biometric login skipped - availability: ${_isBiometricAvailable.value}, enabled: $isBiometricEnabled", tag = TAG)
            return
        }

        SDKLogger.i("Attempting automatic biometric login...", tag = TAG)
        _isBiometricLoggingIn.value = true

        // Show biometric prompt via event
        viewModelScope.launch {
            _eventChannel.send(LoginViewEvent.RequestBiometricPrompt(
                title = "Automatic Login",
                subtitle = "Log in with biometrics",
                onPromptSuccess = {
                    SDKLogger.i("Automatic biometric auth succeeded, calling signIn", tag = TAG)
                    signInManager.signIn(userid = lastUser, callback = this@LoginViewModel)
                },
                onPromptFailure = { errCode, errString ->
                    SDKLogger.w("Automatic biometric auth failed: $errCode - $errString", tag = TAG)
                    _isBiometricLoggingIn.value = false
                    // Only show error for non-cancellation errors
                    if (errCode != androidx.biometric.BiometricPrompt.ERROR_USER_CANCELED &&
                        errCode != androidx.biometric.BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                        appViewModel.showAlert("Automatic Login Failed",
                            "Biometric authentication failed: $errString\nPlease log in manually.")
                    }
                }
            ))
        }
    }

    /**
     * Enables biometrics for the current user.
     * First verifies with biometrics, then enables it in preferences.
     */
    fun enableBiometrics(activityContext: Context) {
        val currentUser = _email.value
        if (currentUser.isEmpty()) return

        // Show biometric prompt to verify first
        viewModelScope.launch {
            _eventChannel.send(LoginViewEvent.RequestEnableBiometricPrompt(
                currentUser = currentUser, // Add the current user
                title = "Enable Biometric Login",
                subtitle = "Verify your identity",
                onPromptSuccess = {
                    // Biometric auth succeeded, now enable it in preferences
                    appPrefs.edit() {
                        putBoolean(
                            Constants.PrefsKeys.BIOMETRIC_PREFIX + currentUser,
                            true
                        )
                    }
                    _showBiometricButton.value = true
                    _shouldShowBiometricEnablePrompt.value = false
                    appViewModel.showAlert("Biometrics Enabled", "Biometric login is now active.")
                    appViewModel.userDidLogin(currentUser)
                    SDKLogger.i("Biometrics enabled for user: $currentUser", tag = TAG)
                },
                onPromptFailure = { errCode, errString ->
                    // Biometric auth failed or was canceled
                    _shouldShowBiometricEnablePrompt.value = false

                    if (errCode == androidx.biometric.BiometricPrompt.ERROR_USER_CANCELED ||
                        errCode == androidx.biometric.BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                        appViewModel.showAlert("Biometrics Not Enabled", "Biometric setup was canceled.")
                    } else {
                        appViewModel.showAlert("Biometrics Not Enabled", "Verification failed: $errString")
                    }
                    appViewModel.userDidLogin(currentUser)
                    SDKLogger.w("Biometric enable failed: $errCode - $errString", tag = TAG)
                }
            ))
        }
    }

    fun declineBiometrics() {
        _shouldShowBiometricEnablePrompt.value = false
        val currentUser = _email.value
        if (currentUser.isNotEmpty()) {
            appViewModel.userDidLogin(currentUser)
        }
    }

    fun clearNavigationTriggers() {
        _navigateToSignUp.value = null
        _navigateToAddDevice.value = null
    }

    // --- SignInCallback Implementation ---

    override fun onSuccessfulLogin(loggedInEmail: String, accessToken: String, refreshToken: String) {
        viewModelScope.launch {
            _isLoggingIn.value = false
            _isBiometricLoggingIn.value = false

            // Save last user hint to app's SharedPreferences
            appPrefs.edit() { putString(Constants.PrefsKeys.LAST_USER, loggedInEmail) }

            checkBiometricAvailability(loggedInEmail)
            val isBiometricEnabledForUser = appPrefs.getBoolean(Constants.PrefsKeys.BIOMETRIC_PREFIX + loggedInEmail, false)

            if (_isBiometricAvailable.value && !isBiometricEnabledForUser) {
                SDKLogger.i("Showing biometric enable prompt for $loggedInEmail", tag = TAG)
                _shouldShowBiometricEnablePrompt.value = true
            } else {
                appViewModel.userDidLogin(loggedInEmail)
            }
            onAutoLoginResult?.invoke(true)
            onAutoLoginResult = null
        }
    }

    // The rest of the methods remain the same...
    override fun requestUserAuthentication(
        onSuccess: () -> Unit,
        onFailure: (error: SignInError, message: String) -> Unit
    ) {
        viewModelScope.launch {
            SDKLogger.i("SDK requested user authentication", tag = "LoginViewModel")
            _eventChannel.send(LoginViewEvent.RequestBiometricPrompt(
                title = "Authentication Required",
                subtitle = "Please verify your identity to continue",
                onPromptSuccess = {
                    SDKLogger.i("SDK-requested biometric prompt succeeded", tag = "LoginViewModel")
                    onSuccess()
                },
                onPromptFailure = { errCode, errString ->
                    SDKLogger.w("SDK-requested biometric prompt failed (Code: $errCode)", tag = "LoginViewModel")
                    _isLoggingIn.value = false
                    _isBiometricLoggingIn.value = false
                    onFailure(
                        SignInError.KEYSTORE_ERROR,
                        getSignInErrorMessage(SignInError.KEYSTORE_ERROR, "User authentication failed (Code: $errCode, Msg: $errString)")
                    )
                }
            ))
        }
    }

    override fun onError(error: SignInError, message: String) {
        viewModelScope.launch {
            _isLoggingIn.value = false
            _isBiometricLoggingIn.value = false
            val userFriendlyMessage = getSignInErrorMessage(error, message)

            when (error) {
                SignInError.USER_NOT_FOUND -> {
                    appViewModel.showAlert("Account Not Found", userFriendlyMessage)
                    kotlinx.coroutines.delay(1500)
                    _navigateToSignUp.value = _email.value.ifEmpty { null }
                }
                SignInError.ADD_DEVICE_REQUIRED,
                SignInError.INVALID_DEVICE_TOKEN,
                SignInError.MISSING_KEY_DATA -> {
                    appViewModel.showAlert("Device Registration Required", userFriendlyMessage)
                    kotlinx.coroutines.delay(1500)

                    // Store email in AppViewModel for AddDeviceViewModel
                    appViewModel.setAddDeviceEmail(_email.value)

                    // Set navigateToAddDevice to trigger navigation in LoginScreen
                    _navigateToAddDevice.value = "trigger"
                }
                SignInError.KEYSTORE_ERROR -> {
                    if (message.contains("User authentication failed or cancelled", ignoreCase = true)) {
                        appViewModel.showAlert("Authentication Failed", "Biometric/Device lock verification failed or was cancelled.")
                    } else if (message.contains("User authentication required", ignoreCase = true)) {
                        appViewModel.showAlert("Authentication Required", "Please authenticate using your device lock or biometric to proceed.")
                    }
                    else {
                        appViewModel.showAlert("Login Failed (Security)", userFriendlyMessage)
                    }
                }
                else -> {
                    appViewModel.showAlert("Login Failed", userFriendlyMessage)
                }
            }
            onAutoLoginResult?.invoke(false)
            onAutoLoginResult = null
        }
    }

    override fun initiateAddDeviceRegistrationFlow(forUserId: String) {
        viewModelScope.launch {
            _isLoggingIn.value = false
            _isBiometricLoggingIn.value = false

            // Store email in AppViewModel for AddDeviceViewModel
            appViewModel.setAddDeviceEmail(forUserId)

            appViewModel.showAlert(
                "Device Registration Required",
                "This device needs to be added to your account ($forUserId)."
            )

            kotlinx.coroutines.delay(1500)

            // Trigger navigation to Add Device
            _navigateToAddDevice.value = "trigger"

            onAutoLoginResult?.invoke(false)
            onAutoLoginResult = null
        }
    }

    override fun navigateToRegistration(forUserId: String) {
        viewModelScope.launch {
            _isLoggingIn.value = false
            _isBiometricLoggingIn.value = false
            appViewModel.showAlert(
                "Account Not Found",
                "No account found for $forUserId. Please sign up."
            )
            kotlinx.coroutines.delay(1500)
            _navigateToSignUp.value = forUserId
            onAutoLoginResult?.invoke(false)
            onAutoLoginResult = null
        }
    }
}