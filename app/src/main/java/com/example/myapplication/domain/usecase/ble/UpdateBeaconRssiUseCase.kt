package com.example.myapplication.domain.usecase.ble

import com.example.myapplication.data.repository.IBeaconRepository
import com.example.myapplication.domain.usecase.CompletableUseCase
import com.example.myapplication.domain.usecase.UseCase
import timber.log.Timber

/**
 * Use case for updating the RSSI value for a beacon.
 */
class UpdateBeaconRssiUseCase(
    private val beaconRepository: IBeaconRepository
) : CompletableUseCase<UpdateBeaconRssiUseCase.Params> {
    
    override suspend fun invoke(params: Params) {
        Timber.d("Updating RSSI for beacon ${params.macAddress}: ${params.rssi} dBm")
        beaconRepository.updateRssi(
            macAddress = params.macAddress,
            rssi = params.rssi,
            timestamp = params.timestamp ?: System.currentTimeMillis()
        )
    }
    
    /**
     * Parameters for the UpdateBeaconRssiUseCase.
     *
     * @param macAddress MAC address of the beacon
     * @param rssi New RSSI value in dBm
     * @param timestamp Timestamp of the measurement (optional, defaults to current time)
     */
    data class Params(
        val macAddress: String,
        val rssi: Int,
        val timestamp: Long? = null
    )
}

/**
 * Use case for updating the staleness status for all beacons based on a timeout threshold.
 */
class UpdateBeaconStalenessUseCase(
    private val beaconRepository: IBeaconRepository
) : UseCase<UpdateBeaconStalenessUseCase.Params, Int> {
    
    override suspend fun invoke(params: Params): Int {
        Timber.d("Updating beacon staleness with timeout: ${params.timeoutMs}ms")
        return beaconRepository.updateStaleness(params.timeoutMs)
    }
    
    /**
     * Parameters for the UpdateBeaconStalenessUseCase.
     *
     * @param timeoutMs Timeout threshold in milliseconds
     */
    data class Params(val timeoutMs: Long)
}

/**
 * Use case for updating the transmit power for a beacon.
 */
class UpdateBeaconTxPowerUseCase(
    private val beaconRepository: IBeaconRepository
) : CompletableUseCase<UpdateBeaconTxPowerUseCase.Params> {
    
    override suspend fun invoke(params: Params) {
        Timber.d("Updating TxPower for beacon ${params.macAddress}: ${params.txPower} dBm")
        beaconRepository.updateTxPower(params.macAddress, params.txPower)
    }
    
    /**
     * Parameters for the UpdateBeaconTxPowerUseCase.
     *
     * @param macAddress MAC address of the beacon
     * @param txPower New transmit power value in dBm
     */
    data class Params(val macAddress: String, val txPower: Int)
}