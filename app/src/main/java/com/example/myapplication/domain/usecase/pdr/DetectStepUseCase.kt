package com.example.myapplication.domain.usecase.pdr

import com.example.myapplication.domain.model.SensorData
import com.example.myapplication.domain.usecase.UseCase
import timber.log.Timber
import kotlin.math.abs

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
        filteredAccelMagnitude = applyLowPassFilter(accelMagnitude, filteredAccelMagnitude, params.filterAlpha)
        
        // Detect step based on peak-valley pattern
        val stepDetected = detectStep(
            filteredAccelMagnitude,
            timestamp,
            params.peakThreshold,
            params.valleyThreshold,
            params.minPeakValleyHeight,
            params.minStepInterval,
            params.maxStepInterval
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
        return alpha * currentValue + (1 - alpha) * lastFilteredValue
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
        val timeMs = timestamp / 1_000_000
        
        // Detect peaks and valleys
        if (!inPeak && accelMagnitude > peakThreshold && accelMagnitude > lastAccelMagnitude) {
            // Transition from valley to peak
            inPeak = true
            lastPeakValue = accelMagnitude
            lastPeakTime = timeMs
            
            // Check if we have a valid peak-valley pattern
            if (lastValleyValue > 0 && 
                (lastPeakValue - lastValleyValue) >= minPeakValleyHeight &&
                (timeMs - lastStepTime) >= minStepInterval &&
                (timeMs - lastStepTime) <= maxStepInterval) {
                
                // Step detected!
                stepDetected = true
                lastStepTime = timeMs
                
                Timber.d("Step detected at $timeMs ms, peak-valley height: ${lastPeakValue - lastValleyValue}")
            }
        } else if (inPeak && accelMagnitude < valleyThreshold && accelMagnitude < lastAccelMagnitude) {
            // Transition from peak to valley
            inPeak = false
            lastValleyValue = accelMagnitude
            lastValleyTime = timeMs
        }
        
        return stepDetected
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
        val filterAlpha: Float = 0.3f,
        val peakThreshold: Float = 10.5f,
        val valleyThreshold: Float = 9.5f,
        val minPeakValleyHeight: Float = 0.7f,
        val minStepInterval: Long = 250,  // 250ms = max 4 steps per second
        val maxStepInterval: Long = 2000  // 2000ms = min 0.5 steps per second
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
    
    companion object {
        /**
         * Default filter alpha value.
         */
        const val DEFAULT_FILTER_ALPHA = 0.3f
        
        /**
         * Default peak threshold.
         */
        const val DEFAULT_PEAK_THRESHOLD = 10.5f
        
        /**
         * Default valley threshold.
         */
        const val DEFAULT_VALLEY_THRESHOLD = 9.5f
        
        /**
         * Default minimum peak-valley height.
         */
        const val DEFAULT_MIN_PEAK_VALLEY_HEIGHT = 0.7f
        
        /**
         * Default minimum step interval (ms).
         */
        const val DEFAULT_MIN_STEP_INTERVAL = 250L
        
        /**
         * Default maximum step interval (ms).
         */
        const val DEFAULT_MAX_STEP_INTERVAL = 2000L
    }
}

/**
 * Use case for detecting steps using a more advanced algorithm that combines
 * accelerometer and gyroscope data for improved accuracy.
 * 
 * This implementation uses a state machine approach with multiple conditions
 * to reduce false positives and better handle different walking patterns.
 */
