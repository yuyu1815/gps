package com.example.myapplication.domain.usecase.pdr

import kotlin.math.sqrt

/**
 * Utility class containing common functions for step detection algorithms.
 */
object StepDetectionUtils {
    
    /**
     * Conversion factor from nanoseconds to milliseconds.
     */
    const val NANOS_TO_MILLIS = 1_000_000L
    
    /**
     * Default filter alpha value for accelerometer.
     */
    const val DEFAULT_ACCEL_FILTER_ALPHA = 0.3f
    
    /**
     * Default filter alpha value for gyroscope.
     */
    const val DEFAULT_GYRO_FILTER_ALPHA = 0.2f
    
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
    
    /**
     * Default gyroscope threshold for step validation.
     */
    const val DEFAULT_GYRO_THRESHOLD = 0.2f
    
    /**
     * Default minimum peak duration (ms).
     */
    const val DEFAULT_MIN_PEAK_DURATION = 60L
    
    /**
     * Default maximum peak duration (ms).
     */
    const val DEFAULT_MAX_PEAK_DURATION = 500L
    
    /**
     * Applies a low-pass filter to smooth a signal.
     * 
     * @param currentValue Current value to be filtered
     * @param lastFilteredValue Previous filtered value
     * @param alpha Filter coefficient (0-1, lower values = more smoothing)
     * @return Filtered value
     */
    fun applyLowPassFilter(currentValue: Float, lastFilteredValue: Float, alpha: Float): Float {
        return alpha * currentValue + (1 - alpha) * lastFilteredValue
    }
    
    /**
     * Calculates the median of a list of values.
     * 
     * @param values List of values
     * @return Median value, or null if the list is empty
     */
    fun calculateMedian(values: List<Float>): Float? {
        if (values.isEmpty()) return null
        
        val sortedValues = values.sorted()
        val size = sortedValues.size
        
        return if (size % 2 == 0) {
            (sortedValues[size / 2 - 1] + sortedValues[size / 2]) / 2f
        } else {
            sortedValues[size / 2]
        }
    }
    
    /**
     * Calculates the standard deviation of a list of values.
     * 
     * @param values List of values
     * @return Standard deviation, or null if the list is empty
     */
    fun calculateStandardDeviation(values: List<Float>): Float? {
        if (values.isEmpty()) return null
        
        val mean = values.average().toFloat()
        val variance = values.map { (it - mean) * (it - mean) }.sum() / values.size
        
        return sqrt(variance)
    }
    
    /**
     * Calculates the interquartile range (IQR) of a list of values.
     * 
     * @param values List of values
     * @return Pair of (Q1, Q3) values, or null if the list is empty
     */
    fun calculateQuartiles(values: List<Float>): Pair<Float, Float>? {
        if (values.isEmpty()) return null
        
        val sortedValues = values.sorted()
        val size = sortedValues.size
        
        val q1Index = (size * 0.25f).toInt()
        val q3Index = (size * 0.75f).toInt()
        
        return Pair(sortedValues[q1Index], sortedValues[q3Index])
    }
    
    /**
     * Determines if a value is within a specified range.
     * 
     * @param value Value to check
     * @param min Minimum value (inclusive)
     * @param max Maximum value (inclusive)
     * @return True if the value is within the range, false otherwise
     */
    fun isInRange(value: Float, min: Float, max: Float): Boolean {
        return value in min..max
    }
    
    /**
     * Determines if a value is within a specified range.
     * 
     * @param value Value to check
     * @param min Minimum value (inclusive)
     * @param max Maximum value (inclusive)
     * @return True if the value is within the range, false otherwise
     */
    fun isInRange(value: Long, min: Long, max: Long): Boolean {
        return value in min..max
    }
    
    /**
     * Constrains a value to a specified range.
     * 
     * @param value Value to constrain
     * @param min Minimum value
     * @param max Maximum value
     * @return Constrained value
     */
    fun constrain(value: Float, min: Float, max: Float): Float {
        return value.coerceIn(min, max)
    }
}