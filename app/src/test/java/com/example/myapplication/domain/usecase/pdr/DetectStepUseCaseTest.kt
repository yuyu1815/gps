package com.example.myapplication.domain.usecase.pdr

import com.example.myapplication.domain.model.SensorData
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

class DetectStepUseCaseTest {

    private lateinit var detectStepUseCase: DetectStepUseCase
    private lateinit var advancedStepDetectionUseCase: AdvancedStepDetectionUseCase

    @Before
    fun setUp() {
        detectStepUseCase = DetectStepUseCase()
        advancedStepDetectionUseCase = AdvancedStepDetectionUseCase()
    }

    @Test
    fun basicStepDetection_noStepDetectedWithConstantAcceleration() = runBlocking {
        // Given constant acceleration below threshold
        val accelData = SensorData.Accelerometer(0f, 0f, 9.8f, 0L)
        val params = DetectStepUseCase.Params(accelData)

        // When
        val result = detectStepUseCase.invoke(params)

        // Then
        assertFalse(result.stepDetected)
        assertEquals(0, result.stepCount)
    }

    @Test
    fun basicStepDetection_stepDetectedWithPeakValleyPattern() = runBlocking {
        // First, simulate acceleration below threshold
        var accelData = SensorData.Accelerometer(0f, 0f, 9.5f, 0L)
        var params = DetectStepUseCase.Params(accelData)
        detectStepUseCase.invoke(params)

        // Then, simulate a peak
        accelData = SensorData.Accelerometer(0f, 0f, 12f, 100_000_000L) // 100ms later
        params = DetectStepUseCase.Params(accelData)
        var result = detectStepUseCase.invoke(params)
        assertFalse(result.stepDetected) // Peak alone shouldn't trigger step

        // Then, simulate a valley
        accelData = SensorData.Accelerometer(0f, 0f, 8f, 300_000_000L) // 300ms later
        params = DetectStepUseCase.Params(accelData)
        result = detectStepUseCase.invoke(params)
        assertTrue(result.stepDetected) // Peak-valley pattern should trigger step
        assertEquals(1, result.stepCount)
    }

    @Test
    fun advancedStepDetection_noStepDetectedWithConstantAcceleration() = runBlocking {
        // Given constant acceleration below threshold
        val accelData = SensorData.Accelerometer(0f, 0f, 9.8f, 0L)
        val gyroData = SensorData.Gyroscope(0f, 0f, 0f, 0L)
        val params = AdvancedStepDetectionUseCase.Params(accelData, gyroData)

        // When
        val result = advancedStepDetectionUseCase.invoke(params)

        // Then
        assertFalse(result.stepDetected)
        assertEquals(0, result.stepCount)
    }

    @Test
    fun advancedStepDetection_stepDetectedWithRealisticWalkingPattern() = runBlocking {
        // Simulate a realistic walking pattern with both accelerometer and gyroscope data
        
        // Start with normal gravity
        var accelData = SensorData.Accelerometer(0f, 0f, 9.8f, 0L)
        var gyroData = SensorData.Gyroscope(0f, 0f, 0f, 0L)
        var params = AdvancedStepDetectionUseCase.Params(accelData, gyroData)
        advancedStepDetectionUseCase.invoke(params)
        
        // Simulate the acceleration pattern of a step
        val timestamps = LongArray(10) { it * 100_000_000L } // 100ms intervals
        
        // Create a realistic acceleration pattern (peak followed by valley)
        val accelValues = floatArrayOf(10.2f, 11.5f, 12.8f, 11.5f, 10.2f, 9.0f, 8.5f, 9.0f, 9.5f, 9.8f)
        
        // Create corresponding gyroscope data (rotation during step)
        val gyroValues = floatArrayOf(0.1f, 0.3f, 0.5f, 0.3f, 0.1f, -0.1f, -0.3f, -0.1f, 0f, 0f)
        
        var stepDetected = false
        
        // Process the simulated sensor data
        for (i in timestamps.indices) {
            accelData = SensorData.Accelerometer(0f, 0f, accelValues[i], timestamps[i])
            gyroData = SensorData.Gyroscope(0f, gyroValues[i], 0f, timestamps[i])
            params = AdvancedStepDetectionUseCase.Params(accelData, gyroData)
            val result = advancedStepDetectionUseCase.invoke(params)
            
            if (result.stepDetected) {
                stepDetected = true
                break
            }
        }
        
        // A step should be detected within this pattern
        assertTrue(stepDetected)
    }

    @Test
    fun advancedStepDetection_rejectsFalsePatterns() = runBlocking {
        // Simulate patterns that shouldn't be detected as steps
        
        // Small acceleration changes (below threshold)
        var stepDetected = false
        for (i in 0 until 10) {
            val accelData = SensorData.Accelerometer(0f, 0f, 9.8f + (i % 3) * 0.1f, i * 100_000_000L)
            val gyroData = SensorData.Gyroscope(0f, 0f, 0f, i * 100_000_000L)
            val params = AdvancedStepDetectionUseCase.Params(accelData, gyroData)
            val result = advancedStepDetectionUseCase.invoke(params)
            
            if (result.stepDetected) {
                stepDetected = true
                break
            }
        }
        
        // Small variations shouldn't trigger step detection
        assertFalse(stepDetected)
        
        // Reset
        advancedStepDetectionUseCase.reset()
        
        // Too rapid changes (faster than possible steps)
        stepDetected = false
        for (i in 0 until 10) {
            val accelData = SensorData.Accelerometer(0f, 0f, 9.8f + sin(i.toFloat()) * 3f, i * 10_000_000L) // 10ms intervals
            val gyroData = SensorData.Gyroscope(0f, sin(i.toFloat()) * 0.5f, 0f, i * 10_000_000L)
            val params = AdvancedStepDetectionUseCase.Params(accelData, gyroData)
            val result = advancedStepDetectionUseCase.invoke(params)
            
            if (result.stepDetected) {
                stepDetected = true
                break
            }
        }
        
        // Too rapid changes shouldn't trigger step detection
        assertFalse(stepDetected)
    }

    @Test
    fun reset_clearsStepCount() = runBlocking {
        // First, generate a step
        var accelData = SensorData.Accelerometer(0f, 0f, 9.5f, 0L)
        var params = DetectStepUseCase.Params(accelData)
        detectStepUseCase.invoke(params)

        accelData = SensorData.Accelerometer(0f, 0f, 12f, 100_000_000L)
        params = DetectStepUseCase.Params(accelData)
        detectStepUseCase.invoke(params)

        accelData = SensorData.Accelerometer(0f, 0f, 8f, 300_000_000L)
        params = DetectStepUseCase.Params(accelData)
        var result = detectStepUseCase.invoke(params)
        
        // Verify step was detected
        assertTrue(result.stepDetected)
        assertEquals(1, result.stepCount)
        
        // Reset the use case
        detectStepUseCase.reset()
        
        // Verify step count is reset
        accelData = SensorData.Accelerometer(0f, 0f, 9.5f, 400_000_000L)
        params = DetectStepUseCase.Params(accelData)
        result = detectStepUseCase.invoke(params)
        assertEquals(0, result.stepCount)
    }
}