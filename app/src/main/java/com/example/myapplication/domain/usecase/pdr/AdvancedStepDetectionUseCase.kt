package com.example.myapplication.domain.usecase.pdr

import com.example.myapplication.domain.model.SensorData
import com.example.myapplication.domain.usecase.UseCase
import timber.log.Timber
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Use case for detecting steps using a more advanced algorithm that combines
 * accelerometer and gyroscope data for improved accuracy.
 * 
 * This implementation uses a state machine approach with multiple conditions
 * to reduce false positives and better handle different walking patterns.
 */
class AdvancedStepDetectionUseCase : UseCase<AdvancedStepDetectionUseCase.Params, AdvancedStepDetectionUseCase.Result> {
    
    // Constants for the class
    private val WINDOW_SIZE = 50
    
    // Conversion factor from nanoseconds to milliseconds
    private val NANOS_TO_MILLIS = StepDetectionUtils.NANOS_TO_MILLIS
    
    // Factors for state transitions
    private val PEAK_DECREASE_FACTOR = 0.95f
    private val VALLEY_INCREASE_FACTOR = 1.05f
    
    // Weights for adaptive thresholding
    private val IQR_PEAK_WEIGHT = 1.2f
    private val STD_DEV_PEAK_WEIGHT = 0.8f
    private val IQR_VALLEY_WEIGHT = 1.2f
    private val STD_DEV_VALLEY_WEIGHT = 0.8f
    
    // Threshold adjustment factors
    private val MIN_THRESHOLD_FACTOR = 0.6f
    private val MAX_THRESHOLD_FACTOR = 1.4f
    
    // Step detection state machine states
    private enum class State {
        IDLE,       // Waiting for initial acceleration
        RISING,     // Acceleration rising
        PEAK,       // At peak acceleration
        FALLING,    // Acceleration falling
        VALLEY      // At valley (minimum acceleration)
    }
    
    // State variables
    private var state = State.IDLE
    private var stepCount = 0
    private var lastStepTime = 0L
    private var peakValue = 0f
    private var valleyValue = 0f
    private var lastFilteredAccel = 0f
    private var lastFilteredGyro = 0f
    private var peakTime = 0L
    private var valleyTime = 0L
    
    // Window of recent acceleration values for adaptive thresholding
    private val recentAccelValues = ArrayDeque<Float>(WINDOW_SIZE)
    
    override suspend fun invoke(params: Params): Result {
        // Extract sensor data
        val accelData = params.accelerometerData
        val gyroData = params.gyroscopeData
        val timestamp = accelData.timestamp
        
        // Calculate magnitudes
        val accelMagnitude = accelData.magnitude()
        val gyroMagnitude = gyroData?.magnitude() ?: 0f
        
        // Apply low-pass filters
        val filteredAccel = applyLowPassFilter(accelMagnitude, lastFilteredAccel, params.accelFilterAlpha)
        val filteredGyro = if (gyroData != null) {
            applyLowPassFilter(gyroMagnitude, lastFilteredGyro, params.gyroFilterAlpha)
        } else {
            0f
        }
        
        // Update recent acceleration values for adaptive thresholding
        if (recentAccelValues.size >= WINDOW_SIZE) {
            recentAccelValues.removeFirst()
        }
        recentAccelValues.addLast(filteredAccel)
        
        // Calculate adaptive thresholds
        val (adaptivePeakThreshold, adaptiveValleyThreshold) = calculateAdaptiveThresholds(
            params.peakThreshold,
            params.valleyThreshold
        )
        
        // Detect step using state machine
        val stepDetected = detectStepWithStateMachine(
            filteredAccel,
            filteredGyro,
            timestamp,
            adaptivePeakThreshold,
            adaptiveValleyThreshold,
            params.minPeakValleyHeight,
            params.minStepInterval,
            params.maxStepInterval,
            params.gyroThreshold,
            params.minPeakDuration,
            params.maxPeakDuration
        )
        
        // Update last filtered values
        lastFilteredAccel = filteredAccel
        lastFilteredGyro = filteredGyro
        
        // If a step is detected, increment step count
        if (stepDetected) {
            stepCount++
            Timber.d("Step detected (advanced)! Total steps: $stepCount")
        }
        
        return Result(
            stepDetected = stepDetected,
            stepCount = stepCount,
            filteredAcceleration = filteredAccel,
            filteredGyroMagnitude = filteredGyro,
            currentState = state.name,
            timestamp = timestamp
        )
    }
    
