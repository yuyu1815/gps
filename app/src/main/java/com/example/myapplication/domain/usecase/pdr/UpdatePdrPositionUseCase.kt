package com.example.myapplication.domain.usecase.pdr

import com.example.myapplication.data.repository.IPositionRepository
import com.example.myapplication.domain.model.PositionSource
import com.example.myapplication.domain.model.UserPosition
import com.example.myapplication.domain.usecase.UseCase
import timber.log.Timber
import kotlin.math.cos
import kotlin.math.sin

/**
 * Use case for updating the user's position based on Pedestrian Dead Reckoning (PDR).
 * This calculates the new position based on step detection, step length, and heading.
 */
class UpdatePdrPositionUseCase(
    private val positionRepository: IPositionRepository
) : UseCase<UpdatePdrPositionUseCase.Params, UserPosition> {
    
    // State variables for PDR position tracking
    private var lastPosition: UserPosition? = null
    
    override suspend fun invoke(params: Params): UserPosition {
        // Get the last known position or use the provided initial position
        val currentPosition = lastPosition ?: params.initialPosition ?: run {
            try {
                positionRepository.getCurrentPosition()
            } catch (e: Exception) {
                Timber.w("Failed to get current position: ${e.message}")
                UserPosition.invalid()
            }
        }
        
        // If we don't have a valid position and no step was detected, return invalid position
        if (!UserPosition.isValid(currentPosition) && !params.stepDetected) {
            return UserPosition.invalid()
        }
        
        // If no step was detected, return the current position
        if (!params.stepDetected) {
            return currentPosition
        }
        
        // Calculate new position based on step length and heading
        val headingRadians = Math.toRadians(params.heading.toDouble())
        val dx = params.stepLength * sin(headingRadians).toFloat()
        val dy = params.stepLength * cos(headingRadians).toFloat()
        
        // Create new position
        val newPosition = if (UserPosition.isValid(currentPosition)) {
            UserPosition(
                x = currentPosition.x + dx,
                y = currentPosition.y + dy,
                accuracy = currentPosition.accuracy + params.accuracyDecay, // Accuracy decreases with each step
                timestamp = System.currentTimeMillis(),
                source = PositionSource.PDR,
                confidence = calculateConfidence(currentPosition.confidence, params.stepConfidence)
            )
        } else {
            // If we don't have a valid current position but a step was detected,
            // use the initial position if provided
            params.initialPosition?.let {
                UserPosition(
                    x = it.x + dx,
                    y = it.y + dy,
                    accuracy = it.accuracy + params.accuracyDecay,
                    timestamp = System.currentTimeMillis(),
                    source = PositionSource.PDR,
                    confidence = calculateConfidence(it.confidence, params.stepConfidence)
                )
            } ?: UserPosition.invalid()
        }
        
        // Update the position in the repository if requested
        if (params.updateRepository && UserPosition.isValid(newPosition)) {
            positionRepository.updatePosition(newPosition)
        }
        
        // Update last position
        lastPosition = newPosition
        
        Timber.d("PDR position updated: (${newPosition.x}, ${newPosition.y}), heading: ${params.heading}°, step length: ${params.stepLength}m")
        return newPosition
    }
    
    /**
     * Calculates the confidence of the new position based on the previous confidence
     * and the confidence of the current step.
     */
    private fun calculateConfidence(previousConfidence: Float, stepConfidence: Float): Float {
        // Confidence decreases slightly with each step due to accumulating errors
        val confidenceDecay = 0.01f
        return (previousConfidence * (1 - confidenceDecay) * stepConfidence).coerceIn(0f, 1f)
    }
    
    /**
     * Resets the PDR position tracking state.
     */
    fun reset() {
        lastPosition = null
    }
    
    /**
     * Parameters for the UpdatePdrPositionUseCase.
     *
     * @param stepDetected Whether a step was detected
     * @param stepLength Length of the step in meters
     * @param heading Heading direction in degrees (0-359)
     * @param initialPosition Initial position (optional, used if no current position is available)
     * @param updateRepository Whether to update the position in the repository
     * @param accuracyDecay Amount by which accuracy decreases with each step
     * @param stepConfidence Confidence in the step detection (0-1)
     */
    data class Params(
        val stepDetected: Boolean,
        val stepLength: Float = 0f,
        val heading: Float = 0f,
        val initialPosition: UserPosition? = null,
        val updateRepository: Boolean = true,
        val accuracyDecay: Float = 0.05f,
        val stepConfidence: Float = 0.9f
    )
}