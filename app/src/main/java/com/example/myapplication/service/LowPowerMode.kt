package com.example.myapplication.service

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

/**
 * Manages low-power mode for extended usage scenarios.
 * 
 * This class provides a centralized way to control and observe the low-power mode state
 * across the application. When enabled, components should adjust their behavior to
 * minimize battery consumption at the cost of some positioning accuracy.
 */
class LowPowerMode(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "low_power_mode_prefs"
        private const val KEY_LOW_POWER_ENABLED = "low_power_enabled"
        private const val KEY_AUTO_ENABLE_ON_LOW_BATTERY = "auto_enable_on_low_battery"
        private const val KEY_LOW_BATTERY_THRESHOLD = "low_battery_threshold"
        
        // Default values
        private const val DEFAULT_AUTO_ENABLE = true
        private const val DEFAULT_LOW_BATTERY_THRESHOLD = 20 // Percentage
        
        // Scan settings for low-power mode
        const val LOW_POWER_SCAN_PERIOD_MS = 2000L
        const val LOW_POWER_SCAN_INTERVAL_MS = 30000L // 30 seconds between scans
        
        // Sensor settings for low-power mode
        const val LOW_POWER_SENSOR_SAMPLING_PERIOD_MS = 100L // 10Hz instead of typical 50Hz
        const val LOW_POWER_POSITION_UPDATE_INTERVAL_MS = 5000L // 5 seconds between position updates
    }
    
    // Preferences for persisting settings
    private val preferences: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    // State flows for reactive programming
    private val _lowPowerModeEnabled = MutableStateFlow(preferences.getBoolean(KEY_LOW_POWER_ENABLED, false))
    private val _autoEnableOnLowBattery = MutableStateFlow(preferences.getBoolean(KEY_AUTO_ENABLE_ON_LOW_BATTERY, DEFAULT_AUTO_ENABLE))
    private val _lowBatteryThreshold = MutableStateFlow(preferences.getInt(KEY_LOW_BATTERY_THRESHOLD, DEFAULT_LOW_BATTERY_THRESHOLD))
    
    // Public immutable flows for observing state changes
    val lowPowerModeEnabled: StateFlow<Boolean> = _lowPowerModeEnabled.asStateFlow()
    val autoEnableOnLowBattery: StateFlow<Boolean> = _autoEnableOnLowBattery.asStateFlow()
    val lowBatteryThreshold: StateFlow<Int> = _lowBatteryThreshold.asStateFlow()
    
    init {
        Timber.d("LowPowerMode initialized, enabled=${_lowPowerModeEnabled.value}, " +
                "autoEnable=${_autoEnableOnLowBattery.value}, " +
                "threshold=${_lowBatteryThreshold.value}%")
    }
    
    /**
     * Enables or disables low-power mode.
     * 
     * @param enabled True to enable low-power mode, false to disable
     */
    fun setLowPowerModeEnabled(enabled: Boolean) {
        if (_lowPowerModeEnabled.value != enabled) {
            _lowPowerModeEnabled.value = enabled
            preferences.edit().putBoolean(KEY_LOW_POWER_ENABLED, enabled).apply()
            
            Timber.i("Low-power mode ${if (enabled) "enabled" else "disabled"}")
            
            // Notify listeners about the change
            notifyLowPowerModeChanged(enabled)
        }
    }
    
    /**
     * Sets whether low-power mode should be automatically enabled when battery is low.
     * 
     * @param autoEnable True to automatically enable low-power mode on low battery
     */
    fun setAutoEnableOnLowBattery(autoEnable: Boolean) {
        if (_autoEnableOnLowBattery.value != autoEnable) {
            _autoEnableOnLowBattery.value = autoEnable
            preferences.edit().putBoolean(KEY_AUTO_ENABLE_ON_LOW_BATTERY, autoEnable).apply()
            
            Timber.d("Auto-enable low-power mode on low battery: $autoEnable")
        }
    }
    
    /**
     * Sets the battery threshold for automatically enabling low-power mode.
     * 
     * @param threshold Battery percentage threshold (0-100)
     */
    fun setLowBatteryThreshold(threshold: Int) {
        val validThreshold = threshold.coerceIn(5, 50) // Reasonable range
        
        if (_lowBatteryThreshold.value != validThreshold) {
            _lowBatteryThreshold.value = validThreshold
            preferences.edit().putInt(KEY_LOW_BATTERY_THRESHOLD, validThreshold).apply()
            
            Timber.d("Low battery threshold set to $validThreshold%")
        }
    }
    
    /**
     * Checks if low-power mode should be enabled based on current battery level.
     * 
     * @param batteryLevel Current battery level percentage (0-100)
     * @return True if low-power mode should be enabled
     */
    fun shouldEnableLowPowerMode(batteryLevel: Int): Boolean {
        // If already enabled, keep it enabled
        if (_lowPowerModeEnabled.value) {
            return true
        }
        
        // If auto-enable is on and battery is below threshold, enable low-power mode
        return _autoEnableOnLowBattery.value && batteryLevel <= _lowBatteryThreshold.value
    }
    
    /**
     * Updates low-power mode based on current battery level.
     * 
     * @param batteryLevel Current battery level percentage (0-100)
     * @param isCharging Whether the device is currently charging
     * @return True if low-power mode state changed
     */
    fun updateBasedOnBatteryLevel(batteryLevel: Int, isCharging: Boolean): Boolean {
        // If charging, disable low-power mode
        if (isCharging && _lowPowerModeEnabled.value) {
            setLowPowerModeEnabled(false)
            Timber.i("Low-power mode disabled because device is charging")
            return true
        }
        
        // If battery is low and auto-enable is on, enable low-power mode
        if (!isCharging && _autoEnableOnLowBattery.value && 
            batteryLevel <= _lowBatteryThreshold.value && 
            !_lowPowerModeEnabled.value) {
            
            setLowPowerModeEnabled(true)
            Timber.i("Low-power mode automatically enabled due to low battery ($batteryLevel%)")
            return true
        }
        
        return false
    }
    
    // List of listeners to notify when low-power mode changes
    private val listeners = mutableListOf<LowPowerModeListener>()
    
    /**
     * Adds a listener to be notified when low-power mode changes.
     * 
     * @param listener The listener to add
     */
    fun addListener(listener: LowPowerModeListener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener)
            
            // Immediately notify the new listener of the current state
            listener.onLowPowerModeChanged(_lowPowerModeEnabled.value)
        }
    }
    
    /**
     * Removes a previously added listener.
     * 
     * @param listener The listener to remove
     */
    fun removeListener(listener: LowPowerModeListener) {
        listeners.remove(listener)
    }
    
    /**
     * Notifies all listeners about a change in low-power mode.
     * 
     * @param enabled The new low-power mode state
     */
    private fun notifyLowPowerModeChanged(enabled: Boolean) {
        listeners.forEach { it.onLowPowerModeChanged(enabled) }
    }
    
    /**
     * Interface for components that need to be notified of low-power mode changes.
     */
    interface LowPowerModeListener {
        /**
         * Called when low-power mode is enabled or disabled.
         * 
         * @param enabled True if low-power mode is enabled, false otherwise
         */
        fun onLowPowerModeChanged(enabled: Boolean)
    }
}