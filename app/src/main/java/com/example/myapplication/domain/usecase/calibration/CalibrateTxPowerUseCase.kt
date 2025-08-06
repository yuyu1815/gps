package com.example.myapplication.domain.usecase.calibration

import com.example.myapplication.data.repository.IBeaconRepository
import com.example.myapplication.domain.model.Beacon
import com.example.myapplication.domain.usecase.UseCase
import timber.log.Timber

/**
 * Use case for calibrating the TxPower of a beacon.
 * 
 * TxPower calibration is done by measuring the RSSI at a known distance (typically 1 meter)
 * and then setting the TxPower value to the measured RSSI.
 * 
 * This allows for more accurate distance calculations using the log-distance path loss model.
 */
class CalibrateTxPowerUseCase(
    private val beaconRepository: IBeaconRepository
) : UseCase<CalibrateTxPowerUseCase.Params, Int> {
    
    override suspend fun invoke(params: Params): Int {
        Timber.d("Calibrating TxPower for beacon ${params.beacon.macAddress}")
        
        // If we don't have enough RSSI samples, return the current TxPower
        if (params.rssiSamples.isEmpty()) {
            Timber.w("No RSSI samples provided for calibration")
            return params.beacon.txPower
        }
        
        // Calculate the average RSSI from the samples
        val averageRssi = params.rssiSamples.average().toInt()
        
        // Calculate the calibrated TxPower using the log-distance path loss model
        // TxPower = RSSI + 10 * n * log10(d)
        // where d is the calibration distance (typically 1 meter)
        // and n is the path loss exponent (typically 2-4 for indoor environments)
        val calibratedTxPower = if (params.calibrationDistance == 1.0f) {
            // At exactly 1 meter, TxPower = RSSI (since log10(1) = 0)
            averageRssi
        } else {
            // For other distances, apply the formula
            val pathLossFactor = 10 * params.environmentalFactor * Math.log10(params.calibrationDistance.toDouble())
            (averageRssi + pathLossFactor).toInt()
        }
        
        Timber.d("Calibrated TxPower: $calibratedTxPower (from average RSSI: $averageRssi at ${params.calibrationDistance}m)")
        
        // Update the beacon's TxPower in the repository
        beaconRepository.updateTxPower(params.beacon.macAddress, calibratedTxPower)
        
        return calibratedTxPower
    }
    
    /**
     * Parameters for the CalibrateTxPowerUseCase.
     *
     * @param beacon The beacon to calibrate
     * @param rssiSamples List of RSSI samples measured at the calibration distance
     * @param calibrationDistance The distance at which the RSSI samples were measured (typically 1 meter)
     * @param environmentalFactor The environmental factor (path loss exponent)
     */
    data class Params(
        val beacon: Beacon,
        val rssiSamples: List<Int>,
        val calibrationDistance: Float = 1.0f,
        val environmentalFactor: Float = 2.0f
    )
}

/**
 * Use case for collecting RSSI samples for TxPower calibration.
 * 
 * This use case collects a specified number of RSSI samples for a beacon
 * and returns the list of samples for use in calibration.
 */
class CollectCalibrationSamplesUseCase(
    private val beaconRepository: IBeaconRepository
) : UseCase<CollectCalibrationSamplesUseCase.Params, List<Int>> {
    
    override suspend fun invoke(params: Params): List<Int> {
        Timber.d("Collecting ${params.sampleCount} RSSI samples for beacon ${params.macAddress}")
        
        val samples = mutableListOf<Int>()
        val startTime = System.currentTimeMillis()
        
        // Continue collecting samples until we have enough or timeout
        while (samples.size < params.sampleCount && 
               System.currentTimeMillis() - startTime < params.timeoutMs) {
            
            // Get the latest RSSI for the beacon
            val beacon = beaconRepository.getBeacon(params.macAddress)
            
            // If the beacon exists and has a valid RSSI, add it to the samples
            if (beacon != null && beacon.lastRssi != 0) {
                samples.add(beacon.lastRssi)
                Timber.d("Added RSSI sample: ${beacon.lastRssi} dBm (${samples.size}/${params.sampleCount})")
                
                // Wait for the specified interval before collecting the next sample
                kotlinx.coroutines.delay(params.sampleIntervalMs)
            } else {
                // If the beacon doesn't exist or has no RSSI, wait a shorter time before checking again
                kotlinx.coroutines.delay(100)
            }
        }
        
        if (samples.size < params.sampleCount) {
            Timber.w("Timed out while collecting RSSI samples. Collected ${samples.size}/${params.sampleCount}")
        } else {
            Timber.d("Successfully collected ${samples.size} RSSI samples")
        }
        
        return samples
    }
    
    /**
     * Parameters for the CollectCalibrationSamplesUseCase.
     *
     * @param macAddress The MAC address of the beacon to collect samples for
     * @param sampleCount The number of RSSI samples to collect
     * @param sampleIntervalMs The interval between samples in milliseconds
     * @param timeoutMs The maximum time to spend collecting samples in milliseconds
     */
    data class Params(
        val macAddress: String,
        val sampleCount: Int = 10,
        val sampleIntervalMs: Long = 500,
        val timeoutMs: Long = 30000 // 30 seconds
    )
}