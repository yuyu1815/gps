package com.example.myapplication.domain.usecase.ble

import com.example.myapplication.data.repository.IBeaconRepository
import com.example.myapplication.domain.usecase.NoParamsCompletableUseCase
import timber.log.Timber

/**
 * Use case for starting BLE beacon scanning.
 * This initiates the scanning process for nearby BLE beacons.
 */
class StartBleScanningUseCase(
    private val beaconRepository: IBeaconRepository
) : NoParamsCompletableUseCase {
    
    override suspend fun invoke() {
        Timber.d("Starting BLE scanning")
        // The actual scanning implementation will be added when implementing the BLE scanner
        // This use case serves as a business logic entry point
    }
}