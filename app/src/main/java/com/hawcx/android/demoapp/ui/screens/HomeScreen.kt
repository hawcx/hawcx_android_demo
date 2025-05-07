package com.hawcx.android.demoapp.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hawcx.android.demoapp.R
import com.hawcx.android.demoapp.ui.composables.HawcxFooter
import com.hawcx.android.demoapp.ui.composables.LoadingButton
import com.hawcx.android.demoapp.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel
) {
    val username by viewModel.username.collectAsStateWithLifecycle()
    // val devices by viewModel.devices.collectAsStateWithLifecycle() // State for device list if needed
    // val isLoading by viewModel.isLoading.collectAsStateWithLifecycle() // State for loading indicator

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Home") })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()) // Make content scrollable
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween // Push content/footer
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) { // Group top content
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

                Spacer(modifier = Modifier.height(24.dp))

                // Welcome Banner (Card for better appearance)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "Welcome to Hawcx",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Text(
                            username ?: "User", // Display username
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Text(
                            "This is a sample app built using the Hawcx passwordless framework. You can take this as a template and build your application on top of this.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            // Logout Button and Footer (Group bottom content)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(modifier = Modifier.height(32.dp)) // Space above button
                Button(
                    onClick = { viewModel.logoutButtonTapped() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error) // Red color
                ) {
                    Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text("Logout", fontWeight = FontWeight.SemiBold)
                }
                Spacer(modifier = Modifier.height(24.dp)) // Space between button and footer
                HawcxFooter()
            }
        }
    }
}