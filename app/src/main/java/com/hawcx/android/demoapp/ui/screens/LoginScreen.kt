package com.hawcx.android.demoapp.ui.screens

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.hawcx.android.demoapp.R
import com.hawcx.android.demoapp.model.AppScreen
import com.hawcx.android.demoapp.ui.composables.BiometricEnableDialog
import com.hawcx.android.demoapp.ui.composables.HawcxFooter
import com.hawcx.android.demoapp.ui.composables.LoadingButton
import com.hawcx.android.demoapp.viewmodel.LoginViewEvent
import com.hawcx.android.demoapp.viewmodel.LoginViewModel
import com.hawcx.utils.SDKLogger
import kotlinx.coroutines.flow.collectLatest

@Composable
fun LoginScreen(
    navController: NavHostController,
    viewModel: LoginViewModel
) {
    // Set NavController in ViewModel for direct navigation
    LaunchedEffect(navController) {
        viewModel.navController = navController
    }

    val email by viewModel.email.collectAsStateWithLifecycle()
    val isLoggingIn by viewModel.isLoggingIn.collectAsStateWithLifecycle()
    val isBiometricLoggingIn by viewModel.isBiometricLoggingIn.collectAsStateWithLifecycle()
    val isLoginButtonDisabled by viewModel.isLoginButtonDisabled.collectAsStateWithLifecycle()
    val showBiometricButton by viewModel.showBiometricButton.collectAsStateWithLifecycle()
    val isBiometricAvailable by viewModel.isBiometricAvailable.collectAsStateWithLifecycle()
    val shouldShowBiometricEnablePrompt by viewModel.shouldShowBiometricEnablePrompt.collectAsStateWithLifecycle()

    // Navigation observers
    val navigateToSignUp by viewModel.navigateToSignUp.collectAsStateWithLifecycle()
    val navigateToAddDevice by viewModel.navigateToAddDevice.collectAsStateWithLifecycle()

    // Handle SignUp navigation
    LaunchedEffect(navigateToSignUp) {
        navigateToSignUp?.let { emailForSignUp ->
            navController.navigate(AppScreen.SignUp.createRoute(emailForSignUp))
            viewModel.clearNavigationTriggers()
        }
    }

    // Handle AddDevice navigation
    LaunchedEffect(navigateToAddDevice) {
        if (navigateToAddDevice != null) {
            // Navigate to the simplified route, email is in AppViewModel
            val route = AppScreen.AddDevice.createRoute()
            SDKLogger.d("Navigating to simplified route: $route", tag="LoginScreen")
            navController.navigate(route)
            viewModel.clearNavigationTriggers()
        }
    }

    // Get the FragmentActivity context from AppViewModel
    val context = LocalContext.current

    // Get the activity from AppViewModel - CRITICAL for biometric prompt
    val activityContext = remember(context) {
        when (context) {
            is FragmentActivity -> context
            else -> null
        }
    }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var isEmailFieldFocused by remember { mutableStateOf(false) }
    val emailFocusRequester = remember { FocusRequester() }
    val interactionSource = remember { MutableInteractionSource() }

    // --- Enhanced Event Handling for Biometric Prompt ---
    LaunchedEffect(Unit) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is LoginViewEvent.RequestBiometricPrompt -> {
                    if (activityContext != null) {
                        SDKLogger.i("Showing biometric prompt: ${event.title}", tag="LoginScreen")
                        try {
                            // Use the FragmentActivity context for BiometricPrompt
                            val fragmentActivity = activityContext as FragmentActivity
                            // Call BiometricAuthHelper with proper context
                            com.hawcx.android.demoapp.util.BiometricAuthHelper.requestBiometricAuth(
                                context = fragmentActivity,
                                title = event.title,
                                subtitle = event.subtitle,
                                onSuccess = {
                                    SDKLogger.i("Biometric prompt succeeded", tag="LoginScreen")
                                    event.onPromptSuccess()
                                },
                                onFailure = { errCode, errString ->
                                    SDKLogger.w("Biometric prompt failed: $errCode", tag="LoginScreen")
                                    event.onPromptFailure(errCode, errString)
                                }
                            )
                        } catch (e: Exception) {
                            SDKLogger.e("Exception showing biometric prompt", tag="LoginScreen")
                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                            event.onPromptFailure(-1, "Exception: ${e.message}")
                        }
                    } else {
                        SDKLogger.e("Cannot show biometric prompt: Invalid context", tag="LoginScreen")
                        Toast.makeText(context, "Cannot show biometric prompt: Invalid context", Toast.LENGTH_LONG).show()
                        event.onPromptFailure(-1, "Invalid context for biometric prompt")
                    }
                }

                is LoginViewEvent.RequestEnableBiometricPrompt -> {
                    if (activityContext != null) {
                        SDKLogger.i("Showing enable biometric prompt", tag="LoginScreen")
                        try {
                            // Use the FragmentActivity context for BiometricPrompt
                            val fragmentActivity = activityContext as FragmentActivity
                            com.hawcx.android.demoapp.util.BiometricAuthHelper.requestBiometricAuth(
                                context = fragmentActivity,
                                title = event.title,
                                subtitle = event.subtitle,
                                onSuccess = {
                                    SDKLogger.i("Biometric verification succeeded", tag="LoginScreen")
                                    event.onPromptSuccess()
                                },
                                onFailure = { errCode, errString ->
                                    SDKLogger.w("Biometric verification failed: $errCode", tag="LoginScreen")
                                    event.onPromptFailure(errCode, errString)
                                }
                            )
                        } catch (e: Exception) {
                            SDKLogger.e("Exception in biometric verification", tag="LoginScreen")
                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                            event.onPromptFailure(-1, "Exception: ${e.message}")
                        }
                    } else {
                        SDKLogger.e("Cannot show enable biometric prompt: Invalid context", tag="LoginScreen")
                        Toast.makeText(context, "Cannot verify biometrics: Invalid context", Toast.LENGTH_LONG).show()
                        event.onPromptFailure(-1, "Invalid context for biometric verification")
                    }
                }
            }
        }
    }

    // Request focus for email field when the screen appears
    LaunchedEffect(Unit) {
        emailFocusRequester.requestFocus()
        keyboardController?.show()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            // Add clickable modifier to dismiss keyboard when clicking on background
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                focusManager.clearFocus()
                keyboardController?.hide()
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(30.dp))

            // Logo
            Image(
                painter = painterResource(id = R.drawable.hawcx_logo),
                contentDescription = "Hawcx Logo",
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .aspectRatio(1f)
            )
            Text(
                "HAWCX",
                fontSize = 48.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Serif,
                modifier = Modifier.offset(y = (-60).dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "Log in to continue",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Email Field
            OutlinedTextField(
                value = email,
                onValueChange = { viewModel.onEmailChange(it) },
                label = { Text("Email Address") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    focusManager.clearFocus(); keyboardController?.hide(); viewModel.loginButtonTapped()
                }),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(emailFocusRequester)
                    .onFocusChanged { isEmailFieldFocused = it.isFocused }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Login Button
            LoadingButton(
                action = {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                    viewModel.loginButtonTapped()
                },
                isLoading = isLoggingIn,
                isDisabled = isLoginButtonDisabled,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Log In")
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Sign Up Prompt - Fix alignment
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Don't have an account? ", color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(
                    onClick = {
                        viewModel.clearNavigationTriggers()
                        navController.navigate(AppScreen.SignUp.createRoute(email.ifEmpty { null }))
                    },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Text("Sign Up", fontWeight = FontWeight.SemiBold)
                }
            }

            // Biometric Button (Conditional)
            if (showBiometricButton) {
                Spacer(modifier = Modifier.height(16.dp))
                LoadingButton(
                    action = {
                        focusManager.clearFocus()
                        keyboardController?.hide()
                        viewModel.biometricButtonTapped()
                    },
                    isLoading = isBiometricLoggingIn,
                    isDisabled = !isBiometricAvailable || isBiometricLoggingIn || isLoggingIn,
                    isPrimary = true,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Use Fingerprint icon for biometric login
                        Icon(
                            Icons.Default.Fingerprint,
                            contentDescription = null,
                            modifier = Modifier.size(ButtonDefaults.IconSize)
                        )
                        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                        Text("Login with Biometrics")
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            HawcxFooter()
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Biometric Enable Dialog (Overlay)
        if (shouldShowBiometricEnablePrompt) {
            BiometricEnableDialog(
                onEnableClick = {
                    // When user clicks Enable, initiate biometric authentication through ViewModel
                    if (activityContext != null) {
                        viewModel.enableBiometrics(activityContext)
                    } else {
                        Toast.makeText(context, "Error: Could not display biometric prompt.", Toast.LENGTH_SHORT).show()
                        viewModel.declineBiometrics()
                    }
                },
                onDismissClick = {
                    viewModel.declineBiometrics()
                }
            )
        }
    }
}