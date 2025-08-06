package com.example.myapplication.presentation.viewmodel

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.lifecycle.viewModelScope
import com.example.myapplication.service.BleScanService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * ViewModel for the main screen of the application.
 * Handles navigation between different sections and global app state.
 */
class MainViewModel : BaseViewModel<MainViewModel.MainUiState>() {

    private val _navigationState = MutableStateFlow<NavigationState>(NavigationState.Map)
    val navigationState: StateFlow<NavigationState> = _navigationState.asStateFlow()

    init {
        Timber.d("MainViewModel initialized")
        updateState(MainUiState())
    }

    /**
     * Navigates to a different section of the app.
     */
    fun navigateTo(destination: NavigationState) {
        viewModelScope.launch {
            Timber.d("Navigating to $destination")
            _navigationState.value = destination
        }
    }

    /**
     * Represents the possible navigation destinations in the app.
     */
    sealed class NavigationState {
        object Map : NavigationState()
        object Settings : NavigationState()
        object Calibration : NavigationState()
        object Debug : NavigationState()
        data class Details(val id: String) : NavigationState()
    }

    /**
     * Represents the UI state for the main screen.
     */
    data class MainUiState(
        val appTitle: String = "Indoor Positioning",
        val isBluetoothEnabled: Boolean = false,
        val areLocationServicesEnabled: Boolean = false,
        val areSensorsAvailable: Boolean = false,
        val isBackgroundScanningActive: Boolean = false
    )

    /**
     * Updates the Bluetooth state.
     */
    fun updateBluetoothState(enabled: Boolean) {
        uiState.value?.let {
            updateState(it.copy(isBluetoothEnabled = enabled))
        }
    }

    /**
     * Updates the location services state.
     */
    fun updateLocationServicesState(enabled: Boolean) {
        uiState.value?.let {
            updateState(it.copy(areLocationServicesEnabled = enabled))
        }
    }

    /**
     * Updates the sensors availability state.
     */
    fun updateSensorsAvailability(available: Boolean) {
        uiState.value?.let {
            updateState(it.copy(areSensorsAvailable = available))
        }
    }
    
    /**
     * Updates the background scanning service state.
     */
    fun updateBackgroundScanningState(active: Boolean) {
        uiState.value?.let {
            updateState(it.copy(isBackgroundScanningActive = active))
        }
    }
    
    /**
     * Starts the background BLE scanning service.
     * 
     * @param context The context used to start the service
     */
    fun startBackgroundScanning(context: Context) {
        Timber.d("Starting background BLE scanning service")
        
        val serviceIntent = Intent(context, BleScanService::class.java)
        
        // Start the service as a foreground service
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
        
        updateBackgroundScanningState(true)
    }
    
    /**
     * Stops the background BLE scanning service.
     * 
     * @param context The context used to stop the service
     */
    fun stopBackgroundScanning(context: Context) {
        Timber.d("Stopping background BLE scanning service")
        
        val serviceIntent = Intent(context, BleScanService::class.java)
        context.stopService(serviceIntent)
        
        updateBackgroundScanningState(false)
    }
}