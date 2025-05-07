package com.hawcx.android.demoapp.ui.composables

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.* // Import Lottie

@Composable
fun LoadingButton(
    action: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean,
    isDisabled: Boolean,
    isPrimary: Boolean = true, // Controls style
    content: @Composable RowScope.() -> Unit
) {
    val compositionResult: LottieCompositionResult = rememberLottieComposition(LottieCompositionSpec.Asset("FastLoading.json"))
    val progressValue: Float = animateLottieCompositionAsState(
        compositionResult.value,
        iterations = LottieConstants.IterateForever,
        isPlaying = isLoading
    ).value

    Button(
        onClick = action,
        modifier = modifier,
        enabled = !isDisabled && !isLoading, // Disable when explicitly disabled OR loading
        colors = if (isPrimary) ButtonDefaults.buttonColors() else ButtonDefaults.textButtonColors() // Adapt colors
    ) {
        if (isLoading) {
            LottieAnimation(
                composition = compositionResult.value,
                progress = { progressValue },
                modifier = Modifier.size(24.dp) // Adjust size as needed
            )
            // Fallback ProgressIndicator (Uncomment if Lottie fails)
            /*
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp,
                color = if (isPrimary) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
            )
             */
        } else {
            // Use RowScope to allow content to be placed correctly
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth() // Make sure content fills button width
            ) {
                content()
            }
        }
    }
}