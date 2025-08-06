package com.example.myapplication.domain.model

import kotlin.math.sqrt

/**
 * Implementation of a Kalman filter for position estimation.
 * 
 * This Kalman filter is designed for 2D position tracking, using a constant velocity model.
 * The state vector contains [x, y, vx, vy] where:
 * - x, y are the position coordinates
 * - vx, vy are the velocity components
 * 
 * The filter handles measurements from multiple sources with different confidence levels.
 */
class KalmanFilter {
    // State vector: [x, y, vx, vy]
    private var stateVector = FloatArray(4) { 0f }
    
    // State covariance matrix (4x4)
    private var stateCovariance = Array(4) { FloatArray(4) { 0f } }
    
    // Process noise covariance (how much we expect the state to change between updates)
    private var processNoise = Array(4) { FloatArray(4) { 0f } }
    
    // Last update timestamp
    private var lastUpdateTime: Long = 0
    
    // Whether the filter has been initialized
    private var isInitialized = false
    
    /**
     * Initializes the Kalman filter with an initial position.
     * 
     * @param initialX Initial x coordinate
     * @param initialY Initial y coordinate
     * @param initialUncertainty Initial position uncertainty (in meters)
     * @param timestamp Current timestamp (in milliseconds)
     */
    fun initialize(initialX: Float, initialY: Float, initialUncertainty: Float, timestamp: Long) {
        // Initialize state vector with position and zero velocity
        stateVector[0] = initialX
        stateVector[1] = initialY
        stateVector[2] = 0f // vx
        stateVector[3] = 0f // vy
        
        // Initialize state covariance matrix
        for (i in 0 until 4) {
            for (j in 0 until 4) {
                stateCovariance[i][j] = 0f
            }
        }
        
        // Set initial position uncertainty
        stateCovariance[0][0] = initialUncertainty * initialUncertainty // x variance
        stateCovariance[1][1] = initialUncertainty * initialUncertainty // y variance
        
        // Set initial velocity uncertainty (higher since we don't know velocity)
        stateCovariance[2][2] = 1f // vx variance
        stateCovariance[3][3] = 1f // vy variance
        
        // Initialize process noise (tuned for indoor positioning)
        // Position process noise
        processNoise[0][0] = 0.01f
        processNoise[1][1] = 0.01f
        
        // Velocity process noise (higher since velocity can change more rapidly)
        processNoise[2][2] = 0.1f
        processNoise[3][3] = 0.1f
        
        lastUpdateTime = timestamp
        isInitialized = true
    }
    
    /**
     * Predicts the state forward in time.
     * 
     * @param currentTime Current timestamp (in milliseconds)
     */
    fun predict(currentTime: Long) {
        if (!isInitialized) {
            return
        }
        
        // Calculate time delta in seconds
        val dt = (currentTime - lastUpdateTime) / 1000f
        if (dt <= 0) {
            return
        }
        
        // State transition matrix for constant velocity model
        // [ 1 0 dt 0  ]
        // [ 0 1 0  dt ]
        // [ 0 0 1  0  ]
        // [ 0 0 0  1  ]
        
        // Predict state: x = F * x
        val newX = stateVector[0] + stateVector[2] * dt
        val newY = stateVector[1] + stateVector[3] * dt
        
        stateVector[0] = newX
        stateVector[1] = newY
        // Velocity remains unchanged in prediction step
        
        // Predict covariance: P = F * P * F^T + Q
        // For simplicity, we'll implement this directly for our 4x4 case
        
        // Temporary matrix for F*P
        val temp = Array(4) { FloatArray(4) { 0f } }
        
        // Calculate F*P
        for (i in 0 until 4) {
            for (j in 0 until 4) {
                temp[i][j] = stateCovariance[i][j]
            }
        }
        
        // Add dt terms for position-velocity covariances
        temp[0][2] += stateCovariance[0][0] * dt
        temp[0][3] += stateCovariance[0][1] * dt
        temp[1][2] += stateCovariance[1][0] * dt
        temp[1][3] += stateCovariance[1][1] * dt
        
        // Calculate (F*P)*F^T
        val newCovariance = Array(4) { FloatArray(4) { 0f } }
        for (i in 0 until 4) {
            for (j in 0 until 4) {
                newCovariance[i][j] = temp[i][j]
            }
        }
        
        // Add dt terms for position variances
        newCovariance[0][0] += temp[0][2] * dt
        newCovariance[0][1] += temp[0][3] * dt
        newCovariance[1][0] += temp[1][2] * dt
        newCovariance[1][1] += temp[1][3] * dt
        
        // Add process noise
        for (i in 0 until 4) {
            newCovariance[i][i] += processNoise[i][i] * dt
        }
        
        // Update state covariance
        stateCovariance = newCovariance
        
        // Update timestamp
        lastUpdateTime = currentTime
    }
    
