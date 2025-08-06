package com.example.myapplication.domain.usecase.pdr

import com.example.myapplication.domain.model.SensorData
import com.example.myapplication.domain.usecase.UseCase
import timber.log.Timber
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Use case for estimating the user's step length dynamically.
 * Implements a model that adjusts step length based on walking frequency and acceleration patterns.
 * Includes walking pattern recognition to better estimate step length for different gait types.
 */
class EstimateStepLengthUseCase : UseCase<EstimateStepLengthUseCase.Params, Float> {
    
    // State variables for step length estimation
    private var lastStepTimestamp = 0L
    private var stepFrequency = 0f
    private var recentStepLengths = mutableListOf<Float>()
    private val maxRecentSteps = 5
    
    // Variables for walking pattern recognition
    private var recentAccelMagnitudes = mutableListOf<Float>()
    private var recentGyroMagnitudes = mutableListOf<Float>()
    private val maxRecentMagnitudes = 20
    private var currentWalkingPattern = WalkingPattern.NORMAL
    private var patternConfidence = 0f
    private var consecutivePatternCount = 0
    
    override suspend fun invoke(params: Params): Float {
        val sensorData = params.sensorData
        val timestamp = sensorData.timestamp
        
        // Calculate step frequency if we have a previous step
        if (lastStepTimestamp > 0 && timestamp > lastStepTimestamp) {
            val timeDelta = (timestamp - lastStepTimestamp) / 1000f // in seconds
            stepFrequency = 1f / timeDelta
        }
        
        lastStepTimestamp = timestamp
        
        // Get acceleration and gyroscope magnitudes for pattern recognition
        val accelMagnitude = sensorData.linearAccelerationMagnitude()
        val gyroMagnitude = sensorData.rotationMagnitude()
        
        // Update recent magnitudes for pattern recognition
        updateRecentMagnitudes(accelMagnitude, gyroMagnitude)
        
        // Detect walking pattern
        detectWalkingPattern()
        
        // Calculate step length using the enhanced model with walking pattern recognition
        val stepLength = calculateStepLength(
            accelMagnitude,
            params.userHeight,
            params.calibrationFactor
        )
        
        // Add to recent step lengths and maintain maximum size
        recentStepLengths.add(stepLength)
        if (recentStepLengths.size > maxRecentSteps) {
            recentStepLengths.removeAt(0)
        }
        
        // Use average of recent step lengths for stability
        val averageStepLength = if (recentStepLengths.isNotEmpty()) {
            recentStepLengths.average().toFloat()
        } else {
            stepLength
        }
        
        // Ensure step length is within reasonable bounds
        val boundedStepLength = boundStepLength(averageStepLength, params.userHeight)
        
        Timber.d("Step length: $boundedStepLength m (freq: $stepFrequency Hz, accel: $accelMagnitude m/s², pattern: $currentWalkingPattern, confidence: $patternConfidence)")
        return boundedStepLength
    }
    
    /**
     * Updates the recent acceleration and gyroscope magnitudes for pattern recognition.
     */
    private fun updateRecentMagnitudes(accelMagnitude: Float, gyroMagnitude: Float) {
        recentAccelMagnitudes.add(accelMagnitude)
        recentGyroMagnitudes.add(gyroMagnitude)
        
        if (recentAccelMagnitudes.size > maxRecentMagnitudes) {
            recentAccelMagnitudes.removeAt(0)
        }
        
        if (recentGyroMagnitudes.size > maxRecentMagnitudes) {
            recentGyroMagnitudes.removeAt(0)
        }
    }
    
    /**
     * Detects the current walking pattern based on acceleration and gyroscope data.
     */
    private fun detectWalkingPattern() {
        if (recentAccelMagnitudes.size < 10 || recentGyroMagnitudes.size < 10) {
            return
        }
        
        // Calculate statistics for pattern recognition
        val accelMean = recentAccelMagnitudes.average().toFloat()
        val accelStdDev = calculateStandardDeviation(recentAccelMagnitudes)
        val gyroMean = recentGyroMagnitudes.average().toFloat()
        val gyroStdDev = calculateStandardDeviation(recentGyroMagnitudes)
        val stepFreqStability = if (stepFrequency > 0) min(1.0f, 1.0f / (stepFrequency * 0.1f)) else 0.5f
        
        // Detect walking pattern based on sensor characteristics
        val previousPattern = currentWalkingPattern
        val newPattern = when {
            // Running: high acceleration, high frequency, high variability
            accelMean > 15f && stepFrequency > 2.5f && accelStdDev > 5f -> {
                WalkingPattern.RUNNING
            }
            // Fast walking: medium-high acceleration, medium-high frequency
            accelMean > 10f && stepFrequency > 2.0f -> {
                WalkingPattern.FAST
            }
            // Slow walking: low acceleration, low frequency, stable pattern
            accelMean < 5f && stepFrequency < 1.5f && accelStdDev < 2f -> {
                WalkingPattern.SLOW
            }
            // Irregular: high variability in acceleration and gyroscope
            accelStdDev > 4f && gyroStdDev > 1.5f -> {
                WalkingPattern.IRREGULAR
            }
            // Normal walking: medium values for all parameters
            else -> {
                WalkingPattern.NORMAL
            }
        }
        
        // Update pattern confidence and count
        if (newPattern == previousPattern) {
            consecutivePatternCount++
            patternConfidence = min(1.0f, patternConfidence + 0.1f)
        } else {
            consecutivePatternCount = 1
            patternConfidence = 0.3f
        }
        
        // Only change pattern if we have enough confidence
        if (patternConfidence > 0.5f || consecutivePatternCount > 5) {
            currentWalkingPattern = newPattern
        }
    }
    
