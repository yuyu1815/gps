package com.example.myapplication.domain.usecase.fusion

import com.example.myapplication.data.repository.IPositionRepository
import com.example.myapplication.domain.model.KalmanFilter
import com.example.myapplication.domain.model.PositionSource
import com.example.myapplication.domain.model.UserPosition
import com.example.myapplication.domain.usecase.UseCase
import timber.log.Timber
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.atan2

/**
 * Use case for fusing position data from multiple sources (BLE and PDR).
 * Implements an enhanced weighted averaging approach based on confidence levels,
 * accuracy, recency, and movement patterns.
 */
class FuseSensorDataUseCase(
    private val positionRepository: IPositionRepository
) : UseCase<FuseSensorDataUseCase.Params, UserPosition> {
    
    // State variables for tracking movement patterns
    private var lastFusedPosition: UserPosition? = null
    private var movementHistory = mutableListOf<MovementData>()
    private val maxHistorySize = 10
    
    // Position prediction settings
    private val defaultPredictionTimeMs = 100L // Default prediction time (100ms)
    
    // Kalman filter for advanced fusion
    private val kalmanFilter = KalmanFilter()
    
    override suspend fun invoke(params: Params): UserPosition {
        val blePosition = params.blePosition
        val pdrPosition = params.pdrPosition
        
        // If only one position is valid, return it with appropriate handling
        if (!UserPosition.isValid(blePosition) && UserPosition.isValid(pdrPosition)) {
            Timber.d("Using PDR position only (BLE position invalid)")
            // When using only PDR, gradually decrease confidence to prevent drift
            val adjustedPosition = if (lastFusedPosition != null) {
                val timeSinceLastBle = System.currentTimeMillis() - (params.lastBleTimestamp ?: 0L)
                val confidenceDecay = min(0.3f, timeSinceLastBle / 30000f) // Max 30% decay after 30 seconds
                pdrPosition.copy(
                    confidence = max(0.3f, pdrPosition.confidence * (1f - confidenceDecay)),
                    accuracy = pdrPosition.accuracy * (1f + confidenceDecay) // Increase accuracy (uncertainty)
                )
            } else {
                pdrPosition
            }
            
            updateMovementHistory(adjustedPosition)
            lastFusedPosition = adjustedPosition
            
            if (params.updateRepository) {
                positionRepository.updatePosition(adjustedPosition)
            }
            
            return adjustedPosition
        }
        
        if (UserPosition.isValid(blePosition) && !UserPosition.isValid(pdrPosition)) {
            Timber.d("Using BLE position only (PDR position invalid)")
            updateMovementHistory(blePosition)
            lastFusedPosition = blePosition
            
            if (params.updateRepository) {
                positionRepository.updatePosition(blePosition)
            }
            
            return blePosition
        }
        
        if (!UserPosition.isValid(blePosition) && !UserPosition.isValid(pdrPosition)) {
            Timber.d("Both positions invalid, returning invalid position")
            return UserPosition.invalid()
        }
        
        // Choose fusion method based on parameter
        val fusedPosition = when (params.fusionMethod) {
            FusionMethod.KALMAN_FILTER -> {
                Timber.d("Using Kalman filter for sensor fusion")
                performKalmanFusion(blePosition, pdrPosition, params.isUserMoving)
            }
            FusionMethod.WEIGHTED_AVERAGE -> {
                Timber.d("Using weighted averaging for sensor fusion")
                performWeightedAverageFusion(
                    blePosition,
                    pdrPosition,
                    params.bleWeight,
                    params.smoothTransition,
                    params.transitionFactor,
                    params.isUserMoving
                )
            }
        }
        
        // Update movement history
        updateMovementHistory(fusedPosition)
        
        // Update last fused position
        lastFusedPosition = fusedPosition
        
        // Apply position prediction if enabled
        val finalPosition = if (params.enablePrediction && params.isUserMoving) {
            // Use provided prediction time or default
            val predictionTime = params.predictionTimeMs ?: defaultPredictionTimeMs
            
            // Only predict if we have enough movement history and user is moving
            if (movementHistory.size >= 2 && isUserMovingSignificantly()) {
                val predictedPosition = predictPosition(predictionTime, fusedPosition)
                Timber.d("Using predicted position for smoother movement")
                predictedPosition
            } else {
                fusedPosition
            }
        } else {
            fusedPosition
        }
        
        // Update the position in the repository if requested
        if (params.updateRepository) {
            positionRepository.updatePosition(finalPosition)
        }
        
        Timber.d("Final position: (${finalPosition.x}, ${finalPosition.y}), accuracy: ${finalPosition.accuracy}m, confidence: ${finalPosition.confidence}")
        return finalPosition
    }
    
    /**
     * Performs weighted average fusion of BLE and PDR positions.
     */
    private fun performWeightedAverageFusion(
        blePosition: UserPosition,
        pdrPosition: UserPosition,
        baseBleWeight: Float,
        smoothTransition: Boolean,
        transitionFactor: Float,
        isUserMoving: Boolean
    ): UserPosition {
        // Calculate distance between BLE and PDR positions
        val positionDifference = calculatePositionDifference(blePosition, pdrPosition)
        
        // Calculate weights based on multiple factors
        val weights = calculateWeights(
            blePosition, 
            pdrPosition, 
            baseBleWeight,
            positionDifference,
            isUserMoving
        )
        val bleWeight = weights.first
        val pdrWeight = weights.second
        
        Timber.d("Fusion weights: BLE=$bleWeight, PDR=$pdrWeight (diff=${positionDifference.distance}m)")
        
        // Perform weighted average fusion with adaptive smoothing
        return if (smoothTransition && lastFusedPosition != null) {
            // Calculate adaptive transition factor based on movement and consistency
            val adaptiveTransitionFactor = calculateAdaptiveTransitionFactor(
                lastFusedPosition!!,
                blePosition,
                pdrPosition,
                transitionFactor,
                positionDifference,
                isUserMoving
            )
            
            // Calculate new position with weighted average
            val newX = blePosition.x * bleWeight + pdrPosition.x * pdrWeight
            val newY = blePosition.y * bleWeight + pdrPosition.y * pdrWeight
            
            // Apply smooth transition with adaptive factor
            val x = lastFusedPosition!!.x * (1 - adaptiveTransitionFactor) + newX * adaptiveTransitionFactor
            val y = lastFusedPosition!!.y * (1 - adaptiveTransitionFactor) + newY * adaptiveTransitionFactor
            
            // Calculate accuracy and confidence
            val accuracy = calculateFusedAccuracy(blePosition, pdrPosition, bleWeight, pdrWeight)
            val confidence = calculateFusedConfidence(blePosition, pdrPosition, bleWeight, pdrWeight, positionDifference)
            
            UserPosition(
                x = x,
                y = y,
                accuracy = accuracy,
                timestamp = System.currentTimeMillis(),
                source = PositionSource.FUSION,
                confidence = confidence
            )
        } else {
            // Direct weighted average without smooth transition
            val fusedPos = blePosition.weightedAverageTo(pdrPosition, pdrWeight)
            
            // Adjust accuracy and confidence based on position difference
            val accuracy = calculateFusedAccuracy(blePosition, pdrPosition, bleWeight, pdrWeight)
            val confidence = calculateFusedConfidence(blePosition, pdrPosition, bleWeight, pdrWeight, positionDifference)
            
            fusedPos.copy(
                accuracy = accuracy,
                confidence = confidence
            )
        }
    }
    
    /**
     * Calculates the difference between two positions.
     */
    private fun calculatePositionDifference(
        position1: UserPosition,
        position2: UserPosition
    ): PositionDifference {
        val distance = position1.distanceTo(position2)
        val relativeDistance = distance / (position1.accuracy + position2.accuracy + 0.1f)
        
        return PositionDifference(
            distance = distance,
            relativeDistance = relativeDistance
        )
    }
    
    /**
     * Calculates weights for BLE and PDR positions based on multiple factors.
     * Returns a Pair of (bleWeight, pdrWeight) where bleWeight + pdrWeight = 1.0
     */
    private fun calculateWeights(
        blePosition: UserPosition,
        pdrPosition: UserPosition,
        baseBleWeight: Float,
        positionDifference: PositionDifference,
        isUserMoving: Boolean
    ): Pair<Float, Float> {
        // Base weights from parameter
        var bleWeight = baseBleWeight
        var pdrWeight = 1.0f - baseBleWeight
        
        // Adjust weights based on confidence
        val confidenceFactor = 0.6f
        bleWeight *= (1.0f + (blePosition.confidence - 0.5f) * confidenceFactor)
        pdrWeight *= (1.0f + (pdrPosition.confidence - 0.5f) * confidenceFactor)
        
        // Adjust weights based on accuracy (lower accuracy = lower weight)
        val accuracyFactor = 0.4f
        val bleAccuracyAdjustment = max(0.2f, 1f - blePosition.accuracy * accuracyFactor)
        val pdrAccuracyAdjustment = max(0.2f, 1f - pdrPosition.accuracy * accuracyFactor)
        
        bleWeight *= bleAccuracyAdjustment
        pdrWeight *= pdrAccuracyAdjustment
        
        // Adjust weights based on recency (older = lower weight)
        val recencyFactor = 0.3f
        val currentTime = System.currentTimeMillis()
        val bleAge = (currentTime - blePosition.timestamp) / 1000f // in seconds
        val pdrAge = (currentTime - pdrPosition.timestamp) / 1000f // in seconds
        
        val bleRecencyAdjustment = max(0.1f, 1f - bleAge * recencyFactor)
        val pdrRecencyAdjustment = max(0.1f, 1f - pdrAge * recencyFactor)
        
        bleWeight *= bleRecencyAdjustment
        pdrWeight *= pdrRecencyAdjustment
        
        // Adjust weights based on movement state
        if (isUserMoving) {
            // When moving, gradually increase PDR weight for smoother tracking
            pdrWeight *= 1.2f
        } else {
            // When stationary, prefer BLE for absolute positioning
            bleWeight *= 1.3f
        }
        
        // Adjust weights based on position difference
        // If positions are very different, favor the more confident source
        if (positionDifference.relativeDistance > 2.0f) {
            val consistencyFactor = 0.7f
            if (blePosition.confidence > pdrPosition.confidence) {
                bleWeight *= (1.0f + consistencyFactor)
            } else {
                pdrWeight *= (1.0f + consistencyFactor)
            }
        }
        
        // Normalize weights to sum to 1.0
        val totalWeight = bleWeight + pdrWeight
        if (totalWeight > 0) {
            bleWeight /= totalWeight
            pdrWeight /= totalWeight
        } else {
            // If both weights are 0, use base weights
            bleWeight = baseBleWeight
            pdrWeight = 1.0f - baseBleWeight
        }
        
        return Pair(bleWeight, pdrWeight)
    }
    
    /**
     * Calculates an adaptive transition factor for smooth position updates.
     */
    private fun calculateAdaptiveTransitionFactor(
        lastPosition: UserPosition,
        blePosition: UserPosition,
        pdrPosition: UserPosition,
        baseTransitionFactor: Float,
        positionDifference: PositionDifference,
        isUserMoving: Boolean
    ): Float {
        var factor = baseTransitionFactor
        
        // Adjust factor based on position difference
        // Smaller differences allow faster transitions
        if (positionDifference.distance < 1.0f) {
            factor *= 1.5f
        } else if (positionDifference.distance > 5.0f) {
            // For large jumps, transition more slowly
            factor *= 0.5f
        }
        
        // Adjust factor based on movement state
        if (isUserMoving) {
            // When moving, allow faster transitions for PDR updates
            factor *= 1.2f
        } else {
            // When stationary, transition more slowly
            factor *= 0.8f
        }
        
        // Ensure factor is within reasonable bounds
        return factor.coerceIn(0.05f, 0.8f)
    }
    
    /**
     * Calculates the fused accuracy based on individual accuracies and weights.
     */
    private fun calculateFusedAccuracy(
        blePosition: UserPosition,
        pdrPosition: UserPosition,
        bleWeight: Float,
        pdrWeight: Float
    ): Float {
        // Weighted average of accuracies, with minimum threshold
        val weightedAccuracy = blePosition.accuracy * bleWeight + pdrPosition.accuracy * pdrWeight
        
        // Reduce accuracy if positions are consistent
        val positionDifference = blePosition.distanceTo(pdrPosition)
        val consistencyFactor = 1.0f - 0.3f * exp(-positionDifference / 2.0f)
        
        return weightedAccuracy * consistencyFactor
    }
    
    /**
     * Calculates the fused confidence based on individual confidences, weights, and position difference.
     */
    private fun calculateFusedConfidence(
        blePosition: UserPosition,
        pdrPosition: UserPosition,
        bleWeight: Float,
        pdrWeight: Float,
        positionDifference: PositionDifference
    ): Float {
        // Base confidence from weighted average
        var confidence = blePosition.confidence * bleWeight + pdrPosition.confidence * pdrWeight
        
        // Reduce confidence if positions are inconsistent
        val consistencyFactor = exp(-positionDifference.relativeDistance / 3.0f)
        confidence *= consistencyFactor
        
        // Ensure confidence is within valid range
        return confidence.coerceIn(0.1f, 1.0f)
    }
    
    /**
     * Updates the movement history with a new position.
     */
    private fun updateMovementHistory(position: UserPosition) {
        val currentTime = System.currentTimeMillis()
        
        // Calculate speed if we have a previous position
        val speed = if (lastFusedPosition != null) {
            val distance = position.distanceTo(lastFusedPosition!!)
            val timeDelta = (currentTime - lastFusedPosition!!.timestamp) / 1000f // in seconds
            if (timeDelta > 0) distance / timeDelta else 0f
        } else {
            0f
        }
        
        // Add to history
        movementHistory.add(
            MovementData(
                position = position,
                timestamp = currentTime,
                speed = speed
            )
        )
        
        // Maintain maximum history size
        if (movementHistory.size > maxHistorySize) {
            movementHistory.removeAt(0)
        }
    }
    
    /**
     * Predicts the user's position at a future time based on movement history.
     * 
     * @param predictionTimeMs Time in the future to predict (in milliseconds)
     * @param currentPosition Current position to use as a base for prediction
     * @return Predicted position
     */
    fun predictPosition(predictionTimeMs: Long, currentPosition: UserPosition): UserPosition {
        // If we don't have enough movement history, return the current position
        if (movementHistory.size < 2) {
            return currentPosition
        }
        
        // Calculate average speed and direction from recent movement history
        val recentHistory = movementHistory.takeLast(min(5, movementHistory.size))
        
        // Calculate average speed (m/s)
        val avgSpeed = recentHistory
            .filter { it.speed > 0 }
            .map { it.speed }
            .takeIf { it.isNotEmpty() }
            ?.average()
            ?.toFloat() ?: 0f
            
        // If not moving, return current position
        if (avgSpeed < 0.1f) {
            return currentPosition
        }
        
        // Calculate movement direction (in radians)
        val directionVector = calculateMovementDirection(recentHistory)
        val direction = atan2(directionVector.second, directionVector.first)
        
        // Calculate prediction distance based on speed and time
        val predictionTimeSeconds = predictionTimeMs / 1000f
        val predictionDistance = avgSpeed * predictionTimeSeconds
        
        // Calculate predicted position
        val predictedX = currentPosition.x + cos(direction) * predictionDistance
        val predictedY = currentPosition.y + sin(direction) * predictionDistance
        
        // Adjust confidence and accuracy for the prediction
        // Confidence decreases and accuracy (uncertainty) increases with prediction time
        val confidenceDecay = min(0.5f, predictionTimeSeconds / 5f) // Max 50% decay after 5 seconds
        val newConfidence = max(0.2f, currentPosition.confidence * (1f - confidenceDecay))
        val newAccuracy = currentPosition.accuracy * (1f + confidenceDecay * 0.5f)
        
        Timber.d("Predicted position: ($predictedX, $predictedY) from current (${currentPosition.x}, ${currentPosition.y})")
        Timber.d("Prediction based on: speed=${avgSpeed}m/s, direction=${Math.toDegrees(direction.toDouble())}°, time=${predictionTimeSeconds}s")
        
        return UserPosition(
            x = predictedX,
            y = predictedY,
            accuracy = newAccuracy,
            timestamp = System.currentTimeMillis() + predictionTimeMs,
            source = PositionSource.FUSION,
            confidence = newConfidence
        )
    }
    
    /**
     * Determines if the user is moving significantly based on recent movement history.
     * 
     * @return True if the user is moving significantly, false otherwise
     */
    private fun isUserMovingSignificantly(): Boolean {
        if (movementHistory.size < 2) {
            return false
        }
        
        // Calculate average speed from recent history
        val recentHistory = movementHistory.takeLast(min(3, movementHistory.size))
        val avgSpeed = recentHistory
            .map { it.speed }
            .average()
            .toFloat()
            
        // Consider user moving if average speed is above threshold
        val movingThreshold = 0.2f // m/s
        return avgSpeed > movingThreshold
    }
    
    /**
     * Calculates the average movement direction from movement history.
     * Returns a pair of (x, y) components of the direction vector.
     */
    private fun calculateMovementDirection(history: List<MovementData>): Pair<Float, Float> {
        if (history.size < 2) {
            return Pair(1f, 0f) // Default direction (right)
        }
        
        var sumX = 0f
        var sumY = 0f
        
        // Calculate direction vectors between consecutive positions
        for (i in 1 until history.size) {
            val prev = history[i-1].position
            val curr = history[i].position
            
            val dx = curr.x - prev.x
            val dy = curr.y - prev.y
            val distance = sqrt(dx * dx + dy * dy)
            
            // Only consider significant movements
            if (distance > 0.05f) {
                // Normalize and add to sum
                sumX += dx / distance
                sumY += dy / distance
            }
        }
        
        // Normalize the resulting vector
        val magnitude = sqrt(sumX * sumX + sumY * sumY)
        return if (magnitude > 0) {
            Pair(sumX / magnitude, sumY / magnitude)
        } else {
            Pair(1f, 0f) // Default direction if no movement detected
        }
    }
    
    /**
     * Resets the fusion state.
     */
    fun reset() {
        lastFusedPosition = null
        movementHistory.clear()
        kalmanFilter.reset()
    }
    
    /**
     * Performs sensor fusion using a Kalman filter.
     * 
     * @param blePosition Position from BLE triangulation
     * @param pdrPosition Position from Pedestrian Dead Reckoning
     * @param isUserMoving Whether the user is currently moving
     * @return Fused position using Kalman filter
     */
    private fun performKalmanFusion(
        blePosition: UserPosition,
        pdrPosition: UserPosition,
        isUserMoving: Boolean
    ): UserPosition {
        val currentTime = System.currentTimeMillis()
        
        // Initialize Kalman filter if needed
        if (!kalmanFilter.isInitialized() && UserPosition.isValid(blePosition)) {
            Timber.d("Initializing Kalman filter with BLE position")
            kalmanFilter.initialize(
                initialX = blePosition.x,
                initialY = blePosition.y,
                initialUncertainty = blePosition.accuracy,
                timestamp = currentTime
            )
        }
        
        // Update Kalman filter with BLE position if valid
        if (UserPosition.isValid(blePosition)) {
            kalmanFilter.update(
                measuredX = blePosition.x,
                measuredY = blePosition.y,
                measurementUncertainty = blePosition.accuracy,
                confidence = blePosition.confidence,
                timestamp = blePosition.timestamp
            )
            Timber.d("Updated Kalman filter with BLE position (${blePosition.x}, ${blePosition.y})")
        }
        
        // Update Kalman filter with PDR position if valid
        if (UserPosition.isValid(pdrPosition)) {
            // PDR is relative, so we give it less confidence in absolute positioning
            // but it's valuable for tracking movement
            val pdrConfidence = if (isUserMoving) {
                // When moving, PDR is more reliable for relative movement
                pdrPosition.confidence * 0.8f
            } else {
                // When stationary, PDR is less reliable
                pdrPosition.confidence * 0.5f
            }
            
            kalmanFilter.update(
                measuredX = pdrPosition.x,
                measuredY = pdrPosition.y,
                measurementUncertainty = pdrPosition.accuracy,
                confidence = pdrConfidence,
                timestamp = pdrPosition.timestamp
            )
            Timber.d("Updated Kalman filter with PDR position (${pdrPosition.x}, ${pdrPosition.y})")
        }
        
        // Get current position estimate from Kalman filter
        val (x, y) = kalmanFilter.getPosition()
        val uncertainty = kalmanFilter.getPositionUncertainty()
        val speed = kalmanFilter.getSpeed()
        
        // Calculate confidence based on uncertainty and consistency
        val confidence = calculateKalmanConfidence(
            uncertainty,
            blePosition,
            pdrPosition,
            speed,
            isUserMoving
        )
        
        Timber.d("Kalman filter position: ($x, $y), uncertainty: ${uncertainty}m, speed: ${speed}m/s")
        
        return UserPosition(
            x = x,
            y = y,
            accuracy = uncertainty,
            timestamp = currentTime,
            source = PositionSource.FUSION,
            confidence = confidence
        )
    }
    
    /**
     * Calculates confidence for Kalman filter position.
     */
    private fun calculateKalmanConfidence(
        uncertainty: Float,
        blePosition: UserPosition,
        pdrPosition: UserPosition,
        speed: Float,
        isUserMoving: Boolean
    ): Float {
        // Base confidence inversely proportional to uncertainty
        val baseConfidence = 0.9f * exp(-uncertainty / 3.0f)
        
        // Adjust confidence based on measurement consistency
        var adjustedConfidence = baseConfidence
        
        if (UserPosition.isValid(blePosition) && UserPosition.isValid(pdrPosition)) {
            val positionDifference = blePosition.distanceTo(pdrPosition)
            val consistencyFactor = exp(-positionDifference / 5.0f)
            adjustedConfidence *= (0.7f + 0.3f * consistencyFactor)
        }
        
        // Adjust confidence based on movement state and speed
        if (isUserMoving) {
            // Check if speed is reasonable for walking/running (0.5 to 3 m/s)
            val isSpeedReasonable = speed in 0.5f..3.0f
            adjustedConfidence *= if (isSpeedReasonable) 1.1f else 0.9f
        }
        
        return adjustedConfidence.coerceIn(0.1f, 1.0f)
    }
    
    /**
     * Data class for storing position difference information.
     */
    data class PositionDifference(
        val distance: Float,
        val relativeDistance: Float
    )
    
    /**
     * Data class for storing movement data.
     */
    data class MovementData(
        val position: UserPosition,
        val timestamp: Long,
        val speed: Float
    )
    
    /**
     * Fusion method options for sensor data fusion.
     */
    enum class FusionMethod {
        WEIGHTED_AVERAGE,  // Simple weighted averaging
        KALMAN_FILTER      // Advanced Kalman filter fusion
    }
    
    /**
     * Parameters for the FuseSensorDataUseCase.
     *
     * @param blePosition Position from BLE triangulation
     * @param pdrPosition Position from Pedestrian Dead Reckoning
     * @param bleWeight Base weight for BLE position (0-1)
     * @param updateRepository Whether to update the position in the repository
     * @param smoothTransition Whether to apply smooth transition from last position
     * @param lastFusedPosition Last fused position (for smooth transition)
     * @param transitionFactor Factor controlling transition speed (0-1)
     * @param isUserMoving Whether the user is currently moving
     * @param lastBleTimestamp Timestamp of the last valid BLE position
     * @param enablePrediction Whether to enable position prediction for smoother movement
     * @param predictionTimeMs Time in the future to predict (in milliseconds)
     * @param fusionMethod Method to use for sensor fusion (WEIGHTED_AVERAGE or KALMAN_FILTER)
     */
    data class Params(
        val blePosition: UserPosition,
        val pdrPosition: UserPosition,
        val bleWeight: Float = 0.6f,
        val updateRepository: Boolean = true,
        val smoothTransition: Boolean = true,
        val lastFusedPosition: UserPosition? = null,
        val transitionFactor: Float = 0.3f,
        val isUserMoving: Boolean = false,
        val lastBleTimestamp: Long? = null,
        val enablePrediction: Boolean = true,
        val predictionTimeMs: Long? = null,
        val fusionMethod: FusionMethod = FusionMethod.WEIGHTED_AVERAGE
    )
}