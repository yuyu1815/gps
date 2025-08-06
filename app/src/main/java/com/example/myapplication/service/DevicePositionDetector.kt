package com.example.myapplication.service

import com.example.myapplication.domain.model.SensorData
import com.example.myapplication.domain.model.Vector3D
import timber.log.Timber
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Detects the position in which the device is being carried (e.g., in-hand, in-pocket, in-bag).
 * This information is used to adjust sensor readings and improve positioning accuracy.
 */
class DevicePositionDetector {

    // Carrying position states
    enum class CarryingPosition {
        HAND,       // Device held in hand (relatively stable orientation)
        POCKET,     // Device in pocket (vertical orientation, more movement)
        BAG,        // Device in bag (random orientation, dampened movement)
        STATIONARY, // Device not moving (on table, etc.)
        UNKNOWN     // Cannot determine position
    }

    // Current detected position
    private var currentPosition = CarryingPosition.UNKNOWN
    private var positionConfidence = 0.0f
    private var consecutiveDetections = 0
    private val minConsecutiveDetections = 5

    // Buffers for sensor data analysis
    private val accelBuffer = mutableListOf<Vector3D>()
    private val gyroBuffer = mutableListOf<Vector3D>()
    private val gravityBuffer = mutableListOf<Vector3D>()
    private val maxBufferSize = 50

    // Detection thresholds
    private val stationaryAccelThreshold = 0.2f
    private val stationaryGyroThreshold = 0.1f
    private val handAccelVarianceThreshold = 1.5f
    private val pocketAccelVarianceThreshold = 4.0f
    private val bagAccelVarianceThreshold = 2.0f
    private val handGravityAngleThreshold = 30.0f
    private val pocketGravityAngleThreshold = 20.0f

    /**
     * Updates the sensor data buffers with new readings.
     */
    private fun updateBuffers(sensorData: SensorData.Combined) {
        // Add new data to buffers
        accelBuffer.add(sensorData.linearAcceleration)
        gyroBuffer.add(sensorData.gyroscope)
        gravityBuffer.add(sensorData.gravity)

        // Maintain buffer size
        if (accelBuffer.size > maxBufferSize) {
            accelBuffer.removeAt(0)
        }
        if (gyroBuffer.size > maxBufferSize) {
            gyroBuffer.removeAt(0)
        }
        if (gravityBuffer.size > maxBufferSize) {
            gravityBuffer.removeAt(0)
        }
    }

    /**
     * Detects the carrying position based on sensor data patterns.
     * 
     * @param sensorData The latest sensor data
     * @return The detected carrying position
     */
    fun detectPosition(sensorData: SensorData.Combined): CarryingPosition {
        // Update data buffers
        updateBuffers(sensorData)

        // Need enough data for reliable detection
        if (accelBuffer.size < 20 || gyroBuffer.size < 20 || gravityBuffer.size < 20) {
            return CarryingPosition.UNKNOWN
        }

        // Calculate metrics for detection
        val isStationary = isDeviceStationary()
        val accelVariance = calculateAccelerationVariance()
        val gravityStability = calculateGravityStability()
        val gravityOrientation = calculateGravityOrientation()

        // Detect position based on sensor patterns
        val detectedPosition = when {
            // Stationary: very little movement
            isStationary -> {
                CarryingPosition.STATIONARY
            }
            
            // In hand: moderate movement, stable gravity direction
            accelVariance < handAccelVarianceThreshold && 
            gravityStability > 0.8f && 
            gravityOrientation < handGravityAngleThreshold -> {
                CarryingPosition.HAND
            }
            
            // In pocket: high movement, vertical orientation
            accelVariance > pocketAccelVarianceThreshold && 
            gravityOrientation < pocketGravityAngleThreshold -> {
                CarryingPosition.POCKET
            }
            
            // In bag: moderate movement, unstable orientation
            accelVariance > bagAccelVarianceThreshold && 
            gravityStability < 0.6f -> {
                CarryingPosition.BAG
            }
            
            // Default to unknown if no clear pattern
            else -> {
                CarryingPosition.UNKNOWN
            }
        }

        // Update confidence and consecutive detections
        if (detectedPosition == currentPosition) {
            consecutiveDetections++
            positionConfidence = minOf(1.0f, positionConfidence + 0.1f)
        } else {
            consecutiveDetections = 1
            positionConfidence = 0.3f
        }

        // Only change position if we have enough consecutive detections
        if (consecutiveDetections >= minConsecutiveDetections || positionConfidence > 0.7f) {
            if (currentPosition != detectedPosition) {
                Timber.d("Device carrying position changed: $currentPosition -> $detectedPosition (confidence: $positionConfidence)")
                currentPosition = detectedPosition
            }
        }

        return currentPosition
    }

