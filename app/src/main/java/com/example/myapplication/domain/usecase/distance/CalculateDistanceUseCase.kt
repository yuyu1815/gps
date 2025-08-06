package com.example.myapplication.domain.usecase.distance

import com.example.myapplication.data.repository.IBeaconRepository
import com.example.myapplication.domain.model.Beacon
import com.example.myapplication.domain.usecase.UseCase
import timber.log.Timber
import kotlin.math.pow

/**
 * Use case for calculating the distance to a beacon based on RSSI.
 * Uses the log-distance path loss model: d = 10^((TxPower - RSSI) / (10 * n))
 * where n is the path loss exponent (typically 2-4 for indoor environments)
 * 
 * Also calculates a confidence value for the distance estimation based on:
 * 1. RSSI variance (lower variance = higher confidence)
 * 2. Signal strength (stronger signal = higher confidence)
 * 3. Time since last update (more recent = higher confidence)
 */
class CalculateDistanceUseCase(
    private val beaconRepository: IBeaconRepository
) : UseCase<CalculateDistanceUseCase.Params, CalculateDistanceUseCase.DistanceResult> {
    
    override suspend fun invoke(params: Params): CalculateDistanceUseCase.DistanceResult {
        Timber.d("Calculating distance for beacon ${params.beacon.macAddress} with RSSI ${params.beacon.lastRssi} dBm")
        
        // If RSSI is 0, it means no signal was received
        if (params.beacon.lastRssi == 0) {
            return CalculateDistanceUseCase.DistanceResult(Float.MAX_VALUE, 0f)
        }
        
        // Use the beacon's calculateDistance method which also computes confidence
        val distance = params.beacon.calculateDistance(
            environmentalFactor = params.environmentalFactor,
            useFilteredRssi = params.useFilteredRssi,
            currentTime = params.currentTime
        )
        
        // Update the beacon's estimated distance in the repository
        beaconRepository.updateDistance(params.beacon.macAddress, distance)
        
        return CalculateDistanceUseCase.DistanceResult(distance, params.beacon.distanceConfidence)
    }
    
    /**
     * Result of distance calculation, including the distance and confidence level.
     *
     * @param distance The calculated distance in meters
     * @param confidence The confidence level of the distance estimation (0.0 to 1.0)
     */
    data class DistanceResult(
        val distance: Float,
        val confidence: Float
    )
    
    /**
     * Parameters for the CalculateDistanceUseCase.
     *
     * @param beacon The beacon to calculate distance for
     * @param environmentalFactor The environmental factor (path loss exponent)
     * @param useFilteredRssi Whether to use filtered RSSI values (true) or raw values (false)
     * @param currentTime Current time in milliseconds, used for confidence calculation
     */
    data class Params(
        val beacon: Beacon,
        val environmentalFactor: Float = 2.0f,
        val useFilteredRssi: Boolean = true,
        val currentTime: Long = System.currentTimeMillis()
    )
}

/**
 * Use case for calculating distances for all non-stale beacons.
 * Also provides confidence levels for each distance estimation.
 */
class CalculateAllDistancesUseCase(
    private val beaconRepository: IBeaconRepository,
    private val calculateDistanceUseCase: CalculateDistanceUseCase
) : UseCase<CalculateAllDistancesUseCase.Params, List<CalculateAllDistancesUseCase.BeaconDistanceResult>> {
    
    override suspend fun invoke(params: Params): List<CalculateAllDistancesUseCase.BeaconDistanceResult> {
        Timber.d("Calculating distances for all non-stale beacons")
        
        val beacons = beaconRepository.getNonStaleBeacons()
        val results = mutableListOf<CalculateAllDistancesUseCase.BeaconDistanceResult>()
        
        beacons.forEach { beacon ->
            val result = calculateDistanceUseCase(
                CalculateDistanceUseCase.Params(
                    beacon = beacon,
                    environmentalFactor = params.environmentalFactor,
                    useFilteredRssi = params.useFilteredRssi
                )
            )
            Timber.d("Beacon ${beacon.macAddress}: distance = ${result.distance} m, confidence = ${result.confidence}")
            
            results.add(CalculateAllDistancesUseCase.BeaconDistanceResult(beacon, result.distance, result.confidence))
        }
        
        return results
    }
    
    /**
     * Result of distance calculation for a specific beacon.
     *
     * @param beacon The beacon
     * @param distance The calculated distance in meters
     * @param confidence The confidence level of the distance estimation (0.0 to 1.0)
     */
    data class BeaconDistanceResult(
        val beacon: Beacon,
        val distance: Float,
        val confidence: Float
    )
    
    /**
     * Parameters for the CalculateAllDistancesUseCase.
     *
     * @param environmentalFactor The environmental factor (path loss exponent)
     * @param useFilteredRssi Whether to use filtered RSSI values (true) or raw values (false)
     */
    data class Params(
        val environmentalFactor: Float = 2.0f,
        val useFilteredRssi: Boolean = true
    )
}