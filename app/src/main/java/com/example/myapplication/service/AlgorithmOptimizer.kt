package com.example.myapplication.service

import com.example.myapplication.domain.model.UserPosition
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Service for optimizing positioning algorithms based on test results and environment characteristics.
 * 
 * This class analyzes test data to identify optimal algorithm parameters for different environments,
 * and provides adaptive parameter tuning to improve positioning accuracy in various conditions.
 */
class AlgorithmOptimizer {
    
    /**
     * Parameters that can be optimized for positioning algorithms.
     */
    data class AlgorithmParameters(
        // Wi-Fi positioning parameters
        val wifiSignalNoiseThreshold: Int = -85,          // Ignore signals below this threshold (dBm)
        val wifiRssiFilterAlpha: Float = 0.3f,            // Low-pass filter coefficient for RSSI smoothing
        val wifiDistanceExponent: Float = 2.7f,           // Path loss exponent for distance calculation
        val wifiPositionWeight: Float = 0.6f,             // Weight of Wi-Fi positioning in fusion
        
        // PDR parameters
        val stepLengthFactor: Float = 0.75f,              // Base step length in meters
        val stepDetectionThreshold: Float = 1.2f,         // Acceleration threshold for step detection
        val headingFilterAlpha: Float = 0.2f,             // Low-pass filter coefficient for heading
        val pdrPositionWeight: Float = 0.4f,              // Weight of PDR positioning in fusion
        
        // Kalman filter parameters
        val processNoisePosition: Float = 0.5f,           // Process noise for position state
        val processNoiseVelocity: Float = 0.1f,           // Process noise for velocity state
        val measurementNoiseWifi: Float = 2.0f,           // Measurement noise for Wi-Fi observations
        val measurementNoisePdr: Float = 0.5f,            // Measurement noise for PDR observations
        
        // Environment-specific parameters
        val environmentType: EnvironmentClassifier.EnvironmentType = EnvironmentClassifier.EnvironmentType.UNKNOWN,
        val confidenceThreshold: Float = 0.7f             // Confidence threshold for position updates
    )
    
    // Current optimized parameters
    private val _currentParameters = MutableStateFlow(AlgorithmParameters())
    val currentParameters: StateFlow<AlgorithmParameters> = _currentParameters.asStateFlow()
    
    // Optimization results for different environments
    private val environmentParameters = mutableMapOf<EnvironmentClassifier.EnvironmentType, AlgorithmParameters>()
    
    // Performance metrics
    private val _performanceMetrics = MutableStateFlow(PerformanceMetrics())
    val performanceMetrics: StateFlow<PerformanceMetrics> = _performanceMetrics.asStateFlow()
    
    /**
     * Initializes the optimizer with default parameters for different environments.
     */
    init {
        // Initialize with default parameters for each environment type
        EnvironmentClassifier.EnvironmentType.values().forEach { envType ->
            environmentParameters[envType] = createDefaultParametersForEnvironment(envType)
        }
    }
    
