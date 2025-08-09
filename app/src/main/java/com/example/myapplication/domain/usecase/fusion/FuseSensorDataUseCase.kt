package com.example.myapplication.domain.usecase.fusion

import com.example.myapplication.data.repository.IPositionRepository
import com.example.myapplication.domain.model.ExtendedKalmanFilter
import com.example.myapplication.domain.model.Measurement
import com.example.myapplication.domain.model.Motion
import com.example.myapplication.domain.model.PositionSource
import com.example.myapplication.domain.model.UserPosition
import com.example.myapplication.domain.usecase.UseCase
import org.apache.commons.math3.linear.Array2DRowRealMatrix
import timber.log.Timber
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Use case for fusing position data from multiple sources (SLAM, Wi-Fi, and PDR).
 * Implements an Extended Kalman Filter (EKF) for robust sensor fusion.
 */
class FuseSensorDataUseCase(
    private val positionRepository: IPositionRepository
) : UseCase<FuseSensorDataUseCase.Params, UserPosition> {

    private val ekf = ExtendedKalmanFilter()
    private var lastTimestamp: Long = 0

    override suspend fun invoke(params: Params): UserPosition {
        val slamMotion = params.slamMotion
        val wifiPosition = params.wifiPosition
        val pdrMotion = params.pdrMotion

        if (!ekf.isInitialized()) {
            if (UserPosition.isValid(wifiPosition)) {
                Timber.d("Initializing EKF with Wi-Fi position")
                val initialState = Array2DRowRealMatrix(doubleArrayOf(wifiPosition.x.toDouble(), wifiPosition.y.toDouble(), 0.0))
                val initialCovariance = Array2DRowRealMatrix(arrayOf(
                    doubleArrayOf(wifiPosition.accuracy.toDouble() * wifiPosition.accuracy.toDouble(), 0.0, 0.0),
                    doubleArrayOf(0.0, wifiPosition.accuracy.toDouble() * wifiPosition.accuracy.toDouble(), 0.0),
                    doubleArrayOf(0.0, 0.0, 1.0)
                ))
                ekf.initialize(initialState, initialCovariance)
                lastTimestamp = System.currentTimeMillis()
                return wifiPosition
            } else {
                return UserPosition.invalid()
            }
        }

        val currentTime = System.currentTimeMillis()
        val dt = (currentTime - lastTimestamp) / 1000.0
        lastTimestamp = currentTime

        // Combine SLAM and PDR motion data for prediction with weighted fusion
        val combinedMotion = if (pdrMotion != null) {
            // Calculate weights based on motion confidence
            val motionConfidenceValue = params.motionConfidence ?: 0.7f
            
            // Adjust weights based on motion confidence
            // Higher SLAM weight when confidence is high, higher PDR weight when confidence is low
            val slamWeight = motionConfidenceValue
            val pdrWeight = 1.0 - motionConfidenceValue
            
            // Apply weighted fusion of motion data
            Motion(
                velocity = (slamMotion.velocity * slamWeight + pdrMotion.velocity * pdrWeight).toDouble(),
                angularVelocity = (slamMotion.angularVelocity * slamWeight + pdrMotion.angularVelocity * pdrWeight).toDouble()
            )
        } else {
            slamMotion
        }
        
        Timber.d("Motion fusion: SLAM (v=${slamMotion.velocity}, ω=${slamMotion.angularVelocity}), " +
                "PDR (v=${pdrMotion?.velocity ?: 0.0}, ω=${pdrMotion?.angularVelocity ?: 0.0}), " +
                "Combined (v=${combinedMotion.velocity}, ω=${combinedMotion.angularVelocity})")

        // Set process noise parameters based on motion confidence
        val motionConfidence = params.motionConfidence ?: 0.7f
        val positionNoise = 0.05 * (2.0 - motionConfidence)
        val headingNoise = 0.02 * (2.0 - motionConfidence)
        ekf.setProcessNoiseParameters(positionNoise, headingNoise)
        
        // Set drift correction parameters based on Wi-Fi accuracy
        val wifiAccuracy = if (UserPosition.isValid(wifiPosition)) wifiPosition.accuracy.toDouble() else 5.0
        val driftThreshold = wifiAccuracy * 1.5 // Threshold scales with Wi-Fi accuracy
        val driftFactor = 0.3 * wifiPosition.confidence // Factor scales with Wi-Fi confidence
        ekf.setDriftCorrectionParameters(driftThreshold, driftFactor.toDouble())
        
        // Prediction step using combined motion data
        ekf.predict(combinedMotion, dt)
        Timber.d("EKF prediction step completed")

        // Update step with Wi-Fi position measurement
        if (UserPosition.isValid(wifiPosition)) {
            // Adaptive covariance: smaller when confidence is high and accuracy small
            val baseVar = wifiPosition.accuracy.toDouble() * wifiPosition.accuracy.toDouble()
            val conf = wifiPosition.confidence.coerceIn(0.1f, 1.0f).toDouble()
            val scaledVar = (baseVar / conf).coerceAtMost(100.0)
            val measurementCovariance = Array2DRowRealMatrix(arrayOf(
                doubleArrayOf(scaledVar, 0.0),
                doubleArrayOf(0.0, scaledVar)
            ))
            val measurement = Measurement(wifiPosition.x.toDouble(), wifiPosition.y.toDouble(), measurementCovariance)
            ekf.update(measurement)
            Timber.d("EKF update step completed")
            
            // Apply drift correction with adaptive frequency based on Wi-Fi position quality
            val wifiPositionQuality = if (UserPosition.isValid(wifiPosition)) {
                // Calculate quality metric based on accuracy and confidence
                // Higher quality = lower accuracy value and higher confidence
                val accuracyFactor = (10.0f / wifiPosition.accuracy).coerceIn(0.1f, 1.0f)
                val qualityMetric = accuracyFactor * wifiPosition.confidence
                qualityMetric
            } else {
                0.0f
            }
        
            // Determine correction frequency based on Wi-Fi position quality
            // Higher quality = more frequent corrections
            val correctionPeriod = when {
                wifiPositionQuality > 0.8f -> 5000L  // Every 5 seconds for high quality
                wifiPositionQuality > 0.5f -> 8000L  // Every 8 seconds for medium quality
                wifiPositionQuality > 0.2f -> 12000L // Every 12 seconds for low quality
                else -> 15000L                       // Every 15 seconds for very low quality
            }
        
            if (currentTime % correctionPeriod < 100) {
                ekf.applyDriftCorrection()
                Timber.d("Drift correction applied (quality: $wifiPositionQuality, period: ${correctionPeriod}ms)")
            }
        }

        val fusedState = ekf.getState()
        val fusedCovariance = ekf.getCovariance()

        val varX = fusedCovariance.getEntry(0, 0).coerceAtLeast(1e-4)
        val varY = fusedCovariance.getEntry(1, 1).coerceAtLeast(1e-4)
        val sigmaX = kotlin.math.sqrt(varX).toFloat()
        val sigmaY = kotlin.math.sqrt(varY).toFloat()
        val fusedPosition = UserPosition(
            x = fusedState.getEntry(0, 0).toFloat(),
            y = fusedState.getEntry(1, 0).toFloat(),
            accuracy = sqrt(varX + varY).toFloat(),
            timestamp = currentTime,
            source = PositionSource.FUSION,
            confidence = calculateConfidence(fusedCovariance),
            sigmaX = sigmaX,
            sigmaY = sigmaY,
            sigmaTheta = kotlin.math.sqrt(fusedCovariance.getEntry(2, 2).coerceAtLeast(1e-6)).toFloat()
        )

        if (params.updateRepository) {
            positionRepository.updatePosition(fusedPosition)
        }

        Timber.d("Fused position: (${fusedPosition.x}, ${fusedPosition.y}), accuracy: ${fusedPosition.accuracy}m, confidence: ${fusedPosition.confidence}")
        return fusedPosition
    }

    private fun calculateConfidence(covariance: org.apache.commons.math3.linear.RealMatrix): Float {
        val positionUncertainty = sqrt(covariance.getEntry(0, 0) + covariance.getEntry(1, 1)).toFloat()
        return (1.0f - min(1.0f, positionUncertainty / 10.0f)).coerceIn(0.1f, 1.0f)
    }

    fun reset() {
        // The EKF will be re-initialized when a valid Wi-Fi position is received.
    }

    data class Params(
        val slamMotion: Motion,
        val wifiPosition: UserPosition,
        val pdrMotion: Motion? = null,
        val motionConfidence: Float? = null,
        val updateRepository: Boolean = true
    )
}
