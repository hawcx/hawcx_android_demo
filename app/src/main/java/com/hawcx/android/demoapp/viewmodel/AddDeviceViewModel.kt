package com.hawcx.android.demoapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hawcx.android.demoapp.util.isValidOtp
import com.hawcx.internal.HawcxInitializer
import com.hawcx.utils.AddDeviceCallback
import com.hawcx.utils.AddDeviceError
import com.hawcx.utils.getAddDeviceErrorMessage
import com.hawcx.utils.SDKLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import androidx.navigation.NavHostController

// Mimics SignUpViewModel stages
enum class AddDeviceFlowStage {
    INITIAL, SENDING, VERIFICATION
}

class AddDeviceViewModel(
    private val appViewModel: AppViewModel,
    email: String? // Changed to nullable parameter - comes from AppViewModel now
) : ViewModel(), AddDeviceCallback {

    private val TAG = "AddDeviceVM"
    private val addDeviceManager = HawcxInitializer.getInstance().addDeviceManager

    // Email comes directly from constructor parameter, not from savedStateHandle
    val email: String

    // Initialize all StateFlows BEFORE setupBindings() is called
    private val _flowStage = MutableStateFlow(AddDeviceFlowStage.INITIAL)
    val flowStage: StateFlow<AddDeviceFlowStage> = _flowStage.asStateFlow()

    private val _otp = MutableStateFlow("")
    val otp: StateFlow<String> = _otp.asStateFlow()

    // Loading states
    private val _isStartingFlow = MutableStateFlow(false)
    val isStartingFlow: StateFlow<Boolean> = _isStartingFlow.asStateFlow()

    private val _isVerifyingOTP = MutableStateFlow(false)
    val isVerifyingOTP: StateFlow<Boolean> = _isVerifyingOTP.asStateFlow()

    private val _isResendingOTP = MutableStateFlow(false)
    val isResendingOTP: StateFlow<Boolean> = _isResendingOTP.asStateFlow()

    // Button states
    private val _isVerifyButtonDisabled = MutableStateFlow(true)
    val isVerifyButtonDisabled: StateFlow<Boolean> = _isVerifyButtonDisabled.asStateFlow()

    private val _showOtpField = MutableStateFlow(false)
    val showOtpField: StateFlow<Boolean> = _showOtpField.asStateFlow()

    private val _canResendOTP = MutableStateFlow(false)
    val canResendOTP: StateFlow<Boolean> = _canResendOTP.asStateFlow()

    private var navControllerRef: NavHostController? = null

    init {
        // Log initialization
        SDKLogger.d("AddDeviceViewModel init. Email passed in constructor: $email", tag = TAG)

        // Now use requireNotNull on the constructor parameter
        this.email = requireNotNull(email) {
            val errorMsg = "Critical Error: AddDeviceViewModel created without mandatory 'email' parameter"
            SDKLogger.e(errorMsg, tag = TAG)
            appViewModel.showAlert("Navigation Error", "Could not start Add Device flow (missing user info).")
            errorMsg
        }

        // Log the retrieved email immediately after assignment
        SDKLogger.i("AddDeviceViewModel initialized FOR email: ${this.email}", tag = TAG)

        // Now that all StateFlows are initialized, setup bindings
        setupBindings()

        // Automatically start the flow if email is valid
        if (this.email.isNotEmpty()) {
            sendVerificationCode()
        }
    }

    private fun setupBindings() {
        viewModelScope.launch {
            otp.collect { otpValue ->
                _isVerifyButtonDisabled.value = !isValidOtp(otpValue) || _isVerifyingOTP.value || _isResendingOTP.value
            }
        }
    }

    fun setNavController(navController: NavHostController) {
        this.navControllerRef = navController
    }

    fun onOtpChange(newOtp: String) {
        _otp.value = newOtp
    }

    internal fun sendVerificationCode() {
        if (_isStartingFlow.value || _isResendingOTP.value || email.isEmpty()) return

        if (_flowStage.value == AddDeviceFlowStage.INITIAL) {
            SDKLogger.i("Starting Add Device flow (initial OTP send) for $email", tag = TAG)
            _flowStage.value = AddDeviceFlowStage.SENDING
            _isStartingFlow.value = true
        } else {
            SDKLogger.i("Resending OTP for $email", tag = TAG)
            _isResendingOTP.value = true
        }
        _showOtpField.value = false
        _canResendOTP.value = false
        addDeviceManager.startAddDeviceFlow(userid = email, callback = this)
    }

    fun resendOtp() {
        if (email.isEmpty() || !_canResendOTP.value) return
        sendVerificationCode()
    }

    fun verifyOtpButtonTapped() {
        val currentOtp = _otp.value
        if (!isValidOtp(currentOtp) || _isVerifyingOTP.value) return

        _isVerifyingOTP.value = true
        addDeviceManager.handleVerifyOTP(otp = currentOtp)
    }

    // --- AddDeviceCallback Implementation ---

    override fun onAddDeviceSuccess() {
        viewModelScope.launch {
            _isStartingFlow.value = false
            _isVerifyingOTP.value = false
            _isResendingOTP.value = false
            appViewModel.showLoadingAlert("Device Added", "Logging you in...")

            SDKLogger.i("AddDevice successful. Setting result 'loginEmail'=$email and popping back.", tag = TAG)
            navControllerRef?.previousBackStackEntry?.savedStateHandle?.set("loginEmail", email)
            navControllerRef?.popBackStack()
        }
    }

    override fun onGenerateOTPSuccess() {
        viewModelScope.launch {
            _isStartingFlow.value = false
            _isResendingOTP.value = false
            _flowStage.value = AddDeviceFlowStage.VERIFICATION
            _showOtpField.value = true
            _canResendOTP.value = true
            appViewModel.showAlert("Code Sent", "Verification code sent to $email")
        }
    }

    override fun showError(addDeviceErrorCode: AddDeviceError, errorMessage: String) {
        viewModelScope.launch {
            _isStartingFlow.value = false
            _isVerifyingOTP.value = false
            _isResendingOTP.value = false

            val userFriendlyMessage = getAddDeviceErrorMessage(addDeviceErrorCode, errorMessage)
            appViewModel.showAlert("Add Device Failed", userFriendlyMessage)

            if (addDeviceErrorCode == AddDeviceError.VERIFY_OTP_FAILED) {
                _flowStage.value = AddDeviceFlowStage.VERIFICATION
                _otp.value = ""
                _canResendOTP.value = true
                _isVerifyButtonDisabled.value = true
            } else {
                _flowStage.value = AddDeviceFlowStage.INITIAL
                _showOtpField.value = false
                _canResendOTP.value = false
            }
        }
    }
}