    /**
     * Creates default algorithm parameters optimized for a specific environment type.
     * 
     * @param environmentType The type of environment
     * @return Parameters optimized for the specified environment
     */
    private fun createDefaultParametersForEnvironment(
        environmentType: EnvironmentClassifier.EnvironmentType
    ): AlgorithmParameters {
        return when (environmentType) {
            EnvironmentClassifier.EnvironmentType.OFFICE -> {
                // Office environments typically have many Wi-Fi access points and moderate multipath
                AlgorithmParameters(
                    wifiSignalNoiseThreshold = -80,
                    wifiRssiFilterAlpha = 0.25f,
                    wifiDistanceExponent = 2.5f,
                    wifiPositionWeight = 0.7f,
                    stepLengthFactor = 0.7f,
                    stepDetectionThreshold = 1.1f,
                    headingFilterAlpha = 0.15f,
                    pdrPositionWeight = 0.3f,
                    processNoisePosition = 0.4f,
                    processNoiseVelocity = 0.08f,
                    measurementNoiseWifi = 1.5f,
                    measurementNoisePdr = 0.6f,
                    environmentType = environmentType,
                    confidenceThreshold = 0.65f
                )
            }
            EnvironmentClassifier.EnvironmentType.RETAIL -> {
                // Retail environments have high Wi-Fi density but significant multipath and obstacles
                AlgorithmParameters(
                    wifiSignalNoiseThreshold = -78,
                    wifiRssiFilterAlpha = 0.3f,
                    wifiDistanceExponent = 2.8f,
                    wifiPositionWeight = 0.65f,
                    stepLengthFactor = 0.72f,
                    stepDetectionThreshold = 1.3f,
                    headingFilterAlpha = 0.2f,
                    pdrPositionWeight = 0.35f,
                    processNoisePosition = 0.6f,
                    processNoiseVelocity = 0.12f,
                    measurementNoiseWifi = 2.2f,
                    measurementNoisePdr = 0.5f,
                    environmentType = environmentType,
                    confidenceThreshold = 0.7f
                )
            }
            EnvironmentClassifier.EnvironmentType.WAREHOUSE -> {
                // Warehouses have sparse Wi-Fi but open spaces for good PDR
                AlgorithmParameters(
                    wifiSignalNoiseThreshold = -75,
                    wifiRssiFilterAlpha = 0.4f,
                    wifiDistanceExponent = 2.3f,
                    wifiPositionWeight = 0.5f,
                    stepLengthFactor = 0.8f,
                    stepDetectionThreshold = 1.4f,
                    headingFilterAlpha = 0.25f,
                    pdrPositionWeight = 0.5f,
                    processNoisePosition = 0.7f,
                    processNoiseVelocity = 0.15f,
                    measurementNoiseWifi = 3.0f,
                    measurementNoisePdr = 0.4f,
                    environmentType = environmentType,
                    confidenceThreshold = 0.6f
                )
            }
            EnvironmentClassifier.EnvironmentType.CORRIDOR -> {
                // Corridors have linear Wi-Fi distribution and constrained movement
                AlgorithmParameters(
                    wifiSignalNoiseThreshold = -82,
                    wifiRssiFilterAlpha = 0.35f,
                    wifiDistanceExponent = 2.4f,
                    wifiPositionWeight = 0.55f,
                    stepLengthFactor = 0.78f,
                    stepDetectionThreshold = 1.2f,
                    headingFilterAlpha = 0.1f,
                    pdrPositionWeight = 0.45f,
                    processNoisePosition = 0.3f,
                    processNoiseVelocity = 0.05f,
                    measurementNoiseWifi = 1.8f,
                    measurementNoisePdr = 0.3f,
                    environmentType = environmentType,
                    confidenceThreshold = 0.75f
                )
            }
            EnvironmentClassifier.EnvironmentType.STAIRWELL -> {
                // Stairwells have poor Wi-Fi but distinctive motion patterns
                AlgorithmParameters(
                    wifiSignalNoiseThreshold = -70,
                    wifiRssiFilterAlpha = 0.5f,
                    wifiDistanceExponent = 3.0f,
                    wifiPositionWeight = 0.3f,
                    stepLengthFactor = 0.6f,
                    stepDetectionThreshold = 1.5f,
                    headingFilterAlpha = 0.3f,
                    pdrPositionWeight = 0.7f,
                    processNoisePosition = 0.8f,
                    processNoiseVelocity = 0.2f,
                    measurementNoiseWifi = 4.0f,
                    measurementNoisePdr = 0.7f,
                    environmentType = environmentType,
                    confidenceThreshold = 0.8f
                )
            }
            EnvironmentClassifier.EnvironmentType.OPEN_AREA -> {
                // Open areas have good Wi-Fi propagation and PDR performance
                AlgorithmParameters(
                    wifiSignalNoiseThreshold = -85,
                    wifiRssiFilterAlpha = 0.2f,
                    wifiDistanceExponent = 2.2f,
                    wifiPositionWeight = 0.6f,
                    stepLengthFactor = 0.8f,
                    stepDetectionThreshold = 1.1f,
                    headingFilterAlpha = 0.15f,
                    pdrPositionWeight = 0.4f,
                    processNoisePosition = 0.5f,
                    processNoiseVelocity = 0.1f,
                    measurementNoiseWifi = 1.5f,
                    measurementNoisePdr = 0.4f,
                    environmentType = environmentType,
                    confidenceThreshold = 0.65f
                )
            }
            EnvironmentClassifier.EnvironmentType.HIGH_INTERFERENCE -> {
                // High interference environments need aggressive filtering
                AlgorithmParameters(
                    wifiSignalNoiseThreshold = -70,
                    wifiRssiFilterAlpha = 0.6f,
                    wifiDistanceExponent = 3.2f,
                    wifiPositionWeight = 0.4f,
                    stepLengthFactor = 0.75f,
                    stepDetectionThreshold = 1.6f,
                    headingFilterAlpha = 0.4f,
                    pdrPositionWeight = 0.6f,
                    processNoisePosition = 1.0f,
                    processNoiseVelocity = 0.25f,
                    measurementNoiseWifi = 5.0f,
                    measurementNoisePdr = 0.8f,
                    environmentType = environmentType,
                    confidenceThreshold = 0.85f
                )
            }
            EnvironmentClassifier.EnvironmentType.UNKNOWN -> {
                // Default balanced parameters for unknown environments
                AlgorithmParameters(
                    environmentType = environmentType
                )
            }
        }
    }
    
