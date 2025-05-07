package com.hawcx.android.demoapp.ui.theme

import androidx.compose.ui.graphics.Color

// Hawcx colors
val HawcxBlue = Color(0xFF007AFF) // Main brand color
val HawcxBlueLight = Color(0xFF4DA3FF) // Lighter variant for surface highlights
val HawcxBlueDark = Color(0xFF0055B3) // Darker variant for focused states

// Accent colors
val HawcxRed = Color(0xFFFF3B30) // For alerts/errors/logout
val HawcxGray = Color(0xFF8E8E93) // For subtle UI elements
val HawcxLightGray = Color(0xFFE5E5EA) // For backgrounds and disabled states

// Let's not use purple/pink colors at all
// Replacing them with Hawcx blue variants for consistent branding
val Primary = HawcxBlue
val Secondary = HawcxBlueDark
val Tertiary = HawcxBlueLight