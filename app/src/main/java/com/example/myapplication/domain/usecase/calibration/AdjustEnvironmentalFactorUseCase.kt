package com.example.myapplication.domain.usecase.calibration

import com.example.myapplication.domain.model.Beacon
import com.example.myapplication.domain.usecase.UseCase
import timber.log.Timber
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.pow

/**
 * Use case for adjusting the environmental factor (path loss exponent) based on known distances.
 * 
 * The environmental factor is a key parameter in the log-distance path loss model:
 * d = 10^((TxPower - RSSI) / (10 * n))
 * 
 * where n is the environmental factor (path loss exponent).
 * 
 * This use case calculates the optimal environmental factor that minimizes the error
 * between the estimated distances and the known actual distances.
 */
class AdjustEnvironmentalFactorUseCase : UseCase<AdjustEnvironmentalFactorUseCase.Params, AdjustEnvironmentalFactorUseCase.Result> {
    
    override suspend fun invoke(params: Params): Result {
        Timber.d("Adjusting environmental factor based on ${params.calibrationPoints.size} calibration points")
        
        if (params.calibrationPoints.isEmpty()) {
            Timber.w("No calibration points provided")
            return Result(
                environmentalFactor = DEFAULT_ENVIRONMENTAL_FACTOR,
                averageError = 0f,
                calibrationPoints = emptyList()
            )
        }
        
        // Try different environmental factors and find the one with the lowest average error
        var bestFactor = DEFAULT_ENVIRONMENTAL_FACTOR
        var lowestError = Float.MAX_VALUE
        val results = mutableListOf<CalibrationPointResult>()
        
        // Try environmental factors from MIN_FACTOR to MAX_FACTOR with STEP_SIZE increments
        for (factor in generateSequence(MIN_FACTOR) { it + STEP_SIZE }.takeWhile { it <= MAX_FACTOR }) {
            val pointResults = params.calibrationPoints.map { point ->
                // Calculate estimated distance using the current factor
                val ratio = (point.beacon.txPower - point.beacon.filteredRssi) / (10 * factor)
                val estimatedDistance = 10.0.pow(ratio.toDouble()).toFloat()
                
                // Calculate error
                val error = abs(estimatedDistance - point.actualDistance)
                
                CalibrationPointResult(
                    beacon = point.beacon,
                    actualDistance = point.actualDistance,
                    estimatedDistance = estimatedDistance,
                    error = error
                )
            }
            
            // Calculate average error for this factor
            val avgError = pointResults.map { it.error }.average().toFloat()
            
            if (avgError < lowestError) {
                lowestError = avgError
                bestFactor = factor
                results.clear()
                results.addAll(pointResults)
            }
            
            Timber.d("Tried factor: $factor, average error: $avgError meters")
        }
        
        Timber.d("Best environmental factor: $bestFactor with average error: $lowestError meters")
        
        return Result(
            environmentalFactor = bestFactor,
            averageError = lowestError,
            calibrationPoints = results
        )
    }
    
    /**
     * Parameters for the AdjustEnvironmentalFactorUseCase.
     *
     * @param calibrationPoints List of calibration points with known actual distances
     */
    data class Params(
        val calibrationPoints: List<CalibrationPoint>
    )
    
    /**
     * Result of the environmental factor adjustment.
     *
     * @param environmentalFactor The adjusted environmental factor
     * @param averageError The average error in meters with the adjusted factor
     * @param calibrationPoints List of calibration point results with estimated distances
     */
    data class Result(
        val environmentalFactor: Float,
        val averageError: Float,
        val calibrationPoints: List<CalibrationPointResult>
    )
    
    /**
     * Represents a calibration point with a beacon and its known actual distance.
     *
     * @param beacon The beacon
     * @param actualDistance The known actual distance to the beacon in meters
     */
    data class CalibrationPoint(
        val beacon: Beacon,
        val actualDistance: Float
    )
    
    /**
     * Result for a calibration point after adjustment.
     *
     * @param beacon The beacon
     * @param actualDistance The known actual distance in meters
     * @param estimatedDistance The estimated distance using the adjusted factor
     * @param error The absolute error between actual and estimated distances
     */
    data class CalibrationPointResult(
        val beacon: Beacon,
        val actualDistance: Float,
        val estimatedDistance: Float,
        val error: Float
    )
    
    companion object {
        /**
         * Default environmental factor (path loss exponent) for indoor environments.
         */
        const val DEFAULT_ENVIRONMENTAL_FACTOR = 2.0f
        
        /**
         * Minimum environmental factor to try during adjustment.
         */
        const val MIN_FACTOR = 1.5f
        
        /**
         * Maximum environmental factor to try during adjustment.
         */
        const val MAX_FACTOR = 4.0f
        
        /**
         * Step size for environmental factor adjustment.
         */
        const val STEP_SIZE = 0.1f
    }
}

/**
 * Use case for calculating the environmental factor directly from a known distance and RSSI.
 * 
 * Using the log-distance path loss model:
 * d = 10^((TxPower - RSSI) / (10 * n))
 * 
 * We can solve for n:
 * n = (TxPower - RSSI) / (10 * log10(d))
 * 
 * This is useful for quick calibration when you have a single known distance.
 */
class CalculateEnvironmentalFactorUseCase : UseCase<CalculateEnvironmentalFactorUseCase.Params, Float> {
    
    override suspend fun invoke(params: Params): Float {
        Timber.d("Calculating environmental factor for beacon ${params.beacon.macAddress}")
        
        if (params.actualDistance <= 0) {
            Timber.w("Invalid actual distance: ${params.actualDistance}")
            return AdjustEnvironmentalFactorUseCase.DEFAULT_ENVIRONMENTAL_FACTOR
        }
        
        // Use filtered RSSI if available, otherwise use last RSSI
        val rssi = if (params.useFilteredRssi) params.beacon.filteredRssi else params.beacon.lastRssi
        
        if (rssi == 0) {
            Timber.w("Invalid RSSI: $rssi")
            return AdjustEnvironmentalFactorUseCase.DEFAULT_ENVIRONMENTAL_FACTOR
        }
        
        // Calculate environmental factor using the formula: n = (TxPower - RSSI) / (10 * log10(d))
        val numerator = params.beacon.txPower - rssi
        val denominator = 10 * log10(params.actualDistance.toDouble())
        
        val environmentalFactor = (numerator / denominator).toFloat()
        
        // Clamp the result to a reasonable range
        val clampedFactor = environmentalFactor.coerceIn(
            AdjustEnvironmentalFactorUseCase.MIN_FACTOR,
            AdjustEnvironmentalFactorUseCase.MAX_FACTOR
        )
        
        Timber.d("Calculated environmental factor: $clampedFactor for beacon at ${params.actualDistance}m with RSSI $rssi")
        
        return clampedFactor
    }
    
    /**
     * Parameters for the CalculateEnvironmentalFactorUseCase.
     *
     * @param beacon The beacon
     * @param actualDistance The known actual distance to the beacon in meters
     * @param useFilteredRssi Whether to use filtered RSSI (true) or last RSSI (false)
     */
    data class Params(
        val beacon: Beacon,
        val actualDistance: Float,
        val useFilteredRssi: Boolean = true
    )
}