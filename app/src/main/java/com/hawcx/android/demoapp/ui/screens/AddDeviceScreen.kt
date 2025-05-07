package com.hawcx.android.demoapp.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.hawcx.android.demoapp.R
import com.hawcx.android.demoapp.model.AppScreen
import com.hawcx.android.demoapp.ui.composables.HawcxFooter
import com.hawcx.android.demoapp.ui.composables.LoadingButton
import com.hawcx.android.demoapp.viewmodel.AddDeviceFlowStage
import com.hawcx.android.demoapp.viewmodel.AddDeviceViewModel
import androidx.compose.ui.focus.FocusState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDeviceScreen(
    navController: NavHostController,
    viewModel: AddDeviceViewModel
) {
    // ... (State variables remain the same) ...
    val email = viewModel.email
    val otp by viewModel.otp.collectAsStateWithLifecycle()
    val flowStage by viewModel.flowStage.collectAsStateWithLifecycle()
    val isStartingFlow by viewModel.isStartingFlow.collectAsStateWithLifecycle()
    val isVerifyingOTP by viewModel.isVerifyingOTP.collectAsStateWithLifecycle()
    val isResendingOTP by viewModel.isResendingOTP.collectAsStateWithLifecycle()
    val isVerifyButtonDisabled by viewModel.isVerifyButtonDisabled.collectAsStateWithLifecycle()
    val showOtpField by viewModel.showOtpField.collectAsStateWithLifecycle() // This might not be needed if stage dictates UI
    val canResendOTP by viewModel.canResendOTP.collectAsStateWithLifecycle()

    // --- Pass NavController to ViewModel ---
    LaunchedEffect(viewModel, navController) {
        viewModel.setNavController(navController)
    }

    // --- Focus State Management ---
    var isOtpFieldFocused by remember { mutableStateOf(false) } // State now lives here
    val otpFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val interactionSource = remember { MutableInteractionSource() }

    // Request focus for OTP field when it appears (Now based on stage)
    LaunchedEffect(flowStage) {
        if (flowStage == AddDeviceFlowStage.VERIFICATION) {
            kotlinx.coroutines.delay(300) // Delay for layout/transition
            otpFocusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add This Device") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = interactionSource,
                    indication = null
                ) {
                    // Dismiss keyboard when clicking outside input fields
                    focusManager.clearFocus()
                    keyboardController?.hide()
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                Image( /* ... Logo ... */
                    painter = painterResource(id = R.drawable.hawcx_logo),
                    contentDescription = "Hawcx Logo",
                    modifier = Modifier.fillMaxWidth(0.5f).aspectRatio(1f)
                )
                Text( /* ... Hawcx Text ... */
                    "HAWCX",
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.Serif,
                    modifier = Modifier.offset(y = (-50).dp)
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Content based on flow stage
                if (flowStage == AddDeviceFlowStage.VERIFICATION) {
                    VerificationStageContent( // Pass the state and callback correctly
                        viewModel = viewModel,
                        email = email,
                        otp = otp,
                        isVerifyingOTP = isVerifyingOTP,
                        isResendingOTP = isResendingOTP,
                        isVerifyButtonDisabled = isVerifyButtonDisabled,
                        canResendOTP = canResendOTP,
                        isOtpFieldFocused = isOtpFieldFocused, // Pass the state down
                        otpFocusRequester = otpFocusRequester,
                        focusManager = focusManager,
                        keyboardController = keyboardController,
                        onOtpFocusChanged = { isFocused -> isOtpFieldFocused = isFocused } // Update the state here
                    )
                } else {
                    SendingStageContent() // Show loading indicator
                }

                Spacer(modifier = Modifier.weight(1f)) // Pushes footer down
                HawcxFooter()
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}


// --- Stage Composables ---

@Composable
private fun SendingStageContent() {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Spacer(modifier = Modifier.height(60.dp))
        CircularProgressIndicator()
        Text("Requesting verification code...", style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(60.dp))
    }
}

@Composable
private fun VerificationStageContent(
    viewModel: AddDeviceViewModel,
    email: String,
    otp: String,
    isVerifyingOTP: Boolean,
    isResendingOTP: Boolean,
    isVerifyButtonDisabled: Boolean,
    canResendOTP: Boolean,
    isOtpFieldFocused: Boolean, // Receive state
    otpFocusRequester: FocusRequester,
    focusManager: androidx.compose.ui.focus.FocusManager,
    keyboardController: androidx.compose.ui.platform.SoftwareKeyboardController?,
    onOtpFocusChanged: (Boolean) -> Unit // Callback to update state in parent
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            "Enter Verification Code",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Enter the code sent to\n$email",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = otp,
            onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) viewModel.onOtpChange(it) },
            label = { Text("6-Digit Code") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.NumberPassword,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                    viewModel.verifyOtpButtonTapped()
                }
            ),
            textStyle = LocalTextStyle.current.copy(letterSpacing = 8.sp, textAlign = TextAlign.Center),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(otpFocusRequester)
                // --- CORRECTED ---
                .onFocusChanged { focusState -> onOtpFocusChanged(focusState.isFocused) } // Pass the boolean value
        )

        // Resend Code Button
        AnimatedVisibility(visible = canResendOTP) {
            LoadingButton(
                action = { viewModel.resendOtp() }, // Corrected to call resendOtp
                isLoading = isResendingOTP,
                isDisabled = isVerifyingOTP,
                isPrimary = false,
                modifier = Modifier.padding(top = 0.dp)
            ) {
                Text("Resend Code")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Verify Button
        LoadingButton(
            action = {
                focusManager.clearFocus()
                keyboardController?.hide()
                viewModel.verifyOtpButtonTapped()
            },
            isLoading = isVerifyingOTP,
            isDisabled = isVerifyButtonDisabled,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Verify & Add Device")
        }
    }
}