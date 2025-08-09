package com.example.myapplication.data.repository

import com.example.myapplication.domain.model.FusionMethod
import java.io.File

/**
 * Repository interface for managing application settings.
 * Handles the storage and retrieval of user preferences.
 */
interface ISettingsRepository {
    /**
     * Gets the BLE scan interval in milliseconds.
     * @return The scan interval
     */
    suspend fun getBleScanInterval(): Long
    
    /**
     * Sets the BLE scan interval in milliseconds.
     * @param intervalMs The scan interval to set
     */
    suspend fun setBleScanInterval(intervalMs: Long)
    
    /**
     * Gets the environmental factor used for distance calculations.
     * @return The environmental factor
     */
    suspend fun getEnvironmentalFactor(): Float
    
    /**
     * Sets the environmental factor used for distance calculations.
     * @param factor The environmental factor to set
     */
    suspend fun setEnvironmentalFactor(factor: Float)
    
    /**
     * Gets the step length in centimeters.
     * @return The step length
     */
    suspend fun getStepLength(): Float
    
    /**
     * Sets the step length in centimeters.
     * @param lengthCm The step length to set
     */
    suspend fun setStepLength(lengthCm: Float)
    
    /**
     * Gets the beacon staleness timeout in milliseconds.
     * @return The timeout
     */
    suspend fun getBeaconStalenessTimeout(): Long
    
    /**
     * Sets the beacon staleness timeout in milliseconds.
     * @param timeoutMs The timeout to set
     */
    suspend fun setBeaconStalenessTimeout(timeoutMs: Long)
    
    /**
     * Checks if debug mode is enabled.
     * @return True if debug mode is enabled, false otherwise
     */
    suspend fun isDebugModeEnabled(): Boolean
    
    /**
     * Sets the debug mode state.
     * @param enabled True to enable debug mode, false to disable
     */
    suspend fun setDebugModeEnabled(enabled: Boolean)
    
    /**
     * Checks if data logging is enabled.
     * @return True if data logging is enabled, false otherwise
     */
    suspend fun isDataLoggingEnabled(): Boolean
    
    /**
     * Sets the data logging state.
     * @param enabled True to enable data logging, false to disable
     */
    suspend fun setDataLoggingEnabled(enabled: Boolean)
    
    /**
     * Gets the sensor fusion weight.
     * @return The sensor fusion weight
     */
    suspend fun getSensorFusionWeight(): Float
    
    /**
     * Sets the sensor fusion weight.
     * @param weight The weight to set
     */
    suspend fun setSensorFusionWeight(weight: Float)
    
    /**
     * Gets the fusion method.
     * @return The fusion method
     */
    suspend fun getFusionMethod(): FusionMethod
    
    /**
     * Sets the fusion method.
     * @param method The fusion method to set
     */
    suspend fun setFusionMethod(method: FusionMethod)
    
    /**
     * Gets the sensor monitoring delay in milliseconds.
     * @return The sensor monitoring delay
     */
    suspend fun getSensorMonitoringDelay(): Long
    
    /**
     * Sets the sensor monitoring delay in milliseconds.
     * @param delayMs The sensor monitoring delay to set
     */
    suspend fun setSensorMonitoringDelay(delayMs: Long)
    
    /**
     * Gets the accelerometer monitoring delay in milliseconds.
     * @return The accelerometer monitoring delay
     */
    suspend fun getAccelerometerDelay(): Long
    
    /**
     * Sets the accelerometer monitoring delay in milliseconds.
     * @param delayMs The accelerometer monitoring delay to set
     */
    suspend fun setAccelerometerDelay(delayMs: Long)
    
    /**
     * Gets the gyroscope monitoring delay in milliseconds.
     * @return The gyroscope monitoring delay
     */
    suspend fun getGyroscopeDelay(): Long
    
    /**
     * Sets the gyroscope monitoring delay in milliseconds.
     * @param delayMs The gyroscope monitoring delay to set
     */
    suspend fun setGyroscopeDelay(delayMs: Long)
    
    /**
     * Gets the magnetometer monitoring delay in milliseconds.
     * @return The magnetometer monitoring delay
     */
    suspend fun getMagnetometerDelay(): Long
    