class AdvancedStepDetectionUseCase : UseCase<AdvancedStepDetectionUseCase.Params, AdvancedStepDetectionUseCase.Result> {
    
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
            params.gyroThreshold
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
        return alpha * currentValue + (1 - alpha) * lastFilteredValue
    }
    
    /**
     * Calculates adaptive thresholds based on recent acceleration values.
     * 
     * This method dynamically adjusts the peak and valley thresholds based on the
     * statistical properties (mean and standard deviation) of recent acceleration values.
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
        
        // Calculate mean and standard deviation
        val mean = recentAccelValues.average().toFloat()
        val variance = recentAccelValues.map { (it - mean) * (it - mean) }.average().toFloat()
        val stdDev = kotlin.math.sqrt(variance.toDouble()).toFloat()
        
        // Adjust thresholds based on signal characteristics
        val adaptivePeakThreshold = mean + stdDev * 0.7f
        val adaptiveValleyThreshold = mean - stdDev * 0.3f
        
        // Ensure thresholds are within reasonable bounds
        val finalPeakThreshold = adaptivePeakThreshold.coerceIn(
            basePeakThreshold * 0.7f,
            basePeakThreshold * 1.3f
        )
        
        val finalValleyThreshold = adaptiveValleyThreshold.coerceIn(
            baseValleyThreshold * 0.7f,
            baseValleyThreshold * 1.3f
        )
        
        return Pair(finalPeakThreshold, finalValleyThreshold)
    }
    
    /**
     * Detects steps using a state machine approach.
     * 
     * This method implements a state machine with five states (IDLE, RISING, PEAK, FALLING, VALLEY)
     * to detect steps based on acceleration patterns. It also incorporates gyroscope data
     * to reduce false positives by ensuring there's sufficient rotational movement.
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
     * @param gyroThreshold Minimum gyroscope magnitude required for step validation
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
        gyroThreshold: Float
    ): Boolean {
        var stepDetected = false
        
        // Convert timestamp from nanoseconds to milliseconds
        val timeMs = timestamp / 1_000_000
        
        // State machine for step detection
        when (state) {
            State.IDLE -> {
                if (accelMagnitude > peakThreshold) {
                    state = State.RISING
                    Timber.v("State: IDLE -> RISING")
                }
            }
            
            State.RISING -> {
                if (accelMagnitude < lastFilteredAccel) {
                    // Transition to PEAK when acceleration starts decreasing
                    state = State.PEAK
                    peakValue = lastFilteredAccel
                    Timber.v("State: RISING -> PEAK, peak value: $peakValue")
                }
            }
            
            State.PEAK -> {
                if (accelMagnitude < valleyThreshold) {
                    // Transition to FALLING when acceleration drops below valley threshold
                    state = State.FALLING
                    Timber.v("State: PEAK -> FALLING")
                }
            }
            
            State.FALLING -> {
                if (accelMagnitude > lastFilteredAccel) {
                    // Transition to VALLEY when acceleration starts increasing
                    state = State.VALLEY
                    valleyValue = lastFilteredAccel
                    Timber.v("State: FALLING -> VALLEY, valley value: $valleyValue")
                    
                    // Check if we have a valid step pattern
                    if ((peakValue - valleyValue) >= minPeakValleyHeight &&
                        (timeMs - lastStepTime) >= minStepInterval &&
                        (timeMs - lastStepTime) <= maxStepInterval &&
                        gyroMagnitude >= gyroThreshold) {
                        
                        // Step detected!
                        stepDetected = true
                        lastStepTime = timeMs
                        
                        Timber.d("Step detected at $timeMs ms, peak-valley height: ${peakValue - valleyValue}, gyro: $gyroMagnitude")
                    }
                    
                    // Reset to IDLE to start looking for the next step
                    state = State.IDLE
                }
            }
            
            State.VALLEY -> {
                // This state is transitional, we immediately move back to IDLE
                state = State.IDLE
                Timber.v("State: VALLEY -> IDLE")
            }
        }
        
        return stepDetected
    }
    
    /**
     * Resets the step detection state.
     * 
     * This method resets all internal state variables to their initial values,
     * effectively restarting the step detection process. This is useful when
     * the user's activity changes significantly (e.g., from walking to running)
     * or when the device position changes.
     */
    fun reset() {
        state = State.IDLE
        stepCount = 0
        lastStepTime = 0L
        peakValue = 0f
        valleyValue = 0f
        lastFilteredAccel = 0f
        lastFilteredGyro = 0f
        recentAccelValues.clear()
    }
    
    /**
     * Parameters for the AdvancedStepDetectionUseCase.
     * 
     * @param accelerometerData Accelerometer data containing x, y, z values and timestamp
     * @param gyroscopeData Optional gyroscope data for improved step detection accuracy
     * @param accelFilterAlpha Low-pass filter coefficient for accelerometer data (0-1)
     * @param gyroFilterAlpha Low-pass filter coefficient for gyroscope data (0-1)
     * @param peakThreshold Threshold for peak detection in acceleration magnitude
     * @param valleyThreshold Threshold for valley detection in acceleration magnitude
     * @param minPeakValleyHeight Minimum height difference between peak and valley to consider a valid step
     * @param minStepInterval Minimum time between steps in milliseconds (250ms = max 4 steps per second)
     * @param maxStepInterval Maximum time between steps in milliseconds (2000ms = min 0.5 steps per second)
     * @param gyroThreshold Minimum gyroscope magnitude required to validate a step detection
     */
    data class Params(
        val accelerometerData: SensorData.Accelerometer,
        val gyroscopeData: SensorData.Gyroscope? = null,
        val accelFilterAlpha: Float = 0.2f,
        val gyroFilterAlpha: Float = 0.3f,
        val peakThreshold: Float = 10.5f,
        val valleyThreshold: Float = 9.5f,
        val minPeakValleyHeight: Float = 0.7f,
        val minStepInterval: Long = 250,
        val maxStepInterval: Long = 2000,
        val gyroThreshold: Float = 0.2f
    )
    
    /**
     * Result of the advanced step detection.
     * 
     * This class encapsulates all the information about the step detection result,
     * including whether a step was detected, the current step count, filtered sensor
     * values, the current state of the detection state machine, and the timestamp.
     * 
     * @param stepDetected True if a step was detected in this invocation
     * @param stepCount Total number of steps detected since the last reset
     * @param filteredAcceleration Current filtered acceleration magnitude
     * @param filteredGyroMagnitude Current filtered gyroscope magnitude
     * @param currentState Current state of the step detection state machine
     * @param timestamp Timestamp of the detection in nanoseconds
     */
    data class Result(
        val stepDetected: Boolean,
        val stepCount: Int,
        val filteredAcceleration: Float,
        val filteredGyroMagnitude: Float,
        val currentState: String,
        val timestamp: Long
    )
    
    companion object {
        /**
         * Window size for adaptive thresholding.
         */
        const val WINDOW_SIZE = 50
        
        /**
         * Default accelerometer filter alpha value.
         */
        const val DEFAULT_ACCEL_FILTER_ALPHA = 0.2f
        
        /**
         * Default gyroscope filter alpha value.
         */
        const val DEFAULT_GYRO_FILTER_ALPHA = 0.3f
        
        /**
         * Default gyroscope threshold for step validation.
         */
        const val DEFAULT_GYRO_THRESHOLD = 0.2f
    }
}