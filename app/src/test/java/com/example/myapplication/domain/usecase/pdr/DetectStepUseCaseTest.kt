package com.example.myapplication.domain.usecase.pdr

import com.example.myapplication.domain.model.SensorData
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

class DetectStepUseCaseTest {

    private lateinit var detectStepUseCase: DetectStepUseCase
    private lateinit var advancedStepDetectionUseCase: AdvancedStepDetectionUseCase
    
    @Before
    fun setUp() {
        detectStepUseCase = DetectStepUseCase()
        advancedStepDetectionUseCase = AdvancedStepDetectionUseCase()
        
        // Reset state before each test
        detectStepUseCase.reset()
        advancedStepDetectionUseCase.reset()
    }
    
    @Test
    fun testNoStepDetectedWithConstantAcceleration() {
        runBlocking {
            // Given - constant acceleration below threshold
            val accelData = SensorData.Accelerometer(
                x = 0f,
                y = 0f,
                z = 9.8f,  // Approximately gravity
                timestamp = 1000000000L
            )
            
            // When
            val result = detectStepUseCase(
                DetectStepUseCase.Params(
                    accelerometerData = accelData
                )
            )
            
            // Then
            assertFalse(result.stepDetected)
            assertEquals(0, result.stepCount)
        }
    }
    
    @Test
    fun testStepDetectionWithPeakValleyPattern() {
        runBlocking {
            // Given - a sequence of accelerations that form a peak-valley pattern
            
            // First, a low value (valley)
            val accelData1 = SensorData.Accelerometer(
                x = 0f,
                y = 0f,
                z = 9.0f,
                timestamp = 1000000000L
            )
            
            // Then a high value (peak)
            val accelData2 = SensorData.Accelerometer(
                x = 0f,
                y = 0f,
                z = 11.0f,
                timestamp = 1100000000L
            )
            
            // Then back to a low value (valley)
            val accelData3 = SensorData.Accelerometer(
                x = 0f,
                y = 0f,
                z = 9.0f,
                timestamp = 1200000000L
            )
            
            // When - process the sequence
            val result1 = detectStepUseCase(
                DetectStepUseCase.Params(
                    accelerometerData = accelData1
                )
            )
            
            val result2 = detectStepUseCase(
                DetectStepUseCase.Params(
                    accelerometerData = accelData2
                )
            )
            
            val result3 = detectStepUseCase(
                DetectStepUseCase.Params(
                    accelerometerData = accelData3
                )
            )
            
            // Then
            // No step should be detected for the first two readings
            assertFalse(result1.stepDetected)
            assertFalse(result2.stepDetected)
            
            // A step should be detected on the third reading when we complete the peak-valley pattern
            assertTrue(result3.stepDetected)
            assertEquals(1, result3.stepCount)
        }
    }
    
    @Test
    fun testNoStepDetectionWithInsufficientPeakValleyHeight() {
        runBlocking {
            // Given - a sequence with insufficient peak-valley height
            
            // First, a valley
            val accelData1 = SensorData.Accelerometer(
                x = 0f,
                y = 0f,
                z = 9.5f,
                timestamp = 1000000000L
            )
            
            // Then a small peak (not high enough)
            val accelData2 = SensorData.Accelerometer(
                x = 0f,
                y = 0f,
                z = 10.0f,  // Only 0.5 difference, less than minPeakValleyHeight
                timestamp = 1100000000L
            )
            
            // Then back to a valley
            val accelData3 = SensorData.Accelerometer(
                x = 0f,
                y = 0f,
                z = 9.5f,
                timestamp = 1200000000L
            )
            
            // When - process the sequence with a higher minPeakValleyHeight
            val result1 = detectStepUseCase(
                DetectStepUseCase.Params(
                    accelerometerData = accelData1,
                    minPeakValleyHeight = 0.7f  // Default is 0.7f
                )
            )
            
            val result2 = detectStepUseCase(
                DetectStepUseCase.Params(
                    accelerometerData = accelData2,
                    minPeakValleyHeight = 0.7f
                )
            )
            
            val result3 = detectStepUseCase(
                DetectStepUseCase.Params(
                    accelerometerData = accelData3,
                    minPeakValleyHeight = 0.7f
                )
            )
            
            // Then - no step should be detected
            assertFalse(result1.stepDetected)
            assertFalse(result2.stepDetected)
            assertFalse(result3.stepDetected)
            assertEquals(0, result3.stepCount)
        }
    }
    
