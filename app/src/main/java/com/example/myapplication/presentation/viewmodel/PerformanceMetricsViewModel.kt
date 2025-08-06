package com.example.myapplication.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.service.PerformanceMetricsCollector
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * ViewModel for managing performance metrics collection and exposure to the UI.
 * 
 * This ViewModel is responsible for:
 * - Collecting performance metrics from various components
 * - Exposing the metrics to the UI through StateFlow
 * - Scheduling periodic updates of system metrics
 */
class PerformanceMetricsViewModel : BaseViewModel<PerformanceMetricsViewModel.UiState>(), KoinComponent {
    
    /**
     * UI state for the performance metrics screen.
     * Currently empty as we're using StateFlow directly for metrics.
     */
    data class UiState(
        val dummy: Boolean = true // Placeholder property
    )
    
    private val performanceMetricsCollector: PerformanceMetricsCollector by inject()
    
    /**
     * StateFlow of performance metrics for UI consumption.
     */
    val metrics: StateFlow<PerformanceMetricsCollector.PerformanceMetrics> = 
        performanceMetricsCollector.metrics.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = PerformanceMetricsCollector.PerformanceMetrics()
        )
    
    init {
        // Start periodic updates of system metrics
        startPeriodicMetricsUpdates()
    }
    
    /**
     * Records a frame being rendered.
     * This should be called from the UI rendering path.
     */
    fun recordFrameRendered() {
        performanceMetricsCollector.recordFrameRendered()
    }
    
    /**
     * Records a position update.
     * This should be called whenever a new position is calculated.
     * 
     * @param latencyMs The time it took to calculate the position in milliseconds
     */
    fun recordPositionUpdate(latencyMs: Long) {
        performanceMetricsCollector.recordPositionUpdate(latencyMs)
    }
    
    /**
     * Records a BLE scan.
     * This should be called whenever a BLE scan is performed.
     * 
     * @param activeBeaconCount The number of active beacons detected
     */
    fun recordBleScan(activeBeaconCount: Int) {
        performanceMetricsCollector.recordBleScan(activeBeaconCount)
    }
    
    /**
     * Starts periodic updates of system metrics.
     */
    private fun startPeriodicMetricsUpdates() {
        viewModelScope.launch {
            // Update system metrics every 5 seconds
            kotlinx.coroutines.delay(1000)
            while (true) {
                performanceMetricsCollector.updateSystemMetrics()
                kotlinx.coroutines.delay(5000)
            }
        }
        
        viewModelScope.launch {
            // Update battery metrics every minute
            kotlinx.coroutines.delay(5000)
            while (true) {
                performanceMetricsCollector.updateBatteryMetrics()
                kotlinx.coroutines.delay(60000)
            }
        }
    }
    
    /**
     * Resets all metrics to their default values.
     */
    fun resetMetrics() {
        performanceMetricsCollector.resetMetrics()
    }
    
    // No need to override onCleared() as there's no cleanup required
    // The collector is a singleton managed by Koin
}