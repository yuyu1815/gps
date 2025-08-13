package com.example.myapplication.domain.usecase.pdr

import com.example.myapplication.domain.usecase.NoParamsCompletableUseCase
import timber.log.Timber

/**
 * Use case for starting Pedestrian Dead Reckoning (PDR).
 * This initiates the process of tracking user movement using device sensors.
 */
class StartPdrUseCase : NoParamsCompletableUseCase {
    
    override suspend fun invoke() {
        Timber.d("Starting Pedestrian Dead Reckoning")
        // The actual PDR implementation will be added when implementing the sensor listeners
        // This use case serves as a business logic entry point
    }
}