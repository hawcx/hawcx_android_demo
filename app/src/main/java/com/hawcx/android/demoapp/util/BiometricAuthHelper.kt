package com.hawcx.android.demoapp.util

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.appcompat.app.AppCompatActivity // Or FragmentActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

object BiometricAuthHelper {

    private const val TAG = "BiometricAuthHelper"

    fun isBiometricAvailable(context: Context): Boolean {
        val biometricManager = BiometricManager.from(context)
        // Check for strong biometrics or device credentials
        val canAuthenticate = biometricManager.canAuthenticate(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
        Log.d(TAG, "Biometric check result: $canAuthenticate")
        return canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun requestBiometricAuth(
        context: Context, // Needs FragmentActivity or AppCompatActivity
        title: String,
        subtitle: String,
        negativeButtonText: String = "Cancel",
        onSuccess: () -> Unit,
        onFailure: (errorCode: Int, errString: String) -> Unit
    ) {
        if (context !is FragmentActivity && context !is AppCompatActivity) {
            Log.e(TAG, "Context must be a FragmentActivity or AppCompatActivity to show BiometricPrompt.")
            onFailure(-1, "Invalid context for biometric prompt.")
            return
        }

        val executor = ContextCompat.getMainExecutor(context)
        val biometricPrompt = BiometricPrompt(context as FragmentActivity, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Log.w(TAG, "Biometric Authentication error: $errorCode - $errString")
                    onFailure(errorCode, errString.toString())
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    Log.i(TAG, "Biometric Authentication succeeded!")
                    onSuccess()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Log.w(TAG, "Biometric Authentication failed (biometric not recognized).")
                    // onFailure is called via onAuthenticationError typically for final failure
                    // This callback indicates an intermediate failure (e.g., wrong finger)
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            // Allow device credential fallback (PIN/Pattern/Password)
            .setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
            // Deprecated but needed for older devices if not using setAllowedAuthenticators:
            // .setDeviceCredentialAllowed(true)
            // .setNegativeButtonText(negativeButtonText) // Handled by setAllowedAuthenticators or system
            .build()

        try {
            biometricPrompt.authenticate(promptInfo)
        } catch (e: Exception) {
            Log.e(TAG, "Error launching biometric prompt", e)
            onFailure(-1, "Could not launch biometric prompt: ${e.message}")
        }
    }
}