package com.example.myapplication.service

import android.content.Context
import android.os.BatteryManager
import android.content.Intent
import android.content.IntentFilter
import android.os.Process
import android.os.SystemClock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.BufferedReader
import java.io.FileReader
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Collects and provides performance metrics for the application.
 * 
 * This class is responsible for tracking various performance metrics such as:
 * - Frame rate (UI rendering performance)
 * - Battery impact (battery drain rate)
 * - CPU usage
 * - Memory usage
 * - Positioning update frequency
 * - Positioning accuracy
 */
class PerformanceMetricsCollector(
    private val context: Context
) {
    // Data class to hold all performance metrics
    data class PerformanceMetrics(
        val frameRate: Float = 0f,                // Frames per second
        val batteryDrainRate: Float = 0f,         // % per hour
        val cpuUsage: Float = 0f,                 // % of CPU used by app
        val memoryUsage: Long = 0L,               // Bytes
        val positionUpdateFrequency: Float = 0f,  // Updates per second
        val positioningLatency: Long = 0L,        // Milliseconds
        val scanFrequency: Float = 0f,            // Scans per minute
        val activeBeaconCount: Int = 0,           // Number of active beacons
        val lastUpdateTimestamp: Long = 0L        // System time in milliseconds
    )
    
    // StateFlow to expose the current performance metrics
    private val _metrics = MutableStateFlow(PerformanceMetrics())
    val metrics: StateFlow<PerformanceMetrics> = _metrics.asStateFlow()
    
    // Tracking variables
    private var frameCount = 0
    private var lastFrameCountTime = SystemClock.elapsedRealtime()
    private var lastBatteryLevel = -1
    private var lastBatteryCheckTime = 0L
    private var positionUpdateCount = 0
    private var lastPositionCountTime = SystemClock.elapsedRealtime()
    private var scanCount = 0
    private var lastScanCountTime = SystemClock.elapsedRealtime()
    
    // CPU usage tracking variables
    private var lastCpuTime = 0L
    private var lastCpuCheckTime = SystemClock.elapsedRealtime()
    
    /**
     * Records a new frame being rendered.
     * Call this method from the UI rendering path.
     */
    fun recordFrameRendered() {
        frameCount++
        val now = SystemClock.elapsedRealtime()
        val elapsedMs = now - lastFrameCountTime
        
        // Update FPS every second
        if (elapsedMs >= 1000) {
            val fps = frameCount * 1000f / elapsedMs
            updateMetrics { it.copy(frameRate = fps) }
            
            // Reset counters
            frameCount = 0
            lastFrameCountTime = now
        }
    }
    
    /**
     * Records a position update.
     * Call this method whenever a new position is calculated.
     * 
     * @param latencyMs The time it took to calculate the position in milliseconds
     */
    fun recordPositionUpdate(latencyMs: Long) {
        positionUpdateCount++
        val now = SystemClock.elapsedRealtime()
        val elapsedMs = now - lastPositionCountTime
        
        // Update position metrics every second
        if (elapsedMs >= 1000) {
            val updatesPerSecond = positionUpdateCount * 1000f / elapsedMs
            updateMetrics { 
                it.copy(
                    positionUpdateFrequency = updatesPerSecond,
                    positioningLatency = latencyMs
                )
            }
            
            // Reset counters
            positionUpdateCount = 0
            lastPositionCountTime = now
        }
    }
    
    /**
     * Records a BLE scan.
     * Call this method whenever a BLE scan is performed.
     * 
     * @param activeBeaconCount The number of active beacons detected
     */
    fun recordBleScan(activeBeaconCount: Int) {
        scanCount++
        val now = SystemClock.elapsedRealtime()
        val elapsedMs = now - lastScanCountTime
        
        // Update scan metrics every minute
        if (elapsedMs >= TimeUnit.MINUTES.toMillis(1)) {
            val scansPerMinute = scanCount * 60f / (elapsedMs / 1000f)
            updateMetrics { 
                it.copy(
                    scanFrequency = scansPerMinute,
                    activeBeaconCount = activeBeaconCount
                )
            }
            
            // Reset counters
            scanCount = 0
            lastScanCountTime = now
        }
    }
    
    /**
     * Updates CPU and memory usage metrics.
     * Call this method periodically (e.g., every few seconds).
     */
    fun updateSystemMetrics() {
        // Get memory usage
        val runtime = Runtime.getRuntime()
        val usedMemory = runtime.totalMemory() - runtime.freeMemory()
        
        // Calculate CPU usage
        val cpuUsage = calculateCpuUsage()
        
        updateMetrics {
            it.copy(
                cpuUsage = cpuUsage,
                memoryUsage = usedMemory
            )
        }
    }
    
    /**
     * Calculates the CPU usage of the application.
     * 
     * @return CPU usage as a percentage (0-100)
     */
    private fun calculateCpuUsage(): Float {
        val pid = Process.myPid()
        var cpuUsage = 0f
        
        try {
            // Read process CPU time
            val statPath = "/proc/$pid/stat"
            val statReader = BufferedReader(FileReader(statPath))
            val statLine = statReader.readLine()
            statReader.close()
            
            // Parse the stat line to get utime and stime
            // Format: pid (comm) state ppid ... utime stime ...
            val statParts = statLine.split(" ")
            if (statParts.size >= 15) {
                // utime is at index 13, stime at index 14
                val utime = statParts[13].toLong()
                val stime = statParts[14].toLong()
                val totalCpuTime = utime + stime
                
                val now = SystemClock.elapsedRealtime()
                val elapsedTime = now - lastCpuCheckTime
                
                if (lastCpuTime > 0 && elapsedTime > 0) {
                    val cpuTimeDelta = totalCpuTime - lastCpuTime
                    
                    // Calculate CPU usage as percentage
                    // This is an approximation as we're not accounting for the number of cores
                    cpuUsage = (cpuTimeDelta * 100f) / elapsedTime
                }
                
                lastCpuTime = totalCpuTime
                lastCpuCheckTime = now
            }
        } catch (e: IOException) {
            // Log error or handle exception
            // For now, just return 0
        }
        
        return cpuUsage
    }
    
    /**
     * Updates battery metrics.
     * Call this method periodically (e.g., every minute).
     */
    fun updateBatteryMetrics() {
        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        batteryIntent?.let {
            val level = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = it.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            
            if (level != -1 && scale != -1) {
                val batteryPct = level * 100f / scale
                val now = System.currentTimeMillis()
                
                if (lastBatteryLevel != -1 && lastBatteryCheckTime != 0L) {
                    val elapsedHours = (now - lastBatteryCheckTime) / (1000f * 60f * 60f)
                    if (elapsedHours > 0) {
                        val levelDrop = lastBatteryLevel - batteryPct
                        val drainPerHour = if (levelDrop > 0) levelDrop / elapsedHours else 0f
                        
                        updateMetrics { it.copy(batteryDrainRate = drainPerHour) }
                    }
                }
                
                lastBatteryLevel = batteryPct.toInt()
                lastBatteryCheckTime = now
            }
        }
    }
    
    /**
     * Helper method to update the metrics StateFlow.
     */
    private fun updateMetrics(update: (PerformanceMetrics) -> PerformanceMetrics) {
        val currentMetrics = _metrics.value
        _metrics.value = update(currentMetrics).copy(lastUpdateTimestamp = System.currentTimeMillis())
    }
    
    /**
     * Resets all metrics to their default values.
     */
    fun resetMetrics() {
        _metrics.value = PerformanceMetrics(lastUpdateTimestamp = System.currentTimeMillis())
        frameCount = 0
        lastFrameCountTime = SystemClock.elapsedRealtime()
        lastBatteryLevel = -1
        lastBatteryCheckTime = 0L
        positionUpdateCount = 0
        lastPositionCountTime = SystemClock.elapsedRealtime()
        scanCount = 0
        lastScanCountTime = SystemClock.elapsedRealtime()
    }
}