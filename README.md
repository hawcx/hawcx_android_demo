# Hawcx Demo App

[![Kotlin Version](https://img.shields.io/badge/Kotlin-1.9+-orange.svg)](https://kotlinlang.org)
[![Platform](https://img.shields.io/badge/platform-Android_8.0+-blue.svg)](https://developer.android.com/about)
[![License](https://img.shields.io/badge/license-MIT-green.svg)](LICENSE)

A comprehensive demonstration application showcasing the [Hawcx Android SDK](https://github.com/hawcx/hawcx_android_sdk) for passwordless authentication.

<img src="app/src/main/res/drawable/hawcx_logo.png" alt="Hawcx Demo App" width="200" />

## Features

### 🔐 Passwordless Authentication
- **No password required** - Authenticate users securely without the hassle of passwords
- **Email verification** - Simple OTP-based verification through email
- **Enterprise-grade security** - Backed by Hawcx's robust security infrastructure

### 📱 Multi-Device Support
- **Seamless device registration** - Add new devices to your account with simple verification
- **Cross-device login** - Access your account from any registered device
- **Session management** - View and manage all your active devices

### 👆 Biometric Authentication
- **Fingerprint & Face unlock integration** - Log in quickly with biometric authentication
- **Opt-in biometrics** - User-friendly prompts to enable biometric login
- **Secure implementation** - Uses Android's BiometricPrompt for secure authentication

### 💻 Modern Jetpack Compose Implementation
- **Complete Compose app** - Built entirely with Android's modern UI toolkit
- **MVVM architecture** - Clean separation of concerns with ViewModels
- **Comprehensive error handling** - User-friendly error messages and recovery flows

## Getting Started

### Prerequisites
- Android Studio Iguana or higher
- Android SDK 26+ (Android 8.0 Oreo)
- Kotlin 1.9+
- [Hawcx Android SDK](https://github.com/hawcx/hawcx_android_sdk)

### Installation

1. Clone this repository:
```bash
git clone https://github.com/hawcx/hawcx_android_demo.git
```

2. Open the project in Android Studio.

3. Configure your API key:
    - Open `Constants.kt`
    - Replace the placeholder API key with your actual Hawcx API key

```kotlin
object Constants {
    const val HAWCX_API_KEY = "YOUR_API_KEY_HERE"
    // ...
}
```

4. Build and run the project on an emulator or physical device.

## App Architecture

The demo app is built with a clean MVVM architecture:

### UI Components
- `LoginScreen` - Email login and biometric authentication
- `SignUpScreen` - New user registration with OTP verification
- `AddDeviceScreen` - Register new devices to an existing account
- `HomeScreen` - Post-login view with user details and device management

### ViewModels
- `LoginViewModel` - Handles authentication logic and biometric integration
- `SignUpViewModel` - Manages user registration flow
- `AddDeviceViewModel` - Controls device registration process
- `HomeViewModel` - Manages logged-in user state and device information

### Core Components
- `AppViewModel` - Central state management for authentication status
- `SharedAuthManager` - Facilitates communication between authentication components
- `BiometricAuthHelper` - Wrapper for Android's BiometricPrompt integration

## SDK Integration

The demo app demonstrates complete integration with the Hawcx SDK:

```kotlin
import com.hawcx.internal.HawcxInitializer

// Initialize the SDK
HawcxInitializer.getInstance().init(applicationContext, Constants.HAWCX_API_KEY)

// User authentication
val signInManager = HawcxInitializer.getInstance().signIn
signInManager.signIn(userid = email, callback = this)

// User registration
val signUpManager = HawcxInitializer.getInstance().signUp
signUpManager.signUp(userid = email, callback = this)

// Device management
val addDeviceManager = HawcxInitializer.getInstance().addDeviceManager
addDeviceManager.startAddDeviceFlow(userid = email, callback = this)

// Session information
val devSessionManager = HawcxInitializer.getInstance().devSession
devSessionManager.getDeviceDetails(callback = this)
```

## User Flows

### Registration
1. User enters email address
2. OTP is sent to the email
3. User verifies with OTP
4. Account is created and user is automatically logged in

### Login
1. User enters registered email
2. Authentication is completed automatically
3. Biometric login prompt is shown for future logins

### Add Device
1. User attempts to log in on a new device
2. Device registration flow is initiated
3. OTP verification confirms user identity
4. Device is added to the account

## Best Practices Demonstrated

- **Secure Authentication**: Implementation of passwordless authentication
- **Biometric Security**: Proper integration of Android's BiometricPrompt
- **Error Handling**: Comprehensive error handling and user feedback
- **State Management**: Clean state management using Kotlin Flow and StateFlow
- **UI/UX Design**: User-friendly interfaces using Jetpack Compose

## Resources

- [Hawcx Android SDK](https://github.com/hawcx/hawcx_android_sdk)
- [Documentation](https://docs.hawcx.com)
- [API Reference](https://docs.hawcx.com/android/quickstart)
- [Website](https://www.hawcx.com)
- [Support Email](mailto:info@hawcx.com)

## License

This demo app is available under the MIT license. See the LICENSE file for more info.