    /**
     * Updates the current parameters based on the detected environment.
     * 
     * @param environmentType The detected environment type
     * @param confidence Confidence in the environment classification
     */
    fun updateEnvironment(
        environmentType: EnvironmentClassifier.EnvironmentType,
        confidence: Float
    ) {
        // Only update if we have high confidence in the environment classification
        if (confidence >= 0.6f) {
            val newParams = environmentParameters[environmentType] ?: createDefaultParametersForEnvironment(environmentType)
            _currentParameters.value = newParams
            
            Timber.d("Updated algorithm parameters for environment: $environmentType (confidence: $confidence)")
        }
    }
    
    /**
     * Analyzes test results to optimize algorithm parameters for a specific environment.
     * 
     * @param testResults List of test results with ground truth positions
     * @param environmentType The environment type for which to optimize parameters
     * @return Optimized parameters for the specified environment
     */
    fun optimizeParameters(
        testResults: List<PositionTestResult>,
        environmentType: EnvironmentClassifier.EnvironmentType
    ): AlgorithmParameters {
        if (testResults.isEmpty()) {
            Timber.w("Cannot optimize parameters: no test results provided")
            return environmentParameters[environmentType] ?: createDefaultParametersForEnvironment(environmentType)
        }
        
        // Start with current parameters for this environment
        val currentParams = environmentParameters[environmentType] ?: createDefaultParametersForEnvironment(environmentType)
        
        // Calculate current performance metrics
        val currentMetrics = calculatePerformanceMetrics(testResults)
        _performanceMetrics.value = currentMetrics
        
        // Perform parameter optimization based on test results
        // This is a simplified optimization approach - in a real system, this would use
        // more sophisticated techniques like gradient descent or Bayesian optimization
        
        // Optimize Wi-Fi parameters
        val wifiOptimized = optimizeWifiParameters(currentParams, testResults, currentMetrics)
        
        // Optimize PDR parameters
        val pdrOptimized = optimizePdrParameters(wifiOptimized, testResults, currentMetrics)
        
        // Optimize Kalman filter parameters
        val kalmanOptimized = optimizeKalmanParameters(pdrOptimized, testResults, currentMetrics)
        
        // Store optimized parameters for this environment
        environmentParameters[environmentType] = kalmanOptimized
        
        // If this is the current environment, update current parameters
        if (environmentType == _currentParameters.value.environmentType) {
            _currentParameters.value = kalmanOptimized
        }
        
        Timber.i("Optimized parameters for environment: $environmentType")
        return kalmanOptimized
    }
    