    /**
     * Updates the filter with a new position measurement.
     * 
     * @param measuredX Measured x coordinate
     * @param measuredY Measured y coordinate
     * @param measurementUncertainty Measurement uncertainty (in meters)
     * @param confidence Confidence in the measurement (0-1)
     * @param timestamp Measurement timestamp (in milliseconds)
     */
    fun update(
        measuredX: Float,
        measuredY: Float,
        measurementUncertainty: Float,
        confidence: Float,
        timestamp: Long
    ) {
        if (!isInitialized) {
            // If not initialized, use this measurement as initialization
            initialize(measuredX, measuredY, measurementUncertainty, timestamp)
            return
        }
        
        // Predict state to current time
        predict(timestamp)
        
        // Measurement matrix (2x4) - we only measure position
        // H = [ 1 0 0 0 ]
        //     [ 0 1 0 0 ]
        
        // Measurement vector
        val measurement = floatArrayOf(measuredX, measuredY)
        
        // Predicted measurement
        val predictedMeasurement = floatArrayOf(stateVector[0], stateVector[1])
        
        // Innovation (measurement residual)
        val innovation = floatArrayOf(
            measurement[0] - predictedMeasurement[0],
            measurement[1] - predictedMeasurement[1]
        )
        
        // Measurement uncertainty adjusted by confidence
        // Lower confidence means higher uncertainty
        val adjustedUncertainty = measurementUncertainty / (confidence.coerceIn(0.1f, 1.0f))
        
        // Measurement noise covariance matrix (R)
        val R = Array(2) { FloatArray(2) { 0f } }
        R[0][0] = adjustedUncertainty * adjustedUncertainty
        R[1][1] = adjustedUncertainty * adjustedUncertainty
        
        // Innovation covariance (S = H*P*H^T + R)
        val S = Array(2) { FloatArray(2) { 0f } }
        S[0][0] = stateCovariance[0][0] + R[0][0]
        S[0][1] = stateCovariance[0][1]
        S[1][0] = stateCovariance[1][0]
        S[1][1] = stateCovariance[1][1] + R[1][1]
        
        // Calculate determinant of S for inversion
        val detS = S[0][0] * S[1][1] - S[0][1] * S[1][0]
        if (detS < 1e-6f) {
            // S is nearly singular, skip update
            return
        }
        
        // Inverse of S
        val invS = Array(2) { FloatArray(2) { 0f } }
        invS[0][0] = S[1][1] / detS
        invS[0][1] = -S[0][1] / detS
        invS[1][0] = -S[1][0] / detS
        invS[1][1] = S[0][0] / detS
        
        // Kalman gain (K = P*H^T*S^-1)
        val K = Array(4) { FloatArray(2) { 0f } }
        
        // Calculate P*H^T (simplified since H is just selecting position components)
        for (i in 0 until 4) {
            K[i][0] = stateCovariance[i][0]
            K[i][1] = stateCovariance[i][1]
        }
        
        // Multiply by S^-1
        val kalmanGain = Array(4) { FloatArray(2) { 0f } }
        for (i in 0 until 4) {
            for (j in 0 until 2) {
                for (k in 0 until 2) {
                    kalmanGain[i][j] += K[i][k] * invS[k][j]
                }
            }
        }
        
        // Update state (x = x + K*y)
        for (i in 0 until 4) {
            for (j in 0 until 2) {
                stateVector[i] += kalmanGain[i][j] * innovation[j]
            }
        }
        
        // Update covariance (P = (I - K*H)*P)
        // Since H just selects the position components, K*H is a 4x4 matrix
        // with the first two columns of K and zeros elsewhere
        val IMinusKH = Array(4) { i -> FloatArray(4) { j -> if (i == j) 1f else 0f } }
        for (i in 0 until 4) {
            IMinusKH[i][0] -= kalmanGain[i][0]
            IMinusKH[i][1] -= kalmanGain[i][1]
        }
        
        // Calculate (I - K*H)*P
        val newCovariance = Array(4) { FloatArray(4) { 0f } }
        for (i in 0 until 4) {
            for (j in 0 until 4) {
                for (k in 0 until 4) {
                    newCovariance[i][j] += IMinusKH[i][k] * stateCovariance[k][j]
                }
            }
        }
        
        stateCovariance = newCovariance
        
        // Update timestamp
        lastUpdateTime = timestamp
    }
    
    /**
     * Gets the current estimated position.
     * 
     * @return Pair of (x, y) coordinates
     */
    fun getPosition(): Pair<Float, Float> {
        return Pair(stateVector[0], stateVector[1])
    }
    
    /**
     * Gets the current estimated velocity.
     * 
     * @return Pair of (vx, vy) velocity components
     */
    fun getVelocity(): Pair<Float, Float> {
        return Pair(stateVector[2], stateVector[3])
    }
    
    /**
     * Gets the current position uncertainty (standard deviation).
     * 
     * @return Position uncertainty in meters
     */
    fun getPositionUncertainty(): Float {
        // Average of x and y position variances, converted to standard deviation
        return sqrt((stateCovariance[0][0] + stateCovariance[1][1]) / 2f)
    }
    
    /**
     * Gets the current velocity magnitude.
     * 
     * @return Velocity magnitude in meters per second
     */
    fun getSpeed(): Float {
        return sqrt(stateVector[2] * stateVector[2] + stateVector[3] * stateVector[3])
    }
    
    /**
     * Resets the filter to uninitialized state.
     */
    fun reset() {
        isInitialized = false
    }
    
    /**
     * Checks if the filter has been initialized.
     * 
     * @return True if initialized, false otherwise
     */
    fun isInitialized(): Boolean {
        return isInitialized
    }
}