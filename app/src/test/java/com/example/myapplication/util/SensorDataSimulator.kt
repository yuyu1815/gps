package com.example.myapplication.util

import com.example.myapplication.domain.model.SensorData
import com.example.myapplication.domain.model.Vector3D
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Utility class for generating simulated sensor data for testing purposes.
 * 
 * This class provides methods to generate realistic sensor data patterns
 * that can be used for testing algorithms without requiring actual hardware.
 */
class SensorDataSimulator {

    /**
     * Generates a sequence of accelerometer data simulating walking motion.
     * 
     * @param stepCount Number of steps to simulate
     * @param stepFrequency Steps per second (typical walking is 1.5-2.5 Hz)
     * @param startTime Starting timestamp in milliseconds
     * @param noise Amount of random noise to add (0.0-1.0)
     * @return List of accelerometer data points
     */
    fun generateWalkingAccelerometerData(
        stepCount: Int,
        stepFrequency: Float = 2.0f,
        startTime: Long = System.currentTimeMillis(),
        noise: Float = 0.1f
    ): List<SensorData.Accelerometer> {
        val result = mutableListOf<SensorData.Accelerometer>()
        val samplesPerStep = (SAMPLE_RATE_HZ / stepFrequency).toInt()
        val totalSamples = stepCount * samplesPerStep
        
        // Gravity component
        val gravity = Vector3D(0f, 0f, 9.81f)
        
        for (i in 0 until totalSamples) {
            // Calculate time
            val timeMs = startTime + (i * 1000L / SAMPLE_RATE_HZ)
            
            // Calculate phase within step cycle (0 to 2π)
            val phase = (i % samplesPerStep) * 2 * PI / samplesPerStep
            
            // Vertical acceleration pattern for walking
            // Typically has two peaks per step (heel strike and toe-off)
            val verticalAccel = sin(phase).toFloat() * 2.0f + 
                                sin(phase * 2).toFloat() * 0.5f
            
            // Horizontal acceleration patterns
            val forwardAccel = sin(phase + PI / 2).toFloat() * 0.8f
            val lateralAccel = sin(phase + PI / 4).toFloat() * 0.3f
            
            // Add random noise
            val noiseX = (Math.random() * 2 - 1).toFloat() * noise
            val noiseY = (Math.random() * 2 - 1).toFloat() * noise
            val noiseZ = (Math.random() * 2 - 1).toFloat() * noise
            
            // Combine components
            val accelX = forwardAccel + noiseX
            val accelY = lateralAccel + noiseY
            val accelZ = gravity.z + verticalAccel + noiseZ
            
            result.add(
                SensorData.Accelerometer(
                    x = accelX,
                    y = accelY,
                    z = accelZ,
                    timestamp = timeMs
                )
            )
        }
        
        return result
    }
    
    /**
     * Generates a sequence of gyroscope data simulating walking motion.
     * 
     * @param stepCount Number of steps to simulate
     * @param stepFrequency Steps per second
     * @param startTime Starting timestamp in milliseconds
     * @param noise Amount of random noise to add (0.0-1.0)
     * @return List of gyroscope data points
     */
    fun generateWalkingGyroscopeData(
        stepCount: Int,
        stepFrequency: Float = 2.0f,
        startTime: Long = System.currentTimeMillis(),
        noise: Float = 0.1f
    ): List<SensorData.Gyroscope> {
        val result = mutableListOf<SensorData.Gyroscope>()
        val samplesPerStep = (SAMPLE_RATE_HZ / stepFrequency).toInt()
        val totalSamples = stepCount * samplesPerStep
        
        for (i in 0 until totalSamples) {
            // Calculate time
            val timeMs = startTime + (i * 1000L / SAMPLE_RATE_HZ)
            
            // Calculate phase within step cycle (0 to 2π)
            val phase = (i % samplesPerStep) * 2 * PI / samplesPerStep
            
            // Rotation patterns during walking
            // Primarily rotation around y-axis (pitch) during walking
            val pitchRate = sin(phase).toFloat() * 0.2f
            
            // Some rotation around z-axis (yaw) for direction changes
            val yawRate = sin(phase * 0.5).toFloat() * 0.1f
            
            // Some rotation around x-axis (roll) for side-to-side motion
            val rollRate = sin(phase + PI / 2).toFloat() * 0.15f
            
            // Add random noise
            val noiseX = (Math.random() * 2 - 1).toFloat() * noise
            val noiseY = (Math.random() * 2 - 1).toFloat() * noise
            val noiseZ = (Math.random() * 2 - 1).toFloat() * noise
            
            result.add(
                SensorData.Gyroscope(
                    x = rollRate + noiseX,
                    y = pitchRate + noiseY,
                    z = yawRate + noiseZ,
                    timestamp = timeMs
                )
            )
        }
        
        return result
    }
    
