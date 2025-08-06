package com.example.myapplication.domain.model

import java.util.UUID
import kotlin.collections.ArrayDeque
import kotlin.math.pow
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Enum representing the health status of a beacon.
 */
enum class BeaconHealthStatus {
    /**
     * Beacon is functioning normally with good signal quality and battery level.
     */
    GOOD,
    
    /**
     * Beacon has some issues (e.g., fluctuating signal, medium battery level) but is still usable.
     */
    WARNING,
    
    /**
     * Beacon has critical issues (e.g., very weak signal, low battery) and may not be reliable.
     */
    CRITICAL,
    
    /**
     * Beacon health status is unknown or not yet determined.
     */
    UNKNOWN
}

/**
 * Represents a BLE beacon in the indoor positioning system.
 * Contains information about the beacon's identity, position, and signal characteristics.
 */
data class Beacon(
    /**
     * Unique identifier for the beacon.
     */
    val id: String = UUID.randomUUID().toString(),
    
    /**
     * MAC address of the beacon.
     */
    val macAddress: String,
    
    /**
     * X coordinate of the beacon's position in the indoor map (in meters).
     */
    val x: Float,
    
    /**
     * Y coordinate of the beacon's position in the indoor map (in meters).
     */
    val y: Float,
    
    /**
     * Transmit power of the beacon at 1 meter distance (in dBm).
     * This is used for distance calculation from RSSI.
     */
    val txPower: Int,
    
    /**
     * Name of the beacon (optional).
     */
    val name: String = "",
    
    /**
     * Last time the beacon was detected (timestamp in milliseconds).
     */
    var lastSeenTimestamp: Long = 0,
    
    /**
     * Last measured RSSI value (in dBm).
     */
    var lastRssi: Int = 0,
    
    /**
     * Filtered RSSI value using moving average (in dBm).
     */
    var filteredRssi: Int = 0,
    
    /**
     * Calculated distance to the beacon (in meters).
     */
    var estimatedDistance: Float = 0f,
    
    /**
     * Confidence level of the distance estimation (0.0 to 1.0).
     * Higher values indicate more reliable distance estimates.
     */
    var distanceConfidence: Float = 0f,
    
    /**
     * Flag indicating if the beacon is considered stale (not seen recently).
     */
    var isStale: Boolean = false,
    
    /**
     * Battery level of the beacon (0-100%).
     * -1 indicates that battery level is unknown or not supported.
     */
    var batteryLevel: Int = -1,
    
    /**
     * Health status of the beacon.
     * Represents the overall health considering signal stability, battery level, etc.
     */
    var healthStatus: BeaconHealthStatus = BeaconHealthStatus.UNKNOWN,
    
    /**
     * Count of consecutive failed detection attempts.
     * Used to track intermittent connectivity issues.
     */
    var failedDetectionCount: Int = 0,
    
    /**
     * Flag indicating if there's an alert that requires attention.
     */
    var hasAlert: Boolean = false,
    
    /**
     * Description of the current alert, if any.
     */
    var alertMessage: String = ""
) {
    /**
     * Queue to store recent RSSI values for moving average calculation.
     * Default window size is 5 samples.
     */
    private val rssiHistory = ArrayDeque<Int>(RSSI_WINDOW_SIZE)
    
    companion object {
        /**
         * Default window size for RSSI moving average calculation.
         */
        const val RSSI_WINDOW_SIZE = 5
        
        /**
         * Default staleness timeout in milliseconds (5 seconds).
         */
        const val DEFAULT_STALENESS_TIMEOUT_MS = 5000L
    }
    /**
     * Updates the beacon with new RSSI data and calculates the filtered RSSI using moving average.
     * Also resets failed detection count and evaluates health status.
     * 
     * @param rssi The new RSSI value to add
     * @param timestamp The timestamp when the RSSI was measured
     */
    fun updateRssi(rssi: Int, timestamp: Long) {
        this.lastRssi = rssi
        this.lastSeenTimestamp = timestamp
        this.isStale = false
        
        // Reset failed detection count since we successfully detected the beacon
        resetFailedDetectionCount()
        
        // Add the new RSSI value to history
        rssiHistory.addLast(rssi)
        
        // Keep only the most recent RSSI_WINDOW_SIZE values
        if (rssiHistory.size > RSSI_WINDOW_SIZE) {
            rssiHistory.removeFirst()
        }
        
        // Calculate moving average if we have at least one value
        if (rssiHistory.isNotEmpty()) {
            filteredRssi = rssiHistory.sum() / rssiHistory.size
        } else {
            filteredRssi = rssi
        }
        
        // Evaluate health status with the updated RSSI data
        evaluateHealthStatus()
    }
    
    /**
     * Checks if the beacon is stale based on the current time and a timeout threshold.
     */
    fun checkStaleness(currentTime: Long, timeoutMs: Long): Boolean {
        isStale = (currentTime - lastSeenTimestamp) > timeoutMs
        return isStale
    }
    
    /**
     * Calculates the estimated distance based on filtered RSSI and txPower using the log-distance path loss model.
     * d = 10^((TxPower - RSSI) / (10 * n))
     * where n is the path loss exponent (typically 2-4 for indoor environments)
     * 
     * Also calculates a confidence value for the distance estimation based on:
     * 1. RSSI variance (lower variance = higher confidence)
     * 2. Signal strength (stronger signal = higher confidence)
     * 3. Time since last update (more recent = higher confidence)
     * 
     * @param environmentalFactor The environmental factor (path loss exponent) to use in the calculation
     * @param useFilteredRssi Whether to use the filtered RSSI (true) or the last raw RSSI (false)
     * @param currentTime Current time in milliseconds, used for confidence calculation
     * @return The estimated distance in meters
     */
    fun calculateDistance(
        environmentalFactor: Float = 2.0f, 
        useFilteredRssi: Boolean = true,
        currentTime: Long = System.currentTimeMillis()
    ): Float {
        // Use either filtered or raw RSSI based on parameter
        val rssiValue = if (useFilteredRssi && rssiHistory.isNotEmpty()) filteredRssi else lastRssi
        
        if (rssiValue == 0) {
            distanceConfidence = 0f
            return 0f
        }
        
        val ratio = (txPower - rssiValue) / (10 * environmentalFactor)
        estimatedDistance = Math.pow(10.0, ratio.toDouble()).toFloat()
        
        // Calculate confidence based on multiple factors
        calculateConfidence(rssiValue, currentTime)
        
        return estimatedDistance
    }
    
    /**
     * Calculates the confidence level for the distance estimation.
     * 
     * @param rssiValue The RSSI value used for distance calculation
     * @param currentTime Current time in milliseconds
     */
    private fun calculateConfidence(rssiValue: Int, currentTime: Long) {
        // Factor 1: RSSI variance - lower variance means higher confidence
        val rssiVariance = if (rssiHistory.size >= 2) {
            // Calculate variance if we have at least 2 samples
            val mean = rssiHistory.average()
            rssiHistory.sumOf { (it - mean).pow(2).toDouble() } / rssiHistory.size
        } else {
            // Maximum variance if we don't have enough samples
            100.0
        }
        
        // Normalize variance to 0-1 range (inverted, so lower variance = higher confidence)
        val varianceConfidence = max(0f, min(1f, (1 - (rssiVariance / 100.0)).toFloat()))
        
        // Factor 2: Signal strength - stronger signal means higher confidence
        // RSSI typically ranges from -100 (weak) to -40 (strong)
        val signalStrength = min(1f, max(0f, (abs(rssiValue) - 40) / 60f))
        val signalConfidence = 1f - signalStrength  // Invert so stronger signal = higher confidence
        
        // Factor 3: Recency - more recent updates have higher confidence
        val timeSinceLastUpdate = currentTime - lastSeenTimestamp
        val recencyConfidence = if (timeSinceLastUpdate >= DEFAULT_STALENESS_TIMEOUT_MS) {
            0f
        } else {
            1f - (timeSinceLastUpdate.toFloat() / DEFAULT_STALENESS_TIMEOUT_MS)
        }
        
        // Combine factors with different weights
        // Variance is most important, followed by signal strength, then recency
        distanceConfidence = (0.5f * varianceConfidence) + 
                             (0.3f * signalConfidence) + 
                             (0.2f * recencyConfidence)
    }
    
    /**
     * Updates the beacon's battery level.
     * 
     * @param level Battery level in percentage (0-100)
     */
    fun updateBatteryLevel(level: Int) {
        batteryLevel = level.coerceIn(0, 100)
        evaluateHealthStatus()
    }
    
    /**
     * Records a failed detection attempt and updates health status.
     */
    fun recordFailedDetection() {
        failedDetectionCount++
        evaluateHealthStatus()
    }
    
    /**
     * Resets the failed detection counter.
     * Should be called when the beacon is successfully detected.
     */
    fun resetFailedDetectionCount() {
        if (failedDetectionCount > 0) {
            failedDetectionCount = 0
            evaluateHealthStatus()
        }
    }
    
    /**
     * Evaluates the overall health status of the beacon based on:
     * 1. Signal quality (RSSI strength and stability)
     * 2. Battery level (if available)
     * 3. Detection reliability (failed detection count)
     * 4. Staleness
     * 
     * Also generates alert messages for critical issues.
     */
    fun evaluateHealthStatus() {
        // Start with no alerts
        hasAlert = false
        alertMessage = ""
        
        // Check for staleness first
        if (isStale) {
            healthStatus = BeaconHealthStatus.CRITICAL
            hasAlert = true
            alertMessage = "Beacon not detected for ${DEFAULT_STALENESS_TIMEOUT_MS / 1000} seconds"
            return
        }
        
        // Check for consecutive failed detections
        if (failedDetectionCount >= 3) {
            healthStatus = BeaconHealthStatus.WARNING
            if (failedDetectionCount >= 5) {
                healthStatus = BeaconHealthStatus.CRITICAL
                hasAlert = true
                alertMessage = "Beacon connection unstable (${failedDetectionCount} failed detections)"
            }
            return
        }
        
        // Evaluate signal quality
        val signalQuality = when {
            filteredRssi > -70 -> 2 // Good signal
            filteredRssi > -85 -> 1 // Acceptable signal
            else -> 0 // Poor signal
        }
        
        // Evaluate battery level if available
        val batteryScore = when {
            batteryLevel < 0 -> 1 // Unknown battery
            batteryLevel < 20 -> 0 // Critical battery
            batteryLevel < 50 -> 1 // Low battery
            else -> 2 // Good battery
        }
        
        // Evaluate distance confidence
        val confidenceScore = when {
            distanceConfidence > 0.7f -> 2 // High confidence
            distanceConfidence > 0.4f -> 1 // Medium confidence
            else -> 0 // Low confidence
        }
        
        // Calculate overall health score
        val overallScore = signalQuality + batteryScore + confidenceScore
        
        // Determine health status based on overall score
        healthStatus = when {
            overallScore >= 5 -> BeaconHealthStatus.GOOD
            overallScore >= 3 -> BeaconHealthStatus.WARNING
            else -> BeaconHealthStatus.CRITICAL
        }
        
        // Generate alerts for critical issues
        if (healthStatus == BeaconHealthStatus.CRITICAL) {
            hasAlert = true
            alertMessage = when {
                filteredRssi < -85 -> "Very weak signal: ${filteredRssi} dBm"
                batteryLevel in 0..19 -> "Critical battery level: ${batteryLevel}%"
                distanceConfidence < 0.3f -> "Unreliable positioning data"
                else -> "Multiple issues detected"
            }
        }
    }
}