    /**
     * Optimizes Wi-Fi related parameters based on test results.
     */
    private fun optimizeWifiParameters(
        params: AlgorithmParameters,
        testResults: List<PositionTestResult>,
        currentMetrics: PerformanceMetrics
    ): AlgorithmParameters {
        // Extract Wi-Fi specific test results
        val wifiResults = testResults.filter { it.positionSource == PositionSource.WIFI }
        
        if (wifiResults.isEmpty()) {
            return params
        }
        
        // Calculate Wi-Fi specific metrics
        val wifiErrors = wifiResults.map { it.error }
        val meanWifiError = wifiErrors.average().toFloat()
        
        // Adjust parameters based on Wi-Fi performance
        val noiseThreshold = when {
            meanWifiError > 5.0f -> params.wifiSignalNoiseThreshold + 5 // More aggressive filtering
            meanWifiError < 2.0f -> params.wifiSignalNoiseThreshold - 3 // Less filtering needed
            else -> params.wifiSignalNoiseThreshold
        }.coerceIn(-90, -65)
        
        val filterAlpha = when {
            meanWifiError > 5.0f -> params.wifiRssiFilterAlpha + 0.1f // More smoothing
            meanWifiError < 2.0f -> params.wifiRssiFilterAlpha - 0.05f // Less smoothing
            else -> params.wifiRssiFilterAlpha
        }.coerceIn(0.1f, 0.8f)
        
        val distanceExponent = when {
            meanWifiError > 5.0f -> params.wifiDistanceExponent + 0.2f // Adjust distance model
            meanWifiError < 2.0f -> params.wifiDistanceExponent - 0.1f
            else -> params.wifiDistanceExponent
        }.coerceIn(2.0f, 4.0f)
        
        // Adjust Wi-Fi weight based on relative performance compared to PDR
        val pdrResults = testResults.filter { it.positionSource == PositionSource.PDR }
        val wifiWeight = if (pdrResults.isNotEmpty()) {
            val meanPdrError = pdrResults.map { it.error }.average().toFloat()
            // Increase Wi-Fi weight if it's more accurate than PDR
            if (meanWifiError < meanPdrError) {
                (params.wifiPositionWeight + 0.05f).coerceAtMost(0.8f)
            } else {
                (params.wifiPositionWeight - 0.05f).coerceAtLeast(0.2f)
            }
        } else {
            params.wifiPositionWeight
        }
        
        return params.copy(
            wifiSignalNoiseThreshold = noiseThreshold,
            wifiRssiFilterAlpha = filterAlpha,
            wifiDistanceExponent = distanceExponent,
            wifiPositionWeight = wifiWeight,
            pdrPositionWeight = 1.0f - wifiWeight
        )
    }
    
