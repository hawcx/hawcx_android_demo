package com.hawcx.android.demoapp.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * Custom ViewModelProvider Factory to handle creating ViewModels
 * that extend AndroidViewModel AND require additional dependencies (like AppViewModel).
 */
class ViewModelFactory(
    private val application: Application,
    private val appViewModel: AppViewModel // Pass the shared AppViewModel instance
) : ViewModelProvider.AndroidViewModelFactory(application) {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        // Check which ViewModel is being requested and construct it correctly
        return when {
            modelClass.isAssignableFrom(LoginViewModel::class.java) -> {
                // LoginViewModel IS AndroidViewModel, needs Application and AppViewModel
                LoginViewModel(application, appViewModel) as T
            }
            modelClass.isAssignableFrom(HomeViewModel::class.java) -> {
                // CORRECTED: HomeViewModel is NOT AndroidViewModel in the provided code,
                // its constructor only takes appViewModel.
                HomeViewModel(appViewModel) as T // Pass only appViewModel
            }
            // ... (Add SignUpViewModel/AddDeviceViewModel cases ONLY IF they extend AndroidViewModel AND need appViewModel) ...

            // Fallback for standard AndroidViewModels or other ViewModels handled by default factories
            else -> super.create(modelClass)
        }
    }
}