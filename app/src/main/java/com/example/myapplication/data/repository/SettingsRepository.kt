package com.example.myapplication.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.myapplication.domain.model.FusionMethod
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
        private const val KEY_LOW_POWER_MODE_ENABLED = "low_power_mode_enabled"
        private const val KEY_LOW_POWER_MODE_THRESHOLD = "low_power_mode_threshold"
        private const val KEY_SAMPLING_RATE_REDUCTION_FACTOR = "sampling_rate_reduction_factor"
        private const val KEY_BLE_ENABLED = "ble_enabled"
        private const val KEY_WIFI_ENABLED = "wifi_enabled"
        private const val KEY_SELECTED_WIFI_BSSIDS = "selected_wifi_bssids"
        private const val KEY_APP_LANGUAGE = "app_language"
        private const val KEY_SENSOR_MONITORING_DELAY = "sensor_monitoring_delay_ms"
        private const val KEY_ACCELEROMETER_DELAY = "accelerometer_delay_ms"
        private const val KEY_GYROSCOPE_DELAY = "gyroscope_delay_ms"
        private const val KEY_MAGNETOMETER_DELAY = "magnetometer_delay_ms"
        private const val KEY_LINEAR_ACCELERATION_DELAY = "linear_acceleration_delay_ms"
        private const val KEY_GRAVITY_DELAY = "gravity_delay_ms"
        private const val KEY_INITIAL_FIX_MODE = "initial_fix_mode"
        
        // Default values
        private const val DEFAULT_BLE_SCAN_INTERVAL = 1000L
        private const val DEFAULT_ENVIRONMENTAL_FACTOR = 2.0f
        private const val DEFAULT_STEP_LENGTH = 75.0f
        private const val DEFAULT_BEACON_STALENESS_TIMEOUT = 5000L
        private const val DEFAULT_DEBUG_MODE_ENABLED = false
        private const val DEFAULT_DATA_LOGGING_ENABLED = false
        private const val DEFAULT_SENSOR_FUSION_WEIGHT = 0.5f
        private val DEFAULT_FUSION_METHOD = FusionMethod.WEIGHTED_AVERAGE.ordinal
        private const val DEFAULT_LOW_POWER_MODE_ENABLED = false
        private const val DEFAULT_LOW_POWER_MODE_THRESHOLD = 20 // 20% battery
        private const val DEFAULT_SAMPLING_RATE_REDUCTION_FACTOR = 2.0f // Half the normal rate
        private const val DEFAULT_BLE_ENABLED = true
        private const val DEFAULT_WIFI_ENABLED = false
        private const val DEFAULT_APP_LANGUAGE = "system"
        private const val DEFAULT_SENSOR_MONITORING_DELAY = 100L // 100ms default
        private const val DEFAULT_ACCELEROMETER_DELAY = 50L // 50ms for accelerometer
        private const val DEFAULT_GYROSCOPE_DELAY = 100L // 100ms for gyroscope
        private const val DEFAULT_MAGNETOMETER_DELAY = 200L // 200ms for magnetometer
        private const val DEFAULT_LINEAR_ACCELERATION_DELAY = 100L // 100ms for linear acceleration
        private const val DEFAULT_GRAVITY_DELAY = 500L // 500ms for gravity
        private const val DEFAULT_INITIAL_FIX_MODE = 0 // AUTO
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
    
    override suspend fun isLowPowerModeEnabled(): Boolean = withContext(Dispatchers.IO) {
        val enabled = sharedPreferences.getBoolean(KEY_LOW_POWER_MODE_ENABLED, DEFAULT_LOW_POWER_MODE_ENABLED)
        Timber.d("Retrieved low-power mode enabled: $enabled")
        return@withContext enabled
    }
    
    override suspend fun setLowPowerModeEnabled(enabled: Boolean) = withContext(Dispatchers.IO) {
        Timber.d("Setting low-power mode enabled to $enabled")
        sharedPreferences.edit().putBoolean(KEY_LOW_POWER_MODE_ENABLED, enabled).apply()
    }
    
    override suspend fun getLowPowerModeThreshold(): Int = withContext(Dispatchers.IO) {
        val threshold = sharedPreferences.getInt(KEY_LOW_POWER_MODE_THRESHOLD, DEFAULT_LOW_POWER_MODE_THRESHOLD)
        Timber.d("Retrieved low-power mode threshold: $threshold%")
        return@withContext threshold
    }
    
    override suspend fun setLowPowerModeThreshold(threshold: Int) = withContext(Dispatchers.IO) {
        // Ensure threshold is between 0 and 100
        val validThreshold = threshold.coerceIn(0, 100)
        Timber.d("Setting low-power mode threshold to $validThreshold%")
        sharedPreferences.edit().putInt(KEY_LOW_POWER_MODE_THRESHOLD, validThreshold).apply()
    }
    
    override suspend fun getSamplingRateReductionFactor(): Float = withContext(Dispatchers.IO) {
        val factor = sharedPreferences.getFloat(KEY_SAMPLING_RATE_REDUCTION_FACTOR, DEFAULT_SAMPLING_RATE_REDUCTION_FACTOR)
        Timber.d("Retrieved sampling rate reduction factor: $factor")
        return@withContext factor
    }
    
    override suspend fun setSamplingRateReductionFactor(factor: Float) = withContext(Dispatchers.IO) {
        // Ensure factor is at least 1.0 (no reduction) and not too aggressive
        val validFactor = factor.coerceIn(1.0f, 10.0f)
        Timber.d("Setting sampling rate reduction factor to $validFactor")
        sharedPreferences.edit().putFloat(KEY_SAMPLING_RATE_REDUCTION_FACTOR, validFactor).apply()
    }
    
    override suspend fun getSensorMonitoringDelay(): Long = withContext(Dispatchers.IO) {
        val delay = sharedPreferences.getLong(KEY_SENSOR_MONITORING_DELAY, DEFAULT_SENSOR_MONITORING_DELAY)
        Timber.d("Retrieved sensor monitoring delay: $delay ms")
        return@withContext delay
    }

    override suspend fun setSensorMonitoringDelay(delayMs: Long) = withContext(Dispatchers.IO) {
        Timber.d("Setting sensor monitoring delay to $delayMs ms")
        sharedPreferences.edit().putLong(KEY_SENSOR_MONITORING_DELAY, delayMs).apply()
    }
    
    override suspend fun getAccelerometerDelay(): Long = withContext(Dispatchers.IO) {
        val delay = sharedPreferences.getLong(KEY_ACCELEROMETER_DELAY, DEFAULT_ACCELEROMETER_DELAY)
        Timber.d("Retrieved accelerometer delay: $delay ms")
        return@withContext delay
    }

    override suspend fun setAccelerometerDelay(delayMs: Long) = withContext(Dispatchers.IO) {
        Timber.d("Setting accelerometer delay to $delayMs ms")
        sharedPreferences.edit().putLong(KEY_ACCELEROMETER_DELAY, delayMs).apply()
    }

    override suspend fun getGyroscopeDelay(): Long = withContext(Dispatchers.IO) {
        val delay = sharedPreferences.getLong(KEY_GYROSCOPE_DELAY, DEFAULT_GYROSCOPE_DELAY)
        Timber.d("Retrieved gyroscope delay: $delay ms")
        return@withContext delay
    }

    override suspend fun setGyroscopeDelay(delayMs: Long) = withContext(Dispatchers.IO) {
        Timber.d("Setting gyroscope delay to $delayMs ms")
        sharedPreferences.edit().putLong(KEY_GYROSCOPE_DELAY, delayMs).apply()
    }

    override suspend fun getMagnetometerDelay(): Long = withContext(Dispatchers.IO) {
        val delay = sharedPreferences.getLong(KEY_MAGNETOMETER_DELAY, DEFAULT_MAGNETOMETER_DELAY)
        Timber.d("Retrieved magnetometer delay: $delay ms")
        return@withContext delay
    }

    override suspend fun setMagnetometerDelay(delayMs: Long) = withContext(Dispatchers.IO) {
        Timber.d("Setting magnetometer delay to $delayMs ms")
        sharedPreferences.edit().putLong(KEY_MAGNETOMETER_DELAY, delayMs).apply()
    }

    override suspend fun getLinearAccelerationDelay(): Long = withContext(Dispatchers.IO) {
        val delay = sharedPreferences.getLong(KEY_LINEAR_ACCELERATION_DELAY, DEFAULT_LINEAR_ACCELERATION_DELAY)
        Timber.d("Retrieved linear acceleration delay: $delay ms")
        return@withContext delay
    }

    override suspend fun setLinearAccelerationDelay(delayMs: Long) = withContext(Dispatchers.IO) {
        Timber.d("Setting linear acceleration delay to $delayMs ms")
        sharedPreferences.edit().putLong(KEY_LINEAR_ACCELERATION_DELAY, delayMs).apply()
    }

    override suspend fun getGravityDelay(): Long = withContext(Dispatchers.IO) {
        val delay = sharedPreferences.getLong(KEY_GRAVITY_DELAY, DEFAULT_GRAVITY_DELAY)
        Timber.d("Retrieved gravity delay: $delay ms")
        return@withContext delay
    }

    override suspend fun setGravityDelay(delayMs: Long) = withContext(Dispatchers.IO) {
        Timber.d("Setting gravity delay to $delayMs ms")
        sharedPreferences.edit().putLong(KEY_GRAVITY_DELAY, delayMs).apply()
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
                putBoolean(KEY_LOW_POWER_MODE_ENABLED, DEFAULT_LOW_POWER_MODE_ENABLED)
                putInt(KEY_LOW_POWER_MODE_THRESHOLD, DEFAULT_LOW_POWER_MODE_THRESHOLD)
                putFloat(KEY_SAMPLING_RATE_REDUCTION_FACTOR, DEFAULT_SAMPLING_RATE_REDUCTION_FACTOR)
                putBoolean(KEY_BLE_ENABLED, DEFAULT_BLE_ENABLED)
                putBoolean(KEY_WIFI_ENABLED, DEFAULT_WIFI_ENABLED)
                remove(KEY_SELECTED_WIFI_BSSIDS)
                putString(KEY_APP_LANGUAGE, DEFAULT_APP_LANGUAGE)
                putLong(KEY_SENSOR_MONITORING_DELAY, DEFAULT_SENSOR_MONITORING_DELAY)
                putLong(KEY_ACCELEROMETER_DELAY, DEFAULT_ACCELEROMETER_DELAY)
                putLong(KEY_GYROSCOPE_DELAY, DEFAULT_GYROSCOPE_DELAY)
                putLong(KEY_MAGNETOMETER_DELAY, DEFAULT_MAGNETOMETER_DELAY)
                putLong(KEY_LINEAR_ACCELERATION_DELAY, DEFAULT_LINEAR_ACCELERATION_DELAY)
                putLong(KEY_GRAVITY_DELAY, DEFAULT_GRAVITY_DELAY)
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
                put(KEY_LOW_POWER_MODE_ENABLED, isLowPowerModeEnabled())
                put(KEY_LOW_POWER_MODE_THRESHOLD, getLowPowerModeThreshold())
                put(KEY_SAMPLING_RATE_REDUCTION_FACTOR, getSamplingRateReductionFactor())
                put(KEY_BLE_ENABLED, isBleEnabled())
                put(KEY_WIFI_ENABLED, isWifiEnabled())
                put(KEY_SELECTED_WIFI_BSSIDS, getSelectedWifiBssids().joinToString(","))
                put(KEY_SENSOR_MONITORING_DELAY, getSensorMonitoringDelay())
                put(KEY_ACCELEROMETER_DELAY, getAccelerometerDelay())
                put(KEY_GYROSCOPE_DELAY, getGyroscopeDelay())
                put(KEY_MAGNETOMETER_DELAY, getMagnetometerDelay())
                put(KEY_LINEAR_ACCELERATION_DELAY, getLinearAccelerationDelay())
                put(KEY_GRAVITY_DELAY, getGravityDelay())
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
                // Import low-power mode settings if they exist
                if (jsonObject.has(KEY_LOW_POWER_MODE_ENABLED)) {
                    putBoolean(KEY_LOW_POWER_MODE_ENABLED, jsonObject.getBoolean(KEY_LOW_POWER_MODE_ENABLED))
                }
                if (jsonObject.has(KEY_LOW_POWER_MODE_THRESHOLD)) {
                    val threshold = jsonObject.getInt(KEY_LOW_POWER_MODE_THRESHOLD).coerceIn(0, 100)
                    putInt(KEY_LOW_POWER_MODE_THRESHOLD, threshold)
                }
                if (jsonObject.has(KEY_SAMPLING_RATE_REDUCTION_FACTOR)) {
                    val factor = jsonObject.getDouble(KEY_SAMPLING_RATE_REDUCTION_FACTOR).toFloat().coerceIn(1.0f, 10.0f)
                    putFloat(KEY_SAMPLING_RATE_REDUCTION_FACTOR, factor)
                }
                if (jsonObject.has(KEY_BLE_ENABLED)) {
                    putBoolean(KEY_BLE_ENABLED, jsonObject.getBoolean(KEY_BLE_ENABLED))
                }
                if (jsonObject.has(KEY_WIFI_ENABLED)) {
                    putBoolean(KEY_WIFI_ENABLED, jsonObject.getBoolean(KEY_WIFI_ENABLED))
                }
                if (jsonObject.has(KEY_SELECTED_WIFI_BSSIDS)) {
                    val csv = jsonObject.getString(KEY_SELECTED_WIFI_BSSIDS)
                    putString(KEY_SELECTED_WIFI_BSSIDS, csv)
                }
                if (jsonObject.has(KEY_SENSOR_MONITORING_DELAY)) {
                    val delay = jsonObject.getLong(KEY_SENSOR_MONITORING_DELAY).coerceIn(0L, Long.MAX_VALUE)
                    putLong(KEY_SENSOR_MONITORING_DELAY, delay)
                }
                if (jsonObject.has(KEY_ACCELEROMETER_DELAY)) {
                    val delay = jsonObject.getLong(KEY_ACCELEROMETER_DELAY).coerceIn(0L, Long.MAX_VALUE)
                    putLong(KEY_ACCELEROMETER_DELAY, delay)
                }
                if (jsonObject.has(KEY_GYROSCOPE_DELAY)) {
                    val delay = jsonObject.getLong(KEY_GYROSCOPE_DELAY).coerceIn(0L, Long.MAX_VALUE)
                    putLong(KEY_GYROSCOPE_DELAY, delay)
                }
                if (jsonObject.has(KEY_MAGNETOMETER_DELAY)) {
                    val delay = jsonObject.getLong(KEY_MAGNETOMETER_DELAY).coerceIn(0L, Long.MAX_VALUE)
                    putLong(KEY_MAGNETOMETER_DELAY, delay)
                }
                if (jsonObject.has(KEY_LINEAR_ACCELERATION_DELAY)) {
                    val delay = jsonObject.getLong(KEY_LINEAR_ACCELERATION_DELAY).coerceIn(0L, Long.MAX_VALUE)
                    putLong(KEY_LINEAR_ACCELERATION_DELAY, delay)
                }
                if (jsonObject.has(KEY_GRAVITY_DELAY)) {
                    val delay = jsonObject.getLong(KEY_GRAVITY_DELAY).coerceIn(0L, Long.MAX_VALUE)
                    putLong(KEY_GRAVITY_DELAY, delay)
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

    override suspend fun isBleEnabled(): Boolean = withContext(Dispatchers.IO) {
        sharedPreferences.getBoolean(KEY_BLE_ENABLED, DEFAULT_BLE_ENABLED)
    }

    override suspend fun setBleEnabled(enabled: Boolean) = withContext(Dispatchers.IO) {
        sharedPreferences.edit().putBoolean(KEY_BLE_ENABLED, enabled).apply()
    }

    override suspend fun isWifiEnabled(): Boolean = withContext(Dispatchers.IO) {
        sharedPreferences.getBoolean(KEY_WIFI_ENABLED, DEFAULT_WIFI_ENABLED)
    }

    override suspend fun setWifiEnabled(enabled: Boolean) = withContext(Dispatchers.IO) {
        sharedPreferences.edit().putBoolean(KEY_WIFI_ENABLED, enabled).apply()
    }

    override suspend fun getSelectedWifiBssids(): Set<String> = withContext(Dispatchers.IO) {
        val csv = sharedPreferences.getString(KEY_SELECTED_WIFI_BSSIDS, "") ?: ""
        if (csv.isBlank()) emptySet() else csv.split(',').map { it.trim() }.filter { it.isNotEmpty() }.toSet()
    }

    override suspend fun setSelectedWifiBssids(bssids: Set<String>) = withContext(Dispatchers.IO) {
        sharedPreferences.edit().putString(KEY_SELECTED_WIFI_BSSIDS, bssids.joinToString(",")).apply()
    }

    override suspend fun getAppLanguage(): String = withContext(Dispatchers.IO) {
        sharedPreferences.getString(KEY_APP_LANGUAGE, DEFAULT_APP_LANGUAGE) ?: DEFAULT_APP_LANGUAGE
    }

    override suspend fun setAppLanguage(languageCode: String) = withContext(Dispatchers.IO) {
        sharedPreferences.edit().putString(KEY_APP_LANGUAGE, languageCode).apply()
    }

    override suspend fun getInitialFixMode(): com.example.myapplication.domain.model.InitialFixMode = withContext(Dispatchers.IO) {
        val ordinal = sharedPreferences.getInt(KEY_INITIAL_FIX_MODE, DEFAULT_INITIAL_FIX_MODE)
        val values = com.example.myapplication.domain.model.InitialFixMode.values()
        values.getOrNull(ordinal) ?: com.example.myapplication.domain.model.InitialFixMode.AUTO
    }

    override suspend fun setInitialFixMode(mode: com.example.myapplication.domain.model.InitialFixMode) = withContext(Dispatchers.IO) {
        sharedPreferences.edit().putInt(KEY_INITIAL_FIX_MODE, mode.ordinal).apply()
    }
}