    /**
     * Applies a low-pass filter to smooth the signal.
     * 
     * @param currentValue Current value to be filtered
     * @param lastFilteredValue Previous filtered value
     * @param alpha Filter coefficient (0-1, lower values = more smoothing)
     * @return Filtered value
     */
    private fun applyLowPassFilter(currentValue: Float, lastFilteredValue: Float, alpha: Float): Float {
        return StepDetectionUtils.applyLowPassFilter(currentValue, lastFilteredValue, alpha)
    }
    
    /**
     * Calculates adaptive thresholds based on recent acceleration values.
     * 
     * This method dynamically adjusts the peak and valley thresholds based on the
     * statistical properties (median and interquartile range) of recent acceleration values.
     * This helps the step detection algorithm adapt to different walking patterns and
     * sensor characteristics.
     * 
     * @param basePeakThreshold Base threshold for peak detection
     * @param baseValleyThreshold Base threshold for valley detection
     * @return Pair of adjusted peak and valley thresholds
     */
    private fun calculateAdaptiveThresholds(basePeakThreshold: Float, baseValleyThreshold: Float): Pair<Float, Float> {
        if (recentAccelValues.size < WINDOW_SIZE / 2) {
            return Pair(basePeakThreshold, baseValleyThreshold)
        }
        
        // Calculate median
        val median = StepDetectionUtils.calculateMedian(recentAccelValues.toList()) ?: return Pair(basePeakThreshold, baseValleyThreshold)
        
        // Calculate quartiles
        val quartiles = StepDetectionUtils.calculateQuartiles(recentAccelValues.toList()) ?: return Pair(basePeakThreshold, baseValleyThreshold)
        val (q1, q3) = quartiles
        val iqr = q3 - q1
        
        // Calculate standard deviation
        val stdDev = StepDetectionUtils.calculateStandardDeviation(recentAccelValues.toList()) ?: return Pair(basePeakThreshold, baseValleyThreshold)
        
        // Adjust thresholds based on signal characteristics with improved weighting
        // Use both IQR and standard deviation for more robust thresholds
        val adaptivePeakThreshold = median + (iqr * IQR_PEAK_WEIGHT + stdDev * STD_DEV_PEAK_WEIGHT) / 2f
        val adaptiveValleyThreshold = median - (iqr * IQR_VALLEY_WEIGHT + stdDev * STD_DEV_VALLEY_WEIGHT) / 2f
        
        // Ensure thresholds are within reasonable bounds with wider range for more adaptability
        val finalPeakThreshold = StepDetectionUtils.constrain(
            adaptivePeakThreshold,
            basePeakThreshold * MIN_THRESHOLD_FACTOR,
            basePeakThreshold * MAX_THRESHOLD_FACTOR
        )
        
        val finalValleyThreshold = StepDetectionUtils.constrain(
            adaptiveValleyThreshold,
            baseValleyThreshold * MIN_THRESHOLD_FACTOR,
            baseValleyThreshold * MAX_THRESHOLD_FACTOR
        )
        
        Timber.v("Adaptive thresholds: peak=$finalPeakThreshold, valley=$finalValleyThreshold (base: peak=$basePeakThreshold, valley=$baseValleyThreshold)")
        return Pair(finalPeakThreshold, finalValleyThreshold)
    }
    
