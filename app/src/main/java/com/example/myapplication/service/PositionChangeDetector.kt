package com.example.myapplication.service

import com.example.myapplication.data.repository.IPositionRepository
import com.example.myapplication.domain.model.UserPosition
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

/**
 * Detects and tracks position changes in the indoor positioning system.
 * Provides notifications when the user's position has moved significantly.
 */
class PositionChangeDetector(
    private val positionRepository: IPositionRepository
) {
    
    // Position change detection state
    private var lastKnownPosition: UserPosition? = null
    private var isMoving = false
    private var lastMovementTime = 0L
    
    // Movement detection parameters
    private val movementThreshold = 0.5f // meters
    private val movementTimeoutMs = 5000L // 5 seconds
    private val minMovementIntervalMs = 1000L // 1 second between movement notifications
    
    // State flows for observing position changes
    private val _isMovingFlow = MutableStateFlow(false)
    val isMovingFlow: StateFlow<Boolean> = _isMovingFlow.asStateFlow()
    
    private val _lastMovementDirection = MutableStateFlow<Float?>(null)
    val lastMovementDirection: StateFlow<Float?> = _lastMovementDirection.asStateFlow()
    
    private val _movementDistance = MutableStateFlow(0f)
    val movementDistance: StateFlow<Float> = _movementDistance.asStateFlow()
    
    // Movement history for analysis
    private val movementHistory = mutableListOf<MovementEvent>()
    private val maxHistorySize = 100
    
    /**
     * Updates the position and detects changes.
     * 
     * @param newPosition The new position to check
     */
    fun updatePosition(newPosition: UserPosition) {
        val currentTime = System.currentTimeMillis()
        
        // Check if we have a previous position to compare against
        lastKnownPosition?.let { lastPosition ->
            if (UserPosition.isValid(newPosition) && UserPosition.isValid(lastPosition)) {
                // Check if position has moved significantly
                val hasMoved = newPosition.hasMovedSignificantly(lastPosition, movementThreshold)
                val distance = newPosition.distanceTo(lastPosition)
                val direction = newPosition.movementDirectionFrom(lastPosition)
                
                if (hasMoved) {
                    // Check if enough time has passed since last movement notification
                    val timeSinceLastMovement = currentTime - lastMovementTime
                    if (timeSinceLastMovement >= minMovementIntervalMs) {
                        // Record movement event
                        val movementEvent = MovementEvent(
                            timestamp = currentTime,
                            fromPosition = lastPosition,
                            toPosition = newPosition,
                            distance = distance,
                            direction = direction,
                            source = newPosition.source
                        )
                        recordMovementEvent(movementEvent)
                        
                        // Update movement state
                        isMoving = true
                        lastMovementTime = currentTime
                        _isMovingFlow.value = true
                        _lastMovementDirection.value = direction
                        _movementDistance.value = distance
                        
                        Timber.d("Position changed: moved ${distance}m in direction ${direction}° " +
                                "from (${lastPosition.x}, ${lastPosition.y}) to (${newPosition.x}, ${newPosition.y})")
                    }
                } else {
                    // Check if we should mark as stationary
                    val timeSinceLastMovement = currentTime - lastMovementTime
                    if (timeSinceLastMovement > movementTimeoutMs && isMoving) {
                        isMoving = false
                        _isMovingFlow.value = false
                        Timber.d("User stopped moving (stationary for ${timeSinceLastMovement/1000}s)")
                    }
                }
            }
        }
        
        // Update last known position
        lastKnownPosition = newPosition
    }
    
    /**
     * Records a movement event in the history.
     */
    private fun recordMovementEvent(event: MovementEvent) {
        movementHistory.add(event)
        
        // Maintain history size
        if (movementHistory.size > maxHistorySize) {
            movementHistory.removeAt(0)
        }
    }
    
    /**
     * Gets the recent movement history.
     * 
     * @param count Number of recent events to return
     * @return List of recent movement events
     */
    fun getRecentMovementHistory(count: Int = 10): List<MovementEvent> {
        return movementHistory.takeLast(count)
    }
    
    /**
     * Gets the total distance moved in the recent history.
     * 
     * @param timeWindowMs Time window in milliseconds to consider
     * @return Total distance moved in the time window
     */
    fun getTotalDistanceMoved(timeWindowMs: Long = 60000L): Float {
        val currentTime = System.currentTimeMillis()
        val cutoffTime = currentTime - timeWindowMs
        
        return movementHistory
            .filter { it.timestamp >= cutoffTime }
            .sumOf { it.distance.toDouble() }
            .toFloat()
    }
    
    /**
     * Gets the average movement speed.
     * 
     * @param timeWindowMs Time window in milliseconds to consider
     * @return Average speed in meters per second
     */
    fun getAverageSpeed(timeWindowMs: Long = 60000L): Float {
        val currentTime = System.currentTimeMillis()
        val cutoffTime = currentTime - timeWindowMs
        val recentEvents = movementHistory.filter { it.timestamp >= cutoffTime }
        
        if (recentEvents.isEmpty()) {
            return 0f
        }
        
        val totalDistance = recentEvents.sumOf { it.distance.toDouble() }
        val totalTimeSeconds = (currentTime - cutoffTime) / 1000.0
        
        return (totalDistance / totalTimeSeconds).toFloat()
    }
    
    /**
     * Checks if the user is currently moving.
     * 
     * @return True if the user is moving, false otherwise
     */
    fun isCurrentlyMoving(): Boolean {
        return isMoving
    }
    
    /**
     * Gets the last movement direction.
     * 
     * @return Direction in degrees (0-360), or null if no recent movement
     */
    fun getLastMovementDirection(): Float? {
        return _lastMovementDirection.value
    }
    
    /**
     * Resets the position change detector state.
     */
    fun reset() {
        lastKnownPosition = null
        isMoving = false
        lastMovementTime = 0L
        _isMovingFlow.value = false
        _lastMovementDirection.value = null
        _movementDistance.value = 0f
        movementHistory.clear()
        Timber.d("Position change detector reset")
    }
    
    /**
     * Represents a movement event.
     */
    data class MovementEvent(
        val timestamp: Long,
        val fromPosition: UserPosition,
        val toPosition: UserPosition,
        val distance: Float,
        val direction: Float?,
        val source: com.example.myapplication.domain.model.PositionSource
    )
}


