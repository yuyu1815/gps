package com.example.myapplication.domain.usecase.ble

import com.example.myapplication.data.repository.IBeaconRepository
import com.example.myapplication.domain.usecase.NoParamsCompletableUseCase
import timber.log.Timber

/**
 * Use case for stopping BLE beacon scanning.
 * This terminates the scanning process for nearby BLE beacons.
 */
class StopBleScanningUseCase(
    private val beaconRepository: IBeaconRepository
) : NoParamsCompletableUseCase {
    
    override suspend fun invoke() {
        Timber.d("Stopping BLE scanning")
        // The actual scanning termination implementation will be added when implementing the BLE scanner
        // This use case serves as a business logic entry point
    }
}