    /**
     * Detects steps using an enhanced state machine approach.
     * 
     * This method implements a state machine with five states (IDLE, RISING, PEAK, FALLING, VALLEY)
     * to detect steps based on acceleration patterns. It incorporates gyroscope data
     * to reduce false positives and includes additional validation checks for improved accuracy.
     * 
     * The state transitions are:
     * 1. IDLE → RISING: When acceleration exceeds peak threshold
     * 2. RISING → PEAK: When acceleration starts decreasing after rising
     * 3. PEAK → FALLING: When acceleration drops below valley threshold
     * 4. FALLING → VALLEY: When acceleration starts increasing after falling
     * 5. VALLEY → IDLE: After step detection logic is applied
     * 
     * @param accelMagnitude Current filtered acceleration magnitude
     * @param gyroMagnitude Current filtered gyroscope magnitude
     * @param timestamp Current timestamp in nanoseconds
     * @param peakThreshold Threshold for peak detection
     * @param valleyThreshold Threshold for valley detection
     * @param minPeakValleyHeight Minimum height difference between peak and valley
     * @param minStepInterval Minimum time between steps (ms)
     * @param maxStepInterval Maximum time between steps (ms)
     * @param gyroThreshold Threshold for gyroscope magnitude to validate step
     * @param minPeakDuration Minimum duration for peak state (ms)
     * @param maxPeakDuration Maximum duration for peak state (ms)
     * @return True if a step is detected, false otherwise
     */
    private fun detectStepWithStateMachine(
        accelMagnitude: Float,
        gyroMagnitude: Float,
        timestamp: Long,
        peakThreshold: Float,
        valleyThreshold: Float,
        minPeakValleyHeight: Float,
        minStepInterval: Long,
        maxStepInterval: Long,
        gyroThreshold: Float,
        minPeakDuration: Long,
        maxPeakDuration: Long
    ): Boolean {
        var stepDetected = false
        
        // Convert timestamp from nanoseconds to milliseconds for easier comparison
        val timeMs = timestamp / NANOS_TO_MILLIS
        
        // State machine for step detection
        when (state) {
            State.IDLE -> {
                // Transition to RISING state when acceleration exceeds threshold
                if (accelMagnitude > peakThreshold) {
                    state = State.RISING
                    Timber.v("State transition: IDLE → RISING at $timeMs ms")
                }
            }
            
            State.RISING -> {
                // Update peak value if we find a higher acceleration
                if (accelMagnitude > peakValue) {
                    peakValue = accelMagnitude
                    peakTime = timeMs
                }
                
                // Transition to PEAK state when acceleration starts decreasing
                if (accelMagnitude < peakValue * PEAK_DECREASE_FACTOR) {
                    state = State.PEAK
                    Timber.v("State transition: RISING → PEAK at $timeMs ms, peak value: $peakValue")
                }
            }
            
            State.PEAK -> {
                // Transition to FALLING state when acceleration drops below threshold
                if (accelMagnitude < valleyThreshold) {
                    state = State.FALLING
                    Timber.v("State transition: PEAK → FALLING at $timeMs ms")
                }
                
                // Reset if we've been in PEAK state too long
                val peakDuration = timeMs - peakTime
                if (peakDuration > maxPeakDuration) {
                    Timber.v("Peak duration too long ($peakDuration ms), resetting to IDLE")
                    state = State.IDLE
                    peakValue = 0f
                    valleyValue = 0f
                }
            }
            
            State.FALLING -> {
                // Update valley value if we find a lower acceleration
                if (accelMagnitude < valleyValue || valleyValue == 0f) {
                    valleyValue = accelMagnitude
                    valleyTime = timeMs
                }
                
                // Transition to VALLEY state when acceleration starts increasing
                if (accelMagnitude > valleyValue * VALLEY_INCREASE_FACTOR) {
                    state = State.VALLEY
                    Timber.v("State transition: FALLING → VALLEY at $timeMs ms, valley value: $valleyValue")
                }
            }
            
            State.VALLEY -> {
                // Check if we have a valid step pattern
                val peakValleyHeight = peakValue - valleyValue
                val timeSinceLastStep = timeMs - lastStepTime
                val peakDuration = valleyTime - peakTime
                
                // Validate step with multiple criteria
                if (isValidStepPattern(
                    peakValleyHeight = peakValleyHeight,
                    timeSinceLastStep = timeSinceLastStep,
                    peakDuration = peakDuration,
                    gyroMagnitude = gyroMagnitude,
                    minPeakValleyHeight = minPeakValleyHeight,
                    minStepInterval = minStepInterval,
                    maxStepInterval = maxStepInterval,
                    minPeakDuration = minPeakDuration,
                    maxPeakDuration = maxPeakDuration,
                    gyroThreshold = gyroThreshold
                )) {
                    // Step detected!
                    stepDetected = true
                    lastStepTime = timeMs
                    
                    Timber.d("Step detected at $timeMs ms, peak-valley height: $peakValleyHeight, " +
                            "peak duration: $peakDuration ms, gyro: $gyroMagnitude")
                } else {
                    // Log rejection reasons (handled in isValidStepPattern)
                    logStepRejectionReasons(
                        peakValleyHeight = peakValleyHeight,
                        timeSinceLastStep = timeSinceLastStep,
                        peakDuration = peakDuration,
                        gyroMagnitude = gyroMagnitude,
                        minPeakValleyHeight = minPeakValleyHeight,
                        minStepInterval = minStepInterval,
                        maxStepInterval = maxStepInterval,
                        minPeakDuration = minPeakDuration,
                        maxPeakDuration = maxPeakDuration,
                        gyroThreshold = gyroThreshold
                    )
                }
                
                // Reset state
                state = State.IDLE
                peakValue = 0f
                valleyValue = 0f
            }
            
            // This should never happen, but is needed to make the when expression exhaustive
            else -> {
                Timber.e("Invalid state: $state")
                state = State.IDLE
            }
        }
        
        return stepDetected
    }
    