    /**
     * Optimizes PDR related parameters based on test results.
     */
    private fun optimizePdrParameters(
        params: AlgorithmParameters,
        testResults: List<PositionTestResult>,
        currentMetrics: PerformanceMetrics
    ): AlgorithmParameters {
        // Extract PDR specific test results
        val pdrResults = testResults.filter { it.positionSource == PositionSource.PDR }
        
        if (pdrResults.isEmpty()) {
            return params
        }
        
        // Calculate PDR specific metrics
        val pdrErrors = pdrResults.map { it.error }
        val meanPdrError = pdrErrors.average().toFloat()
        
        // Analyze step length accuracy
        val stepLengthError = pdrResults.filter { it.stepLength > 0 }
            .map { abs(it.estimatedDistance - it.actualDistance) / it.actualDistance }
            .average().toFloat()
        
        // Adjust step length factor based on error
        val stepLengthFactor = when {
            stepLengthError > 0.2f -> params.stepLengthFactor * (1f + stepLengthError / 5f)
            stepLengthError < 0.1f -> params.stepLengthFactor * (1f - stepLengthError / 10f)
            else -> params.stepLengthFactor
        }.coerceIn(0.5f, 1.0f)
        
        // Analyze heading accuracy
        val headingErrors = pdrResults.filter { it.heading != null && it.actualHeading != null }
            .map { minHeadingDifference(it.heading!!, it.actualHeading!!) }
        
        val headingFilterAlpha = if (headingErrors.isNotEmpty()) {
            val meanHeadingError = headingErrors.average().toFloat()
            when {
                meanHeadingError > 20f -> params.headingFilterAlpha + 0.05f // More filtering
                meanHeadingError < 10f -> params.headingFilterAlpha - 0.03f // Less filtering
                else -> params.headingFilterAlpha
            }.coerceIn(0.05f, 0.5f)
        } else {
            params.headingFilterAlpha
        }
        
        // Adjust step detection threshold based on environment
        val stepDetectionThreshold = when (params.environmentType) {
            EnvironmentClassifier.EnvironmentType.STAIRWELL -> 1.5f // Higher threshold for stairs
            EnvironmentClassifier.EnvironmentType.OPEN_AREA -> 1.1f // Lower for open areas
            else -> params.stepDetectionThreshold
        }
        
        return params.copy(
            stepLengthFactor = stepLengthFactor,
            stepDetectionThreshold = stepDetectionThreshold,
            headingFilterAlpha = headingFilterAlpha
        )
    }
    
    /**
     * Optimizes Kalman filter parameters based on test results.
     */
    private fun optimizeKalmanParameters(
        params: AlgorithmParameters,
        testResults: List<PositionTestResult>,
        currentMetrics: PerformanceMetrics
    ): AlgorithmParameters {
        if (testResults.isEmpty()) {
            return params
        }
        
        // Calculate velocity statistics
        val velocities = testResults.mapNotNull { it.velocity }
        val velocityStats = if (velocities.isNotEmpty()) {
            val mean = velocities.average().toFloat()
            val variance = velocities.map { (it - mean).pow(2) }.average().toFloat()
            Pair(mean, sqrt(variance))
        } else {
            Pair(1.0f, 0.5f) // Default values
        }
        
        // Adjust process noise based on movement patterns
        val (meanVelocity, velocityStdDev) = velocityStats
        val processNoisePosition = when {
            velocityStdDev > 0.8f -> 0.8f // High variability in movement
            velocityStdDev < 0.3f -> 0.3f // Very consistent movement
            else -> velocityStdDev * 0.8f
        }
        
        val processNoiseVelocity = when {
            velocityStdDev > 0.8f -> 0.2f // High variability in movement
            velocityStdDev < 0.3f -> 0.05f // Very consistent movement
            else -> velocityStdDev * 0.2f
        }
        
        // Adjust measurement noise based on observed errors
        val wifiErrors = testResults.filter { it.positionSource == PositionSource.WIFI }.map { it.error }
        val pdrErrors = testResults.filter { it.positionSource == PositionSource.PDR }.map { it.error }
        
        val measurementNoiseWifi = if (wifiErrors.isNotEmpty()) {
            val meanError = wifiErrors.average().toFloat()
            val stdDev = sqrt(wifiErrors.map { (it - meanError).pow(2) }.average().toFloat())
            (meanError * 0.5f + stdDev).coerceIn(1.0f, 5.0f)
        } else {
            params.measurementNoiseWifi
        }
        
        val measurementNoisePdr = if (pdrErrors.isNotEmpty()) {
            val meanError = pdrErrors.average().toFloat()
            val stdDev = sqrt(pdrErrors.map { (it - meanError).pow(2) }.average().toFloat())
            (meanError * 0.3f + stdDev * 0.7f).coerceIn(0.3f, 2.0f)
        } else {
            params.measurementNoisePdr
        }
        
        // Adjust confidence threshold based on overall accuracy
        val confidenceThreshold = when {
            currentMetrics.meanError > 4.0f -> 0.8f // Higher threshold for less accurate environments
            currentMetrics.meanError < 2.0f -> 0.6f // Lower threshold for more accurate environments
            else -> 0.7f
        }
        
        return params.copy(
            processNoisePosition = processNoisePosition,
            processNoiseVelocity = processNoiseVelocity,
            measurementNoiseWifi = measurementNoiseWifi,
            measurementNoisePdr = measurementNoisePdr,
            confidenceThreshold = confidenceThreshold
        )
    }
    