    /**
     * Checks if the device is stationary based on acceleration and gyroscope data.
     */
    private fun isDeviceStationary(): Boolean {
        // Calculate average magnitudes
        val avgAccelMagnitude = accelBuffer.map { it.magnitude() }.average().toFloat()
        val avgGyroMagnitude = gyroBuffer.map { it.magnitude() }.average().toFloat()

        return avgAccelMagnitude < stationaryAccelThreshold && 
               avgGyroMagnitude < stationaryGyroThreshold
    }

    /**
     * Calculates the variance of acceleration magnitude.
     */
    private fun calculateAccelerationVariance(): Float {
        val magnitudes = accelBuffer.map { it.magnitude() }
        val mean = magnitudes.average()
        val variance = magnitudes.map { (it - mean) * (it - mean) }.average()
        return variance.toFloat()
    }

    /**
     * Calculates the stability of gravity direction.
     * Returns a value between 0 (unstable) and 1 (stable).
     */
    private fun calculateGravityStability(): Float {
        if (gravityBuffer.size < 2) return 1.0f

        // Calculate average direction
        val avgDirection = Vector3D(
            x = gravityBuffer.map { it.x }.average().toFloat(),
            y = gravityBuffer.map { it.y }.average().toFloat(),
            z = gravityBuffer.map { it.z }.average().toFloat()
        ).normalize()

        // Calculate average deviation from this direction
        val avgDeviation = gravityBuffer.map { 
            val normalized = it.normalize()
            val dotProduct = normalized.dot(avgDirection)
            // Dot product of unit vectors gives cosine of angle between them
            // 1 means same direction, -1 means opposite direction
            1.0f - dotProduct // Convert to deviation (0 = same direction, 2 = opposite)
        }.average().toFloat()

        // Convert to stability score (0 to 1)
        return 1.0f - (avgDeviation / 2.0f)
    }

    /**
     * Calculates the angle between gravity and vertical axis.
     * Returns angle in degrees.
     */
    private fun calculateGravityOrientation(): Float {
        if (gravityBuffer.isEmpty()) return 90.0f

        // Calculate average gravity vector
        val avgGravity = Vector3D(
            x = gravityBuffer.map { it.x }.average().toFloat(),
            y = gravityBuffer.map { it.y }.average().toFloat(),
            z = gravityBuffer.map { it.z }.average().toFloat()
        )

        // Vertical axis is (0, 0, 1) in device coordinates when held upright
        val verticalAxis = Vector3D(0f, 0f, 1f)

        // Calculate angle between gravity and vertical axis
        val dotProduct = avgGravity.normalize().dot(verticalAxis)
        val angleRadians = Math.acos(dotProduct.toDouble().coerceIn(-1.0, 1.0))
        return Math.toDegrees(angleRadians).toFloat()
    }

    /**
     * Gets the orientation correction factors based on the detected carrying position.
     * These factors can be used to adjust sensor readings for more accurate positioning.
     * 
     * @return A triple of (accelFactor, gyroFactor, headingOffset)
     */
    fun getOrientationCorrectionFactors(): Triple<Float, Float, Float> {
        return when (currentPosition) {
            CarryingPosition.HAND -> {
                // Hand position is considered the reference position
                Triple(1.0f, 1.0f, 0.0f)
            }
            CarryingPosition.POCKET -> {
                // In pocket: reduce acceleration sensitivity, increase gyro sensitivity
                Triple(0.8f, 1.2f, 0.0f)
            }
            CarryingPosition.BAG -> {
                // In bag: reduce both acceleration and gyro sensitivity
                Triple(0.7f, 0.8f, 0.0f)
            }
            CarryingPosition.STATIONARY -> {
                // Stationary: high confidence in sensors but ignore small movements
                Triple(0.5f, 0.5f, 0.0f)
            }
            CarryingPosition.UNKNOWN -> {
                // Unknown: use default factors
                Triple(1.0f, 1.0f, 0.0f)
            }
        }
    }

    /**
     * Gets the current carrying position.
     */
    fun getCurrentPosition(): CarryingPosition {
        return currentPosition
    }

    /**
     * Gets the confidence level in the current position detection.
     * Returns a value between 0 (no confidence) and 1 (high confidence).
     */
    fun getPositionConfidence(): Float {
        return positionConfidence
    }

    /**
     * Resets the detector state.
     */
    fun reset() {
        currentPosition = CarryingPosition.UNKNOWN
        positionConfidence = 0.0f
        consecutiveDetections = 0
        accelBuffer.clear()
        gyroBuffer.clear()
        gravityBuffer.clear()
    }
}