package com.example.myapplication.domain.usecase.pdr

import com.example.myapplication.domain.model.SensorData
import com.example.myapplication.domain.model.Vector3D
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.math.abs
import kotlin.math.sqrt

class EstimateStepLengthUseCaseTest {

    private lateinit var estimateStepLengthUseCase: EstimateStepLengthUseCase
    
    // Test constants
    private val userHeight = 1.75f // 175cm
    private val defaultCalibrationFactor = 1.0f
    
    @Before
    fun setUp() {
        estimateStepLengthUseCase = EstimateStepLengthUseCase()
    }
    
    @Test
    fun initialStepLength_isProportionalToUserHeight() = runBlocking {
        // Given
        val sensorData = createSensorData(
            accelX = 0f, accelY = 0f, accelZ = 9.8f,
            gyroX = 0f, gyroY = 0f, gyroZ = 0f,
            timestamp = 0L
        )
        
        val params = EstimateStepLengthUseCase.Params(
            sensorData = sensorData,
            userHeight = userHeight,
            calibrationFactor = defaultCalibrationFactor
        )
        
        // When
        val stepLength = estimateStepLengthUseCase.invoke(params)
        
        // Then
        // Step length should be approximately 0.41 * height for normal walking
        val expectedStepLength = 0.41f * userHeight
        assertTrue(
            "Step length should be proportional to user height",
            abs(stepLength - expectedStepLength) < 0.1f
        )
    }
    
    @Test
    fun stepLength_increasesWithHigherAcceleration() = runBlocking {
        // First get a baseline with normal acceleration
        val normalSensorData = createSensorData(
            accelX = 0f, accelY = 0f, accelZ = 9.8f,
            gyroX = 0f, gyroY = 0f, gyroZ = 0f,
            timestamp = 0L
        )
        
        val normalParams = EstimateStepLengthUseCase.Params(
            sensorData = normalSensorData,
            userHeight = userHeight,
            calibrationFactor = defaultCalibrationFactor
        )
        
        val normalStepLength = estimateStepLengthUseCase.invoke(normalParams)
        
        // Then test with higher acceleration
        val highAccelSensorData = createSensorData(
            accelX = 0f, accelY = 0f, accelZ = 15f, // Higher acceleration
            gyroX = 0f, gyroY = 0f, gyroZ = 0f,
            timestamp = 1_000_000_000L // 1 second later
        )
        
        val highAccelParams = EstimateStepLengthUseCase.Params(
            sensorData = highAccelSensorData,
            userHeight = userHeight,
            calibrationFactor = defaultCalibrationFactor
        )
        
        val highAccelStepLength = estimateStepLengthUseCase.invoke(highAccelParams)
        
        // Higher acceleration should result in longer step length
        assertTrue(
            "Step length should increase with higher acceleration",
            highAccelStepLength > normalStepLength
        )
    }
    
    @Test
    fun walkingPattern_affectsStepLength() = runBlocking {
        // Simulate a sequence of steps with different patterns
        
        // First, simulate normal walking
        simulateWalkingPattern(
            EstimateStepLengthUseCase.WalkingPattern.NORMAL,
            accelMagnitude = 10f,
            stepFrequency = 1.8f
        )
        
        val normalStepLength = getCurrentStepLength()
        
        // Then simulate running
        simulateWalkingPattern(
            EstimateStepLengthUseCase.WalkingPattern.RUNNING,
            accelMagnitude = 18f,
            stepFrequency = 3.0f
        )
        
        val runningStepLength = getCurrentStepLength()
        
        // Then simulate slow walking
        simulateWalkingPattern(
            EstimateStepLengthUseCase.WalkingPattern.SLOW,
            accelMagnitude = 3.5f,
            stepFrequency = 1.2f
        )
        
        val slowStepLength = getCurrentStepLength()
        
        // Verify pattern affects step length
        assertTrue(
            "Running should have longer step length than normal walking",
            runningStepLength > normalStepLength
        )
        
        assertTrue(
            "Slow walking should have shorter step length than normal walking",
            slowStepLength < normalStepLength
        )
    }
    
    @Test
    fun calibrationFactor_scalesStepLength() = runBlocking {
        // Given
        val sensorData = createSensorData(
            accelX = 0f, accelY = 0f, accelZ = 9.8f,
            gyroX = 0f, gyroY = 0f, gyroZ = 0f,
            timestamp = 0L
        )
        
        // First get baseline with default calibration
        val defaultParams = EstimateStepLengthUseCase.Params(
            sensorData = sensorData,
            userHeight = userHeight,
            calibrationFactor = 1.0f
        )
        
        val defaultStepLength = estimateStepLengthUseCase.invoke(defaultParams)
        
        // Then test with higher calibration factor
        val highCalibParams = EstimateStepLengthUseCase.Params(
            sensorData = sensorData,
            userHeight = userHeight,
            calibrationFactor = 1.5f
        )
        
        val highCalibStepLength = estimateStepLengthUseCase.invoke(highCalibParams)
        
        // Then test with lower calibration factor
        val lowCalibParams = EstimateStepLengthUseCase.Params(
            sensorData = sensorData,
            userHeight = userHeight,
            calibrationFactor = 0.8f
        )
        
        val lowCalibStepLength = estimateStepLengthUseCase.invoke(lowCalibParams)
        
        // Verify calibration factor scales step length proportionally
        assertEquals(
            "Calibration factor should scale step length proportionally",
            defaultStepLength * 1.5f,
            highCalibStepLength,
            0.01f
        )
        
        assertEquals(
            "Calibration factor should scale step length proportionally",
            defaultStepLength * 0.8f,
            lowCalibStepLength,
            0.01f
        )
    }
    
