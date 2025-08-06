package com.example.myapplication.presentation.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.domain.model.Beacon
import com.example.myapplication.domain.model.PositionSource
import com.example.myapplication.domain.model.SensorData
import com.example.myapplication.domain.model.UserPosition
import com.example.myapplication.service.LogFileManager
import com.example.myapplication.service.LogReplayService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File

/**
 * ViewModel for managing log replay functionality.
 * 
 * This ViewModel handles the UI state for replaying log files and visualizing the results.
 */
class LogReplayViewModel(application: Application) : AndroidViewModel(application) {
    
    private val logReplayService = LogReplayService(application)
    private val logFileManager = LogFileManager(application)
    
    // Replay state
    val isReplaying: StateFlow<Boolean> = logReplayService.isReplaying
    val replayProgress: StateFlow<Float> = logReplayService.replayProgress
    val replaySpeed: StateFlow<Float> = logReplayService.replaySpeed
    val currentLogFile: StateFlow<File?> = logReplayService.currentLogFile
    
    // Replay data
    val sensorData: StateFlow<SensorData?> = logReplayService.sensorData
    val beaconData: StateFlow<List<Beacon>> = logReplayService.beaconData
    
    // UI state
    private val _selectedLogFile = MutableStateFlow<LogFileManager.LogFileInfo?>(null)
    val selectedLogFile: StateFlow<LogFileManager.LogFileInfo?> = _selectedLogFile
    
    var replaySpeedOptions by mutableStateOf(listOf(0.5f, 1.0f, 2.0f, 5.0f))
        private set
    
    var errorMessage by mutableStateOf<String?>(null)
        private set
    
    // Calculated position based on replayed data
    private val _calculatedPosition = MutableStateFlow<UserPosition?>(null)
    val calculatedPosition: StateFlow<UserPosition?> = _calculatedPosition
    
    init {
        // Observe replayed data to calculate position
        viewModelScope.launch {
            logReplayService.beaconData.collect { beacons ->
                if (beacons.isNotEmpty()) {
                    // In a real implementation, this would use the positioning algorithms
                    // to calculate a position based on the beacon data
                    calculatePositionFromBeacons(beacons)
                }
            }
        }
    }
    
    /**
     * Selects a log file for replay.
     * 
     * @param logFile The log file to select
     */
    fun selectLogFile(logFile: LogFileManager.LogFileInfo?) {
        _selectedLogFile.value = logFile
    }
    
    /**
     * Starts replaying the selected log file.
     */
    fun startReplay() {
        val logFile = _selectedLogFile.value?.file ?: return
        
        viewModelScope.launch {
            try {
                logReplayService.startReplay(logFile, viewModelScope)
            } catch (e: Exception) {
                Timber.e(e, "Error starting replay: ${logFile.name}")
                errorMessage = "Failed to start replay: ${e.message}"
            }
        }
    }
    
    /**
     * Stops the current replay.
     */
    fun stopReplay() {
        logReplayService.stopReplay()
    }
    
    /**
     * Sets the replay speed.
     * 
     * @param speed The replay speed (1.0 = normal, 2.0 = double speed, etc.)
     */
    fun setReplaySpeed(speed: Float) {
        logReplayService.setReplaySpeed(speed)
    }
    
    /**
     * Clears the error message.
     */
    fun clearErrorMessage() {
        errorMessage = null
    }
    
    /**
     * Calculates a user position from beacon data.
     * 
     * This is a simplified implementation that would be replaced with actual
     * positioning algorithms in a real implementation.
     * 
     * @param beacons The list of beacons to use for position calculation
     */
    private fun calculatePositionFromBeacons(beacons: List<Beacon>) {
        // This is a placeholder implementation
        // In a real app, this would use triangulation or other positioning algorithms
        
        // For now, just calculate a simple weighted average of beacon positions
        // based on signal strength (closer beacons have more weight)
        if (beacons.isEmpty()) {
            _calculatedPosition.value = null
            return
        }
        
        var totalWeight = 0f
        var weightedX = 0f
        var weightedY = 0f
        
        beacons.forEach { beacon ->
            // Convert RSSI to a weight (stronger signal = higher weight)
            // RSSI is typically negative, so we use abs() and invert the relationship
            val rssiAbs = Math.abs(beacon.lastRssi).toFloat()
            val weight = if (rssiAbs > 0) 1f / rssiAbs else 0f
            
            weightedX += beacon.x * weight
            weightedY += beacon.y * weight
            totalWeight += weight
        }
        
        if (totalWeight > 0) {
            val x = weightedX / totalWeight
            val y = weightedY / totalWeight
            
            // Create a user position with medium confidence
            _calculatedPosition.value = UserPosition(
                x = x,
                y = y,
                accuracy = 2.0f, // Estimated accuracy of 2 meters
                confidence = 0.5f,
                timestamp = System.currentTimeMillis(),
                source = PositionSource.BLE
            )
        } else {
            _calculatedPosition.value = null
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        stopReplay()
    }
}