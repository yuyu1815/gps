package com.example.myapplication.domain.usecase.pdr

import android.hardware.SensorManager
import com.example.myapplication.domain.model.SensorData
import com.example.myapplication.domain.usecase.UseCase
import timber.log.Timber
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Use case for estimating heading (direction) using gyroscope data.
 * 
 * This implementation uses gyroscope data to track changes in orientation,
 * and optionally uses magnetometer data for absolute heading reference.
 * 
 * The algorithm works as follows:
 * 1. Use gyroscope data to calculate rotation around the z-axis (yaw)
 * 2. Integrate the rotation rate over time to get the change in heading
 * 3. Apply a complementary filter with magnetometer data (if available) to correct drift
 * 4. Normalize the heading to 0-360 degrees (0 = North, 90 = East, 180 = South, 270 = West)
 */
class EstimateHeadingUseCase : UseCase<EstimateHeadingUseCase.Params, EstimateHeadingUseCase.Result> {
    
    // Current heading in degrees (0-360, 0 = North, 90 = East, etc.)
    private var currentHeading = 0f
    
    // Last timestamp for integration
    private var lastTimestamp = 0L
    
    // Rotation matrix and orientation angles for sensor fusion
    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)
    
    // Last accelerometer and magnetometer values for sensor fusion
    private var lastAccelValues: FloatArray? = null
    private var lastMagValues: FloatArray? = null
    
    override suspend fun invoke(params: Params): Result {
        // Extract sensor data
        val gyroData = params.gyroscopeData
        val accelData = params.accelerometerData
        val magData = params.magnetometerData
        val rotVectorData = params.rotationVectorData
        val timestamp = gyroData.timestamp
        
        // If this is the first call, initialize the timestamp
        if (lastTimestamp == 0L) {
            lastTimestamp = timestamp
            
            // If rotation vector is available, use it for initial heading
            if (rotVectorData != null) {
                currentHeading = getHeadingFromRotationVector(rotVectorData)
                Timber.d("Initial heading from rotation vector: $currentHeading°")
            }
            // Otherwise, try to use magnetometer and accelerometer
            else if (accelData != null && magData != null) {
                updateLastSensorValues(accelData, magData)
                val magHeading = getHeadingFromMagnetometer()
                if (!magHeading.isNaN()) {
                    currentHeading = magHeading
                    Timber.d("Initial heading from magnetometer: $currentHeading°")
                }
            }
            
            return Result(
                heading = currentHeading,
                headingAccuracy = HeadingAccuracy.LOW,
                timestamp = timestamp
            )
        }
        
        // Calculate time delta in seconds (timestamps are in nanoseconds)
        val dt = (timestamp - lastTimestamp) / 1_000_000_000.0f
        lastTimestamp = timestamp
        
        // Update heading using gyroscope data (dead reckoning)
        // Note: We use the negative of gyro.z because positive rotation around z-axis
        // corresponds to counter-clockwise rotation, but heading increases clockwise
        val gyroHeadingChange = -gyroData.z * dt * RADIANS_TO_DEGREES
        
        // Apply gyroscope data with high-pass filter (removes slow drift)
        currentHeading += gyroHeadingChange * params.gyroWeight
        
        // Normalize heading to 0-360 degrees
        currentHeading = normalizeHeading(currentHeading)
        
        // Determine heading source and accuracy
        var headingSource = HeadingSource.GYROSCOPE
        var headingAccuracy = HeadingAccuracy.MEDIUM
        
        // If rotation vector is available, use it for absolute heading reference
        if (rotVectorData != null) {
            val rotVectorHeading = getHeadingFromRotationVector(rotVectorData)
            
            // Apply complementary filter with rotation vector data
            applyComplementaryFilter(rotVectorHeading, params.complementaryFilterAlpha)
            
            headingSource = HeadingSource.ROTATION_VECTOR
            headingAccuracy = HeadingAccuracy.HIGH
            
            Timber.v("Heading updated with rotation vector: $currentHeading°")
        }
        // Otherwise, try to use magnetometer and accelerometer
        else if (accelData != null && magData != null) {
            updateLastSensorValues(accelData, magData)
            val magHeading = getHeadingFromMagnetometer()
            
            if (!magHeading.isNaN()) {
                // Apply complementary filter with magnetometer data
                applyComplementaryFilter(magHeading, params.complementaryFilterAlpha)
                
                headingSource = HeadingSource.MAGNETOMETER
                headingAccuracy = HeadingAccuracy.MEDIUM
                
                Timber.v("Heading updated with magnetometer: $currentHeading°")
            }
        }
        
        Timber.d("Current heading: $currentHeading° (source: $headingSource, accuracy: $headingAccuracy)")
        
        return Result(
            heading = currentHeading,
            headingAccuracy = headingAccuracy,
            headingSource = headingSource,
            gyroHeadingChange = gyroHeadingChange,
            timestamp = timestamp
        )
    }
    
    /**
     * Updates the last accelerometer and magnetometer values for sensor fusion.
     */
    private fun updateLastSensorValues(accelData: SensorData.Accelerometer, magData: SensorData.Magnetometer) {
        lastAccelValues = floatArrayOf(accelData.x, accelData.y, accelData.z)
        lastMagValues = floatArrayOf(magData.x, magData.y, magData.z)
    }
    
    /**
     * Gets heading from magnetometer and accelerometer data using the device's coordinate system.
     * 
     * @return Heading in degrees (0-360, 0 = North, 90 = East, etc.) or NaN if calculation fails
     */
    private fun getHeadingFromMagnetometer(): Float {
        if (lastAccelValues == null || lastMagValues == null) {
            return Float.NaN
        }
        
        // Get rotation matrix from accelerometer and magnetometer
        val success = SensorManager.getRotationMatrix(
            rotationMatrix,
            null,
            lastAccelValues,
            lastMagValues
        )
        
        if (!success) {
            return Float.NaN
        }
        
        // Get orientation angles from rotation matrix
        SensorManager.getOrientation(rotationMatrix, orientationAngles)
        
        // Convert azimuth from radians to degrees
        val azimuthRadians = orientationAngles[0]
        val azimuthDegrees = Math.toDegrees(azimuthRadians.toDouble()).toFloat()
        
        // Normalize to 0-360 degrees
        return normalizeHeading(azimuthDegrees)
    }
    
    /**
     * Gets heading from rotation vector data.
     * 
     * @param rotVectorData Rotation vector sensor data
     * @return Heading in degrees (0-360, 0 = North, 90 = East, etc.)
     */
    private fun getHeadingFromRotationVector(rotVectorData: SensorData.RotationVector): Float {
        // Convert rotation vector to rotation matrix
        SensorManager.getRotationMatrixFromVector(
            rotationMatrix,
            floatArrayOf(rotVectorData.x, rotVectorData.y, rotVectorData.z, rotVectorData.w)
        )
        
        // Get orientation angles from rotation matrix
        SensorManager.getOrientation(rotationMatrix, orientationAngles)
        
        // Convert azimuth from radians to degrees
        val azimuthRadians = orientationAngles[0]
        val azimuthDegrees = Math.toDegrees(azimuthRadians.toDouble()).toFloat()
        
        // Normalize to 0-360 degrees
        return normalizeHeading(azimuthDegrees)
    }
    
    /**
     * Applies a complementary filter to combine gyroscope-based heading with absolute heading.
     * 
     * @param absoluteHeading Absolute heading from magnetometer or rotation vector
     * @param alpha Filter coefficient (0-1, higher values = more weight to gyroscope)
     */
    private fun applyComplementaryFilter(absoluteHeading: Float, alpha: Float) {
        // Calculate the shortest path to the target heading
        val diff = calculateHeadingDifference(currentHeading, absoluteHeading)
        
        // Apply complementary filter
        // Use more weight for gyroscope (alpha) and less for absolute heading (1-alpha)
        currentHeading += diff * (1 - alpha)
        
        // Normalize heading to 0-360 degrees
        currentHeading = normalizeHeading(currentHeading)
    }
    
    /**
     * Calculates the shortest difference between two headings.
     * 
     * @param heading1 First heading in degrees
     * @param heading2 Second heading in degrees
     * @return Shortest difference in degrees (-180 to 180)
     */
    private fun calculateHeadingDifference(heading1: Float, heading2: Float): Float {
        var diff = heading2 - heading1
        
        // Ensure the difference is in the range -180 to 180 degrees
        if (diff > 180) {
            diff -= 360
        } else if (diff < -180) {
            diff += 360
        }
        
        return diff
    }
    
    /**
     * Normalizes heading to 0-360 degrees.
     * 
     * @param heading Heading in degrees
     * @return Normalized heading in degrees (0-360)
     */
    private fun normalizeHeading(heading: Float): Float {
        var result = heading % 360
        if (result < 0) {
            result += 360
        }
        return result
    }
    
    /**
     * Resets the heading estimation state.
     */
    fun reset() {
        currentHeading = 0f
        lastTimestamp = 0L
        lastAccelValues = null
        lastMagValues = null
    }
    
    /**
     * Parameters for the EstimateHeadingUseCase.
     *
     * @param gyroscopeData Gyroscope data
     * @param accelerometerData Accelerometer data (optional, for sensor fusion)
     * @param magnetometerData Magnetometer data (optional, for sensor fusion)
     * @param rotationVectorData Rotation vector data (optional, for sensor fusion)
     * @param gyroWeight Weight for gyroscope data (0-1)
     * @param complementaryFilterAlpha Complementary filter coefficient (0-1)
     */
    data class Params(
        val gyroscopeData: SensorData.Gyroscope,
        val accelerometerData: SensorData.Accelerometer? = null,
        val magnetometerData: SensorData.Magnetometer? = null,
        val rotationVectorData: SensorData.RotationVector? = null,
        val gyroWeight: Float = 0.98f,
        val complementaryFilterAlpha: Float = 0.98f
    )
    
    /**
     * Result of the heading estimation.
     *
     * @param heading Estimated heading in degrees (0-360, 0 = North, 90 = East, etc.)
     * @param headingAccuracy Accuracy of the heading estimation
     * @param headingSource Source of the heading estimation
     * @param gyroHeadingChange Change in heading from gyroscope in degrees
     * @param timestamp Timestamp of the estimation
     */
    data class Result(
        val heading: Float,
        val headingAccuracy: HeadingAccuracy = HeadingAccuracy.LOW,
        val headingSource: HeadingSource = HeadingSource.GYROSCOPE,
        val gyroHeadingChange: Float = 0f,
        val timestamp: Long
    )
    
    /**
     * Enum for heading accuracy levels.
     */
    enum class HeadingAccuracy {
        LOW,    // Gyroscope only, significant drift over time
        MEDIUM, // Gyroscope with occasional magnetometer corrections
        HIGH    // Rotation vector or frequent magnetometer corrections
    }
    
    /**
     * Enum for heading data sources.
     */
    enum class HeadingSource {
        GYROSCOPE,      // Heading from gyroscope integration
        MAGNETOMETER,   // Heading from magnetometer
        ROTATION_VECTOR // Heading from rotation vector sensor
    }
    
    companion object {
        /**
         * Conversion factor from radians to degrees.
         */
        const val RADIANS_TO_DEGREES = 57.2957795f // 180/π
        
        /**
         * Default gyroscope weight for heading calculation.
         */
        const val DEFAULT_GYRO_WEIGHT = 0.98f
        
        /**
         * Default complementary filter alpha value.
         */
        const val DEFAULT_COMPLEMENTARY_FILTER_ALPHA = 0.98f
    }
}