    @Test
    fun testStepDetectionWithCustomThresholds() {
        runBlocking {
            // Given - a sequence with values that would only trigger with custom thresholds
            
            // First, a valley
            val accelData1 = SensorData.Accelerometer(
                x = 0f,
                y = 0f,
                z = 8.0f,
                timestamp = 1000000000L
            )
            
            // Then a peak
            val accelData2 = SensorData.Accelerometer(
                x = 0f,
                y = 0f,
                z = 9.0f,
                timestamp = 1100000000L
            )
            
            // Then back to a valley
            val accelData3 = SensorData.Accelerometer(
                x = 0f,
                y = 0f,
                z = 8.0f,
                timestamp = 1200000000L
            )
            
            // When - process the sequence with custom thresholds
            val result1 = detectStepUseCase(
                DetectStepUseCase.Params(
                    accelerometerData = accelData1,
                    peakThreshold = 8.5f,     // Lower than default
                    valleyThreshold = 8.2f,   // Lower than default
                    minPeakValleyHeight = 0.5f  // Lower than default
                )
            )
            
            val result2 = detectStepUseCase(
                DetectStepUseCase.Params(
                    accelerometerData = accelData2,
                    peakThreshold = 8.5f,
                    valleyThreshold = 8.2f,
                    minPeakValleyHeight = 0.5f
                )
            )
            
            val result3 = detectStepUseCase(
                DetectStepUseCase.Params(
                    accelerometerData = accelData3,
                    peakThreshold = 8.5f,
                    valleyThreshold = 8.2f,
                    minPeakValleyHeight = 0.5f
                )
            )
            
            // Then
            assertFalse(result1.stepDetected)
            assertFalse(result2.stepDetected)
            assertTrue(result3.stepDetected)  // Should detect with custom thresholds
            assertEquals(1, result3.stepCount)
        }
    }
    
    @Test
    fun testNoStepDetectionWithTooFrequentSteps() {
        runBlocking {
            // Given - a valid peak-valley pattern but with timestamps too close together
            
            // First, a valley
            val accelData1 = SensorData.Accelerometer(
                x = 0f,
                y = 0f,
                z = 9.0f,
                timestamp = 1000000000L
            )
            
            // Then a peak
            val accelData2 = SensorData.Accelerometer(
                x = 0f,
                y = 0f,
                z = 11.0f,
                timestamp = 1050000000L
            )
            
            // Then back to a valley (only 100ms after first valley, less than minStepInterval)
            val accelData3 = SensorData.Accelerometer(
                x = 0f,
                y = 0f,
                z = 9.0f,
                timestamp = 1100000000L
            )
            
            // When - process the sequence with a higher minStepInterval
            val result1 = detectStepUseCase(
                DetectStepUseCase.Params(
                    accelerometerData = accelData1,
                    minStepInterval = 200  // 200ms minimum between steps
                )
            )
            
            val result2 = detectStepUseCase(
                DetectStepUseCase.Params(
                    accelerometerData = accelData2,
                    minStepInterval = 200
                )
            )
            
            val result3 = detectStepUseCase(
                DetectStepUseCase.Params(
                    accelerometerData = accelData3,
                    minStepInterval = 200
                )
            )
            
            // Then - no step should be detected due to too frequent readings
            assertFalse(result1.stepDetected)
            assertFalse(result2.stepDetected)
            assertFalse(result3.stepDetected)
            assertEquals(0, result3.stepCount)
        }
    }
    
    @Test
    fun testAdvancedStepDetectionWithGyroscope() {
        runBlocking {
            // Given - a sequence with both accelerometer and gyroscope data
            
            // First, a valley with some gyro movement
            val accelData1 = SensorData.Accelerometer(
                x = 0f,
                y = 0f,
                z = 9.0f,
                timestamp = 1000000000L
            )
            val gyroData1 = SensorData.Gyroscope(
                x = 0.1f,
                y = 0.1f,
                z = 0.1f,
                timestamp = 1000000000L
            )
            
            // Then a peak with more gyro movement
            val accelData2 = SensorData.Accelerometer(
                x = 0f,
                y = 0f,
                z = 11.0f,
                timestamp = 1200000000L
            )
            val gyroData2 = SensorData.Gyroscope(
                x = 0.3f,
                y = 0.3f,
                z = 0.3f,
                timestamp = 1200000000L
            )
            
            // Then back to a valley with continued gyro movement
            val accelData3 = SensorData.Accelerometer(
                x = 0f,
                y = 0f,
                z = 9.0f,
                timestamp = 1400000000L
            )
            val gyroData3 = SensorData.Gyroscope(
                x = 0.2f,
                y = 0.2f,
                z = 0.2f,
                timestamp = 1400000000L
            )
            
            // When - process the sequence with the advanced detection
            val result1 = advancedStepDetectionUseCase(
                AdvancedStepDetectionUseCase.Params(
                    accelerometerData = accelData1,
                    gyroscopeData = gyroData1
                )
            )
            
            val result2 = advancedStepDetectionUseCase(
                AdvancedStepDetectionUseCase.Params(
                    accelerometerData = accelData2,
                    gyroscopeData = gyroData2
                )
            )
            
            val result3 = advancedStepDetectionUseCase(
                AdvancedStepDetectionUseCase.Params(
                    accelerometerData = accelData3,
                    gyroscopeData = gyroData3
                )
            )
            
            // Then
            assertFalse(result1.stepDetected)
            assertFalse(result2.stepDetected)
            
            // The advanced detection should recognize this as a step
            assertTrue(result3.stepDetected)
            assertEquals(1, result3.stepCount)
        }
    }
    