    /**
     * Calculates the standard deviation of a list of float values.
     */
    private fun calculateStandardDeviation(values: List<Float>): Float {
        if (values.isEmpty()) return 0f
        
        val mean = values.average()
        val variance = values.map { (it - mean) * (it - mean) }.average()
        return sqrt(variance.toFloat())
    }
    
    /**
     * Calculates step length using an enhanced model that considers walking pattern.
     */
    private fun calculateStepLength(
        accelMagnitude: Float,
        userHeight: Float,
        calibrationFactor: Float
    ): Float {
        // Base step length as a function of user height (approximately 0.4 * height)
        val baseStepLength = 0.4f * userHeight
        
        // Adjust step length based on acceleration magnitude (step vigor)
        // and step frequency (walking speed)
        val accelFactor = min(1.3f, max(0.7f, sqrt(accelMagnitude) / 3f))
        val freqFactor = if (stepFrequency > 0) {
            min(1.3f, max(0.7f, stepFrequency / 2f))
        } else {
            1.0f
        }
        
        // Apply walking pattern adjustment factor
        val patternFactor = when (currentWalkingPattern) {
            WalkingPattern.RUNNING -> 1.4f
            WalkingPattern.FAST -> 1.2f
            WalkingPattern.NORMAL -> 1.0f
            WalkingPattern.SLOW -> 0.8f
            WalkingPattern.IRREGULAR -> 0.9f
        }
        
        // Apply confidence weighting to pattern factor
        val weightedPatternFactor = 1.0f + (patternFactor - 1.0f) * patternConfidence
        
        return baseStepLength * accelFactor * freqFactor * weightedPatternFactor * calibrationFactor
    }
    
    /**
     * Ensures the step length is within reasonable bounds based on user height and walking pattern.
     */
    private fun boundStepLength(stepLength: Float, userHeight: Float): Float {
        // Adjust bounds based on walking pattern
        val patternMinFactor = when (currentWalkingPattern) {
            WalkingPattern.RUNNING -> 0.5f
            WalkingPattern.FAST -> 0.4f
            WalkingPattern.NORMAL -> 0.3f
            WalkingPattern.SLOW -> 0.2f
            WalkingPattern.IRREGULAR -> 0.25f
        }
        
        val patternMaxFactor = when (currentWalkingPattern) {
            WalkingPattern.RUNNING -> 1.1f
            WalkingPattern.FAST -> 0.9f
            WalkingPattern.NORMAL -> 0.8f
            WalkingPattern.SLOW -> 0.6f
            WalkingPattern.IRREGULAR -> 0.7f
        }
        
        val minStepLength = patternMinFactor * userHeight
        val maxStepLength = patternMaxFactor * userHeight
        return min(maxStepLength, max(minStepLength, stepLength))
    }
    
    /**
     * Resets the step length estimation state.
     */
    fun reset() {
        lastStepTimestamp = 0L
        stepFrequency = 0f
        recentStepLengths.clear()
        recentAccelMagnitudes.clear()
        recentGyroMagnitudes.clear()
        currentWalkingPattern = WalkingPattern.NORMAL
        patternConfidence = 0f
        consecutivePatternCount = 0
    }
    
    /**
     * Enum representing different walking patterns.
     */
    enum class WalkingPattern {
        RUNNING,    // Fast pace with high impact
        FAST,       // Quick walking
        NORMAL,     // Regular walking pace
        SLOW,       // Slow, deliberate steps
        IRREGULAR   // Uneven or inconsistent pattern
    }
    
    /**
     * Parameters for the EstimateStepLengthUseCase.
     *
     * @param sensorData The sensor data to analyze
     * @param userHeight User's height in meters
     * @param calibrationFactor Calibration factor to adjust the step length model (default: 1.0f)
     */
    data class Params(
        val sensorData: SensorData.Combined,
        val userHeight: Float,
        val calibrationFactor: Float = 1.0f
    )
}