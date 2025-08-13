package com.example.myapplication.service

import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.example.myapplication.data.repository.ISettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

/**
 * Coordinates power-saving features across the application.
 * Manages low-power mode state and notifies components when power mode changes.
 */
class LowPowerMode(
    private val context: Context
) : DefaultLifecycleObserver, KoinComponent {

    private val settingsRepository: ISettingsRepository by inject()
    private val batteryMonitor: BatteryMonitor by inject()
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    
    // Low-power mode state
    private val _isLowPowerModeActive = MutableStateFlow(false)
    val isLowPowerModeActive: StateFlow<Boolean> = _isLowPowerModeActive.asStateFlow()
    
    // Sampling rate reduction factor
    private val _samplingRateReductionFactor = MutableStateFlow(2.0f)
    val samplingRateReductionFactor: StateFlow<Float> = _samplingRateReductionFactor.asStateFlow()
    
    companion object {
        // Constants for low-power scanning
        const val LOW_POWER_SCAN_PERIOD_MS: Long = 1000
        const val LOW_POWER_SCAN_INTERVAL_MS: Long = 5000
    }
    
    // List of components that need to be notified of power mode changes
    private val powerModeListeners = mutableListOf<PowerModeListener>()
    
    init {
        // Register as a battery monitor listener
        batteryMonitor.addPowerModeListener(object : BatteryMonitor.PowerModeListener {
            override fun onPowerModeChanged(lowPowerMode: Boolean, samplingRateReductionFactor: Float) {
                _isLowPowerModeActive.value = lowPowerMode
                _samplingRateReductionFactor.value = samplingRateReductionFactor
                notifyPowerModeChanged()
            }
        })
        
        // Load initial settings
        coroutineScope.launch {
            _isLowPowerModeActive.value = settingsRepository.isLowPowerModeEnabled()
            _samplingRateReductionFactor.value = settingsRepository.getSamplingRateReductionFactor()
            Timber.d("LowPowerMode initialized: active=${_isLowPowerModeActive.value}, " +
                    "reduction factor=${_samplingRateReductionFactor.value}x")
        }
    }
    
    override fun onResume(owner: LifecycleOwner) {
        Timber.d("LowPowerMode: onResume")
        // Refresh settings when resuming
        coroutineScope.launch {
            val enabled = settingsRepository.isLowPowerModeEnabled()
            val factor = settingsRepository.getSamplingRateReductionFactor()
            
            if (enabled != _isLowPowerModeActive.value || factor != _samplingRateReductionFactor.value) {
                _isLowPowerModeActive.value = enabled
                _samplingRateReductionFactor.value = factor
                notifyPowerModeChanged()
            }
        }
    }
    
    /**
     * Enables or disables low-power mode manually.
     * This will override the automatic battery-based activation.
     */
    fun setLowPowerMode(enabled: Boolean) {
        coroutineScope.launch {
            settingsRepository.setLowPowerModeEnabled(enabled)
            _isLowPowerModeActive.value = enabled
            notifyPowerModeChanged()
            Timber.d("Low-power mode ${if (enabled) "enabled" else "disabled"} manually")
        }
    }
    
    /**
     * Updates the sampling rate reduction factor for low-power mode.
     */
    fun setSamplingRateReductionFactor(factor: Float) {
        coroutineScope.launch {
            val validFactor = factor.coerceIn(1.0f, 10.0f)
            settingsRepository.setSamplingRateReductionFactor(validFactor)
            _samplingRateReductionFactor.value = validFactor
            notifyPowerModeChanged()
            Timber.d("Sampling rate reduction factor updated to ${validFactor}x")
        }
    }
    
    /**
     * Registers a component to be notified of power mode changes.
     */
    fun registerPowerModeListener(listener: PowerModeListener) {
        if (!powerModeListeners.contains(listener)) {
            powerModeListeners.add(listener)
            // Notify the new listener of the current state
            listener.onPowerModeChanged(
                _isLowPowerModeActive.value,
                _samplingRateReductionFactor.value
            )
        }
    }
    
    /**
     * Unregisters a component from power mode change notifications.
     */
    fun unregisterPowerModeListener(listener: PowerModeListener) {
        powerModeListeners.remove(listener)
    }
    
    /**
     * Notifies all registered components of a power mode change.
     */
    private fun notifyPowerModeChanged() {
        val isActive = _isLowPowerModeActive.value
        val factor = _samplingRateReductionFactor.value
        
        powerModeListeners.forEach { listener ->
            listener.onPowerModeChanged(isActive, factor)
        }
    }
    
    /**
     * Calculates a reduced interval based on the normal interval and the current reduction factor.
     * Used to reduce sampling rates in low-power mode.
     * 
     * @param normalIntervalMs The normal interval in milliseconds
     * @return The adjusted interval in milliseconds
     */
    fun getAdjustedInterval(normalIntervalMs: Long): Long {
        return if (_isLowPowerModeActive.value) {
            (normalIntervalMs * _samplingRateReductionFactor.value).toLong()
        } else {
            normalIntervalMs
        }
    }
    
    /**
     * Interface for components that need to respond to power mode changes.
     */
    interface PowerModeListener {
        /**
         * Called when the power mode changes.
         * 
         * @param lowPowerMode True if low-power mode is active, false otherwise
         * @param samplingRateReductionFactor The factor by which to reduce sampling rates
         */
        fun onPowerModeChanged(lowPowerMode: Boolean, samplingRateReductionFactor: Float)
    }
    
    /**
     * Interface for components that need to be notified of low power mode changes.
     */
    interface LowPowerModeListener {
        /**
         * Called when low-power mode is enabled or disabled.
         * 
         * @param enabled True if low-power mode is enabled, false otherwise
         */
        fun onLowPowerModeChanged(enabled: Boolean)
    }
    
    // List of low power mode listeners
    private val lowPowerModeListeners = mutableListOf<LowPowerModeListener>()
    
    /**
     * Registers a component to be notified of low power mode changes.
     */
    fun addListener(listener: LowPowerModeListener) {
        if (!lowPowerModeListeners.contains(listener)) {
            lowPowerModeListeners.add(listener)
            // Notify the new listener of the current state
            listener.onLowPowerModeChanged(_isLowPowerModeActive.value)
        }
    }
    
    /**
     * Unregisters a component from low power mode change notifications.
     */
    fun removeListener(listener: LowPowerModeListener) {
        lowPowerModeListeners.remove(listener)
    }
    
    // Low power mode enabled state for UI components
    val lowPowerModeEnabled: StateFlow<Boolean> = _isLowPowerModeActive.asStateFlow()
    
    // Auto-enable on low battery settings
    private val _autoEnableOnLowBattery = MutableStateFlow(true)
    val autoEnableOnLowBattery: StateFlow<Boolean> = _autoEnableOnLowBattery.asStateFlow()
    
    // Low battery threshold
    private val _lowBatteryThreshold = MutableStateFlow(20)
    val lowBatteryThreshold: StateFlow<Int> = _lowBatteryThreshold.asStateFlow()
    
    /**
     * Sets whether low power mode should be automatically enabled on low battery.
     */
    fun setAutoEnableOnLowBattery(enabled: Boolean) {
        _autoEnableOnLowBattery.value = enabled
    }
    
    /**
     * Sets the battery threshold for automatic low power mode activation.
     */
    fun setLowBatteryThreshold(threshold: Int) {
        _lowBatteryThreshold.value = threshold.coerceIn(5, 50)
    }
    
    /**
     * Sets the low power mode enabled state and notifies listeners.
     */
    fun setLowPowerModeEnabled(enabled: Boolean) {
        _isLowPowerModeActive.value = enabled
        lowPowerModeListeners.forEach { listener ->
            listener.onLowPowerModeChanged(enabled)
        }
    }
    
    /**
     * Updates low-power mode based on battery level and charging state.
     * Automatically enables low-power mode when battery is below threshold
     * and device is not charging, if auto-enable is turned on.
     * 
     * @param batteryLevel Current battery level (0-100)
     * @param isCharging Whether the device is currently charging
     */
    fun updateBasedOnBatteryLevel(batteryLevel: Int, isCharging: Boolean) {
        if (!_autoEnableOnLowBattery.value) {
            return  // Auto-enable is turned off
        }
        
        val threshold = _lowBatteryThreshold.value
        val shouldEnable = batteryLevel <= threshold && !isCharging
        
        if (shouldEnable != _isLowPowerModeActive.value) {
            Timber.d("Auto-adjusting low power mode: enabled=$shouldEnable (battery=$batteryLevel%, threshold=$threshold%, charging=$isCharging)")
            setLowPowerModeEnabled(shouldEnable)
        }
    }
}