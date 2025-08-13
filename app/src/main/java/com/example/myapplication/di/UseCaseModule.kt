package com.example.myapplication.di

import com.example.myapplication.data.repository.IPositionRepository
import com.example.myapplication.domain.usecase.position.GetCurrentPositionUseCase
import com.example.myapplication.domain.usecase.position.GetPositionHistoryUseCase
import com.example.myapplication.domain.usecase.position.UpdatePositionUseCase
import com.example.myapplication.domain.usecase.pdr.DetectStepUseCase
import com.example.myapplication.domain.usecase.pdr.EstimateStepLengthUseCase
import com.example.myapplication.domain.usecase.pdr.KalmanHeadingUseCase
import com.example.myapplication.domain.usecase.pdr.UpdatePdrPositionUseCase
import org.koin.dsl.module

/**
 * Koin module that provides domain use cases.
 */
val useCaseModule = module {
    // BLE-related use cases removed for minimal build

    // Position-related use cases
    factory {
        GetCurrentPositionUseCase(
            positionRepository = get<IPositionRepository>()
        )
    }
    factory {
        GetPositionHistoryUseCase(
            positionRepository = get<IPositionRepository>()
        )
    }
    factory {
        UpdatePositionUseCase(
            positionRepository = get<IPositionRepository>()
        )
    }

    // PDR-related use cases
    factory { DetectStepUseCase() }
    factory { EstimateStepLengthUseCase() }
    factory { KalmanHeadingUseCase() }
    factory {
        UpdatePdrPositionUseCase(
            positionRepository = get<IPositionRepository>()
        )
    }
}