    /**
     * Resets the step detection state.
     */
    fun reset() {
        state = State.IDLE
        stepCount = 0
        lastStepTime = 0L
        peakValue = 0f
        valleyValue = 0f
        lastFilteredAccel = 0f
        lastFilteredGyro = 0f
        peakTime = 0L
        valleyTime = 0L
        recentAccelValues.clear()
    }
    
    /**
     * Parameters for the AdvancedStepDetectionUseCase.
     *
     * @param accelerometerData Accelerometer data
     * @param gyroscopeData Optional gyroscope data for improved accuracy
     * @param accelFilterAlpha Low-pass filter coefficient for accelerometer (0-1)
     * @param gyroFilterAlpha Low-pass filter coefficient for gyroscope (0-1)
     * @param peakThreshold Base threshold for peak detection
     * @param valleyThreshold Base threshold for valley detection
     * @param minPeakValleyHeight Minimum height difference between peak and valley
     * @param minStepInterval Minimum time between steps (ms)
     * @param maxStepInterval Maximum time between steps (ms)
     * @param gyroThreshold Threshold for gyroscope magnitude to validate step
     * @param minPeakDuration Minimum duration for peak state (ms)
     * @param maxPeakDuration Maximum duration for peak state (ms)
     */
    data class Params(
        val accelerometerData: SensorData.Accelerometer,
        val gyroscopeData: SensorData.Gyroscope? = null,
        val accelFilterAlpha: Float = StepDetectionUtils.DEFAULT_ACCEL_FILTER_ALPHA,
        val gyroFilterAlpha: Float = StepDetectionUtils.DEFAULT_GYRO_FILTER_ALPHA,
        val peakThreshold: Float = StepDetectionUtils.DEFAULT_PEAK_THRESHOLD,
        val valleyThreshold: Float = StepDetectionUtils.DEFAULT_VALLEY_THRESHOLD,
        val minPeakValleyHeight: Float = StepDetectionUtils.DEFAULT_MIN_PEAK_VALLEY_HEIGHT,
        val minStepInterval: Long = StepDetectionUtils.DEFAULT_MIN_STEP_INTERVAL,
        val maxStepInterval: Long = StepDetectionUtils.DEFAULT_MAX_STEP_INTERVAL,
        val gyroThreshold: Float = StepDetectionUtils.DEFAULT_GYRO_THRESHOLD,
        val minPeakDuration: Long = StepDetectionUtils.DEFAULT_MIN_PEAK_DURATION,
        val maxPeakDuration: Long = StepDetectionUtils.DEFAULT_MAX_PEAK_DURATION
    )
    
