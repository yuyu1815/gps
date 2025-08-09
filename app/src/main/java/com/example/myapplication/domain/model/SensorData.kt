package com.example.myapplication.domain.model

import android.hardware.SensorManager

/**
 * Represents sensor data from the device's motion and position sensors.
 * Used for Pedestrian Dead Reckoning (PDR) and sensor fusion.
 */
sealed class SensorData {
    /**
     * Represents accelerometer sensor data.
     * 
     * @param x X-axis acceleration in m/s²
     * @param y Y-axis acceleration in m/s²
     * @param z Z-axis acceleration in m/s²
     * @param timestamp Timestamp in nanoseconds
     */
    data class Accelerometer(
        val x: Float = 0f,
        val y: Float = 0f,
        val z: Float = 0f,
        val timestamp: Long = 0L
    ) : SensorData() {
        /**
         * Converts to Vector3D representation.
         */
        fun toVector3D(): Vector3D = Vector3D(x, y, z)
        
        /**
         * Calculates the magnitude of the acceleration vector.
         */
        fun magnitude(): Float = Math.sqrt((x * x + y * y + z * z).toDouble()).toFloat()
    }
    
    /**
     * Represents gyroscope sensor data.
     * 
     * @param x X-axis rotation in rad/s
     * @param y Y-axis rotation in rad/s
     * @param z Z-axis rotation in rad/s
     * @param timestamp Timestamp in nanoseconds
     */
    data class Gyroscope(
        val x: Float = 0f,
        val y: Float = 0f,
        val z: Float = 0f,
        val timestamp: Long = 0L
    ) : SensorData() {
        /**
         * Converts to Vector3D representation.
         */
        fun toVector3D(): Vector3D = Vector3D(x, y, z)
        
        /**
         * Calculates the magnitude of the rotation vector.
         */
        fun magnitude(): Float = Math.sqrt((x * x + y * y + z * z).toDouble()).toFloat()
    }
    
    /**
     * Represents magnetometer sensor data.
     * 
     * @param x X-axis magnetic field in μT
     * @param y Y-axis magnetic field in μT
     * @param z Z-axis magnetic field in μT
     * @param timestamp Timestamp in nanoseconds
     */
    data class Magnetometer(
        val x: Float = 0f,
        val y: Float = 0f,
        val z: Float = 0f,
        val timestamp: Long = 0L
    ) : SensorData() {
        /**
         * Converts to Vector3D representation.
         */
        fun toVector3D(): Vector3D = Vector3D(x, y, z)
        
        /**
         * Calculates the magnitude of the magnetic field vector.
         */
        fun magnitude(): Float = Math.sqrt((x * x + y * y + z * z).toDouble()).toFloat()
    }
    
    /**
     * Represents rotation vector sensor data.
     * 
     * @param x X component of the rotation vector (x * sin(θ/2))
     * @param y Y component of the rotation vector (y * sin(θ/2))
     * @param z Z component of the rotation vector (z * sin(θ/2))
     * @param w W component of the rotation vector (cos(θ/2))
     * @param timestamp Timestamp in nanoseconds
     */
    data class RotationVector(
        val x: Float = 0f,
        val y: Float = 0f,
        val z: Float = 0f,
        val w: Float = 0f,
        val timestamp: Long = 0L
    ) : SensorData() {
        /**
         * Converts to Vector3D representation (ignoring w component).
         */
        fun toVector3D(): Vector3D = Vector3D(x, y, z)
        
        /**
         * Calculates the heading (azimuth) in degrees.
         * 0 = North, 90 = East, 180 = South, 270 = West.
         */
        fun getHeading(): Float {
            // Convert rotation vector to rotation matrix
            val rotationMatrix = FloatArray(9)
            val orientationAngles = FloatArray(3)
            
            SensorManager.getRotationMatrixFromVector(rotationMatrix, floatArrayOf(x, y, z, w))
            
            // Get orientation angles from rotation matrix
            SensorManager.getOrientation(rotationMatrix, orientationAngles)
            
            // Convert azimuth from radians to degrees
            val azimuthRadians = orientationAngles[0]
            val azimuthDegrees = Math.toDegrees(azimuthRadians.toDouble()).toFloat()
            
            // Normalize to 0-360 degrees
            return (azimuthDegrees + 360) % 360
        }
    }
    