    /**
     * Generates a sequence of magnetometer data simulating walking in a specific direction.
     * 
     * @param stepCount Number of steps to simulate
     * @param stepFrequency Steps per second
     * @param heading Heading direction in degrees (0-359, 0 = North, 90 = East, etc.)
     * @param startTime Starting timestamp in milliseconds
     * @param noise Amount of random noise to add (0.0-1.0)
     * @return List of magnetometer data points
     */
    fun generateWalkingMagnetometerData(
        stepCount: Int,
        stepFrequency: Float = 2.0f,
        heading: Float = 0f,
        startTime: Long = System.currentTimeMillis(),
        noise: Float = 0.1f
    ): List<SensorData.Magnetometer> {
        val result = mutableListOf<SensorData.Magnetometer>()
        val samplesPerStep = (SAMPLE_RATE_HZ / stepFrequency).toInt()
        val totalSamples = stepCount * samplesPerStep
        
        // Convert heading to radians
        val headingRad = heading * PI.toFloat() / 180f
        
        // Base magnetic field components (typical values for Earth's magnetic field)
        val baseFieldStrength = 45f // Typical value in μT
        
        // Calculate base field components based on heading
        val baseX = baseFieldStrength * cos(headingRad)
        val baseY = baseFieldStrength * sin(headingRad)
        val baseZ = baseFieldStrength * 0.5f // Downward component
        
        for (i in 0 until totalSamples) {
            // Calculate time
            val timeMs = startTime + (i * 1000L / SAMPLE_RATE_HZ)
            
            // Calculate phase within step cycle (0 to 2π)
            val phase = (i % samplesPerStep) * 2 * PI / samplesPerStep
            
            // Small variations in magnetic field due to device motion
            val varX = sin(phase).toFloat() * 0.5f
            val varY = sin(phase + PI / 2).toFloat() * 0.5f
            val varZ = sin(phase * 2).toFloat() * 0.3f
            
            // Add random noise
            val noiseX = (Math.random() * 2 - 1).toFloat() * noise * baseFieldStrength
            val noiseY = (Math.random() * 2 - 1).toFloat() * noise * baseFieldStrength
            val noiseZ = (Math.random() * 2 - 1).toFloat() * noise * baseFieldStrength
            
            result.add(
                SensorData.Magnetometer(
                    x = baseX + varX + noiseX,
                    y = baseY + varY + noiseY,
                    z = baseZ + varZ + noiseZ,
                    timestamp = timeMs
                )
            )
        }
        
        return result
    }
    
    /**
     * Generates a sequence of combined sensor data simulating walking motion.
     * 
     * @param stepCount Number of steps to simulate
     * @param stepFrequency Steps per second
     * @param heading Heading direction in degrees
     * @param startTime Starting timestamp in milliseconds
     * @param noise Amount of random noise to add (0.0-1.0)
     * @return List of combined sensor data points
     */
    fun generateWalkingCombinedData(
        stepCount: Int,
        stepFrequency: Float = 2.0f,
        heading: Float = 0f,
        startTime: Long = System.currentTimeMillis(),
        noise: Float = 0.1f
    ): List<SensorData.Combined> {
        val result = mutableListOf<SensorData.Combined>()
        val samplesPerStep = (SAMPLE_RATE_HZ / stepFrequency).toInt()
        val totalSamples = stepCount * samplesPerStep
        
        // Generate individual sensor data
        val accelData = generateWalkingAccelerometerData(stepCount, stepFrequency, startTime, noise)
        val gyroData = generateWalkingGyroscopeData(stepCount, stepFrequency, startTime, noise)
        val magData = generateWalkingMagnetometerData(stepCount, stepFrequency, heading, startTime, noise)
        
        // Combine data
        for (i in 0 until totalSamples) {
            val timeMs = startTime + (i * 1000L / SAMPLE_RATE_HZ)
            
            // Calculate step detection
            // Typically a step is detected at specific phases of the walking cycle
            val stepPhase = (i % samplesPerStep) * 2 * PI / samplesPerStep
            val stepDetected = stepPhase in (PI * 0.8)..(PI * 1.2)
            
            // Calculate step length (typically 0.7-0.8m for normal walking)
            val stepLength = if (stepDetected) 0.75f else 0f
            
            result.add(
                SensorData.Combined(
                    timestamp = timeMs,
                    accelerometer = Vector3D(
                        x = accelData[i].x,
                        y = accelData[i].y,
                        z = accelData[i].z
                    ),
                    gyroscope = Vector3D(
                        x = gyroData[i].x,
                        y = gyroData[i].y,
                        z = gyroData[i].z
                    ),
                    magnetometer = Vector3D(
                        x = magData[i].x,
                        y = magData[i].y,
                        z = magData[i].z
                    ),
                    orientation = Vector3D(), // Would need additional calculation
                    linearAcceleration = Vector3D(
                        x = accelData[i].x,
                        y = accelData[i].y,
                        z = accelData[i].z - 9.81f // Remove gravity
                    ),
                    gravity = Vector3D(0f, 0f, 9.81f),
                    stepDetected = stepDetected,
                    stepLength = stepLength,
                    heading = heading
                )
            )
        }
        
        return result
    }
    
    companion object {
        /**
         * Sample rate for simulated sensor data in Hz.
         */
        const val SAMPLE_RATE_HZ = 50
    }
}