    @Test
    fun reset_clearsStepLengthState() = runBlocking {
        // First, simulate a sequence of steps to build up state
        simulateWalkingPattern(
            EstimateStepLengthUseCase.WalkingPattern.RUNNING,
            accelMagnitude = 18f,
            stepFrequency = 3.0f
        )
        
        val lengthBeforeReset = getCurrentStepLength()
        
        // Reset the use case
        estimateStepLengthUseCase.reset()
        
        // Get step length after reset
        val sensorData = createSensorData(
            accelX = 0f, accelY = 0f, accelZ = 9.8f,
            gyroX = 0f, gyroY = 0f, gyroZ = 0f,
            timestamp = 10_000_000_000L
        )
        
        val params = EstimateStepLengthUseCase.Params(
            sensorData = sensorData,
            userHeight = userHeight,
            calibrationFactor = defaultCalibrationFactor
        )
        
        val lengthAfterReset = estimateStepLengthUseCase.invoke(params)
        
        // After reset, step length should be different from running pattern
        assertTrue(
            "Step length should be reset to default after reset()",
            abs(lengthAfterReset - lengthBeforeReset) > 0.1f
        )
    }
    
    /**
     * Helper method to create sensor data with specified values.
     */
    private fun createSensorData(
        accelX: Float, accelY: Float, accelZ: Float,
        gyroX: Float, gyroY: Float, gyroZ: Float,
        timestamp: Long
    ): SensorData.Combined {
        val accel = SensorData.Accelerometer(accelX, accelY, accelZ, timestamp)
        val gyro = SensorData.Gyroscope(gyroX, gyroY, gyroZ, timestamp)
        
        return SensorData.Combined(
            accelerometer = accel.toVector3D(),
            gyroscope = gyro.toVector3D(),
            magnetometer = Vector3D(), // Default empty vector instead of null
            rotationVector = null,
            linearAcceleration = accel.toVector3D(),
            timestamp = timestamp
        )
    }
    
    /**
     * Helper method to simulate a walking pattern by sending multiple sensor readings.
     */
    private suspend fun simulateWalkingPattern(
        pattern: EstimateStepLengthUseCase.WalkingPattern,
        accelMagnitude: Float,
        stepFrequency: Float
    ) {
        // Calculate time between steps based on frequency
        val stepInterval = (1_000_000_000L / stepFrequency).toLong()
        
        // Simulate 10 steps with the given pattern
        for (i in 0 until 10) {
            val timestamp = i * stepInterval
            
            // Create sensor data that would trigger the desired pattern
            val sensorData = when (pattern) {
                EstimateStepLengthUseCase.WalkingPattern.RUNNING -> {
                    createSensorData(
                        accelX = 0f, accelY = 0f, accelZ = accelMagnitude,
                        gyroX = 0.5f, gyroY = 0.5f, gyroZ = 0.5f,
                        timestamp = timestamp
                    )
                }
                EstimateStepLengthUseCase.WalkingPattern.FAST -> {
                    createSensorData(
                        accelX = 0f, accelY = 0f, accelZ = accelMagnitude,
                        gyroX = 0.3f, gyroY = 0.3f, gyroZ = 0.3f,
                        timestamp = timestamp
                    )
                }
                EstimateStepLengthUseCase.WalkingPattern.NORMAL -> {
                    createSensorData(
                        accelX = 0f, accelY = 0f, accelZ = accelMagnitude,
                        gyroX = 0.2f, gyroY = 0.2f, gyroZ = 0.2f,
                        timestamp = timestamp
                    )
                }
                EstimateStepLengthUseCase.WalkingPattern.SLOW -> {
                    createSensorData(
                        accelX = 0f, accelY = 0f, accelZ = accelMagnitude,
                        gyroX = 0.1f, gyroY = 0.1f, gyroZ = 0.1f,
                        timestamp = timestamp
                    )
                }
                EstimateStepLengthUseCase.WalkingPattern.SHUFFLE -> {
                    createSensorData(
                        accelX = 0f, accelY = 0f, accelZ = accelMagnitude,
                        gyroX = 0.4f, gyroY = 0.4f, gyroZ = 0.4f,
                        timestamp = timestamp
                    )
                }
                EstimateStepLengthUseCase.WalkingPattern.IRREGULAR -> {
                    createSensorData(
                        accelX = 0f, accelY = 0f, accelZ = accelMagnitude,
                        gyroX = if (i % 2 == 0) 0.5f else 0.1f,
                        gyroY = if (i % 3 == 0) 0.5f else 0.1f,
                        gyroZ = if (i % 4 == 0) 0.5f else 0.1f,
                        timestamp = timestamp
                    )
                }
            }
            
            val params = EstimateStepLengthUseCase.Params(
                sensorData = sensorData,
                userHeight = userHeight,
                calibrationFactor = defaultCalibrationFactor
            )
            
            estimateStepLengthUseCase.invoke(params)
        }
    }
    
    /**
     * Helper method to get the current step length.
     */
    private suspend fun getCurrentStepLength(): Float {
        val sensorData = createSensorData(
            accelX = 0f, accelY = 0f, accelZ = 9.8f,
            gyroX = 0f, gyroY = 0f, gyroZ = 0f,
            timestamp = System.nanoTime()
        )
        
        val params = EstimateStepLengthUseCase.Params(
            sensorData = sensorData,
            userHeight = userHeight,
            calibrationFactor = defaultCalibrationFactor
        )
        
        return estimateStepLengthUseCase.invoke(params)
    }
}