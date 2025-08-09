package com.example.myapplication.domain.model

import org.apache.commons.math3.linear.Array2DRowRealMatrix
import org.apache.commons.math3.linear.LUDecomposition
import org.apache.commons.math3.linear.RealMatrix
import timber.log.Timber
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Extended Kalman Filter implementation for sensor fusion in indoor positioning.
 * Combines data from Wi-Fi positioning, SLAM, and PDR to provide accurate position estimates.
 */
class ExtendedKalmanFilter {

    // State vector: [x, y, theta]
    private var state: RealMatrix = Array2DRowRealMatrix(doubleArrayOf(0.0, 0.0, 0.0))
    
    // Covariance matrix
    private var covariance: RealMatrix = Array2DRowRealMatrix(arrayOf(
        doubleArrayOf(1.0, 0.0, 0.0),
        doubleArrayOf(0.0, 1.0, 0.0),
        doubleArrayOf(0.0, 0.0, 1.0)
    ))

    // Process noise parameters - tunable
    private var positionProcessNoise = 0.05 // Position process noise (m²/s²)
    private var headingProcessNoise = 0.02 // Heading process noise (rad²/s²)
    
    // Drift correction parameters
    private var lastWifiPosition: Pair<Double, Double>? = null
    private var driftCorrectionThreshold = 5.0 // Threshold for drift correction (meters)
    private var driftCorrectionFactor = 0.3 // Factor for drift correction (0-1)
    
    private var isInitialized = false
    private var lastUpdateTime = 0L

    /**
     * Checks if the filter has been initialized.
     */
    fun isInitialized(): Boolean = isInitialized

    /**
     * Gets the current state estimate.
     */
    fun getState(): RealMatrix = state

    /**
     * Gets the current state covariance.
     */
    fun getCovariance(): RealMatrix = covariance
    
    /**
     * Sets the process noise parameters.
     * Higher values make the filter more responsive to measurements but more susceptible to noise.
     * Lower values make the filter more stable but slower to respond to changes.
     */
    fun setProcessNoiseParameters(positionNoise: Double, headingNoise: Double) {
        this.positionProcessNoise = positionNoise
        this.headingProcessNoise = headingNoise
        Timber.d("Process noise parameters updated: position=$positionNoise, heading=$headingNoise")
    }
    
    /**
     * Sets the drift correction parameters.
     * 
     * @param threshold Distance threshold in meters for applying drift correction
     * @param factor Correction factor (0-1) determining how much to correct
     */
    fun setDriftCorrectionParameters(threshold: Double, factor: Double) {
        this.driftCorrectionThreshold = threshold
        this.driftCorrectionFactor = factor.coerceIn(0.0, 1.0)
        Timber.d("Drift correction parameters updated: threshold=$threshold, factor=$factor")
    }

    /**
     * Prediction step of the EKF.
     * Updates the state estimate based on motion model.
     */
    fun predict(motion: Motion, dt: Double) {
        if (!isInitialized) return
        
        try {
            val x = state.getEntry(0, 0)
            val y = state.getEntry(1, 0)
            val theta = state.getEntry(2, 0)
    
            val v = motion.velocity
            val w = motion.angularVelocity
            
            // Handle special case to avoid division by zero
            val newX: Double
            val newY: Double
            
            if (abs(w) < 0.0001) {
                // Straight line motion
                newX = x + v * cos(theta) * dt
                newY = y + v * sin(theta) * dt
            } else {
                // Curved motion
                newX = x - v / w * sin(theta) + v / w * sin(theta + w * dt)
                newY = y + v / w * cos(theta) - v / w * cos(theta + w * dt)
            }
            
            val newTheta = theta + w * dt
            
            // Jacobian of state transition function
            val G = Array2DRowRealMatrix(arrayOf(
                doubleArrayOf(1.0, 0.0, if (abs(w) < 0.0001) -v * sin(theta) * dt else -v / w * cos(theta) + v / w * cos(theta + w * dt)),
                doubleArrayOf(0.0, 1.0, if (abs(w) < 0.0001) v * cos(theta) * dt else -v / w * sin(theta) + v / w * sin(theta + w * dt)),
                doubleArrayOf(0.0, 0.0, 1.0)
            ))
    
            // Process noise covariance - scaled by dt and velocity
            val velocityFactor = 1.0 + v * 2.0 // Increase noise with velocity
            val R = Array2DRowRealMatrix(arrayOf(
                doubleArrayOf(positionProcessNoise * dt * velocityFactor, 0.0, 0.0),
                doubleArrayOf(0.0, positionProcessNoise * dt * velocityFactor, 0.0),
                doubleArrayOf(0.0, 0.0, headingProcessNoise * dt * velocityFactor)
            ))
    
            // Update state and covariance
            state = Array2DRowRealMatrix(doubleArrayOf(newX, newY, newTheta))
            covariance = G.multiply(covariance).multiply(G.transpose()).add(R)
            
            Timber.v("EKF prediction: x=$newX, y=$newY, theta=$newTheta")
        } catch (e: Exception) {
            Timber.e(e, "Error in EKF prediction step")
        }
    }

