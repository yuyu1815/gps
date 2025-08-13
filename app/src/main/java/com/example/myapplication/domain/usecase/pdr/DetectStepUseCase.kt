package com.example.myapplication.domain.usecase.pdr

import com.example.myapplication.domain.model.SensorData
import com.example.myapplication.domain.usecase.UseCase
import timber.log.Timber
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Use case for detecting steps using the peak-valley pattern in accelerometer data.
 * 
 * This implementation uses a peak-valley detection algorithm that identifies steps
 * by looking for significant peaks and valleys in the acceleration magnitude.
 * 
 * The algorithm works as follows:
 * 1. Calculate the magnitude of the acceleration vector
 * 2. Apply a low-pass filter to smooth the signal
 * 3. Detect peaks and valleys in the filtered signal
 * 4. Identify steps based on peak-valley patterns that meet certain criteria
 */
class DetectStepUseCase : UseCase<DetectStepUseCase.Params, DetectStepUseCase.Result> {
    
    // Import constants from StepDetectionUtils
    private val NANOS_TO_MILLIS = StepDetectionUtils.NANOS_TO_MILLIS
    
    // State variables for step detection
    private var lastAccelMagnitude = 0f
    private var filteredAccelMagnitude = 0f
    private var lastPeakValue = 0f
    private var lastValleyValue = 0f
    private var lastPeakTime = 0L
    private var lastValleyTime = 0L
    private var lastStepTime = 0L
    private var inPeak = false
    private var stepCount = 0
    
    override suspend fun invoke(params: Params): Result {
        // Extract accelerometer data
        val accelData = params.accelerometerData
        val timestamp = accelData.timestamp
        
        // Calculate acceleration magnitude
        val accelMagnitude = accelData.magnitude()
        
        // Apply low-pass filter to smooth the signal
        filteredAccelMagnitude = applyLowPassFilter(
            currentValue = accelMagnitude, 
            lastFilteredValue = filteredAccelMagnitude, 
            alpha = params.filterAlpha
        )
        
        // Detect step based on peak-valley pattern
        val stepDetected = detectStep(
            accelMagnitude = filteredAccelMagnitude,
            timestamp = timestamp,
            peakThreshold = params.peakThreshold,
            valleyThreshold = params.valleyThreshold,
            minPeakValleyHeight = params.minPeakValleyHeight,
            minStepInterval = params.minStepInterval,
            maxStepInterval = params.maxStepInterval
        )
        
        // Update last acceleration magnitude
        lastAccelMagnitude = accelMagnitude
        
        // If a step is detected, increment step count
        if (stepDetected) {
            stepCount++
            Timber.d("Step detected! Total steps: $stepCount")
        }
        
        return Result(
            stepDetected = stepDetected,
            stepCount = stepCount,
            filteredAcceleration = filteredAccelMagnitude,
            timestamp = timestamp
        )
    }
    
    /**
     * Applies a low-pass filter to smooth the acceleration signal.
     * 
     * @param currentValue Current acceleration magnitude
     * @param lastFilteredValue Last filtered acceleration magnitude
     * @param alpha Filter coefficient (0-1, lower values = more smoothing)
     * @return Filtered acceleration magnitude
     */
    private fun applyLowPassFilter(currentValue: Float, lastFilteredValue: Float, alpha: Float): Float {
        return StepDetectionUtils.applyLowPassFilter(currentValue, lastFilteredValue, alpha)
    }
    
    /**
     * Detects steps based on peak-valley patterns in the acceleration signal.
     * 
     * @param accelMagnitude Current filtered acceleration magnitude
     * @param timestamp Current timestamp
     * @param peakThreshold Threshold for peak detection
     * @param valleyThreshold Threshold for valley detection
     * @param minPeakValleyHeight Minimum height difference between peak and valley
     * @param minStepInterval Minimum time between steps (ms)
     * @param maxStepInterval Maximum time between steps (ms)
     * @return True if a step is detected, false otherwise
     */
    private fun detectStep(
        accelMagnitude: Float,
        timestamp: Long,
        peakThreshold: Float,
        valleyThreshold: Float,
        minPeakValleyHeight: Float,
        minStepInterval: Long,
        maxStepInterval: Long
    ): Boolean {
        var stepDetected = false
        
        // Convert timestamp from nanoseconds to milliseconds for easier comparison
        val timeMs = timestamp / NANOS_TO_MILLIS
        
        // Detect peaks and valleys
        if (isPeakDetected(accelMagnitude, peakThreshold)) {
            // Transition from valley to peak
            inPeak = true
            lastPeakValue = accelMagnitude
            lastPeakTime = timeMs
            
            // Check if we have a valid peak-valley pattern
            if (isValidStepPattern(timeMs, lastPeakValue, lastValleyValue, minPeakValleyHeight, minStepInterval, maxStepInterval)) {
                // Step detected!
                stepDetected = true
                lastStepTime = timeMs
                
                Timber.d("Step detected at $timeMs ms, peak-valley height: ${lastPeakValue - lastValleyValue}")
            }
        } else if (isValleyDetected(accelMagnitude, valleyThreshold)) {
            // Transition from peak to valley
            inPeak = false
            lastValleyValue = accelMagnitude
            lastValleyTime = timeMs
        }
        
        return stepDetected
    }
    
