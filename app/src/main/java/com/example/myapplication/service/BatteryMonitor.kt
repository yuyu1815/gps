package com.example.myapplication.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
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
 * Monitors battery level and manages power-saving features.
 * Automatically enables low-power mode when battery level drops below the configured threshold.
 */
class BatteryMonitor(
    private val context: Context
) : DefaultLifecycleObserver, KoinComponent {

    private val settingsRepository: ISettingsRepository by inject()
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    
    // Battery state
    private val _batteryLevel = MutableStateFlow(100)
    val batteryLevel: StateFlow<Int> = _batteryLevel.asStateFlow()
    
    private val _isCharging = MutableStateFlow(false)
    val isCharging: StateFlow<Boolean> = _isCharging.asStateFlow()
    
    private val _lowPowerModeActive = MutableStateFlow(false)
    val lowPowerModeActive: StateFlow<Boolean> = _lowPowerModeActive.asStateFlow()
    
    // List of power mode listeners
    private val powerModeListeners = mutableListOf<PowerModeListener>()
    
    // Battery receiver
    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_BATTERY_CHANGED) {
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                val batteryPct = if (level != -1 && scale != -1) {
                    (level * 100 / scale.toFloat()).toInt()
                } else {
                    -1
                }
                
                val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || 
                                 status == BatteryManager.BATTERY_STATUS_FULL
                
                _batteryLevel.value = batteryPct
                _isCharging.value = isCharging
                
                Timber.d("Battery level: $batteryPct%, Charging: $isCharging")
                
                // Check if we need to enable low-power mode
                checkLowPowerMode(batteryPct, isCharging)
            }
        }
    }
    
    init {
        // Initial battery check
        val batteryStatus = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        if (batteryStatus != null) {
            val level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val batteryPct = if (level != -1 && scale != -1) {
                (level * 100 / scale.toFloat()).toInt()
            } else {
                100
            }
            
            val status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || 
                             status == BatteryManager.BATTERY_STATUS_FULL
            
            _batteryLevel.value = batteryPct
            _isCharging.value = isCharging
            
            // Initial low-power mode check
            checkLowPowerMode(batteryPct, isCharging)
        }
    }
    
    override fun onResume(owner: LifecycleOwner) {
        Timber.d("BatteryMonitor: onResume")
        context.registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    }
    
    override fun onPause(owner: LifecycleOwner) {
        Timber.d("BatteryMonitor: onPause")
        try {
            context.unregisterReceiver(batteryReceiver)
        } catch (e: Exception) {
            Timber.e(e, "Error unregistering battery receiver")
        }
    }
    
    /**
     * Checks if low-power mode should be enabled based on battery level and charging state.
     */
    private fun checkLowPowerMode(batteryLevel: Int, isCharging: Boolean) {
        coroutineScope.launch {
            val userEnabled = settingsRepository.isLowPowerModeEnabled()
            val threshold = settingsRepository.getLowPowerModeThreshold()
            
            // Determine if low-power mode should be active
            val shouldBeActive = userEnabled || (!isCharging && batteryLevel <= threshold)
            
            // Only update if there's a change
            if (shouldBeActive != _lowPowerModeActive.value) {
                _lowPowerModeActive.value = shouldBeActive
                
                // Notify listeners
                val samplingRateReductionFactor = settingsRepository.getSamplingRateReductionFactor()
                notifyPowerModeChanged(shouldBeActive, samplingRateReductionFactor)
                
                Timber.d("Low-power mode ${if (shouldBeActive) "activated" else "deactivated"}")
                if (shouldBeActive && !userEnabled) {
                    Timber.d("Auto-activated due to battery level ($batteryLevel%) below threshold ($threshold%)")
                }
            }
        }
    }
    
    /**
     * Manually sets the low-power mode state.
     * This will override the automatic battery-based activation until the next battery check.
     */
    fun setLowPowerMode(enabled: Boolean) {
        coroutineScope.launch {
            settingsRepository.setLowPowerModeEnabled(enabled)
            checkLowPowerMode(_batteryLevel.value, _isCharging.value)
        }
    }
    
    /**
     * Adds a listener for power mode changes.
     */
    fun addPowerModeListener(listener: PowerModeListener) {
        if (!powerModeListeners.contains(listener)) {
            powerModeListeners.add(listener)
            
            // Notify the new listener of the current state
            coroutineScope.launch {
                val samplingRateReductionFactor = settingsRepository.getSamplingRateReductionFactor()
                listener.onPowerModeChanged(_lowPowerModeActive.value, samplingRateReductionFactor)
            }
        }
    }
    
    /**
     * Removes a power mode listener.
     */
    fun removePowerModeListener(listener: PowerModeListener) {
        powerModeListeners.remove(listener)
    }
    
    /**
     * Notifies all listeners of a power mode change.
     */
    private fun notifyPowerModeChanged(lowPowerMode: Boolean, samplingRateReductionFactor: Float) {
        powerModeListeners.forEach { listener ->
            listener.onPowerModeChanged(lowPowerMode, samplingRateReductionFactor)
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
         * @param samplingRateReductionFactor The factor by which to reduce sampling rates (e.g., 2.0 = half rate)
         */
        fun onPowerModeChanged(lowPowerMode: Boolean, samplingRateReductionFactor: Float)
    }
}