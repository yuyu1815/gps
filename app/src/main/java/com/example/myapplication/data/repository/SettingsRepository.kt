package com.example.myapplication.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.myapplication.domain.usecase.fusion.FuseSensorDataUseCase.FusionMethod
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import timber.log.Timber
import java.io.File

/**
 * Implementation of [ISettingsRepository] that uses SharedPreferences for storage.
 * This class handles the persistence of application settings.
 */
class SettingsRepository(context: Context) : ISettingsRepository {
    
    companion object {
        private const val PREFS_NAME = "indoor_positioning_settings"
        private const val KEY_BLE_SCAN_INTERVAL = "ble_scan_interval_ms"
        private const val KEY_ENVIRONMENTAL_FACTOR = "environmental_factor"
        private const val KEY_STEP_LENGTH = "step_length_cm"
        private const val KEY_BEACON_STALENESS_TIMEOUT = "beacon_staleness_timeout_ms"
        private const val KEY_DEBUG_MODE_ENABLED = "debug_mode_enabled"
        private const val KEY_DATA_LOGGING_ENABLED = "data_logging_enabled"
        private const val KEY_SENSOR_FUSION_WEIGHT = "sensor_fusion_weight"
        private const val KEY_FUSION_METHOD = "fusion_method"
        
        // Default values
        private const val DEFAULT_BLE_SCAN_INTERVAL = 1000L
        private const val DEFAULT_ENVIRONMENTAL_FACTOR = 2.0f
        private const val DEFAULT_STEP_LENGTH = 75.0f
        private const val DEFAULT_BEACON_STALENESS_TIMEOUT = 5000L
        private const val DEFAULT_DEBUG_MODE_ENABLED = false
        private const val DEFAULT_DATA_LOGGING_ENABLED = false
        private const val DEFAULT_SENSOR_FUSION_WEIGHT = 0.5f
        private const val DEFAULT_FUSION_METHOD = 0 // WEIGHTED_AVERAGE
    }
    
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME, Context.MODE_PRIVATE
    )
    
    override suspend fun getBleScanInterval(): Long = withContext(Dispatchers.IO) {
        val interval = sharedPreferences.getLong(KEY_BLE_SCAN_INTERVAL, DEFAULT_BLE_SCAN_INTERVAL)
        Timber.d("Retrieved BLE scan interval: $interval ms")
        return@withContext interval
    }
    
    override suspend fun setBleScanInterval(intervalMs: Long) = withContext(Dispatchers.IO) {
        Timber.d("Setting BLE scan interval to $intervalMs ms")
        sharedPreferences.edit().putLong(KEY_BLE_SCAN_INTERVAL, intervalMs).apply()
    }
    
    override suspend fun getEnvironmentalFactor(): Float = withContext(Dispatchers.IO) {
        val factor = sharedPreferences.getFloat(KEY_ENVIRONMENTAL_FACTOR, DEFAULT_ENVIRONMENTAL_FACTOR)
        Timber.d("Retrieved environmental factor: $factor")
        return@withContext factor
    }
    
    override suspend fun setEnvironmentalFactor(factor: Float) = withContext(Dispatchers.IO) {
        Timber.d("Setting environmental factor to $factor")
        sharedPreferences.edit().putFloat(KEY_ENVIRONMENTAL_FACTOR, factor).apply()
    }
    
    override suspend fun getStepLength(): Float = withContext(Dispatchers.IO) {
        val length = sharedPreferences.getFloat(KEY_STEP_LENGTH, DEFAULT_STEP_LENGTH)
        Timber.d("Retrieved step length: $length cm")
        return@withContext length
    }
    
    override suspend fun setStepLength(lengthCm: Float) = withContext(Dispatchers.IO) {
        Timber.d("Setting step length to $lengthCm cm")
        sharedPreferences.edit().putFloat(KEY_STEP_LENGTH, lengthCm).apply()
    }
    
    override suspend fun getBeaconStalenessTimeout(): Long = withContext(Dispatchers.IO) {
        val timeout = sharedPreferences.getLong(KEY_BEACON_STALENESS_TIMEOUT, DEFAULT_BEACON_STALENESS_TIMEOUT)
        Timber.d("Retrieved beacon staleness timeout: $timeout ms")
        return@withContext timeout
    }
    
    override suspend fun setBeaconStalenessTimeout(timeoutMs: Long) = withContext(Dispatchers.IO) {
        Timber.d("Setting beacon staleness timeout to $timeoutMs ms")
        sharedPreferences.edit().putLong(KEY_BEACON_STALENESS_TIMEOUT, timeoutMs).apply()
    }
    
    override suspend fun isDebugModeEnabled(): Boolean = withContext(Dispatchers.IO) {
        val enabled = sharedPreferences.getBoolean(KEY_DEBUG_MODE_ENABLED, DEFAULT_DEBUG_MODE_ENABLED)
        Timber.d("Retrieved debug mode enabled: $enabled")
        return@withContext enabled
    }
    
    override suspend fun setDebugModeEnabled(enabled: Boolean) = withContext(Dispatchers.IO) {
        Timber.d("Setting debug mode enabled to $enabled")
        sharedPreferences.edit().putBoolean(KEY_DEBUG_MODE_ENABLED, enabled).apply()
    }
    
    override suspend fun isDataLoggingEnabled(): Boolean = withContext(Dispatchers.IO) {
        val enabled = sharedPreferences.getBoolean(KEY_DATA_LOGGING_ENABLED, DEFAULT_DATA_LOGGING_ENABLED)
        Timber.d("Retrieved data logging enabled: $enabled")
        return@withContext enabled
    }
    
    override suspend fun setDataLoggingEnabled(enabled: Boolean) = withContext(Dispatchers.IO) {
        Timber.d("Setting data logging enabled to $enabled")
        sharedPreferences.edit().putBoolean(KEY_DATA_LOGGING_ENABLED, enabled).apply()
    }
    
    override suspend fun getSensorFusionWeight(): Float = withContext(Dispatchers.IO) {
        val weight = sharedPreferences.getFloat(KEY_SENSOR_FUSION_WEIGHT, DEFAULT_SENSOR_FUSION_WEIGHT)
        Timber.d("Retrieved sensor fusion weight: $weight")
        return@withContext weight
    }
    
    override suspend fun setSensorFusionWeight(weight: Float) = withContext(Dispatchers.IO) {
        Timber.d("Setting sensor fusion weight to $weight")
        sharedPreferences.edit().putFloat(KEY_SENSOR_FUSION_WEIGHT, weight).apply()
    }
    
    override suspend fun getFusionMethod(): FusionMethod = withContext(Dispatchers.IO) {
        val methodOrdinal = sharedPreferences.getInt(KEY_FUSION_METHOD, DEFAULT_FUSION_METHOD)
        val method = FusionMethod.values()[methodOrdinal]
        Timber.d("Retrieved fusion method: $method")
        return@withContext method
    }
    
    override suspend fun setFusionMethod(method: FusionMethod) = withContext(Dispatchers.IO) {
        Timber.d("Setting fusion method to $method")
        sharedPreferences.edit().putInt(KEY_FUSION_METHOD, method.ordinal).apply()
    }
    
    override suspend fun resetToDefaults() {
        withContext(Dispatchers.IO) {
            Timber.d("Resetting all settings to defaults")
            sharedPreferences.edit().apply {
                putLong(KEY_BLE_SCAN_INTERVAL, DEFAULT_BLE_SCAN_INTERVAL)
                putFloat(KEY_ENVIRONMENTAL_FACTOR, DEFAULT_ENVIRONMENTAL_FACTOR)
                putFloat(KEY_STEP_LENGTH, DEFAULT_STEP_LENGTH)
                putLong(KEY_BEACON_STALENESS_TIMEOUT, DEFAULT_BEACON_STALENESS_TIMEOUT)
                putBoolean(KEY_DEBUG_MODE_ENABLED, DEFAULT_DEBUG_MODE_ENABLED)
                putBoolean(KEY_DATA_LOGGING_ENABLED, DEFAULT_DATA_LOGGING_ENABLED)
                putFloat(KEY_SENSOR_FUSION_WEIGHT, DEFAULT_SENSOR_FUSION_WEIGHT)
                putInt(KEY_FUSION_METHOD, DEFAULT_FUSION_METHOD)
                apply()
            }
        }
    }
    
    override suspend fun exportSettings(file: File): Boolean = withContext(Dispatchers.IO) {
        try {
            Timber.d("Exporting settings to file: ${file.absolutePath}")
            
            val jsonObject = JSONObject().apply {
                put(KEY_BLE_SCAN_INTERVAL, getBleScanInterval())
                put(KEY_ENVIRONMENTAL_FACTOR, getEnvironmentalFactor())
                put(KEY_STEP_LENGTH, getStepLength())
                put(KEY_BEACON_STALENESS_TIMEOUT, getBeaconStalenessTimeout())
                put(KEY_DEBUG_MODE_ENABLED, isDebugModeEnabled())
                put(KEY_DATA_LOGGING_ENABLED, isDataLoggingEnabled())
                put(KEY_SENSOR_FUSION_WEIGHT, getSensorFusionWeight())
                put(KEY_FUSION_METHOD, getFusionMethod().ordinal)
            }
            
            file.writeText(jsonObject.toString(4)) // Pretty print with 4-space indentation
            Timber.d("Settings exported successfully")
            return@withContext true
        } catch (e: Exception) {
            Timber.e(e, "Failed to export settings")
            return@withContext false
        }
    }
    
    override suspend fun importSettings(file: File): Boolean = withContext(Dispatchers.IO) {
        try {
            Timber.d("Importing settings from file: ${file.absolutePath}")
            
            if (!file.exists() || !file.isFile) {
                Timber.e("Import file does not exist or is not a file")
                return@withContext false
            }
            
            val jsonString = file.readText()
            val jsonObject = JSONObject(jsonString)
            
            sharedPreferences.edit().apply {
                // Only update settings that exist in the JSON file
                if (jsonObject.has(KEY_BLE_SCAN_INTERVAL)) {
                    putLong(KEY_BLE_SCAN_INTERVAL, jsonObject.getLong(KEY_BLE_SCAN_INTERVAL))
                }
                if (jsonObject.has(KEY_ENVIRONMENTAL_FACTOR)) {
                    putFloat(KEY_ENVIRONMENTAL_FACTOR, jsonObject.getDouble(KEY_ENVIRONMENTAL_FACTOR).toFloat())
                }
                if (jsonObject.has(KEY_STEP_LENGTH)) {
                    putFloat(KEY_STEP_LENGTH, jsonObject.getDouble(KEY_STEP_LENGTH).toFloat())
                }
                if (jsonObject.has(KEY_BEACON_STALENESS_TIMEOUT)) {
                    putLong(KEY_BEACON_STALENESS_TIMEOUT, jsonObject.getLong(KEY_BEACON_STALENESS_TIMEOUT))
                }
                if (jsonObject.has(KEY_DEBUG_MODE_ENABLED)) {
                    putBoolean(KEY_DEBUG_MODE_ENABLED, jsonObject.getBoolean(KEY_DEBUG_MODE_ENABLED))
                }
                if (jsonObject.has(KEY_DATA_LOGGING_ENABLED)) {
                    putBoolean(KEY_DATA_LOGGING_ENABLED, jsonObject.getBoolean(KEY_DATA_LOGGING_ENABLED))
                }
                if (jsonObject.has(KEY_SENSOR_FUSION_WEIGHT)) {
                    putFloat(KEY_SENSOR_FUSION_WEIGHT, jsonObject.getDouble(KEY_SENSOR_FUSION_WEIGHT).toFloat())
                }
                if (jsonObject.has(KEY_FUSION_METHOD)) {
                    val methodOrdinal = jsonObject.getInt(KEY_FUSION_METHOD)
                    if (methodOrdinal in FusionMethod.values().indices) {
                        putInt(KEY_FUSION_METHOD, methodOrdinal)
                    }
                }
                apply()
            }
            
            Timber.d("Settings imported successfully")
            return@withContext true
        } catch (e: Exception) {
            Timber.e(e, "Failed to import settings: ${e.message}")
            return@withContext false
        }
    }
}