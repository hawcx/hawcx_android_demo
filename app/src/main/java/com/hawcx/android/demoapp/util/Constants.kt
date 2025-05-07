package com.hawcx.android.demoapp.util

import java.util.regex.Pattern

object Constants {
    // Use the same API key as the iOS demo
    const val HAWCX_API_KEY: String = "LmVKd2x5VXNLQWpFTUFOQzdaQzJTTnAwNGNlVk5TdHEwVUhTd3pBY1I4ZTRXM0w3M2dXTXJhMndHVjlqTHR0OFdiWTl6Zmk1d0F1MzlIODVUME0wR3JmcUsybHU4bF9kd25FUVZaUnFkZlhiQmVVU3VsaTZlaFdkaU5xdGlVbEppOWxJeHp6NHdVVVVqWWdzSTN4X1dfU1VmLlpyTDJqZy52cXVvUVMxVzZUemI5akFYY3ctMFAtT3NnSnk4dmhOLTJnci1CUnAtQlVEbEVoZlg1cW5IQ0tybmxtckQ0YkV4U2F0OUxRUk80YjJndHRfTHgyVzdsZw=="

    // UserDefaults Keys translated to SharedPreferences Keys
    object PrefsKeys {
        const val LAST_USER = "lastLoggedInUser"
        const val BIOMETRIC_PREFIX = "biometric_pref_"
        const val APP_PREFS_NAME = "HawcxDemoAppPrefs"
        const val SDK_DEVICE_DETAILS = "devDetails"
        const val SDK_SESSION_DETAILS = "sessionDetails"
        const val SDK_WEB_TOKEN = "web_token"
    }
}

// --- Utility Functions ---

private val EMAIL_ADDRESS_PATTERN: Pattern = Pattern.compile(
    "[a-zA-Z0-9\\+\\.\\_\\%\\-\\+]{1,256}" +
            "\\@" +
            "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}" +
            "(" +
            "\\." +
            "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25}" +
            ")+"
)

fun isValidEmail(email: String?): Boolean {
    return !email.isNullOrBlank() && EMAIL_ADDRESS_PATTERN.matcher(email).matches()
}

fun isValidOtp(otp: String?): Boolean {
    return otp?.length == 6 && otp.all { it.isDigit() }
}