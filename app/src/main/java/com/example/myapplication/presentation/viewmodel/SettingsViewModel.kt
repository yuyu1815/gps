package com.example.myapplication.presentation.viewmodel

import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.repository.IBeaconRepository
import com.example.myapplication.data.repository.ISettingsRepository
import com.example.myapplication.domain.usecase.fusion.FuseSensorDataUseCase.FusionMethod
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File

/**
 * ViewModel for the settings screen of the application.
 * Handles user preferences and configuration options.
 */
class SettingsViewModel(
    private val beaconRepository: IBeaconRepository,
    private val settingsRepository: ISettingsRepository
) : BaseViewModel<SettingsViewModel.SettingsUiState>() {

    init {
        Timber.d("SettingsViewModel initialized with repositories")
        loadSettings()
    }

    /**
     * Loads all settings from the repository and updates the UI state.
     */
    private fun loadSettings() {
        viewModelScope.launch {
            try {
                val bleScanInterval = settingsRepository.getBleScanInterval()
                val environmentalFactor = settingsRepository.getEnvironmentalFactor()
                val stepLength = settingsRepository.getStepLength()
                val beaconStalenessTimeout = settingsRepository.getBeaconStalenessTimeout()
                val debugModeEnabled = settingsRepository.isDebugModeEnabled()
                val dataLoggingEnabled = settingsRepository.isDataLoggingEnabled()
                val sensorFusionWeight = settingsRepository.getSensorFusionWeight()
                val fusionMethod = settingsRepository.getFusionMethod()
                
                updateState(
                    SettingsUiState(
                        bleScanIntervalMs = bleScanInterval,
                        environmentalFactor = environmentalFactor,
                        stepLengthCm = stepLength,
                        beaconStalenessTimeoutMs = beaconStalenessTimeout,
                        debugModeEnabled = debugModeEnabled,
                        dataLoggingEnabled = dataLoggingEnabled,
                        sensorFusionWeight = sensorFusionWeight,
                        fusionMethod = fusionMethod
                    )
                )
                Timber.d("Settings loaded successfully")
            } catch (e: Exception) {
                Timber.e(e, "Error loading settings")
                updateState(SettingsUiState())
            }
        }
    }

    /**
     * Updates the BLE scan interval setting.
     */
    fun updateBleScanInterval(intervalMs: Long) {
        viewModelScope.launch {
            try {
                settingsRepository.setBleScanInterval(intervalMs)
                uiState.value?.let {
                    Timber.d("Updating BLE scan interval to $intervalMs ms")
                    updateState(it.copy(bleScanIntervalMs = intervalMs))
                }
            } catch (e: Exception) {
                Timber.e(e, "Error updating BLE scan interval")
            }
        }
    }

    /**
     * Updates the environmental factor setting.
     */
    fun updateEnvironmentalFactor(factor: Float) {
        viewModelScope.launch {
            try {
                settingsRepository.setEnvironmentalFactor(factor)
                uiState.value?.let {
                    Timber.d("Updating environmental factor to $factor")
                    updateState(it.copy(environmentalFactor = factor))
                }
            } catch (e: Exception) {
                Timber.e(e, "Error updating environmental factor")
            }
        }
    }

    /**
     * Updates the step length setting.
     */
    fun updateStepLength(lengthCm: Float) {
        viewModelScope.launch {
            try {
                settingsRepository.setStepLength(lengthCm)
                uiState.value?.let {
                    Timber.d("Updating step length to $lengthCm cm")
                    updateState(it.copy(stepLengthCm = lengthCm))
                }
            } catch (e: Exception) {
                Timber.e(e, "Error updating step length")
            }
        }
    }

    /**
     * Updates the beacon staleness timeout setting.
     */
    fun updateBeaconStalenessTimeout(timeoutMs: Long) {
        viewModelScope.launch {
            try {
                settingsRepository.setBeaconStalenessTimeout(timeoutMs)
                uiState.value?.let {
                    Timber.d("Updating beacon staleness timeout to $timeoutMs ms")
                    updateState(it.copy(beaconStalenessTimeoutMs = timeoutMs))
                }
            } catch (e: Exception) {
                Timber.e(e, "Error updating beacon staleness timeout")
            }
        }
    }

    /**
     * Updates the debug mode setting.
     */
    fun updateDebugMode(enabled: Boolean) {
        viewModelScope.launch {
            try {
                settingsRepository.setDebugModeEnabled(enabled)
                uiState.value?.let {
                    Timber.d("Updating debug mode to $enabled")
                    updateState(it.copy(debugModeEnabled = enabled))
                }
            } catch (e: Exception) {
                Timber.e(e, "Error updating debug mode")
            }
        }
    }

    /**
     * Updates the data logging setting.
     */
    fun updateDataLogging(enabled: Boolean) {
        viewModelScope.launch {
            try {
                settingsRepository.setDataLoggingEnabled(enabled)
                uiState.value?.let {
                    Timber.d("Updating data logging to $enabled")
                    updateState(it.copy(dataLoggingEnabled = enabled))
                }
            } catch (e: Exception) {
                Timber.e(e, "Error updating data logging")
            }
        }
    }

    /**
     * Updates the sensor fusion weight setting.
     */
    fun updateSensorFusionWeight(weight: Float) {
        viewModelScope.launch {
            try {
                settingsRepository.setSensorFusionWeight(weight)
                uiState.value?.let {
                    Timber.d("Updating sensor fusion weight to $weight")
                    updateState(it.copy(sensorFusionWeight = weight))
                }
            } catch (e: Exception) {
                Timber.e(e, "Error updating sensor fusion weight")
            }
        }
    }
    
    /**
     * Updates the fusion method setting.
     * Switches between weighted averaging and Kalman filter for sensor fusion.
     */
    fun updateFusionMethod(method: FusionMethod) {
        viewModelScope.launch {
            try {
                settingsRepository.setFusionMethod(method)
                uiState.value?.let {
                    Timber.d("Updating fusion method to $method")
                    updateState(it.copy(fusionMethod = method))
                }
            } catch (e: Exception) {
                Timber.e(e, "Error updating fusion method")
            }
        }
    }
    
    /**
     * Resets all settings to their default values.
     */
    fun resetToDefaults() {
        viewModelScope.launch {
            try {
                settingsRepository.resetToDefaults()
                loadSettings()
                Timber.d("Settings reset to defaults")
            } catch (e: Exception) {
                Timber.e(e, "Error resetting settings to defaults")
            }
        }
    }
    
    // Export/Import operation status
    private val _exportImportStatus = MutableStateFlow<ExportImportStatus>(ExportImportStatus.Idle)
    val exportImportStatus: StateFlow<ExportImportStatus> = _exportImportStatus.asStateFlow()
    
    /**
     * Exports all settings to a JSON file.
     * @param file The file to export settings to
     */
    fun exportSettings(file: File) {
        viewModelScope.launch {
            try {
                _exportImportStatus.value = ExportImportStatus.InProgress("Exporting settings...")
                val success = settingsRepository.exportSettings(file)
                if (success) {
                    Timber.d("Settings exported successfully to ${file.absolutePath}")
                    _exportImportStatus.value = ExportImportStatus.Success("Settings exported successfully")
                } else {
                    Timber.e("Failed to export settings")
                    _exportImportStatus.value = ExportImportStatus.Error("Failed to export settings")
                }
            } catch (e: Exception) {
                Timber.e(e, "Error exporting settings")
                _exportImportStatus.value = ExportImportStatus.Error("Error: ${e.message}")
            }
        }
    }
    
    /**
     * Imports settings from a JSON file.
     * @param file The file to import settings from
     */
    fun importSettings(file: File) {
        viewModelScope.launch {
            try {
                _exportImportStatus.value = ExportImportStatus.InProgress("Importing settings...")
                val success = settingsRepository.importSettings(file)
                if (success) {
                    // Reload settings to update UI
                    loadSettings()
                    Timber.d("Settings imported successfully from ${file.absolutePath}")
                    _exportImportStatus.value = ExportImportStatus.Success("Settings imported successfully")
                } else {
                    Timber.e("Failed to import settings")
                    _exportImportStatus.value = ExportImportStatus.Error("Failed to import settings")
                }
            } catch (e: Exception) {
                Timber.e(e, "Error importing settings")
                _exportImportStatus.value = ExportImportStatus.Error("Error: ${e.message}")
            }
        }
    }
    
    /**
     * Resets the export/import status to idle.
     */
    fun resetExportImportStatus() {
        _exportImportStatus.value = ExportImportStatus.Idle
    }
    
    /**
     * Represents the status of export/import operations.
     */
    sealed class ExportImportStatus {
        object Idle : ExportImportStatus()
        data class InProgress(val message: String) : ExportImportStatus()
        data class Success(val message: String) : ExportImportStatus()
        data class Error(val message: String) : ExportImportStatus()
    }

    /**
     * Represents the UI state for the settings screen.
     */
    data class SettingsUiState(
        val bleScanIntervalMs: Long = 1000,
        val environmentalFactor: Float = 2.0f,
        val stepLengthCm: Float = 75.0f,
        val beaconStalenessTimeoutMs: Long = 5000,
        val debugModeEnabled: Boolean = false,
        val dataLoggingEnabled: Boolean = false,
        val sensorFusionWeight: Float = 0.5f,
        val fusionMethod: FusionMethod = FusionMethod.WEIGHTED_AVERAGE
    )
}