    @Test
    fun testAdvancedStepDetectionWithoutGyroscope() {
        runBlocking {
            // Given - a sequence with only accelerometer data (no gyroscope)
            
            // First, a valley
            val accelData1 = SensorData.Accelerometer(
                x = 0f,
                y = 0f,
                z = 9.0f,
                timestamp = 1000000000L
            )
            
            // Then a peak
            val accelData2 = SensorData.Accelerometer(
                x = 0f,
                y = 0f,
                z = 11.0f,
                timestamp = 1200000000L
            )
            
            // Then back to a valley
            val accelData3 = SensorData.Accelerometer(
                x = 0f,
                y = 0f,
                z = 9.0f,
                timestamp = 1400000000L
            )
            
            // When - process the sequence with the advanced detection but no gyro data
            val result1 = advancedStepDetectionUseCase(
                AdvancedStepDetectionUseCase.Params(
                    accelerometerData = accelData1,
                    gyroscopeData = null  // No gyroscope data
                )
            )
            
            val result2 = advancedStepDetectionUseCase(
                AdvancedStepDetectionUseCase.Params(
                    accelerometerData = accelData2,
                    gyroscopeData = null
                )
            )
            
            val result3 = advancedStepDetectionUseCase(
                AdvancedStepDetectionUseCase.Params(
                    accelerometerData = accelData3,
                    gyroscopeData = null
                )
            )
            
            // Then
            assertFalse(result1.stepDetected)
            assertFalse(result2.stepDetected)
            
            // Without gyroscope data, the step might not be detected due to gyroThreshold
            // This depends on the implementation, but we're testing the behavior
            assertFalse(result3.stepDetected)
            assertEquals(0, result3.stepCount)
        }
    }
    
    @Test
    fun testMultipleStepsDetection() {
        runBlocking {
            // Given - multiple peak-valley patterns
            
            // First step pattern
            val accelData1 = SensorData.Accelerometer(x = 0f, y = 0f, z = 9.0f, timestamp = 1000000000L)
            val accelData2 = SensorData.Accelerometer(x = 0f, y = 0f, z = 11.0f, timestamp = 1100000000L)
            val accelData3 = SensorData.Accelerometer(x = 0f, y = 0f, z = 9.0f, timestamp = 1200000000L)
            
            // Second step pattern (500ms later)
            val accelData4 = SensorData.Accelerometer(x = 0f, y = 0f, z = 9.0f, timestamp = 1700000000L)
            val accelData5 = SensorData.Accelerometer(x = 0f, y = 0f, z = 11.0f, timestamp = 1800000000L)
            val accelData6 = SensorData.Accelerometer(x = 0f, y = 0f, z = 9.0f, timestamp = 1900000000L)
            
            // When - process all readings
            val results = listOf(
                detectStepUseCase(DetectStepUseCase.Params(accelData1)),
                detectStepUseCase(DetectStepUseCase.Params(accelData2)),
                detectStepUseCase(DetectStepUseCase.Params(accelData3)),
                detectStepUseCase(DetectStepUseCase.Params(accelData4)),
                detectStepUseCase(DetectStepUseCase.Params(accelData5)),
                detectStepUseCase(DetectStepUseCase.Params(accelData6))
            )
            
            // Then
            // Only the 3rd and 6th readings should detect steps
            assertFalse(results[0].stepDetected)
            assertFalse(results[1].stepDetected)
            assertTrue(results[2].stepDetected)
            assertFalse(results[3].stepDetected)
            assertFalse(results[4].stepDetected)
            assertTrue(results[5].stepDetected)
            
            // Final step count should be 2
            assertEquals(2, results.last().stepCount)
        }
    }
}