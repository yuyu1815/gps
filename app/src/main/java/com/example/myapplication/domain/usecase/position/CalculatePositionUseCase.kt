package com.example.myapplication.domain.usecase.position

import com.example.myapplication.data.repository.IBeaconRepository
import com.example.myapplication.data.repository.IPositionRepository
import com.example.myapplication.domain.model.Beacon
import com.example.myapplication.domain.model.PositionSource
import com.example.myapplication.domain.model.UserPosition
import com.example.myapplication.domain.usecase.UseCase
import timber.log.Timber
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Use case for calculating the user's position using triangulation from beacon distances.
 * Implements a least squares method for position estimation.
 */
class CalculatePositionUseCase(
    private val beaconRepository: IBeaconRepository,
    private val positionRepository: IPositionRepository
) : UseCase<CalculatePositionUseCase.Params, UserPosition> {
    
    override suspend fun invoke(params: Params): UserPosition {
        Timber.d("Calculating position using triangulation")
        
        // Get non-stale beacons
        val beacons = if (params.beacons.isNotEmpty()) {
            params.beacons
        } else {
            beaconRepository.getNonStaleBeacons()
        }
        
        // Need at least 3 beacons for triangulation
        if (beacons.size < 3) {
            Timber.w("Not enough beacons for triangulation (${beacons.size} available, need at least 3)")
            return UserPosition.invalid()
        }
        
        // Calculate position using least squares method
        val position = calculatePositionLeastSquares(beacons)
        
        // Update the position in the repository if requested
        if (params.updateRepository) {
            positionRepository.updatePosition(position)
        }
        
        return position
    }
    
    /**
     * Calculates the user's position using the least squares method.
     * This method minimizes the sum of squared errors between the measured distances
     * and the calculated distances based on the estimated position.
     */
    private fun calculatePositionLeastSquares(beacons: List<Beacon>): UserPosition {
        // Initial position estimate (average of beacon positions)
        var x = beacons.map { it.x }.average().toFloat()
        var y = beacons.map { it.y }.average().toFloat()
        
        // Iterative least squares optimization
        val maxIterations = 10
        val convergenceThreshold = 0.1f
        var iteration = 0
        var delta = Float.MAX_VALUE
        
        while (iteration < maxIterations && delta > convergenceThreshold) {
            // Calculate Jacobian matrix and residuals
            val jacobian = Array(beacons.size) { FloatArray(2) }
            val residuals = FloatArray(beacons.size)
            
            for (i in beacons.indices) {
                val beacon = beacons[i]
                val dx = x - beacon.x
                val dy = y - beacon.y
                val calculatedDistance = sqrt(dx * dx + dy * dy)
                val measuredDistance = beacon.estimatedDistance
                
                // Avoid division by zero
                if (calculatedDistance > 0.1f) {
                    jacobian[i][0] = dx / calculatedDistance
                    jacobian[i][1] = dy / calculatedDistance
                } else {
                    jacobian[i][0] = 0f
                    jacobian[i][1] = 0f
                }
                
                residuals[i] = calculatedDistance - measuredDistance
            }
            
            // Calculate J^T * J and J^T * r
            val jtj = Array(2) { FloatArray(2) }
            val jtr = FloatArray(2)
            
            for (i in 0 until 2) {
                for (j in 0 until 2) {
                    jtj[i][j] = 0f
                    for (k in beacons.indices) {
                        jtj[i][j] += jacobian[k][i] * jacobian[k][j]
                    }
                }
                
                jtr[i] = 0f
                for (k in beacons.indices) {
                    jtr[i] += jacobian[k][i] * residuals[k]
                }
            }
            
            // Solve the 2x2 system (J^T * J) * delta = J^T * r
            val det = jtj[0][0] * jtj[1][1] - jtj[0][1] * jtj[1][0]
            if (det.isNaN() || det < 1e-6f) {
                // Singular matrix, can't solve
                break
            }
            
            val deltaX = (jtj[1][1] * jtr[0] - jtj[0][1] * jtr[1]) / det
            val deltaY = (jtj[0][0] * jtr[1] - jtj[1][0] * jtr[0]) / det
            
            // Update position
            x -= deltaX
            y -= deltaY
            
            // Check convergence
            delta = sqrt(deltaX * deltaX + deltaY * deltaY)
            iteration++
        }
        
        // Calculate accuracy based on residuals
        var sumSquaredResiduals = 0f
        for (beacon in beacons) {
            val dx = x - beacon.x
            val dy = y - beacon.y
            val calculatedDistance = sqrt(dx * dx + dy * dy)
            val measuredDistance = beacon.estimatedDistance
            val residual = calculatedDistance - measuredDistance
            sumSquaredResiduals += residual * residual
        }
        
        val rmse = sqrt(sumSquaredResiduals / beacons.size)
        
        // Calculate confidence based on RMSE and number of beacons
        // Higher confidence with more beacons and lower RMSE
        val beaconCountFactor = min(1f, beacons.size / 6f)
        val rmseFactor = max(0f, 1f - min(1f, rmse / 10f))
        val confidence = beaconCountFactor * rmseFactor
        
        return UserPosition(
            x = x,
            y = y,
            accuracy = rmse,
            timestamp = System.currentTimeMillis(),
            source = PositionSource.BLE,
            confidence = confidence
        )
    }
    
    /**
     * Parameters for the CalculatePositionUseCase.
     *
     * @param beacons List of beacons to use for triangulation (empty to use all non-stale beacons)
     * @param updateRepository Whether to update the position in the repository
     */
    data class Params(
        val beacons: List<Beacon> = emptyList(),
        val updateRepository: Boolean = true
    )
}