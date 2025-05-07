package com.hawcx.android.demoapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hawcx.android.demoapp.util.Constants
import com.hawcx.internal.HawcxInitializer
import com.hawcx.model.DeviceDetails // Use SDK's DeviceDetails model
import com.hawcx.utils.DevSessionCallback
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect

class HomeViewModel(
    val appViewModel: AppViewModel // Inject AppViewModel
) : ViewModel(), DevSessionCallback {

    // Access SDK components via the Initializer
    private val devSessionManager = HawcxInitializer.getInstance().devSession

    // State exposed to the UI - now updated dynamically from appViewModel
    private val _username = MutableStateFlow(appViewModel.loggedInUsername.value ?: "User")
    val username: StateFlow<String> = _username.asStateFlow()

    // Use the SDK's DeviceDetails model directly
    private val _devices = MutableStateFlow<List<DeviceDetails>>(emptyList())
    val devices: StateFlow<List<DeviceDetails>> = _devices.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        // Observe changes to the loggedInUsername in AppViewModel
        viewModelScope.launch {
            appViewModel.loggedInUsername.collect { newUsername ->
                _username.value = newUsername ?: "User"
            }
        }

        // Optionally load previously fetched details immediately
        loadStoredDeviceDetails()
        // Automatically fetch details on init? Or require button press?
        // fetchDeviceDetails() // Uncomment to fetch on load
    }

    private fun loadStoredDeviceDetails() {
        viewModelScope.launch {
            _isLoading.value = true
            _devices.value = devSessionManager.getStoredDeviceDetails() ?: emptyList()
            _isLoading.value = false
        }
    }

    fun fetchDeviceDetails() {
        if (_isLoading.value) return
        _isLoading.value = true
        // DevSession manager handles its own coroutines/threading
        devSessionManager.getDeviceDetails(callback = this)
    }

    fun logoutButtonTapped() {
        // Delegate logout logic to AppViewModel to handle state and navigation
        appViewModel.logout {
            // Optional: Add any HomeViewModel specific cleanup here if needed
        }
    }

    // --- DevSessionCallback Implementation ---

    override fun onSuccess() {
        // Callback is already on Main thread from SDK's DevSession implementation
        _isLoading.value = false
        // Re-load the stored details after successful fetch & save
        loadStoredDeviceDetails()
        // Optionally show a success message (e.g., using Snackbar)
        // appViewModel.showSnackbar("Device details updated.")
    }

    override fun onError() {
        // Callback is already on Main thread
        _isLoading.value = false
        // Show error using AppViewModel's alert mechanism
        appViewModel.showAlert("Error", "Failed to fetch device session information.")
        // Optionally clear the list or keep stale data
        // _devices.value = emptyList()
    }
}