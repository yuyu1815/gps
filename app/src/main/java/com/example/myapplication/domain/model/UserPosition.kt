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
    val confidence: Float = 0.5f
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
    UNKNOWN
}