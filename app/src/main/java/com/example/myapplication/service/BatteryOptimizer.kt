package com.example.myapplication.service

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.PowerManager
import timber.log.Timber
import java.util.Calendar
import kotlinx.coroutines.flow.StateFlow

/**
 * Manages battery optimization strategies for BLE scanning and sensor monitoring.
 * Provides dynamic adjustment of scanning parameters based on:
 * - Current battery level
 * - Battery charging state
 * - Time of day (reduces scanning frequency during typical inactive hours)
 * - Usage patterns
 */
class BatteryOptimizer(
    private val context: Context,
    private val lowPowerMode: LowPowerMode? = null
) : LowPowerMode.LowPowerModeListener {

    // Battery state
    private var batteryLevel = 100
    private var isCharging = false
    private var chargeType = ChargingType.NONE
    
    // Battery thresholds
    private var lowBatteryThreshold = 20
    private var criticalBatteryThreshold = 10
    
    // Usage pattern tracking
    private val usagePatterns = UsagePatternTracker()
    
    // Time-based optimization
    private val nightModeStartHour = 23  // 11 PM
    private val nightModeEndHour = 6     // 6 AM
    private var isNightMode = false
    
    // Low-power mode state
    private var isLowPowerModeEnabled = false
    
    // Power save mode detection
    private val powerManager: PowerManager by lazy {
        context.getSystemService(Context.POWER_SERVICE) as PowerManager
    }
    
    init {
        // Register as a listener for low-power mode changes if available
        lowPowerMode?.addListener(this)
        
        // Initialize low-power mode state
        isLowPowerModeEnabled = lowPowerMode?.lowPowerModeEnabled?.value ?: false
    }
    
    /**
     * Implements LowPowerModeListener interface.
     * Called when low-power mode is enabled or disabled.
     * 
     * @param enabled True if low-power mode is enabled, false otherwise
     */
    override fun onLowPowerModeChanged(enabled: Boolean) {
        isLowPowerModeEnabled = enabled
        Timber.d("Low-power mode changed in BatteryOptimizer: enabled=$enabled")
    }
    
    /**
     * Charging types for different optimization strategies
     */
    enum class ChargingType {
        NONE,       // Not charging
        AC,         // AC charger (fast charging)
        USB,        // USB charging (slower)
        WIRELESS    // Wireless charging
    }
    
    /**
     * Optimization levels that can be applied
     */
    enum class OptimizationLevel {
        MAXIMUM_PERFORMANCE,  // Highest accuracy, highest battery usage
        BALANCED,             // Good balance between accuracy and battery life
        BATTERY_SAVING,       // Reduced accuracy to save battery
        CRITICAL              // Minimal functionality to preserve battery
    }
    
    /**
     * Scan interval configuration based on optimization level
     */
    data class ScanConfig(
        val scanPeriodMs: Long,
        val scanIntervalMs: Long,
        val scanMode: Int,
        val description: String
    )
    
    /**
     * Updates battery state information.
     * Should be called periodically to keep battery information current.
     * 
     * @return True if battery state has changed since last update
     */
    fun updateBatteryState(): Boolean {
        val previousLevel = batteryLevel
        val previousCharging = isCharging
        
        val batteryIntent = context.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )
        
        batteryIntent?.let {
            // Get battery level
            val level = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = it.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            
            if (level != -1 && scale != -1) {
                batteryLevel = (level * 100 / scale.toFloat()).toInt()
            }
            
            // Get charging state
            val status = it.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || 
                         status == BatteryManager.BATTERY_STATUS_FULL
            
            // Get charge type
            val chargePlug = it.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
            chargeType = when (chargePlug) {
                BatteryManager.BATTERY_PLUGGED_AC -> ChargingType.AC
                BatteryManager.BATTERY_PLUGGED_USB -> ChargingType.USB
                BatteryManager.BATTERY_PLUGGED_WIRELESS -> ChargingType.WIRELESS
                else -> ChargingType.NONE
            }
        }
        
        // Check if it's night time
        updateTimeBasedMode()
        
        // Update low-power mode based on battery level if available
        lowPowerMode?.updateBasedOnBatteryLevel(batteryLevel, isCharging)
        
        // Log changes
        if (previousLevel != batteryLevel || previousCharging != isCharging) {
            Timber.d("Battery state updated: level=$batteryLevel%, charging=$isCharging, type=$chargeType")
            return true
        }
        
        return false
    }
    
    /**
     * Updates time-based optimization mode.
     */
    private fun updateTimeBasedMode() {
        val calendar = Calendar.getInstance()
        val hourOfDay = calendar.get(Calendar.HOUR_OF_DAY)
        
        val wasNightMode = isNightMode
        isNightMode = hourOfDay >= nightModeStartHour || hourOfDay < nightModeEndHour
        
        if (wasNightMode != isNightMode) {
            Timber.d("Time-based mode changed: nightMode=$isNightMode")
        }
    }
    
    /**
     * Determines the appropriate optimization level based on current device state.
     * 
     * @return The recommended optimization level
     */
    fun getOptimizationLevel(): OptimizationLevel {
        // If low-power mode is enabled, use BATTERY_SAVING or CRITICAL based on battery level
        if (isLowPowerModeEnabled) {
            return if (batteryLevel <= criticalBatteryThreshold) {
                OptimizationLevel.CRITICAL
            } else {
                OptimizationLevel.BATTERY_SAVING
            }
        }
        
        // If charging with AC, we can use maximum performance
        if (isCharging && chargeType == ChargingType.AC) {
            return OptimizationLevel.MAXIMUM_PERFORMANCE
        }
        
        // If in power save mode, use battery saving
        if (powerManager.isPowerSaveMode) {
            return OptimizationLevel.BATTERY_SAVING
        }
        
        // Critical battery level
        if (batteryLevel <= criticalBatteryThreshold) {
            return OptimizationLevel.CRITICAL
        }
        
        // Low battery level
        if (batteryLevel <= lowBatteryThreshold) {
            return OptimizationLevel.BATTERY_SAVING
        }
        
        // Night mode with reduced scanning
        if (isNightMode && !usagePatterns.isActiveTime()) {
            return OptimizationLevel.BATTERY_SAVING
        }
        
        // Default to balanced
        return OptimizationLevel.BALANCED
    }
    
    /**
     * Gets the recommended scan configuration based on the current optimization level
     * and device state.
     * 
     * @param isStatic Whether the device is currently static
     * @param isLongStatic Whether the device has been static for a long period
     * @param activityLevel The current activity level (HIGH, NORMAL, LOW)
     * @param defaultScanMode The default scan mode to use for BALANCED level
     * @param lowPowerScanMode The scan mode to use for BATTERY_SAVING level
     * @param highPrecisionScanMode The scan mode to use for MAXIMUM_PERFORMANCE level
     * @return The recommended scan configuration
     */
    fun getScanConfig(
        isStatic: Boolean,
        isLongStatic: Boolean,
        activityLevel: ActivityLevel,
        defaultScanMode: Int,
        lowPowerScanMode: Int,
        highPrecisionScanMode: Int
    ): ScanConfig {
        // If low-power mode is explicitly enabled, use the low-power scan settings
        if (isLowPowerModeEnabled) {
            return ScanConfig(
                scanPeriodMs = LowPowerMode.LOW_POWER_SCAN_PERIOD_MS,
                scanIntervalMs = LowPowerMode.LOW_POWER_SCAN_INTERVAL_MS,
                scanMode = lowPowerScanMode,
                description = "Low-power mode"
            )
        }
        
        val optimizationLevel = getOptimizationLevel()
        
        return when (optimizationLevel) {
            OptimizationLevel.MAXIMUM_PERFORMANCE -> {
                // High performance mode - frequent scans with high precision
                if (isStatic) {
                    ScanConfig(
                        scanPeriodMs = 4000,
                        scanIntervalMs = 4000,
                        scanMode = defaultScanMode,
                        description = "Maximum performance (static)"
                    )
                } else {
                    ScanConfig(
                        scanPeriodMs = 6000,
                        scanIntervalMs = 2000,
                        scanMode = highPrecisionScanMode,
                        description = "Maximum performance (moving)"
                    )
                }
            }
            
            OptimizationLevel.BALANCED -> {
                // Balanced mode - adjust based on movement and activity
                when {
                    isLongStatic -> {
                        ScanConfig(
                            scanPeriodMs = 3000,
                            scanIntervalMs = 10000,
                            scanMode = lowPowerScanMode,
                            description = "Balanced (long static)"
                        )
                    }
                    isStatic -> {
                        ScanConfig(
                            scanPeriodMs = 4000,
                            scanIntervalMs = 6000,
                            scanMode = defaultScanMode,
                            description = "Balanced (static)"
                        )
                    }
                    activityLevel == ActivityLevel.HIGH -> {
                        ScanConfig(
                            scanPeriodMs = 5000,
                            scanIntervalMs = 3000,
                            scanMode = defaultScanMode,
                            description = "Balanced (high activity)"
                        )
                    }
                    else -> {
                        ScanConfig(
                            scanPeriodMs = 5000,
                            scanIntervalMs = 5000,
                            scanMode = defaultScanMode,
                            description = "Balanced (normal)"
                        )
                    }
                }
            }
            
            OptimizationLevel.BATTERY_SAVING -> {
                // Battery saving mode - less frequent scans
                when {
                    isLongStatic -> {
                        ScanConfig(
                            scanPeriodMs = 3000,
                            scanIntervalMs = 20000,
                            scanMode = lowPowerScanMode,
                            description = "Battery saving (long static)"
                        )
                    }
                    isStatic -> {
                        ScanConfig(
                            scanPeriodMs = 3000,
                            scanIntervalMs = 15000,
                            scanMode = lowPowerScanMode,
                            description = "Battery saving (static)"
                        )
                    }
                    else -> {
                        ScanConfig(
                            scanPeriodMs = 4000,
                            scanIntervalMs = 10000,
                            scanMode = lowPowerScanMode,
                            description = "Battery saving (moving)"
                        )
                    }
                }
            }
            
            OptimizationLevel.CRITICAL -> {
                // Critical battery - minimal scanning
                ScanConfig(
                    scanPeriodMs = 2000,
                    scanIntervalMs = 30000,
                    scanMode = lowPowerScanMode,
                    description = "Critical battery mode"
                )
            }
        }
    }
    
    /**
     * Sets the battery thresholds for optimization levels.
     * 
     * @param lowThreshold The threshold for low battery mode (percentage)
     * @param criticalThreshold The threshold for critical battery mode (percentage)
     */
    fun setBatteryThresholds(lowThreshold: Int, criticalThreshold: Int) {
        lowBatteryThreshold = lowThreshold
        criticalBatteryThreshold = criticalThreshold
        Timber.d("Battery thresholds updated: low=$lowThreshold%, critical=$criticalThreshold%")
    }
    
    /**
     * Records app usage to improve optimization strategies.
     */
    fun recordAppUsage() {
        usagePatterns.recordUsage()
    }
    
    /**
     * Gets the current battery level.
     * 
     * @return Battery level as percentage (0-100)
     */
    fun getBatteryLevel(): Int {
        return batteryLevel
    }
    
    /**
     * Checks if the device is currently charging.
     * 
     * @return True if charging, false otherwise
     */
    fun isCharging(): Boolean {
        return isCharging
    }
    
    /**
     * Gets the current charging type.
     * 
     * @return The charging type (NONE, AC, USB, WIRELESS)
     */
    fun getChargingType(): ChargingType {
        return chargeType
    }
    
    /**
     * Checks if the device is in night mode.
     * 
     * @return True if in night mode, false otherwise
     */
    fun isInNightMode(): Boolean {
        return isNightMode
    }
    
    /**
     * Tracks app usage patterns to optimize scanning during typical usage times.
     */
    private inner class UsagePatternTracker {
        // Track usage by hour of day (0-23)
        private val hourlyUsage = IntArray(24) { 0 }
        
        // Usage threshold to consider an hour as "active"
        private val activeThreshold = 3
        
        /**
         * Records current usage to build usage pattern.
         */
        fun recordUsage() {
            val calendar = Calendar.getInstance()
            val hourOfDay = calendar.get(Calendar.HOUR_OF_DAY)
            
            hourlyUsage[hourOfDay]++
            
            // Log every 10 usages
            if (hourlyUsage[hourOfDay] % 10 == 0) {
                Timber.d("Usage pattern: hour $hourOfDay has ${hourlyUsage[hourOfDay]} recorded usages")
            }
        }
        
        /**
         * Checks if the current time is typically an active usage time.
         * 
         * @return True if current hour is typically active, false otherwise
         */
        fun isActiveTime(): Boolean {
            val calendar = Calendar.getInstance()
            val hourOfDay = calendar.get(Calendar.HOUR_OF_DAY)
            
            return hourlyUsage[hourOfDay] >= activeThreshold
        }
        
        /**
         * Gets the most active hour of the day.
         * 
         * @return Hour with most usage (0-23)
         */
        fun getMostActiveHour(): Int {
            return hourlyUsage.indices.maxByOrNull { hourlyUsage[it] } ?: 12
        }
    }
}