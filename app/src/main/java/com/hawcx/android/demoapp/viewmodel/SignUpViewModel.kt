package com.hawcx.android.demoapp.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController // <<< Import NavHostController
import com.hawcx.android.demoapp.model.AppScreen
import com.hawcx.android.demoapp.util.isValidEmail
import com.hawcx.android.demoapp.util.isValidOtp
import com.hawcx.internal.HawcxInitializer
import com.hawcx.utils.SDKLogger // <<< Import SDKLogger
import com.hawcx.utils.SignUpCallback
import com.hawcx.utils.SignUpError
import com.hawcx.utils.getSignUpErrorMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay // <<< Import delay if needed

class SignUpViewModel(
    private val appViewModel: AppViewModel,
    savedStateHandle: SavedStateHandle // For retrieving navigation arguments
) : ViewModel(), SignUpCallback {

    private val TAG = "SignUpViewModel" // <<< Added TAG for logging
    private val signUpManager = HawcxInitializer.getInstance().signUp

    // Retrieve email from navigation arguments if passed
    private val initialEmail: String? = savedStateHandle[AppScreen.emailArg]

    private val _email = MutableStateFlow(initialEmail ?: "")
    val email: StateFlow<String> = _email.asStateFlow()

    private val _otp = MutableStateFlow("")
    val otp: StateFlow<String> = _otp.asStateFlow()

    private val _isSignUpLoading = MutableStateFlow(false)
    val isSignUpLoading: StateFlow<Boolean> = _isSignUpLoading.asStateFlow()

    private val _isVerifyingOTP = MutableStateFlow(false)
    val isVerifyingOTP: StateFlow<Boolean> = _isVerifyingOTP.asStateFlow()

    private val _isResendingOTP = MutableStateFlow(false)
    val isResendingOTP: StateFlow<Boolean> = _isResendingOTP.asStateFlow()

    private val _isSignUpButtonDisabled = MutableStateFlow(true)
    val isSignUpButtonDisabled: StateFlow<Boolean> = _isSignUpButtonDisabled.asStateFlow()

    private val _isVerifyOtpButtonDisabled = MutableStateFlow(true)
    val isVerifyOtpButtonDisabled: StateFlow<Boolean> = _isVerifyOtpButtonDisabled.asStateFlow()

    private val _showOtpField = MutableStateFlow(false)
    val showOtpField: StateFlow<Boolean> = _showOtpField.asStateFlow()

    private val _canResendOTP = MutableStateFlow(false)
    val canResendOTP: StateFlow<Boolean> = _canResendOTP.asStateFlow()

    // Navigation triggers
    // --- REMOVED _navigateToLogin StateFlow ---
    // private val _navigateToLogin = MutableStateFlow<String?>(null)
    // val navigateToLogin: StateFlow<String?> = _navigateToLogin.asStateFlow()

    // Keep this one for handling existing user error
    private val _navigateBackToLoginExistingUser = MutableStateFlow<String?>(null)
    val navigateBackToLoginExistingUser: StateFlow<String?> = _navigateBackToLoginExistingUser.asStateFlow()

    // --- ADDED NavController reference ---
    private var navControllerRef: NavHostController? = null

    init {
        setupBindings()
    }

    private fun setupBindings() {
        viewModelScope.launch {
            email.collect { emailValue ->
                _isSignUpButtonDisabled.value = !isValidEmail(emailValue) || _isSignUpLoading.value || _showOtpField.value
            }
        }
        viewModelScope.launch {
            otp.collect { otpValue ->
                _isVerifyOtpButtonDisabled.value = !isValidOtp(otpValue) || _isVerifyingOTP.value || _isResendingOTP.value
            }
        }
    }

    // --- ADDED function to set NavController ---
    fun setNavController(navController: NavHostController) {
        this.navControllerRef = navController
    }

    fun onEmailChange(newEmail: String) {
        _email.value = newEmail
    }

    fun onOtpChange(newOtp: String) {
        _otp.value = newOtp
    }

    fun signUpButtonTapped() {
        val currentEmail = _email.value
        if (!isValidEmail(currentEmail) || _isSignUpLoading.value || _showOtpField.value) return

        _isSignUpLoading.value = true
        _canResendOTP.value = false // Disable resend initially
        signUpManager.signUp(userid = currentEmail, callback = this)
    }

    fun verifyOtpButtonTapped() {
        val currentOtp = _otp.value
        if (!isValidOtp(currentOtp) || _isVerifyingOTP.value || _isResendingOTP.value) return

        _isVerifyingOTP.value = true
        signUpManager.handleVerifyOTP(otp = currentOtp, callback = this)
    }

    fun changeEmail() {
        _showOtpField.value = false
        _canResendOTP.value = false
        _otp.value = ""
        _isSignUpLoading.value = false
        _isVerifyingOTP.value = false
        _isResendingOTP.value = false
        // Re-enable sign up button based on email validity
        _isSignUpButtonDisabled.value = !isValidEmail(_email.value)
    }

    fun resendOtp() {
        val currentEmail = _email.value
        if (!isValidEmail(currentEmail) || _isResendingOTP.value || !_canResendOTP.value) return

        _isResendingOTP.value = true
        _canResendOTP.value = false // Disable while resending
        // SDK's signUp function also initiates OTP generation
        signUpManager.signUp(userid = currentEmail, callback = this)
    }

    // Only need clear for the existing user flow now
    fun clearNavigationTrigger() {
        _navigateBackToLoginExistingUser.value = null
    }

    // --- SignUpCallback Implementation ---

    override fun onError(error: SignUpError, message: String) {
        viewModelScope.launch { // Ensure UI updates on main thread
            _isSignUpLoading.value = false
            _isVerifyingOTP.value = false
            _isResendingOTP.value = false

            val userFriendlyMessage = getSignUpErrorMessage(error, message)
            if (error == SignUpError.USER_ALREADY_EXISTS) {
                appViewModel.showAlert("Account Exists", userFriendlyMessage)
                // Trigger navigation back to login with email prefill
                kotlinx.coroutines.delay(1500)
                _navigateBackToLoginExistingUser.value = _email.value
            } else if (error == SignUpError.VERIFY_OTP_FAILED) {
                appViewModel.showAlert("Verification Failed", userFriendlyMessage)
                _otp.value = "" // Clear OTP field
                _canResendOTP.value = true // Allow resend
                _isVerifyOtpButtonDisabled.value = true // Re-disable verify button
            } else {
                appViewModel.showAlert("Sign Up Failed", userFriendlyMessage)
                // If OTP generation failed, stay on email screen
                if (error == SignUpError.GENERATE_OTP_FAILED || error == SignUpError.NETWORK_ERROR && !_showOtpField.value) {
                    changeEmail() // Reset to email entry state
                } else {
                    _canResendOTP.value = true // Allow resend for other errors during OTP phase
                }
            }
        }
    }

    override fun onGenerateOTPSuccess() {
        viewModelScope.launch {
            _isSignUpLoading.value = false
            _isResendingOTP.value = false
            _showOtpField.value = true
            _canResendOTP.value = true // Allow resend after initial success
            _isSignUpButtonDisabled.value = true // Disable sign up button now
            // Consider showing a toast/snackbar instead of a full alert
            appViewModel.showAlert("Code Sent", "Verification code sent to ${_email.value}")
        }
    }

    // --- UPDATED to use Navigation Result ---
    override fun onSuccessfulSignUpOrDeviceAdd() {
        viewModelScope.launch {
            _isSignUpLoading.value = false
            _isVerifyingOTP.value = false
            _isResendingOTP.value = false
            appViewModel.showLoadingAlert("Account Created", "Signing you in...") // Keep this feedback

            // Set result on previous screen's SavedStateHandle and pop back
            val emailToLogin = _email.value
            SDKLogger.i("SignUp successful. Setting result 'loginEmail'=$emailToLogin and popping back.", tag = TAG)
            navControllerRef?.previousBackStackEntry?.savedStateHandle?.set("loginEmail", emailToLogin)
            navControllerRef?.popBackStack()
            // No longer update _navigateToLogin state flow
        }
    }
}