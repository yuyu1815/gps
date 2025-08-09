package com.example.myapplication.domain.model

/**
 * Represents the user's position in the indoor positioning system.
 * Contains information about the position coordinates, accuracy, and source.
 */
data class UserPosition(
    /**
     * X coordinate of the user's position in the indoor map (in meters).
     */
    val x: Float,
    
    /**
     * Y coordinate of the user's position in the indoor map (in meters).
     */
    val y: Float,
    
    /**
     * Estimated accuracy of the position (in meters).
     * Lower values indicate higher accuracy.
     */
    val accuracy: Float,
    
    /**
     * Timestamp when this position was calculated (in milliseconds).
     */
    val timestamp: Long = System.currentTimeMillis(),
    
    /**
     * Source of the position data.
     */
    val source: PositionSource = PositionSource.UNKNOWN,
    
    /**
     * Confidence level of the position (0.0 to 1.0).
     * Higher values indicate higher confidence.
     */
    val confidence: Float = 0.5f,
    // Optional uncertainty ellipse (standard deviations in meters and heading in radians)
    val sigmaX: Float? = null,
    val sigmaY: Float? = null,
    val sigmaTheta: Float? = null
) {
    /**
     * Calculates the distance to another position.
     */
    fun distanceTo(other: UserPosition): Float {
        val dx = this.x - other.x
        val dy = this.y - other.y
        return Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
    }
    
    /**
     * Checks if this position has moved significantly from another position.
     * 
     * @param other The position to compare against
     * @param threshold The minimum distance threshold for considering movement significant
     * @return True if the position has moved significantly, false otherwise
     */
    fun hasMovedSignificantly(other: UserPosition, threshold: Float = 0.5f): Boolean {
        if (!UserPosition.isValid(this) || !UserPosition.isValid(other)) {
            return false
        }
        return distanceTo(other) > threshold
    }
    
    /**
     * Calculates the movement direction from another position.
     * 
     * @param other The position to calculate direction from
     * @return Direction in degrees (0-360), or null if positions are invalid
     */
    fun movementDirectionFrom(other: UserPosition): Float? {
        if (!UserPosition.isValid(this) || !UserPosition.isValid(other)) {
            return null
        }
        val dx = this.x - other.x
        val dy = this.y - other.y
        return Math.toDegrees(Math.atan2(dy.toDouble(), dx.toDouble())).toFloat()
    }
    
    /**
     * Calculates a weighted average position between this position and another.
     */
    fun weightedAverageTo(other: UserPosition, weight: Float): UserPosition {
        val w = weight.coerceIn(0f, 1f)
        val newX = this.x * (1 - w) + other.x * w
        val newY = this.y * (1 - w) + other.y * w
        val newAccuracy = this.accuracy * (1 - w) + other.accuracy * w
        val newConfidence = (this.confidence * (1 - w) + other.confidence * w).coerceIn(0f, 1f)
        
        return UserPosition(
            x = newX,
            y = newY,
            accuracy = newAccuracy,
            timestamp = System.currentTimeMillis(),
            source = PositionSource.FUSION,
            confidence = newConfidence
        )
    }
    
    companion object {
        /**
         * Creates an invalid position.
         */
        fun invalid(): UserPosition {
            return UserPosition(
                x = Float.NaN,
                y = Float.NaN,
                accuracy = Float.MAX_VALUE,
                confidence = 0f,
                source = PositionSource.UNKNOWN
            )
        }
        
        /**
         * Checks if a position is valid.
         */
        fun isValid(position: UserPosition): Boolean {
            return !position.x.isNaN() && !position.y.isNaN() && position.accuracy < Float.MAX_VALUE
        }
    }
}

/**
 * Represents the source of a position.
 */
enum class PositionSource {
    /**
     * Position calculated from BLE beacons.
     */
    BLE,
    /** Position calculated from Wi‑Fi fingerprinting. */
    WIFI,
    
    /**
     * Position calculated from Pedestrian Dead Reckoning.
     */
    PDR,
    
    /**
     * Position calculated from fusion of multiple sources.
     */
    FUSION,
    
    /**
     * Position from an unknown source.
     */
    UNKNOWN,
    
    /**
     * Position from ground truth data (for testing and evaluation).
     */
    GROUND_TRUTH
}