    /**
     * Determines if a peak is detected in the acceleration signal.
     * 
     * @param accelMagnitude Current acceleration magnitude
     * @param peakThreshold Threshold for peak detection
     * @return True if a peak is detected, false otherwise
     */
    private fun isPeakDetected(accelMagnitude: Float, peakThreshold: Float): Boolean {
        return !inPeak && accelMagnitude > peakThreshold && accelMagnitude > lastAccelMagnitude
    }
    
    /**
     * Determines if a valley is detected in the acceleration signal.
     * 
     * @param accelMagnitude Current acceleration magnitude
     * @param valleyThreshold Threshold for valley detection
     * @return True if a valley is detected, false otherwise
     */
    private fun isValleyDetected(accelMagnitude: Float, valleyThreshold: Float): Boolean {
        return inPeak && accelMagnitude < valleyThreshold && accelMagnitude < lastAccelMagnitude
    }
    
    /**
     * Determines if the current peak-valley pattern represents a valid step.
     * 
     * @param currentTime Current time in milliseconds
     * @param peakValue Peak acceleration value
     * @param valleyValue Valley acceleration value
     * @param minPeakValleyHeight Minimum height difference between peak and valley
     * @param minStepInterval Minimum time between steps (ms)
     * @param maxStepInterval Maximum time between steps (ms)
     * @return True if the pattern represents a valid step, false otherwise
     */
    private fun isValidStepPattern(
        currentTime: Long,
        peakValue: Float,
        valleyValue: Float,
        minPeakValleyHeight: Float,
        minStepInterval: Long,
        maxStepInterval: Long
    ): Boolean {
        val peakValleyHeight = peakValue - valleyValue
        val timeSinceLastStep = currentTime - lastStepTime
        
        return valleyValue > 0 && 
               peakValleyHeight >= minPeakValleyHeight &&
               timeSinceLastStep >= minStepInterval &&
               timeSinceLastStep <= maxStepInterval
    }
    
    /**
     * Resets the step detection state.
     */
    fun reset() {
        lastAccelMagnitude = 0f
        filteredAccelMagnitude = 0f
        lastPeakValue = 0f
        lastValleyValue = 0f
        lastPeakTime = 0L
        lastValleyTime = 0L
        lastStepTime = 0L
        inPeak = false
        stepCount = 0
    }
    
    /**
     * Parameters for the DetectStepUseCase.
     *
     * @param accelerometerData Accelerometer data
     * @param filterAlpha Low-pass filter coefficient (0-1)
     * @param peakThreshold Threshold for peak detection
     * @param valleyThreshold Threshold for valley detection
     * @param minPeakValleyHeight Minimum height difference between peak and valley
     * @param minStepInterval Minimum time between steps (ms)
     * @param maxStepInterval Maximum time between steps (ms)
     */
    data class Params(
        val accelerometerData: SensorData.Accelerometer,
        val filterAlpha: Float = StepDetectionUtils.DEFAULT_ACCEL_FILTER_ALPHA,
        val peakThreshold: Float = StepDetectionUtils.DEFAULT_PEAK_THRESHOLD,
        val valleyThreshold: Float = StepDetectionUtils.DEFAULT_VALLEY_THRESHOLD,
        val minPeakValleyHeight: Float = StepDetectionUtils.DEFAULT_MIN_PEAK_VALLEY_HEIGHT,
        val minStepInterval: Long = StepDetectionUtils.DEFAULT_MIN_STEP_INTERVAL,  // 250ms = max 4 steps per second
        val maxStepInterval: Long = StepDetectionUtils.DEFAULT_MAX_STEP_INTERVAL  // 2000ms = min 0.5 steps per second
    )
    
    /**
     * Result of the step detection.
     *
     * @param stepDetected True if a step was detected
     * @param stepCount Total number of steps detected
     * @param filteredAcceleration Filtered acceleration magnitude
     * @param timestamp Timestamp of the detection
     */
    data class Result(
        val stepDetected: Boolean,
        val stepCount: Int,
        val filteredAcceleration: Float,
        val timestamp: Long
    )
}

