package com.example.myapplication.domain.usecase.position

import com.example.myapplication.data.repository.IPositionRepository
import com.example.myapplication.domain.model.UserPosition
import com.example.myapplication.domain.usecase.UseCase
import timber.log.Timber

/**
 * Use case for getting the position history.
 */
class GetPositionHistoryUseCase(
    private val positionRepository: IPositionRepository
) : UseCase<GetPositionHistoryUseCase.Params, List<UserPosition>> {
    
    override suspend fun invoke(params: Params): List<UserPosition> {
        Timber.d("Getting position history with limit: ${params.limit}")
        return positionRepository.getPositionHistory(params.limit)
    }
    
    /**
     * Parameters for the GetPositionHistoryUseCase.
     *
     * @param limit Maximum number of positions to return (0 for all)
     */
    data class Params(val limit: Int = 0)
}