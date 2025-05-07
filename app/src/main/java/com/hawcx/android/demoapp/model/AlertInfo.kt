package com.hawcx.android.demoapp.model

import java.util.UUID

// For dismissible alerts
data class AlertInfo(
    val id: UUID = UUID.randomUUID(),
    val title: String,
    val message: String
)

// For non-dismissible loading alerts
data class LoadingAlertInfo(
    val id: UUID = UUID.randomUUID(),
    val title: String,
    val message: String
)