    /**
     * Update step of the EKF.
     * Corrects the state estimate based on measurements.
     */
    fun update(measurement: Measurement) {
        if (!isInitialized) return
        
        try {
            // Observation matrix - maps state to measurements
            val H = Array2DRowRealMatrix(arrayOf(
                doubleArrayOf(1.0, 0.0, 0.0),
                doubleArrayOf(0.0, 1.0, 0.0)
            ))
    
            // Measurement vector
            val z = Array2DRowRealMatrix(doubleArrayOf(measurement.x, measurement.y))
            
            // Predicted measurement
            val zPred = H.multiply(state)
            
            // Innovation (measurement residual)
            val y = z.subtract(zPred)
            
            // Innovation covariance
            val S = H.multiply(covariance).multiply(H.transpose()).add(measurement.covariance)
            
            // Kalman gain
            val K = covariance.multiply(H.transpose()).multiply(LUDecomposition(S).solver.inverse)
            
            // Update state
            state = state.add(K.multiply(y))
            
            // Update covariance
            val I = Array2DRowRealMatrix(arrayOf(
                doubleArrayOf(1.0, 0.0, 0.0),
                doubleArrayOf(0.0, 1.0, 0.0),
                doubleArrayOf(0.0, 0.0, 1.0)
            ))
            covariance = I.subtract(K.multiply(H)).multiply(covariance)
            
            // Store Wi-Fi position for drift correction
            lastWifiPosition = Pair(measurement.x, measurement.y)
            
            Timber.v("EKF update: innovation=(${y.getEntry(0, 0)}, ${y.getEntry(1, 0)})")
        } catch (e: Exception) {
            Timber.e(e, "Error in EKF update step")
        }
    }
    
    /**
     * Applies drift correction based on the difference between estimated position and Wi-Fi position.
     * This helps prevent long-term drift in the position estimate.
     */
    fun applyDriftCorrection() {
        if (!isInitialized || lastWifiPosition == null) return
        
        try {
            val currentX = state.getEntry(0, 0)
            val currentY = state.getEntry(1, 0)
            val wifiX = lastWifiPosition!!.first
            val wifiY = lastWifiPosition!!.second
            
            // Calculate drift
            val dx = wifiX - currentX
            val dy = wifiY - currentY
            val driftDistance = sqrt(dx * dx + dy * dy)
            
            // Apply correction if drift exceeds threshold
            if (driftDistance > driftCorrectionThreshold) {
                Timber.d("Applying drift correction. Drift: $driftDistance m")
                
                // Apply partial correction
                val newX = currentX + dx * driftCorrectionFactor
                val newY = currentY + dy * driftCorrectionFactor
                
                // Update state
                state.setEntry(0, 0, newX)
                state.setEntry(1, 0, newY)
                
                // Increase covariance to reflect uncertainty after correction
                covariance.setEntry(0, 0, covariance.getEntry(0, 0) * 1.5)
                covariance.setEntry(1, 1, covariance.getEntry(1, 1) * 1.5)
            }
        } catch (e: Exception) {
            Timber.e(e, "Error in drift correction")
        }
    }

    /**
     * Initializes the filter with an initial state and covariance.
     */
    fun initialize(initialState: RealMatrix, initialCovariance: RealMatrix) {
        this.state = initialState
        this.covariance = initialCovariance
        this.isInitialized = true
        this.lastUpdateTime = System.currentTimeMillis()
        Timber.d("EKF initialized with state: x=${initialState.getEntry(0, 0)}, y=${initialState.getEntry(1, 0)}")
    }
    
    /**
     * Resets the filter to an uninitialized state.
     */
    fun reset() {
        this.isInitialized = false
        this.lastWifiPosition = null
        Timber.d("EKF reset")
    }
}

/**
 * Represents motion data for the EKF prediction step.
 */
data class Motion(val velocity: Double, val angularVelocity: Double)

/**
 * Represents measurement data for the EKF update step.
 */
data class Measurement(val x: Double, val y: Double, val covariance: RealMatrix)
