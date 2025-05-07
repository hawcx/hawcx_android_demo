import java.io.FileInputStream
import java.util.Properties

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.hawcx.android.demoapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.hawcx.android.demoapp"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true // Added for Compose vector support
        }
    }

    signingConfigs {
        create("release") {
            if (keystorePropertiesFile.exists()) {
                storeFile = file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
            } else {
                // Fallback for CI
                storeFile = file("/Users/agambhullar/keystores")
                storePassword = System.getenv("KEYSTORE_PASSWORD") ?: "password"
                keyAlias = System.getenv("KEY_ALIAS") ?: "hawcx"
                keyPassword = System.getenv("KEY_PASSWORD") ?: "password"
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true // Enable minification for release
            isShrinkResources = true // Enable resource shrinking
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            isDebuggable = false
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            isMinifyEnabled = false // Usually false for debug
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
    // ADDED: Compose compiler options
    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.composeCompiler.get()
    }
    // ADDED: Packaging options for Compose/Kotlin libraries
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/LICENSE.md"
            excludes += "/META-INF/LICENSE-notice.md"
        }
    }
}

dependencies {

    // ADDED: Hawcx SDK (referencing the AAR in app/libs)
    implementation(files("libs/hawcx-3.0.0.aar")) // Keep this line

    // Core AndroidX & Kotlin (Existing)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.splashscreen)

    // Jetpack Compose UI Toolkit (Existing)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    // ADDED: Material Components for XML themes/attributes (likely needed by a library)
    implementation(libs.google.android.material)

    // ADDED: ViewModel and Lifecycle for Compose
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // ADDED: Navigation Compose
    implementation(libs.androidx.navigation.compose)

    // ADDED: Coroutines (Already added correctly)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // ADDED: Biometrics KTX (Already added correctly)
    implementation(libs.androidx.biometric.ktx)

    // ADDED: Lottie for Compose (Optional)
    implementation(libs.lottie.compose)

    // ADDED: Material Icons Extended
    implementation("androidx.compose.material:material-icons-extended:1.7.8")

    // Testing (Existing)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // --- CORRECTED Explicit Dependencies for hawcx-1.0.aar ---
    // Use function call syntax implementation(...) / api(...) consistently
    // Ensure these aliases exist in your gradle/libs.versions.toml

    // Gson (Required for SDK's internal JSON handling)
    // Using implementation is generally fine here unless SDK exposes Gson types in its public API
    implementation(libs.gson)

    // Retrofit & OkHttp (Required for SDK's networking)
    implementation(libs.retrofit)
    implementation(libs.converter.gson) // Correct alias
    implementation(libs.okhttp)
    implementation(libs.logging.interceptor)

    // NOTE: Coroutines and Biometrics are already declared above, no need to repeat them here.

}