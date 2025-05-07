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
import com.hawcx.android.demoapp.viewmodel.SignUpViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(
    navController: NavHostController,
    viewModel: SignUpViewModel
) {
    val email by viewModel.email.collectAsStateWithLifecycle()
    val otp by viewModel.otp.collectAsStateWithLifecycle()
    val isSignUpLoading by viewModel.isSignUpLoading.collectAsStateWithLifecycle()
    val isVerifyingOTP by viewModel.isVerifyingOTP.collectAsStateWithLifecycle()
    val isResendingOTP by viewModel.isResendingOTP.collectAsStateWithLifecycle()
    val isSignUpButtonDisabled by viewModel.isSignUpButtonDisabled.collectAsStateWithLifecycle()
    val isVerifyOtpButtonDisabled by viewModel.isVerifyOtpButtonDisabled.collectAsStateWithLifecycle()
    val showOtpField by viewModel.showOtpField.collectAsStateWithLifecycle()
    val canResendOTP by viewModel.canResendOTP.collectAsStateWithLifecycle()

    // Navigation observer ONLY for existing user case
    val navigateBackToLoginExistingUser by viewModel.navigateBackToLoginExistingUser.collectAsStateWithLifecycle()

    // --- Pass NavController to ViewModel ---
    LaunchedEffect(viewModel, navController) {
        viewModel.setNavController(navController)
    }

    // --- Keep LaunchedEffect for navigating back on USER_ALREADY_EXISTS ---
    LaunchedEffect(navigateBackToLoginExistingUser) {
        navigateBackToLoginExistingUser?.let { existingEmail ->
            navController.popBackStack(AppScreen.Login.route, inclusive = false)
            viewModel.clearNavigationTrigger()
        }
    }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var isEmailFieldFocused by remember { mutableStateOf(false) }
    var isOtpFieldFocused by remember { mutableStateOf(false) }
    val emailFocusRequester = remember { FocusRequester() }
    val otpFocusRequester = remember { FocusRequester() }
    val interactionSource = remember { MutableInteractionSource() }

    // Request focus logic
    LaunchedEffect(showOtpField) {
        kotlinx.coroutines.delay(300) // Delay for layout/transition
        if (showOtpField) {
            otpFocusRequester.requestFocus()
            keyboardController?.show()
        } else {
            emailFocusRequester.requestFocus()
            // Don't force show keyboard when returning to email field
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Account") },
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
                // ... Rest of the UI inside the Column ...
                Spacer(modifier = Modifier.height(16.dp)) // Space below TopAppBar

                // Logo
                Image(
                    painter = painterResource(id = R.drawable.hawcx_logo),
                    contentDescription = "Hawcx Logo",
                    modifier = Modifier.fillMaxWidth(0.5f).aspectRatio(1f)
                )
                Text(
                    "HAWCX",
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.Serif,
                    modifier = Modifier.offset(y = (-50).dp) // Adjust overlap
                )

                Text(
                    if (showOtpField) "Enter the OTP sent to your email" else "Enter your email to get started",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Email Field
                OutlinedTextField(
                    value = email,
                    onValueChange = { viewModel.onEmailChange(it) },
                    label = { Text("Email Address") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = {
                            if (!showOtpField) {
                                viewModel.signUpButtonTapped()
                            } else {
                                focusManager.clearFocus() // Should move to OTP if shown
                            }
                        }
                    ),
                    enabled = !showOtpField, // Disable when showing OTP
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(emailFocusRequester)
                        .onFocusChanged { isEmailFieldFocused = it.isFocused }
                )

                // OTP Change Email Row
                AnimatedVisibility(visible = showOtpField) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp, start = 4.dp, end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Code sent to $email",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextButton(onClick = { viewModel.changeEmail() }, enabled = !isVerifyingOTP && !isResendingOTP) {
                            Text("Change")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // OTP Field (Animated Visibility)
                AnimatedVisibility(visible = showOtpField) {
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
                        textStyle = LocalTextStyle.current.copy(letterSpacing = 8.sp, textAlign = TextAlign.Center), // Spacing
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(otpFocusRequester)
                            .onFocusChanged { isOtpFieldFocused = it.isFocused }
                    )
                }

                // Resend Code Button
                AnimatedVisibility(visible = showOtpField && canResendOTP) {
                    LoadingButton(
                        action = { viewModel.resendOtp() },
                        isLoading = isResendingOTP,
                        isDisabled = isVerifyingOTP, // Disable if verifying
                        isPrimary = false, // Secondary style
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text("Resend Code")
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Sign Up / Submit Button
                LoadingButton(
                    action = {
                        focusManager.clearFocus()
                        keyboardController?.hide()
                        if (showOtpField) viewModel.verifyOtpButtonTapped() else viewModel.signUpButtonTapped()
                    },
                    isLoading = if (showOtpField) isVerifyingOTP else isSignUpLoading,
                    isDisabled = if (showOtpField) isVerifyOtpButtonDisabled else isSignUpButtonDisabled,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (showOtpField) "Verify & Sign Up" else "Sign Up")
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Login Prompt (Hide when OTP field is shown) - Fixed alignment
                AnimatedVisibility(visible = !showOtpField) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Already have an account? ", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        TextButton(
                            onClick = { navController.popBackStack() },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                        ) {
                            Text("Log In", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f)) // Pushes footer down
                HawcxFooter()
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}