    /**
     * Sets the magnetometer monitoring delay in milliseconds.
     * @param delayMs The magnetometer monitoring delay to set
     */
    suspend fun setMagnetometerDelay(delayMs: Long)
    
    /**
     * Gets the linear acceleration monitoring delay in milliseconds.
     * @return The linear acceleration monitoring delay
     */
    suspend fun getLinearAccelerationDelay(): Long
    
    /**
     * Sets the linear acceleration monitoring delay in milliseconds.
     * @param delayMs The linear acceleration monitoring delay to set
     */
    suspend fun setLinearAccelerationDelay(delayMs: Long)
    
    /**
     * Gets the gravity monitoring delay in milliseconds.
     * @return The gravity monitoring delay
     */
    suspend fun getGravityDelay(): Long
    
    /**
     * Sets the gravity monitoring delay in milliseconds.
     * @param delayMs The gravity monitoring delay to set
     */
    suspend fun setGravityDelay(delayMs: Long)
    
    /**
     * Checks if low-power mode is enabled.
     * @return True if low-power mode is enabled, false otherwise
     */
    suspend fun isLowPowerModeEnabled(): Boolean
    
    /**
     * Sets the low-power mode state.
     * @param enabled True to enable low-power mode, false to disable
     */
    suspend fun setLowPowerModeEnabled(enabled: Boolean)
    
    /**
     * Gets the battery threshold percentage at which low-power mode is automatically activated.
     * @return The battery threshold percentage (0-100)
     */
    suspend fun getLowPowerModeThreshold(): Int
    
    /**
     * Sets the battery threshold percentage at which low-power mode is automatically activated.
     * @param threshold The battery threshold percentage (0-100)
     */
    suspend fun setLowPowerModeThreshold(threshold: Int)
    
    /**
     * Gets the sensor sampling rate reduction factor for low-power mode.
     * Higher values mean more aggressive reduction (e.g., 2.0 = half the normal rate).
     * @return The sampling rate reduction factor
     */
    suspend fun getSamplingRateReductionFactor(): Float
    
    /**
     * Sets the sensor sampling rate reduction factor for low-power mode.
     * Higher values mean more aggressive reduction (e.g., 2.0 = half the normal rate).
     * @param factor The sampling rate reduction factor
     */
    suspend fun setSamplingRateReductionFactor(factor: Float)
    
    /**
     * Resets all settings to their default values.
     */
    suspend fun resetToDefaults()
    
    /**
     * Exports all settings to a JSON file.
     * @param file The file to export settings to
     * @return True if export was successful, false otherwise
     */
    suspend fun exportSettings(file: File): Boolean
    
    /**
     * Imports settings from a JSON file.
     * @param file The file to import settings from
     * @return True if import was successful, false otherwise
     */
    suspend fun importSettings(file: File): Boolean

    /**
     * Checks if BLE scanning is enabled by user.
     */
    suspend fun isBleEnabled(): Boolean

    /**
     * Enables or disables BLE scanning.
     */
    suspend fun setBleEnabled(enabled: Boolean)

    /**
     * Checks if Wi‑Fi scanning is enabled by user.
     */
    suspend fun isWifiEnabled(): Boolean

    /**
     * Enables or disables Wi‑Fi scanning.
     */
    suspend fun setWifiEnabled(enabled: Boolean)

    /**
     * Gets the set of selected Wi‑Fi BSSIDs to use.
     */
    suspend fun getSelectedWifiBssids(): Set<String>

    /**
     * Sets the selected Wi‑Fi BSSIDs to use.
     */
    suspend fun setSelectedWifiBssids(bssids: Set<String>)

    /**
     * Gets the stored app language code (e.g., "system", "en", "ja").
     */
    suspend fun getAppLanguage(): String

    /**
     * Persists the app language code (e.g., "system", "en", "ja").
     */
    suspend fun setAppLanguage(languageCode: String)

    /** 初期位置の確定モード（AUTO/MANUAL/TEMPORARY） */
    suspend fun getInitialFixMode(): com.example.myapplication.domain.model.InitialFixMode
    suspend fun setInitialFixMode(mode: com.example.myapplication.domain.model.InitialFixMode)
}