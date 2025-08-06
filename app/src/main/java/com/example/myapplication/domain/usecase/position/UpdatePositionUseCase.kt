package com.example.myapplication.domain.usecase.position

import com.example.myapplication.data.repository.IPositionRepository
import com.example.myapplication.domain.model.UserPosition
import com.example.myapplication.domain.usecase.CompletableUseCase
import timber.log.Timber

/**
 * Use case for updating the current user position.
 */
class UpdatePositionUseCase(
    private val positionRepository: IPositionRepository
) : CompletableUseCase<UserPosition> {
    
    override suspend fun invoke(params: UserPosition) {
        Timber.d("Updating position to (${params.x}, ${params.y})")
        positionRepository.updatePosition(params)
    }
}