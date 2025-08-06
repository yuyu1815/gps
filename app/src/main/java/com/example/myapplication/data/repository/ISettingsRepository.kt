package com.example.myapplication.data.repository

import com.example.myapplication.domain.usecase.fusion.FuseSensorDataUseCase.FusionMethod
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
     * @param method The method to set
     */
    suspend fun setFusionMethod(method: FusionMethod)
    
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
}