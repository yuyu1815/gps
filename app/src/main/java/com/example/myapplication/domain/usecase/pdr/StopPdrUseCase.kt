package com.example.myapplication.domain.usecase.pdr

import com.example.myapplication.domain.usecase.NoParamsCompletableUseCase
import timber.log.Timber

/**
 * Use case for stopping Pedestrian Dead Reckoning (PDR).
 * This terminates the process of tracking user movement using device sensors.
 */
class StopPdrUseCase : NoParamsCompletableUseCase {
    
    override suspend fun invoke() {
        Timber.d("Stopping Pedestrian Dead Reckoning")
        // The actual PDR termination implementation will be added when implementing the sensor listeners
        // This use case serves as a business logic entry point
    }
}