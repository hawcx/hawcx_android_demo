# app/proguard-rules.pro (Fixed version)

# Proguard rules for the Hawcx Demo Application

#===============================================================================
# Keep App Entry Points & Core Components
#===============================================================================
-keep public class * extends android.app.Application
-keep public class * extends androidx.activity.ComponentActivity {
    public void *(android.os.Bundle);
}
-keep public class com.hawcx.android.demoapp.MainApplication # Keep custom Application class

# Keep attributes useful for debugging stack traces
-keepattributes Signature,SourceFile,LineNumberTable

#===============================================================================
# Jetpack Compose Rules
#===============================================================================
# Keep Composables used by tools (like preview) and runtime
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}
-keep class * {
    @androidx.compose.runtime.Composable <methods>;
}
# Keep classes used in Compose previews
-keepclasseswithmembers class * {
    @androidx.compose.ui.tooling.preview.Preview <methods>;
}
# Keep classes related to Saver/SaveableStateRegistry if using rememberSaveable extensively
-keepclassmembers class * implements androidx.compose.runtime.saveable.Saver {
    <init>();
    *[] newArray(int);
}
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

#===============================================================================
# Keep ViewModel Rules
#===============================================================================
-keep class * extends androidx.lifecycle.ViewModel { *; }
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}
-keep class androidx.lifecycle.ViewModelProvider$Factory { *; }


#===============================================================================
# Keep Kotlin Coroutines Rules
#===============================================================================
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.android.AndroidExceptionPreHandler {}
-keepnames class kotlinx.coroutines.android.AndroidDispatcherFactory {}
-keepclassmembernames class kotlinx.coroutines.flow.** {
    volatile <fields>;
}
-keepclassmembernames class kotlinx.coroutines.internal.** {
     volatile <fields>;
     private <fields>;
}

#===============================================================================
# Hawcx SDK Rules
#===============================================================================
# Keep the main Hawcx classes with wildcard to avoid signature issues
-keep public class com.hawcx.internal.HawcxInitializer { *; }
-keep public class com.hawcx.auth.** { *; }
-keep public class com.hawcx.model.** { *; }
-keep public interface com.hawcx.utils.*Callback { *; }
-keep public class com.hawcx.utils.*Error { *; }
-keep public enum com.hawcx.utils.** { *; }

# These rules are more general but safer than specific method signatures
-keep class com.hawcx.internal.** { public *; }
-keep class com.hawcx.storage.** { public *; }

#===============================================================================
# Third-Party Library Rules
#===============================================================================
# Retrofit & OkHttp rules
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions
-dontwarn okhttp3.**
-dontwarn okio.**

# Gson rules
-keep class com.google.gson.** { *; }
-keepattributes *Annotation*
-dontwarn sun.misc.**

# Biometric rules
-keep class androidx.biometric.** { *; }
-dontwarn androidx.biometric.**