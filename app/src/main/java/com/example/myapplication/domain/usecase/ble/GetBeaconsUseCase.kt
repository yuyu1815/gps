package com.example.myapplication.domain.usecase.ble

import com.example.myapplication.data.repository.IBeaconRepository
import com.example.myapplication.domain.model.Beacon
import com.example.myapplication.domain.usecase.NoParamsUseCase
import com.example.myapplication.domain.usecase.UseCase
import kotlinx.coroutines.flow.Flow
import timber.log.Timber

/**
 * Use case for getting all beacons.
 */
class GetBeaconsUseCase(
    private val beaconRepository: IBeaconRepository
) : NoParamsUseCase<List<Beacon>> {
    
    override suspend fun invoke(): List<Beacon> {
        Timber.d("Getting all beacons")
        return beaconRepository.getAll()
    }
}

/**
 * Use case for getting a flow of all beacons, which emits updates when the beacon data changes.
 */
class GetBeaconsFlowUseCase(
    private val beaconRepository: IBeaconRepository
) : NoParamsUseCase<Flow<List<Beacon>>> {
    
    override suspend fun invoke(): Flow<List<Beacon>> {
        Timber.d("Getting beacons flow")
        return beaconRepository.getBeaconsFlow()
    }
}

/**
 * Use case for getting non-stale beacons.
 */
class GetNonStaleBeaconsUseCase(
    private val beaconRepository: IBeaconRepository
) : NoParamsUseCase<List<Beacon>> {
    
    override suspend fun invoke(): List<Beacon> {
        Timber.d("Getting non-stale beacons")
        return beaconRepository.getNonStaleBeacons()
    }
}

/**
 * Use case for getting beacons within a specified radius from a position.
 */
class GetBeaconsInRadiusUseCase(
    private val beaconRepository: IBeaconRepository
) : UseCase<GetBeaconsInRadiusUseCase.Params, List<Beacon>> {
    
    override suspend fun invoke(params: Params): List<Beacon> {
        Timber.d("Getting beacons in radius: ${params.radiusMeters}m from (${params.x}, ${params.y})")
        return beaconRepository.getBeaconsInRadius(params.x, params.y, params.radiusMeters)
    }
    
    /**
     * Parameters for the GetBeaconsInRadiusUseCase.
     *
     * @param x X coordinate in meters
     * @param y Y coordinate in meters
     * @param radiusMeters Radius in meters
     */
    data class Params(val x: Float, val y: Float, val radiusMeters: Float)
}