package com.example.myapplication.domain.usecase.position

import com.example.myapplication.data.repository.IPositionRepository
import com.example.myapplication.domain.model.UserPosition
import com.example.myapplication.domain.usecase.NoParamsUseCase
import timber.log.Timber

/**
 * Use case for getting the current user position.
 */
class GetCurrentPositionUseCase(
    private val positionRepository: IPositionRepository
) : NoParamsUseCase<UserPosition> {
    
    override suspend fun invoke(): UserPosition {
        Timber.d("Getting current position")
        return positionRepository.getCurrentPosition()
    }
}