    /**
     * Result of the advanced step detection.
     *
     * @param stepDetected True if a step was detected
     * @param stepCount Total number of steps detected
     * @param filteredAcceleration Filtered acceleration magnitude
     * @param filteredGyroMagnitude Filtered gyroscope magnitude
     * @param currentState Current state of the step detection state machine
     * @param timestamp Timestamp of the detection
     */
    data class Result(
        val stepDetected: Boolean,
        val stepCount: Int,
        val filteredAcceleration: Float,
        val filteredGyroMagnitude: Float,
        val currentState: String,
        val timestamp: Long
    )
    
    /**
     * Determines if the current peak-valley pattern represents a valid step.
     * 
     * @param peakValleyHeight Height difference between peak and valley
     * @param timeSinceLastStep Time since the last detected step (ms)
     * @param peakDuration Duration of the peak state (ms)
     * @param gyroMagnitude Magnitude of gyroscope data
     * @param minPeakValleyHeight Minimum required peak-valley height
     * @param minStepInterval Minimum time between steps (ms)
     * @param maxStepInterval Maximum time between steps (ms)
     * @param minPeakDuration Minimum duration for peak state (ms)
     * @param maxPeakDuration Maximum duration for peak state (ms)
     * @param gyroThreshold Threshold for gyroscope magnitude to validate step
     * @return True if the pattern represents a valid step, false otherwise
     */
    private fun isValidStepPattern(
        peakValleyHeight: Float,
        timeSinceLastStep: Long,
        peakDuration: Long,
        gyroMagnitude: Float,
        minPeakValleyHeight: Float,
        minStepInterval: Long,
        maxStepInterval: Long,
        minPeakDuration: Long,
        maxPeakDuration: Long,
        gyroThreshold: Float
    ): Boolean {
        val validHeight = peakValleyHeight >= minPeakValleyHeight
        val validTiming = StepDetectionUtils.isInRange(timeSinceLastStep, minStepInterval, maxStepInterval)
        val validPeakDuration = StepDetectionUtils.isInRange(peakDuration, minPeakDuration, maxPeakDuration)
        val validGyro = gyroMagnitude >= gyroThreshold
        
        // Consider gyro validation only if gyro data is available
        return validHeight && validTiming && validPeakDuration && (validGyro || gyroMagnitude == 0f)
    }
    
    /**
     * Logs the reasons why a step was rejected.
     * 
     * @param peakValleyHeight Height difference between peak and valley
     * @param timeSinceLastStep Time since the last detected step (ms)
     * @param peakDuration Duration of the peak state (ms)
     * @param gyroMagnitude Magnitude of gyroscope data
     * @param minPeakValleyHeight Minimum required peak-valley height
     * @param minStepInterval Minimum time between steps (ms)
     * @param maxStepInterval Maximum time between steps (ms)
     * @param minPeakDuration Minimum duration for peak state (ms)
     * @param maxPeakDuration Maximum duration for peak state (ms)
     * @param gyroThreshold Threshold for gyroscope magnitude to validate step
     */
    private fun logStepRejectionReasons(
        peakValleyHeight: Float,
        timeSinceLastStep: Long,
        peakDuration: Long,
        gyroMagnitude: Float,
        minPeakValleyHeight: Float,
        minStepInterval: Long,
        maxStepInterval: Long,
        minPeakDuration: Long,
        maxPeakDuration: Long,
        gyroThreshold: Float
    ) {
        if (peakValleyHeight < minPeakValleyHeight) {
            Timber.v("Step rejected: insufficient peak-valley height ($peakValleyHeight < $minPeakValleyHeight)")
        }
        
        if (timeSinceLastStep < minStepInterval || timeSinceLastStep > maxStepInterval) {
            Timber.v("Step rejected: invalid timing (time since last step: $timeSinceLastStep ms)")
        }
        
        if (peakDuration < minPeakDuration || peakDuration > maxPeakDuration) {
            Timber.v("Step rejected: invalid peak duration ($peakDuration ms)")
        }
        
        if (gyroMagnitude < gyroThreshold && gyroMagnitude > 0f) {
            Timber.v("Step rejected: insufficient gyro magnitude ($gyroMagnitude < $gyroThreshold)")
        }
    }
    
    // No companion object needed as constants are defined at class level
}