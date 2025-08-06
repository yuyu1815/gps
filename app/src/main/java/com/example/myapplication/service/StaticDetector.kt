package com.example.myapplication.service

import com.example.myapplication.domain.model.SensorData
import com.example.myapplication.domain.model.Vector3D
import timber.log.Timber
import kotlin.math.abs

/**
 * Detects when the device is in a static state to optimize sensor and BLE scanning frequency.
 * This helps reduce battery consumption when the device is not moving.
 */
class StaticDetector {

    // Static state detection thresholds
    private val accelStaticThreshold = 0.15f
    private val gyroStaticThreshold = 0.08f
    private val minStaticDurationMs = 3000L // 3 seconds of minimal movement to consider static
    private val longStaticDurationMs = 60000L // 1 minute of minimal movement for long static state

    // Detection state
    private var isStatic = false
    private var isLongStatic = false
    private var staticStartTime = 0L
    private var staticConfidence = 0.0f
    private var consecutiveStaticDetections = 0
    private val minConsecutiveDetections = 3

    // Buffers for sensor data analysis
    private val accelBuffer = mutableListOf<Vector3D>()
    private val gyroBuffer = mutableListOf<Vector3D>()
    private val maxBufferSize = 20

    /**
     * Updates the sensor data buffers with new readings.
     */
    private fun updateBuffers(sensorData: SensorData.Combined) {
        // Add new data to buffers
        accelBuffer.add(sensorData.linearAcceleration)
        gyroBuffer.add(sensorData.gyroscope)

        // Maintain buffer size
        if (accelBuffer.size > maxBufferSize) {
            accelBuffer.removeAt(0)
        }
        if (gyroBuffer.size > maxBufferSize) {
            gyroBuffer.removeAt(0)
        }
    }

    /**
     * Detects if the device is in a static state based on sensor data.
     * 
     * @param sensorData The latest sensor data
     * @return True if the device is static, false otherwise
     */
    fun detectStaticState(sensorData: SensorData.Combined): Boolean {
        // Update data buffers
        updateBuffers(sensorData)

        // Need enough data for reliable detection
        if (accelBuffer.size < 5 || gyroBuffer.size < 5) {
            return false
        }

        // Calculate average magnitudes
        val avgAccelMagnitude = accelBuffer.map { it.magnitude() }.average().toFloat()
        val avgGyroMagnitude = gyroBuffer.map { it.magnitude() }.average().toFloat()

        // Check if device is static based on thresholds
        val currentlyStatic = avgAccelMagnitude < accelStaticThreshold && 
                              avgGyroMagnitude < gyroStaticThreshold

        // Update static state tracking
        val currentTime = System.currentTimeMillis()
        
        if (currentlyStatic) {
            if (!isStatic) {
                // Just became static, record start time
                staticStartTime = currentTime
                consecutiveStaticDetections = 1
                staticConfidence = 0.3f
            } else {
                // Continue being static
                consecutiveStaticDetections++
                staticConfidence = minOf(1.0f, staticConfidence + 0.1f)
                
                // Check for long static duration
                val staticDuration = currentTime - staticStartTime
                val wasLongStatic = isLongStatic
                isLongStatic = staticDuration > longStaticDurationMs
                
                // Log when long static state changes
                if (wasLongStatic != isLongStatic && isLongStatic) {
                    Timber.d("Device entered long static state (duration: ${staticDuration/1000}s)")
                }
            }
        } else {
            // Reset static state if movement detected
            if (isStatic && consecutiveStaticDetections > minConsecutiveDetections) {
                val staticDuration = currentTime - staticStartTime
                Timber.d("Device exited static state after ${staticDuration/1000}s")
            }
            
            isStatic = false
            isLongStatic = false
            consecutiveStaticDetections = 0
            staticConfidence = 0.0f
        }

        // Only change state if we have enough consecutive detections
        if (consecutiveStaticDetections >= minConsecutiveDetections || staticConfidence > 0.7f) {
            if (isStatic != currentlyStatic) {
                isStatic = currentlyStatic
                if (isStatic) {
                    Timber.d("Device entered static state (confidence: $staticConfidence)")
                }
            }
        }

        return isStatic
    }

    /**
     * Checks if the device has been static for a long period.
     * This can be used for more aggressive power saving.
     * 
     * @return True if the device has been static for a long period, false otherwise
     */
    fun isLongStaticState(): Boolean {
        return isLongStatic
    }

    /**
     * Gets the confidence level in the current static state detection.
     * Returns a value between 0 (no confidence) and 1 (high confidence).
     */
    fun getStaticConfidence(): Float {
        return staticConfidence
    }

    /**
     * Gets the duration of the current static state in milliseconds.
     */
    fun getStaticDuration(): Long {
        if (!isStatic) return 0L
        return System.currentTimeMillis() - staticStartTime
    }

    /**
     * Resets the detector state.
     */
    fun reset() {
        isStatic = false
        isLongStatic = false
        staticStartTime = 0L
        staticConfidence = 0.0f
        consecutiveStaticDetections = 0
        accelBuffer.clear()
        gyroBuffer.clear()
    }
}