    /**
     * Calculates performance metrics from test results.
     */
    private fun calculatePerformanceMetrics(testResults: List<PositionTestResult>): PerformanceMetrics {
        if (testResults.isEmpty()) {
            return PerformanceMetrics()
        }
        
        val errors = testResults.map { it.error }
        val meanError = errors.average().toFloat()
        val maxError = errors.maxOrNull() ?: 0f
        
        // Calculate RMSE
        val rmse = sqrt(errors.map { it * it }.average()).toFloat()
        
        // Calculate 95th percentile error
        val sortedErrors = errors.sorted()
        val index95 = (sortedErrors.size * 0.95).toInt().coerceAtMost(sortedErrors.size - 1)
        val percentile95Error = sortedErrors.getOrNull(index95) ?: 0f
        
        // Calculate standard deviation
        val variance = errors.map { (it - meanError).pow(2) }.average().toFloat()
        val stdDev = sqrt(variance)
        
        // Calculate mean confidence
        val meanConfidence = testResults.map { it.confidence }.average().toFloat()
        
        // Calculate update rate (positions per second)
        val maxTime = testResults.maxOfOrNull { it.timestamp } ?: 0L
        val minTime = testResults.minOfOrNull { it.timestamp } ?: 0L
        val timeRange = maxTime - minTime
        val updateRate = if (timeRange > 0) {
            testResults.size / (timeRange / 1000f)
        } else {
            0f
        }
        
        return PerformanceMetrics(
            meanError = meanError,
            maxError = maxError,
            rmseError = rmse,
            percentile95Error = percentile95Error,
            standardDeviation = stdDev,
            meanConfidence = meanConfidence,
            updateRate = updateRate,
            sampleCount = testResults.size
        )
    }
    
    /**
     * Calculates the minimum difference between two heading angles.
     */
    private fun minHeadingDifference(heading1: Float, heading2: Float): Float {
        val diff = Math.abs(heading1 - heading2) % 360f
        return if (diff > 180f) 360f - diff else diff
    }
    
    /**
     * Calculates the absolute value of a float.
     */
    private fun abs(value: Float): Float {
        return if (value < 0) -value else value
    }
    
    /**
     * Data class representing performance metrics for positioning algorithms.
     */
    data class PerformanceMetrics(
        val meanError: Float = 0f,
        val maxError: Float = 0f,
        val rmseError: Float = 0f,
        val percentile95Error: Float = 0f,
        val standardDeviation: Float = 0f,
        val meanConfidence: Float = 0f,
        val updateRate: Float = 0f,
        val sampleCount: Int = 0
    )
    
    /**
     * Data class representing a position test result with ground truth.
     */
    data class PositionTestResult(
        val timestamp: Long,
        val estimatedX: Float,
        val estimatedY: Float,
        val actualX: Float,
        val actualY: Float,
        val error: Float,
        val confidence: Float,
        val positionSource: PositionSource,
        val velocity: Float? = null,
        val heading: Float? = null,
        val actualHeading: Float? = null,
        val stepLength: Float = 0f,
        val estimatedDistance: Float = 0f,
        val actualDistance: Float = 0f
    )
    
    /**
     * Enum representing the source of a position estimate.
     */
    enum class PositionSource {
        WIFI,
        PDR,
        FUSION,
        SLAM,
        UNKNOWN
    }
}