/**
 * Use case for estimating heading using a Kalman filter for optimal sensor fusion.
 * 
 * This implementation uses a Kalman filter to combine gyroscope, accelerometer,
 * and magnetometer data for more accurate and stable heading estimation.
 * 
 * The Kalman filter provides optimal estimation by considering the noise
 * characteristics of each sensor and dynamically adjusting their weights.
 */
class KalmanHeadingUseCase : UseCase<KalmanHeadingUseCase.Params, KalmanHeadingUseCase.Result> {
    
    // State variables for Kalman filter
    private var heading = 0f          // Current heading estimate (state)
    private var headingRate = 0f      // Current heading rate estimate (state)
    private var variance = 10f        // Estimate uncertainty (P matrix diagonal)
    private var rateVariance = 10f    // Estimate uncertainty (P matrix diagonal)
    private var lastTimestamp = 0L    // Last update timestamp
    
    // Adaptive noise parameters
    private var adaptiveGyroNoise = 0.01f
    private var adaptiveMagnetometerNoise = 0.05f
    private var adaptiveProcessNoise = 0.001f
    private var magneticDisturbanceLevel = 0f
    private var lastMagnetometerValues = FloatArray(3) { 0f }
    
    // Rotation matrix and orientation angles for sensor fusion
    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)
    
    override suspend fun invoke(params: Params): Result {
        // Extract sensor data
        val gyroData = params.gyroscopeData
        val accelData = params.accelerometerData
        val magData = params.magnetometerData
        val rotVectorData = params.rotationVectorData
        val timestamp = gyroData.timestamp
        
        // If this is the first call, initialize the state
        if (lastTimestamp == 0L) {
            lastTimestamp = timestamp
            
            // Initialize heading from absolute sensors if available
            if (rotVectorData != null) {
                heading = getHeadingFromRotationVector(rotVectorData)
                Timber.d("Initial Kalman heading from rotation vector: $heading°")
            } else if (accelData != null && magData != null) {
                val magHeading = getHeadingFromMagnetometer(accelData, magData)
                if (!magHeading.isNaN()) {
                    heading = magHeading
                    Timber.d("Initial Kalman heading from magnetometer: $heading°")
                }
            }
            
            return Result(
                heading = heading,
                headingRate = headingRate,
                variance = variance,
                headingAccuracy = getAccuracyFromVariance(variance),
                timestamp = timestamp
            )
        }
        
        // Calculate time delta in seconds
        val dt = (timestamp - lastTimestamp) / 1_000_000_000.0f
        lastTimestamp = timestamp
        
        // Limit dt to reasonable values to prevent instability
        val limitedDt = dt.coerceAtMost(0.1f)
        
        // Update adaptive noise parameters based on sensor data
        updateAdaptiveNoiseParameters(gyroData, magData)
        
        // 1. Prediction step
        // Update state estimate using gyroscope data
        val gyroZ = -gyroData.z  // Negative because heading increases clockwise
        
        // Predict new heading and heading rate
        heading += (headingRate + gyroZ) * limitedDt
        headingRate = gyroZ  // Assume gyro reading is the new rate
        
        // Normalize heading to 0-360 degrees
        heading = normalizeHeading(heading * RADIANS_TO_DEGREES) / RADIANS_TO_DEGREES
        
        // Update variance (P matrix)
        // Add process noise (Q matrix) - now using adaptive noise
        variance += limitedDt * limitedDt * adaptiveProcessNoise + adaptiveGyroNoise
        rateVariance += adaptiveProcessNoise
        
        // 2. Correction step (if absolute heading is available)
        var absoluteHeading = Float.NaN
        var measurementNoise = 0f
        
        // Try to get absolute heading from rotation vector (most accurate)
        if (rotVectorData != null) {
            absoluteHeading = getHeadingFromRotationVector(rotVectorData) / RADIANS_TO_DEGREES
            measurementNoise = params.rotVectorNoise
        }
        // Otherwise try magnetometer + accelerometer
        else if (accelData != null && magData != null) {
            val magHeading = getHeadingFromMagnetometer(accelData, magData) / RADIANS_TO_DEGREES
            if (!magHeading.isNaN()) {
                absoluteHeading = magHeading
                // Use adaptive noise for magnetometer based on detected disturbance
                measurementNoise = adaptiveMagnetometerNoise
            }
        }
        
        // If we have an absolute heading measurement, apply Kalman correction
        if (!absoluteHeading.isNaN()) {
            // Calculate innovation (measurement - prediction)
            var innovation = absoluteHeading - heading
            
            // Ensure the innovation is in the range -π to π radians
            if (innovation > Math.PI) {
                innovation -= 2 * Math.PI.toFloat()
            } else if (innovation < -Math.PI) {
                innovation += 2 * Math.PI.toFloat()
            }
            
            // Calculate Kalman gain
            val kalmanGain = variance / (variance + measurementNoise)
            
            // Adaptive trust in measurements based on innovation magnitude
            // If innovation is very large, it might be due to magnetic disturbance
            val adaptedKalmanGain = if (abs(innovation) > 0.5) {
                // Reduce the gain when innovation is suspiciously large
                kalmanGain * 0.5f
            } else {
                kalmanGain
            }
            
            // Update state estimate
            heading += adaptedKalmanGain * innovation
            
            // Update variance
            variance = (1 - adaptedKalmanGain) * variance
            
            Timber.v("Kalman correction: innovation=$innovation, gain=$adaptedKalmanGain, variance=$variance")
        }
        
        // Convert heading back to degrees for the result
        val headingDegrees = normalizeHeading(heading * RADIANS_TO_DEGREES)
        
        Timber.d("Kalman heading: $headingDegrees°, rate: ${headingRate * RADIANS_TO_DEGREES}°/s, variance: $variance")
        
        return Result(
            heading = headingDegrees,
            headingRate = headingRate * RADIANS_TO_DEGREES,
            variance = variance,
            headingAccuracy = getAccuracyFromVariance(variance),
            timestamp = timestamp
        )
    }
    
    /**
     * Updates adaptive noise parameters based on sensor data.
     * 
     * This method adjusts the noise parameters dynamically based on the current
     * sensor readings and detected disturbances.
     * 
     * @param gyroData Gyroscope data
     * @param magData Magnetometer data (optional)
     */
    private fun updateAdaptiveNoiseParameters(
        gyroData: SensorData.Gyroscope,
        magData: SensorData.Magnetometer?
    ) {
        // 1. Adjust gyroscope noise based on motion intensity
        val gyroMagnitude = sqrt(gyroData.x * gyroData.x + gyroData.y * gyroData.y + gyroData.z * gyroData.z)
        
        // Higher gyro noise during rapid movements
        adaptiveGyroNoise = if (gyroMagnitude > 1.0f) {
            // Increase noise parameter during rapid rotation
            0.03f
        } else if (gyroMagnitude > 0.5f) {
            // Moderate rotation
            0.015f
        } else {
            // Slow or no rotation
            0.01f
        }
        
        // 2. Adjust magnetometer noise based on magnetic disturbance detection
        if (magData != null) {
            val currentMagValues = floatArrayOf(magData.x, magData.y, magData.z)
            
            // Calculate magnetic field change
            if (lastMagnetometerValues.any { it != 0f }) {
                val magDiffX = abs(currentMagValues[0] - lastMagnetometerValues[0])
                val magDiffY = abs(currentMagValues[1] - lastMagnetometerValues[1])
                val magDiffZ = abs(currentMagValues[2] - lastMagnetometerValues[2])
                
                // Calculate total magnitude change
                val magDiffTotal = magDiffX + magDiffY + magDiffZ
                
                // Update disturbance level with exponential smoothing
                magneticDisturbanceLevel = 0.9f * magneticDisturbanceLevel + 0.1f * magDiffTotal
                
                // Adjust magnetometer noise based on disturbance level
                adaptiveMagnetometerNoise = when {
                    magneticDisturbanceLevel > 5.0f -> 0.2f  // High disturbance
                    magneticDisturbanceLevel > 2.0f -> 0.1f  // Moderate disturbance
                    else -> 0.05f                            // Low disturbance
                }
            }
            
            // Update last values
            lastMagnetometerValues = currentMagValues.clone()
        }
        
        // 3. Adjust process noise based on motion consistency
        // Lower process noise during consistent motion, higher during erratic motion
        adaptiveProcessNoise = when {
            gyroMagnitude < 0.1f -> 0.0005f  // Very stable (almost stationary)
            gyroMagnitude < 0.3f -> 0.001f   // Stable motion
            gyroMagnitude < 0.7f -> 0.002f   // Moderate motion
            else -> 0.005f                   // Erratic motion
        }
        
        Timber.v("Adaptive noise: gyro=$adaptiveGyroNoise, mag=$adaptiveMagnetometerNoise, process=$adaptiveProcessNoise")
    }
    
    /**
     * Gets heading from magnetometer and accelerometer data.
     * 
     * @param accelData Accelerometer data
     * @param magData Magnetometer data
     * @return Heading in degrees (0-360, 0 = North, 90 = East, etc.) or NaN if calculation fails
     */
    private fun getHeadingFromMagnetometer(
        accelData: SensorData.Accelerometer,
        magData: SensorData.Magnetometer
    ): Float {
        // Get rotation matrix from accelerometer and magnetometer
        val success = SensorManager.getRotationMatrix(
            rotationMatrix,
            null,
            floatArrayOf(accelData.x, accelData.y, accelData.z),
            floatArrayOf(magData.x, magData.y, magData.z)
        )
        
        if (!success) {
            return Float.NaN
        }
        
        // Get orientation angles from rotation matrix
        SensorManager.getOrientation(rotationMatrix, orientationAngles)
        
        // Convert azimuth from radians to degrees
        val azimuthRadians = orientationAngles[0]
        val azimuthDegrees = Math.toDegrees(azimuthRadians.toDouble()).toFloat()
        
        // Normalize to 0-360 degrees
        return normalizeHeading(azimuthDegrees)
    }
    
    /**
     * Gets heading from rotation vector data.
     * 
     * @param rotVectorData Rotation vector sensor data
     * @return Heading in degrees (0-360, 0 = North, 90 = East, etc.)
     */
    private fun getHeadingFromRotationVector(rotVectorData: SensorData.RotationVector): Float {
        // Convert rotation vector to rotation matrix
        SensorManager.getRotationMatrixFromVector(
            rotationMatrix,
            floatArrayOf(rotVectorData.x, rotVectorData.y, rotVectorData.z, rotVectorData.w)
        )
        
        // Get orientation angles from rotation matrix
        SensorManager.getOrientation(rotationMatrix, orientationAngles)
        
        // Convert azimuth from radians to degrees
        val azimuthRadians = orientationAngles[0]
        val azimuthDegrees = Math.toDegrees(azimuthRadians.toDouble()).toFloat()
        
        // Normalize to 0-360 degrees
        return normalizeHeading(azimuthDegrees)
    }
    
    /**
     * Normalizes heading to 0-360 degrees.
     * 
     * @param heading Heading in degrees
     * @return Normalized heading in degrees (0-360)
     */
    private fun normalizeHeading(heading: Float): Float {
        var result = heading % 360
        if (result < 0) {
            result += 360
        }
        return result
    }
    
    /**
     * Converts variance to heading accuracy level.
     * 
     * @param variance Variance from Kalman filter
     * @return Heading accuracy level
     */
    private fun getAccuracyFromVariance(variance: Float): EstimateHeadingUseCase.HeadingAccuracy {
        return when {
            variance < 0.01f -> EstimateHeadingUseCase.HeadingAccuracy.HIGH
            variance < 0.1f -> EstimateHeadingUseCase.HeadingAccuracy.MEDIUM
            else -> EstimateHeadingUseCase.HeadingAccuracy.LOW
        }
    }
    
    /**
     * Resets the Kalman filter state.
     */
    fun reset() {
        heading = 0f
        headingRate = 0f
        variance = 10f
        rateVariance = 10f
        lastTimestamp = 0L
    }
    
    /**
     * Parameters for the KalmanHeadingUseCase.
     */
    data class Params(
        val gyroscopeData: SensorData.Gyroscope,
        val accelerometerData: SensorData.Accelerometer? = null,
        val magnetometerData: SensorData.Magnetometer? = null,
        val rotationVectorData: SensorData.RotationVector? = null,
        val gyroNoise: Float = 0.01f,
        val magnetometerNoise: Float = 0.05f,
        val rotVectorNoise: Float = 0.01f,
        val processNoise: Float = 0.001f
    )
    
    /**
     * Result of the Kalman heading estimation.
     */
    data class Result(
        val heading: Float,
        val headingRate: Float,
        val variance: Float,
        val headingAccuracy: EstimateHeadingUseCase.HeadingAccuracy,
        val timestamp: Long
    )
    
    companion object {
        /**
         * Conversion factor from radians to degrees.
         */
        const val RADIANS_TO_DEGREES = 57.2957795f // 180/π
        
        /**
         * Default gyroscope noise parameter.
         */
        const val DEFAULT_GYRO_NOISE = 0.01f
        
        /**
         * Default magnetometer noise parameter.
         */
        const val DEFAULT_MAGNETOMETER_NOISE = 0.05f
        
        /**
         * Default rotation vector noise parameter.
         */
        const val DEFAULT_ROTVECTOR_NOISE = 0.01f
        
        /**
         * Default process noise parameter.
         */
        const val DEFAULT_PROCESS_NOISE = 0.001f
    }
}