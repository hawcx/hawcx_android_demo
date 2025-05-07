package com.hawcx.android.demoapp.ui.composables

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.window.Dialog // Use Dialog for non-dismissible loading
import com.hawcx.android.demoapp.model.AlertInfo
import com.hawcx.android.demoapp.model.LoadingAlertInfo
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle

// Reusable AlertDialog for dismissible messages
@Composable
fun AppAlertDialog(
    alertInfo: AlertInfo,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(alertInfo.title) },
        text = { Text(alertInfo.message) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
}

// Reusable Dialog for non-dismissible loading indicators
@Composable
fun AppLoadingDialog(
    loadingInfo: LoadingAlertInfo
    // No onDismiss needed
) {
    Dialog(onDismissRequest = { /* Do nothing, non-dismissible */ }) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.8f) // Adjust width
                .padding(16.dp),
            shape = MaterialTheme.shapes.large,
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(loadingInfo.title, style = MaterialTheme.typography.titleLarge)
                Text(loadingInfo.message, style = MaterialTheme.typography.bodyMedium, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                Spacer(modifier = Modifier.height(8.dp))
                CircularProgressIndicator()
            }
        }
    }
}

// Helper composable to manage showing the correct dialog from AppViewModel
@Composable
fun AppAlertDialogs(appViewModel: com.hawcx.android.demoapp.viewmodel.AppViewModel) {
    val alertInfo by appViewModel.alertInfo.collectAsStateWithLifecycle()
    val loadingInfo by appViewModel.loadingAlertInfo.collectAsStateWithLifecycle()

    alertInfo?.let { info ->
        AppAlertDialog(alertInfo = info, onDismiss = { appViewModel.clearAlert() })
    }

    loadingInfo?.let { info ->
        AppLoadingDialog(loadingInfo = info)
    }
}