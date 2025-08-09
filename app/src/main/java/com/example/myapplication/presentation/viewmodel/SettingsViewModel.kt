package com.example.myapplication.presentation.viewmodel

import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.repository.ISettingsRepository
import com.example.myapplication.domain.model.FusionMethod
import com.example.myapplication.wifi.WifiFingerprintManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import com.example.myapplication.service.EventBus
import com.example.myapplication.domain.model.InitialFixMode

/**
 * ViewModel for the settings screen of the application.
 * Handles user preferences and configuration options.
 */
class SettingsViewModel(
    private val settingsRepository: ISettingsRepository,
    private val wifiFingerprintManager: WifiFingerprintManager
) : BaseViewModel<SettingsViewModel.SettingsUiState>() {

    init {
        Timber.d("SettingsViewModel initialized with repositories")
        // 初期状態を即座に設定し、設定の読み込みを非同期で実行
        updateState(SettingsUiState())
        loadSettingsAsync()
    }

    /**
     * Loads all settings from the repository and updates the UI state asynchronously.
     */
    private fun loadSettingsAsync() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val environmentalFactor = settingsRepository.getEnvironmentalFactor()
                val stepLength = settingsRepository.getStepLength()
                val debugModeEnabled = settingsRepository.isDebugModeEnabled()
                val dataLoggingEnabled = settingsRepository.isDataLoggingEnabled()
                val sensorFusionWeight = settingsRepository.getSensorFusionWeight()
                val fusionMethod = settingsRepository.getFusionMethod()
                val lowPowerModeEnabled = settingsRepository.isLowPowerModeEnabled()
                val lowPowerModeThreshold = settingsRepository.getLowPowerModeThreshold()
                val samplingRateReductionFactor = settingsRepository.getSamplingRateReductionFactor()
                val wifiEnabled = settingsRepository.isWifiEnabled()
                val selectedWifiBssids = settingsRepository.getSelectedWifiBssids()
                val appLanguage = settingsRepository.getAppLanguage()
                val sensorMonitoringDelay = settingsRepository.getSensorMonitoringDelay()
                val accelerometerDelay = settingsRepository.getAccelerometerDelay()
                val gyroscopeDelay = settingsRepository.getGyroscopeDelay()
                val magnetometerDelay = settingsRepository.getMagnetometerDelay()
                val linearAccelerationDelay = settingsRepository.getLinearAccelerationDelay()
                val gravityDelay = settingsRepository.getGravityDelay()
                val initialFixMode = settingsRepository.getInitialFixMode()
                
                withContext(Dispatchers.Main) {
                    updateState(
                        SettingsUiState(
                            environmentalFactor = environmentalFactor,
                            stepLengthCm = stepLength,
                            debugModeEnabled = debugModeEnabled,
                            dataLoggingEnabled = dataLoggingEnabled,
                            sensorFusionWeight = sensorFusionWeight,
                            fusionMethod = fusionMethod,
                            lowPowerModeEnabled = lowPowerModeEnabled,
                            lowPowerModeThreshold = lowPowerModeThreshold,
                            samplingRateReductionFactor = samplingRateReductionFactor,
                            wifiEnabled = wifiEnabled,
                            selectedWifiBssids = selectedWifiBssids,
                            appLanguage = appLanguage,
                            sensorMonitoringDelay = sensorMonitoringDelay.toInt(),
                            accelerometerDelay = accelerometerDelay.toInt(),
                            gyroscopeDelay = gyroscopeDelay.toInt(),
                            magnetometerDelay = magnetometerDelay.toInt(),
                            linearAccelerationDelay = linearAccelerationDelay.toInt(),
                            gravityDelay = gravityDelay.toInt(),
                            initialFixMode = initialFixMode
                        )
                    )
                }
                Timber.d("Settings loaded successfully")
            } catch (e: Exception) {
                Timber.e(e, "Error loading settings")
                withContext(Dispatchers.Main) {
                    updateState(SettingsUiState())
                }
            }
        }
    }

    /**
     * Updates the environmental factor setting.
     */
    fun updateEnvironmentalFactor(factor: Float) {
        // 即座にUIを更新
        uiState.value?.let { updateState(it.copy(environmentalFactor = factor)) }
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                settingsRepository.setEnvironmentalFactor(factor)
                Timber.d("Updating environmental factor to $factor")
            } catch (e: Exception) {
                Timber.e(e, "Error updating environmental factor")
            }
        }
    }

    /**
     * Updates the step length setting.
     */
    fun updateStepLength(lengthCm: Float) {
        // 即座にUIを更新
        uiState.value?.let { updateState(it.copy(stepLengthCm = lengthCm)) }
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                settingsRepository.setStepLength(lengthCm)
                Timber.d("Updating step length to $lengthCm cm")
            } catch (e: Exception) {
                Timber.e(e, "Error updating step length")
            }
        }
    }

    /**
     * Updates the debug mode setting.
     */
    fun updateDebugMode(enabled: Boolean) {
        // 即座にUIを更新
        uiState.value?.let { updateState(it.copy(debugModeEnabled = enabled)) }
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                settingsRepository.setDebugModeEnabled(enabled)
                Timber.d("Updating debug mode to $enabled")
            } catch (e: Exception) {
                Timber.e(e, "Error updating debug mode")
            }
        }
    }

    /**
     * Updates the data logging setting.
     */
    fun updateDataLogging(enabled: Boolean) {
        // 即座にUIを更新
        uiState.value?.let { updateState(it.copy(dataLoggingEnabled = enabled)) }
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                settingsRepository.setDataLoggingEnabled(enabled)
                Timber.d("Updating data logging to $enabled")
            } catch (e: Exception) {
                Timber.e(e, "Error updating data logging")
            }
        }
    }

    /**
     * Updates the sensor fusion weight setting.
     */
    fun updateSensorFusionWeight(weight: Float) {
        // 即座にUIを更新
        uiState.value?.let { updateState(it.copy(sensorFusionWeight = weight)) }
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                settingsRepository.setSensorFusionWeight(weight)
                Timber.d("Updating sensor fusion weight to $weight")
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
        // 即座にUIを更新
        uiState.value?.let { updateState(it.copy(fusionMethod = method)) }
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                settingsRepository.setFusionMethod(method)
                Timber.d("Updating fusion method to $method")
            } catch (e: Exception) {
                Timber.e(e, "Error updating fusion method")
            }
        }
    }
    
    /**
     * Updates the low-power mode setting.
     * Enables or disables battery-saving features.
     */
    fun updateLowPowerMode(enabled: Boolean) {
        // 即座にUIを更新
        uiState.value?.let { updateState(it.copy(lowPowerModeEnabled = enabled)) }
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                settingsRepository.setLowPowerModeEnabled(enabled)
                Timber.d("Updating low-power mode to $enabled")
            } catch (e: Exception) {
                Timber.e(e, "Error updating low-power mode")
            }
        }
    }
    
    /**
     * Updates the low-power mode threshold setting.
     * Sets the battery percentage at which low-power mode is automatically activated.
     */
    fun updateLowPowerModeThreshold(threshold: Int) {
        // 即座にUIを更新
        uiState.value?.let { updateState(it.copy(lowPowerModeThreshold = threshold)) }
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                settingsRepository.setLowPowerModeThreshold(threshold)
                Timber.d("Updating low-power mode threshold to $threshold%")
            } catch (e: Exception) {
                Timber.e(e, "Error updating low-power mode threshold")
            }
        }
    }
    
    /**
     * Updates the sampling rate reduction factor for low-power mode.
     * Higher values mean more aggressive reduction (e.g., 2.0 = half the normal rate).
     */
    fun updateSamplingRateReductionFactor(factor: Float) {
        // 即座にUIを更新
        uiState.value?.let { updateState(it.copy(samplingRateReductionFactor = factor)) }
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                settingsRepository.setSamplingRateReductionFactor(factor)
                Timber.d("Updating sampling rate reduction factor to $factor")
            } catch (e: Exception) {
                Timber.e(e, "Error updating sampling rate reduction factor")
            }
        }
    }
    
    /**
     * Resets all settings to their default values.
     */
    fun resetToDefaults() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                settingsRepository.resetToDefaults()
                loadSettingsAsync()
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
        viewModelScope.launch(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) {
                    _exportImportStatus.value = ExportImportStatus.InProgress("Exporting settings...")
                }
                val success = settingsRepository.exportSettings(file)
                withContext(Dispatchers.Main) {
                    if (success) {
                        Timber.d("Settings exported successfully to ${file.absolutePath}")
                        _exportImportStatus.value = ExportImportStatus.Success("Settings exported successfully")
                    } else {
                        Timber.e("Failed to export settings")
                        _exportImportStatus.value = ExportImportStatus.Error("Failed to export settings")
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error exporting settings")
                withContext(Dispatchers.Main) {
                    _exportImportStatus.value = ExportImportStatus.Error("Error: ${e.message}")
                }
            }
        }
    }
    
    /**
     * Imports settings from a JSON file.
     * @param file The file to import settings from
     */
    fun importSettings(file: File) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) {
                    _exportImportStatus.value = ExportImportStatus.InProgress("Importing settings...")
                }
                val success = settingsRepository.importSettings(file)
                if (success) {
                    // Reload settings to update UI
                    loadSettingsAsync()
                    withContext(Dispatchers.Main) {
                        Timber.d("Settings imported successfully from ${file.absolutePath}")
                        _exportImportStatus.value = ExportImportStatus.Success("Settings imported successfully")
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Timber.e("Failed to import settings")
                        _exportImportStatus.value = ExportImportStatus.Error("Failed to import settings")
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error importing settings")
                withContext(Dispatchers.Main) {
                    _exportImportStatus.value = ExportImportStatus.Error("Error: ${e.message}")
                }
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
     * Gets the count of stored Wi‑Fi fingerprints.
     */
    fun getWifiFingerprintCount(): Int {
        return try {
            wifiFingerprintManager.getAllFingerprints().size
        } catch (e: Exception) {
            Timber.e(e, "Error getting fingerprint count")
            0
        }
    }

    /**
     * Imports Wi‑Fi fingerprints from a JSON file.
     */
    fun importWifiFingerprints(file: java.io.File) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) {
                    _exportImportStatus.value = ExportImportStatus.InProgress("Importing Wi‑Fi fingerprints...")
                }
                val count = wifiFingerprintManager.loadFromFile(file.absolutePath)
                withContext(Dispatchers.Main) {
                    _exportImportStatus.value = ExportImportStatus.Success("Imported $count Wi‑Fi fingerprints")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _exportImportStatus.value = ExportImportStatus.Error("Failed to import Wi‑Fi fingerprints: ${e.message}")
                }
            }
        }
    }

    /**
     * Clears all stored Wi‑Fi fingerprints.
     */
    fun clearWifiFingerprints() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val before = wifiFingerprintManager.getAllFingerprints().size
                wifiFingerprintManager.clearFingerprints()
                withContext(Dispatchers.Main) {
                    _exportImportStatus.value = ExportImportStatus.Success("Cleared $before Wi‑Fi fingerprints")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _exportImportStatus.value = ExportImportStatus.Error("Failed to clear Wi‑Fi fingerprints: ${e.message}")
                }
            }
        }
    }

    fun exportWifiFingerprints(outFile: File) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) {
                    _exportImportStatus.value = ExportImportStatus.InProgress("Exporting Wi‑Fi fingerprints...")
                }
                val ok = wifiFingerprintManager.saveToFile(outFile.absolutePath)
                withContext(Dispatchers.Main) {
                    if (ok) {
                        _exportImportStatus.value = ExportImportStatus.Success("Exported Wi‑Fi fingerprints")
                    } else {
                        _exportImportStatus.value = ExportImportStatus.Error("Failed to export Wi‑Fi fingerprints")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _exportImportStatus.value = ExportImportStatus.Error("Failed to export Wi‑Fi fingerprints: ${e.message}")
                }
            }
        }
    }

    fun updateWifiEnabled(enabled: Boolean) {
        val previous = uiState.value?.wifiEnabled ?: false
        // 即時UI反映 + ローディング表示
        uiState.value?.let { updateState(it.copy(wifiEnabled = enabled, isWifiUpdating = true)) }
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                settingsRepository.setWifiEnabled(enabled)
            } catch (e: Exception) {
                Timber.e(e, "Error updating Wi‑Fi enabled")
                // 失敗時は元に戻す
                withContext(Dispatchers.Main) {
                    uiState.value?.let { updateState(it.copy(wifiEnabled = previous)) }
                }
            } finally {
                // ローディング解除
                withContext(Dispatchers.Main) {
                    uiState.value?.let { updateState(it.copy(isWifiUpdating = false)) }
                }
            }
        }
    }

    fun updateSelectedWifiBssids(bssids: Set<String>) {
        // 即時UI反映 → バックグラウンドで保存してラグを無くす
        uiState.value?.let { updateState(it.copy(selectedWifiBssids = bssids)) }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                settingsRepository.setSelectedWifiBssids(bssids)
            } catch (e: Exception) {
                Timber.e(e, "Error updating selected Wi‑Fi BSSIDs")
            }
        }
    }

    /**
     * Wi‑Fi スキャン中フラグの更新（UI専用）
     */
    fun setWifiScanning(isScanning: Boolean) {
        uiState.value?.let { updateState(it.copy(isWifiScanning = isScanning)) }
    }

    /**
     * Updates the app language setting.
     */
    fun updateAppLanguage(language: String) {
        // 即座にUIを更新
        uiState.value?.let { updateState(it.copy(appLanguage = language)) }
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                settingsRepository.setAppLanguage(language)
                Timber.d("Updating app language to $language")
            } catch (e: Exception) {
                Timber.e(e, "Error updating app language")
            }
        }
    }

    /**
     * Updates the sensor monitoring delay setting.
     */
    fun updateSensorMonitoringDelay(delayMs: Int) {
        // 即座にUIを更新
        uiState.value?.let { updateState(it.copy(sensorMonitoringDelay = delayMs)) }
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                settingsRepository.setSensorMonitoringDelay(delayMs.toLong())
                Timber.d("Updating sensor monitoring delay to ${delayMs}ms")
            } catch (e: Exception) {
                Timber.e(e, "Error updating sensor monitoring delay")
            }
        }
    }

    /**
     * Updates the accelerometer delay setting.
     */
    fun updateAccelerometerDelay(delayMs: Int) {
        // 即座にUIを更新
        uiState.value?.let { updateState(it.copy(accelerometerDelay = delayMs)) }
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                settingsRepository.setAccelerometerDelay(delayMs.toLong())
                Timber.d("Updating accelerometer delay to ${delayMs}ms")
            } catch (e: Exception) {
                Timber.e(e, "Error updating accelerometer delay")
            }
        }
    }

    /**
     * Updates the gyroscope delay setting.
     */
    fun updateGyroscopeDelay(delayMs: Int) {
        // 即座にUIを更新
        uiState.value?.let { updateState(it.copy(gyroscopeDelay = delayMs)) }
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                settingsRepository.setGyroscopeDelay(delayMs.toLong())
                Timber.d("Updating gyroscope delay to ${delayMs}ms")
            } catch (e: Exception) {
                Timber.e(e, "Error updating gyroscope delay")
            }
        }
    }

    /**
     * Updates the magnetometer delay setting.
     */
    fun updateMagnetometerDelay(delayMs: Int) {
        // 即座にUIを更新
        uiState.value?.let { updateState(it.copy(magnetometerDelay = delayMs)) }
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                settingsRepository.setMagnetometerDelay(delayMs.toLong())
                Timber.d("Updating magnetometer delay to ${delayMs}ms")
            } catch (e: Exception) {
                Timber.e(e, "Error updating magnetometer delay")
            }
        }
    }

    /**
     * Updates the linear acceleration delay setting.
     */
    fun updateLinearAccelerationDelay(delayMs: Int) {
        // 即座にUIを更新
        uiState.value?.let { updateState(it.copy(linearAccelerationDelay = delayMs)) }
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                settingsRepository.setLinearAccelerationDelay(delayMs.toLong())
                Timber.d("Updating linear acceleration delay to ${delayMs}ms")
            } catch (e: Exception) {
                Timber.e(e, "Error updating linear acceleration delay")
            }
        }
    }

    /**
     * Updates the gravity delay setting.
     */
    fun updateGravityDelay(delayMs: Int) {
        // 即座にUIを更新
        uiState.value?.let { updateState(it.copy(gravityDelay = delayMs)) }
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                settingsRepository.setGravityDelay(delayMs.toLong())
                Timber.d("Updating gravity delay to ${delayMs}ms")
            } catch (e: Exception) {
                Timber.e(e, "Error updating gravity delay")
            }
        }
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
        val environmentalFactor: Float = 2.0f,
        val stepLengthCm: Float = 75.0f,
        val debugModeEnabled: Boolean = false,
        val dataLoggingEnabled: Boolean = false,
        val sensorFusionWeight: Float = 0.5f,
        val fusionMethod: FusionMethod = FusionMethod.WEIGHTED_AVERAGE,
        val lowPowerModeEnabled: Boolean = false,
        val lowPowerModeThreshold: Int = 20,
        val samplingRateReductionFactor: Float = 2.0f,
        val wifiEnabled: Boolean = false,
        val isWifiUpdating: Boolean = false,
            val selectedWifiBssids: Set<String> = emptySet(),
            val appLanguage: String = "system",
            val isWifiScanning: Boolean = false,
            val sensorMonitoringDelay: Int = 1000, // センサー遅延時間のデフォルト値
            val accelerometerDelay: Int = 100,
            val gyroscopeDelay: Int = 100,
            val magnetometerDelay: Int = 100,
            val linearAccelerationDelay: Int = 100,
            val gravityDelay: Int = 100,
            val initialFixMode: InitialFixMode = InitialFixMode.AUTO
    )
    
    fun updateInitialFixMode(mode: InitialFixMode) {
        // 即座にUIを更新
        uiState.value?.let { updateState(it.copy(initialFixMode = mode)) }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                settingsRepository.setInitialFixMode(mode)
                Timber.d("Updating initial fix mode to $mode")
            } catch (e: Exception) {
                Timber.e(e, "Error updating initial fix mode")
            }
        }
    }
}