    /**
     * Represents combined sensor data for PDR.
     * 
     * @param timestamp Timestamp when the data was collected (in milliseconds)
     * @param accelerometer Accelerometer data in m/s²
     * @param gyroscope Gyroscope data in rad/s
     * @param magnetometer Magnetometer data in μT
     * @param rotationVector Rotation vector data
     * @param orientation Device orientation in degrees
     * @param linearAcceleration Linear acceleration (acceleration minus gravity) in m/s²
     * @param gravity Gravity vector in m/s²
     * @param stepDetected True if a step was detected at this timestamp
     * @param stepLength Estimated step length in meters
     * @param heading Heading direction in degrees (0-359)
     */
    data class Combined(
        val timestamp: Long = System.currentTimeMillis(),
        val accelerometer: Vector3D = Vector3D(),
        val gyroscope: Vector3D = Vector3D(),
        val magnetometer: Vector3D = Vector3D(),
        val rotationVector: RotationVector? = null,
        val orientation: Vector3D = Vector3D(),
        val linearAcceleration: Vector3D = Vector3D(),
        val gravity: Vector3D = Vector3D(),
        val stepDetected: Boolean = false,
        val stepLength: Float = 0f,
        val heading: Float = 0f
    ) : SensorData() {
        /**
         * Calculates the magnitude of the acceleration vector.
         */
        fun accelerationMagnitude(): Float {
            return accelerometer.magnitude()
        }
        
        /**
         * Calculates the magnitude of the linear acceleration vector.
         */
        fun linearAccelerationMagnitude(): Float {
            return linearAcceleration.magnitude()
        }
        
        /**
         * Calculates the magnitude of the gyroscope vector.
         */
        fun rotationMagnitude(): Float {
            return gyroscope.magnitude()
        }
        
        /**
         * Checks if the device is stationary based on acceleration and rotation.
         */
        fun isStationary(accelerationThreshold: Float = 0.1f, rotationThreshold: Float = 0.1f): Boolean {
            return linearAccelerationMagnitude() < accelerationThreshold && 
                   rotationMagnitude() < rotationThreshold
        }
    }
}

/**
 * Represents a 3D vector with x, y, and z components.
 * Used for sensor data representation.
 */
data class Vector3D(
    val x: Float = 0f,
    val y: Float = 0f,
    val z: Float = 0f
) {
    /**
     * Calculates the magnitude (length) of the vector.
     */
    fun magnitude(): Float {
        return Math.sqrt((x * x + y * y + z * z).toDouble()).toFloat()
    }
    
    /**
     * Adds another vector to this one.
     */
    operator fun plus(other: Vector3D): Vector3D {
        return Vector3D(
            x = this.x + other.x,
            y = this.y + other.y,
            z = this.z + other.z
        )
    }
    
    /**
     * Subtracts another vector from this one.
     */
    operator fun minus(other: Vector3D): Vector3D {
        return Vector3D(
            x = this.x - other.x,
            y = this.y - other.y,
            z = this.z - other.z
        )
    }
    
    /**
     * Multiplies the vector by a scalar.
     */
    operator fun times(scalar: Float): Vector3D {
        return Vector3D(
            x = this.x * scalar,
            y = this.y * scalar,
            z = this.z * scalar
        )
    }
    
    /**
     * Divides the vector by a scalar.
     */
    operator fun div(scalar: Float): Vector3D {
        if (scalar == 0f) {
            throw IllegalArgumentException("Cannot divide by zero")
        }
        return Vector3D(
            x = this.x / scalar,
            y = this.y / scalar,
            z = this.z / scalar
        )
    }
    
    /**
     * Calculates the dot product with another vector.
     */
    fun dot(other: Vector3D): Float {
        return this.x * other.x + this.y * other.y + this.z * other.z
    }
    
    /**
     * Calculates the cross product with another vector.
     */
    fun cross(other: Vector3D): Vector3D {
        return Vector3D(
            x = this.y * other.z - this.z * other.y,
            y = this.z * other.x - this.x * other.z,
            z = this.x * other.y - this.y * other.x
        )
    }
    
    /**
     * Returns a normalized version of the vector (unit vector).
     */
    fun normalize(): Vector3D {
        val mag = magnitude()
        return if (mag > 0) {
            this / mag
        } else {
            this
        }
    }
}