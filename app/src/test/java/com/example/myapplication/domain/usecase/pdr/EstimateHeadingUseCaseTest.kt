package com.example.myapplication.domain.usecase.pdr

import com.example.myapplication.domain.model.SensorData
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.math.abs

class EstimateHeadingUseCaseTest {

    private lateinit var estimateHeadingUseCase: EstimateHeadingUseCase
    private lateinit var kalmanHeadingUseCase: KalmanHeadingUseCase

    @Before
    fun setUp() {
        estimateHeadingUseCase = EstimateHeadingUseCase()
        kalmanHeadingUseCase = KalmanHeadingUseCase()
    }

    @Test
    fun basicHeadingEstimation_returnsValidHeading() = runBlocking {
        // Given gyroscope data
        val gyroData = SensorData.Gyroscope(0f, 0f, 0.1f, 0L)
        val params = EstimateHeadingUseCase.Params(gyroData)

        // When
        val result = estimateHeadingUseCase.invoke(params)

        // Then
        assertTrue(result.heading >= 0f && result.heading < 360f)
        assertEquals(EstimateHeadingUseCase.HeadingSource.GYROSCOPE, result.headingSource)
    }

    @Test
    fun headingEstimation_updatesWithGyroscopeRotation() = runBlocking {
        // First call to initialize
        var gyroData = SensorData.Gyroscope(0f, 0f, 0f, 0L)
        var params = EstimateHeadingUseCase.Params(gyroData)
        var result = estimateHeadingUseCase.invoke(params)
        val initialHeading = result.heading

        // Simulate rotation around z-axis (90 degrees per second, for 0.5 seconds)
        gyroData = SensorData.Gyroscope(0f, 0f, 1.571f, 500_000_000L) // π/2 radians/s = 90 degrees/s
        params = EstimateHeadingUseCase.Params(gyroData)
        result = estimateHeadingUseCase.invoke(params)

        // Heading should change by approximately 45 degrees (90 degrees/s * 0.5s)
        // Allow for some error due to filtering
        val expectedChange = 45f
        val actualChange = normalizeHeadingDifference(result.heading - initialHeading)
        
        assertTrue("Expected heading change around $expectedChange degrees, but was $actualChange", 
            abs(actualChange - expectedChange) < 10f)
    }

    @Test
    fun kalmanHeadingEstimation_returnsValidHeading() = runBlocking {
        // Given gyroscope data
        val gyroData = SensorData.Gyroscope(0f, 0f, 0.1f, 0L)
        val params = KalmanHeadingUseCase.Params(gyroData)

        // When
        val result = kalmanHeadingUseCase.invoke(params)

        // Then
        assertTrue(result.heading >= 0f && result.heading < 360f)
        assertTrue(result.variance > 0f) // Should have some uncertainty
    }

    @Test
    fun kalmanHeadingEstimation_fusesGyroscopeAndMagnetometer() = runBlocking {
        // First call to initialize
        var gyroData = SensorData.Gyroscope(0f, 0f, 0f, 0L)
        var accelData = SensorData.Accelerometer(0f, 0f, 9.8f, 0L)
        var magData = SensorData.Magnetometer(20f, 0f, 40f, 0L) // Pointing north
        
        var params = KalmanHeadingUseCase.Params(
            gyroscopeData = gyroData,
            accelerometerData = accelData,
            magnetometerData = magData
        )
        
        var result = kalmanHeadingUseCase.invoke(params)
        val initialHeading = result.heading
        val initialVariance = result.variance
        
        // Simulate gyroscope indicating rotation, but magnetometer still pointing same direction
        // This should result in a fusion where the heading changes less than what gyro alone would indicate
        gyroData = SensorData.Gyroscope(0f, 0f, 0.5f, 1_000_000_000L) // 0.5 rad/s for 1 second
        accelData = SensorData.Accelerometer(0f, 0f, 9.8f, 1_000_000_000L)
        magData = SensorData.Magnetometer(20f, 0f, 40f, 1_000_000_000L) // Still pointing north
        
        params = KalmanHeadingUseCase.Params(
            gyroscopeData = gyroData,
            accelerometerData = accelData,
            magnetometerData = magData
        )
        
        result = kalmanHeadingUseCase.invoke(params)
        
        // Variance should decrease with absolute measurements
        assertTrue("Variance should decrease with absolute measurements", 
            result.variance < initialVariance)
            
        // Heading should change due to gyro, but less than expected due to fusion with magnetometer
        val gyroOnlyChange = 0.5f * 57.2957795f // 0.5 rad/s * 1s converted to degrees
        val actualChange = normalizeHeadingDifference(result.heading - initialHeading)
        
        assertTrue("Heading change should be less than gyro-only prediction", 
            actualChange < gyroOnlyChange)
    }

    @Test
    fun reset_clearsHeadingState() = runBlocking {
        // First, initialize and update heading
        var gyroData = SensorData.Gyroscope(0f, 0f, 0f, 0L)
        var params = EstimateHeadingUseCase.Params(gyroData)
        estimateHeadingUseCase.invoke(params)

        gyroData = SensorData.Gyroscope(0f, 0f, 1.0f, 500_000_000L)
        params = EstimateHeadingUseCase.Params(gyroData)
        val resultBeforeReset = estimateHeadingUseCase.invoke(params)
        
        // Reset the use case
        estimateHeadingUseCase.reset()
        
        // After reset, should behave like first initialization
        gyroData = SensorData.Gyroscope(0f, 0f, 0f, 1_000_000_000L)
        params = EstimateHeadingUseCase.Params(gyroData)
        val resultAfterReset = estimateHeadingUseCase.invoke(params)
        
        // Heading should be different after reset
        assertNotEquals(resultBeforeReset.heading, resultAfterReset.heading)
    }
    
    /**
     * Normalizes the difference between two headings to be in the range -180 to 180 degrees.
     */
    private fun normalizeHeadingDifference(difference: Float): Float {
        var result = difference % 360
        if (result > 180) {
            result -= 360
        } else if (result < -180) {
            result += 360
        }
        return result
    }
}