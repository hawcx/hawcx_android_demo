package com.hawcx.android.demoapp

import android.app.Application
import com.hawcx.android.demoapp.util.Constants
import com.hawcx.internal.HawcxInitializer // Import the SDK's Initializer
import com.hawcx.utils.SDKLogger // Import the SDK's Logger

class MainApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize the Hawcx SDK
        try {
            HawcxInitializer.getInstance().init(
                context = this,
                apiKey = Constants.HAWCX_API_KEY
                // Logging is now controlled internally by the SDK build configuration
            )
            // SDKLogger will log its own initialization status
        } catch (e: Exception) {
            // Log critical initialization error
            SDKLogger.e("CRITICAL: Hawcx SDK initialization failed!", tag = "MainApplication", throwable = e)
            // Depending on the app's needs, you might want to crash,
            // show an error message, or disable SDK-dependent features.
        }
    }
}