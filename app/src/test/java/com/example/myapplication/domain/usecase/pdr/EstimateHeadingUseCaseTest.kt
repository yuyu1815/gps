package com.example.myapplication.domain.usecase.pdr

import com.example.myapplication.domain.model.SensorData
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import kotlin.math.abs

class EstimateHeadingUseCaseTest {

    private lateinit var estimateHeadingUseCase: EstimateHeadingUseCase
    private lateinit var kalmanHeadingUseCase: KalmanHeadingUseCase
    
    @Before
    fun setUp() {
        estimateHeadingUseCase = EstimateHeadingUseCase()
        kalmanHeadingUseCase = KalmanHeadingUseCase()
        
        // Reset state before each test
        estimateHeadingUseCase.reset()
        kalmanHeadingUseCase.reset()
    }
    
    @Test
    fun testInitialHeadingWithGyroscopeOnly() {
        runBlocking {
            // Given - only gyroscope data
            val gyroData = SensorData.Gyroscope(
                x = 0f,
                y = 0f,
                z = 0f,
                timestamp = 1000000000L
            )
            
            // When
            val result = estimateHeadingUseCase(
                EstimateHeadingUseCase.Params(
                    gyroscopeData = gyroData
                )
            )
            
            // Then
            assertEquals(0f, result.heading, 0.01f)
            assertEquals(EstimateHeadingUseCase.HeadingAccuracy.LOW, result.headingAccuracy)
            assertEquals(EstimateHeadingUseCase.HeadingSource.GYROSCOPE, result.headingSource)
        }
    }
    
    @Test
    fun testHeadingChangeWithGyroscope() {
        runBlocking {
            // Given - initial gyroscope reading
            val gyroData1 = SensorData.Gyroscope(
                x = 0f,
                y = 0f,
                z = 0f,
                timestamp = 1000000000L
            )
            
            // When - first reading to initialize
            estimateHeadingUseCase(
                EstimateHeadingUseCase.Params(
                    gyroscopeData = gyroData1
                )
            )
            
            // Given - second gyroscope reading with rotation around z-axis
            // Negative z rotation corresponds to clockwise rotation (increasing heading)
            val gyroData2 = SensorData.Gyroscope(
                x = 0f,
                y = 0f,
                z = -0.1f,  // -0.1 rad/s around z-axis (clockwise)
                timestamp = 2000000000L  // 1 second later
            )
            
            // When - second reading
            val result = estimateHeadingUseCase(
                EstimateHeadingUseCase.Params(
                    gyroscopeData = gyroData2
                )
            )
            
            // Then
            // Expected heading change: -0.1 rad/s * 1s * 57.3 deg/rad * 0.98 (gyroWeight) ≈ 5.6 degrees
            val expectedHeadingChange = 0.1f * 1f * 57.2957795f * 0.98f
            assertEquals(expectedHeadingChange, result.heading, 0.1f)
            assertEquals(EstimateHeadingUseCase.HeadingAccuracy.MEDIUM, result.headingAccuracy)
            assertEquals(EstimateHeadingUseCase.HeadingSource.GYROSCOPE, result.headingSource)
        }
    }
    
    @Test
    fun testHeadingWithRotationVector() {
        runBlocking {
            // Given - initial gyroscope reading with rotation vector
            val gyroData1 = SensorData.Gyroscope(
                x = 0f,
                y = 0f,
                z = 0f,
                timestamp = 1000000000L
            )
            
            // Rotation vector representing 90 degrees east
            val rotVectorData1 = SensorData.RotationVector(
                x = 0f,
                y = 0f,
                z = 0.7071f,  // sin(π/4)
                w = 0.7071f,  // cos(π/4)
                timestamp = 1000000000L
            )
            
            // When - first reading to initialize with rotation vector
            val result1 = estimateHeadingUseCase(
                EstimateHeadingUseCase.Params(
                    gyroscopeData = gyroData1,
                    rotationVectorData = rotVectorData1
                )
            )
            
            // Then - heading should be initialized from rotation vector
            // Note: We can't test the exact value since SensorManager is not available in unit tests
            // But we can verify the source and accuracy
            assertEquals(EstimateHeadingUseCase.HeadingSource.ROTATION_VECTOR, result1.headingSource)
            assertEquals(EstimateHeadingUseCase.HeadingAccuracy.HIGH, result1.headingAccuracy)
        }
    }
    
    @Test
    fun testHeadingWithMagnetometer() {
        runBlocking {
            // Given - initial gyroscope reading with magnetometer and accelerometer
            val gyroData1 = SensorData.Gyroscope(
                x = 0f,
                y = 0f,
                z = 0f,
                timestamp = 1000000000L
            )
            
            // Accelerometer pointing down (gravity)
            val accelData1 = SensorData.Accelerometer(
                x = 0f,
                y = 0f,
                z = 9.8f,
                timestamp = 1000000000L
            )
            
            // Magnetometer pointing north
            val magData1 = SensorData.Magnetometer(
                x = 0f,
                y = 20f,  // North in device coordinates
                z = 40f,  // Downward component
                timestamp = 1000000000L
            )
            
            // When - first reading to initialize with magnetometer
            val result1 = estimateHeadingUseCase(
                EstimateHeadingUseCase.Params(
                    gyroscopeData = gyroData1,
                    accelerometerData = accelData1,
                    magnetometerData = magData1
                )
            )
            
            // Then - heading should be initialized from magnetometer
            // Note: We can't test the exact value since SensorManager is not available in unit tests
            // But we can verify the source and accuracy
            assertEquals(EstimateHeadingUseCase.HeadingAccuracy.LOW, result1.headingAccuracy)
        }
    }
    
    @Test
    fun testComplementaryFilter() {
        runBlocking {
            // Given - initial gyroscope reading
            val gyroData1 = SensorData.Gyroscope(
                x = 0f,
                y = 0f,
                z = 0f,
                timestamp = 1000000000L
            )
            
            // When - first reading to initialize
            estimateHeadingUseCase(
                EstimateHeadingUseCase.Params(
                    gyroscopeData = gyroData1
                )
            )
            
            // Given - second gyroscope reading with rotation and magnetometer
            val gyroData2 = SensorData.Gyroscope(
                x = 0f,
                y = 0f,
                z = -0.1f,  // -0.1 rad/s around z-axis (clockwise)
                timestamp = 2000000000L  // 1 second later
            )
            
            // Accelerometer pointing down (gravity)
            val accelData2 = SensorData.Accelerometer(
                x = 0f,
                y = 0f,
                z = 9.8f,
                timestamp = 2000000000L
            )
            
            // Magnetometer pointing east (90 degrees)
            val magData2 = SensorData.Magnetometer(
                x = 20f,  // East in device coordinates
                y = 0f,
                z = 40f,  // Downward component
                timestamp = 2000000000L
            )
            
            // When - second reading with complementary filter
            val result = estimateHeadingUseCase(
                EstimateHeadingUseCase.Params(
                    gyroscopeData = gyroData2,
                    accelerometerData = accelData2,
                    magnetometerData = magData2,
                    complementaryFilterAlpha = 0.5f  // Equal weight to gyro and mag
                )
            )
            
            // Then
            assertEquals(EstimateHeadingUseCase.HeadingSource.MAGNETOMETER, result.headingSource)
            assertEquals(EstimateHeadingUseCase.HeadingAccuracy.MEDIUM, result.headingAccuracy)
        }
    }
    
    @Test
    fun testHeadingNormalization() {
        runBlocking {
            // Given - initial gyroscope reading
            val gyroData1 = SensorData.Gyroscope(
                x = 0f,
                y = 0f,
                z = 0f,
                timestamp = 1000000000L
            )
            
            // When - first reading to initialize
            estimateHeadingUseCase(
                EstimateHeadingUseCase.Params(
                    gyroscopeData = gyroData1
                )
            )
            
            // Given - gyroscope reading with large negative rotation
            // This should cause heading to go below 0 and wrap around to 350+
            val gyroData2 = SensorData.Gyroscope(
                x = 0f,
                y = 0f,
                z = 0.2f,  // 0.2 rad/s around z-axis (counter-clockwise)
                timestamp = 2000000000L  // 1 second later
            )
            
            // When
            val result = estimateHeadingUseCase(
                EstimateHeadingUseCase.Params(
                    gyroscopeData = gyroData2
                )
            )
            
            // Then
            // Expected heading change: 0.2 rad/s * 1s * 57.3 deg/rad * 0.98 (gyroWeight) ≈ -11.2 degrees
            // But normalized to 0-360, so should be around 348.8 degrees
            val expectedHeading = 360f - (0.2f * 1f * 57.2957795f * 0.98f)
            assertTrue(result.heading >= 0f && result.heading < 360f)
            assertEquals(expectedHeading, result.heading, 0.1f)
        }
    }
    
    @Test
    fun testKalmanFilterInitialization() {
        runBlocking {
            // Given - only gyroscope data
            val gyroData = SensorData.Gyroscope(
                x = 0f,
                y = 0f,
                z = 0f,
                timestamp = 1000000000L
            )
            
            // When
            val result = kalmanHeadingUseCase(
                KalmanHeadingUseCase.Params(
                    gyroscopeData = gyroData
                )
            )
            
            // Then
            assertEquals(0f, result.heading, 0.01f)
            assertEquals(0f, result.headingRate, 0.01f)
            assertTrue(result.variance > 0f)  // Should have some initial uncertainty
            assertEquals(EstimateHeadingUseCase.HeadingAccuracy.LOW, result.headingAccuracy)
        }
    }
    
    @Test
    fun testKalmanFilterWithGyroscope() {
        runBlocking {
            // Given - initial gyroscope reading
            val gyroData1 = SensorData.Gyroscope(
                x = 0f,
                y = 0f,
                z = 0f,
                timestamp = 1000000000L
            )
            
            // When - first reading to initialize
            kalmanHeadingUseCase(
                KalmanHeadingUseCase.Params(
                    gyroscopeData = gyroData1
                )
            )
            
            // Given - second gyroscope reading with rotation
            val gyroData2 = SensorData.Gyroscope(
                x = 0f,
                y = 0f,
                z = -0.1f,  // -0.1 rad/s around z-axis (clockwise)
                timestamp = 2000000000L  // 1 second later
            )
            
            // When - second reading
            val result = kalmanHeadingUseCase(
                KalmanHeadingUseCase.Params(
                    gyroscopeData = gyroData2
                )
            )
            
            // Then
            // Expected heading change: -0.1 rad/s * 1s * 57.3 deg/rad ≈ 5.7 degrees
            val expectedHeadingChange = 0.1f * 1f * 57.2957795f
            assertEquals(expectedHeadingChange, result.heading, 0.1f)
            assertEquals(0.1f * 57.2957795f, abs(result.headingRate), 0.1f)  // Should match gyro rate
        }
    }
    
    @Test
    fun testKalmanFilterWithMeasurementUpdate() {
        runBlocking {
            // Given - initial gyroscope reading
            val gyroData1 = SensorData.Gyroscope(
                x = 0f,
                y = 0f,
                z = 0f,
                timestamp = 1000000000L
            )
            
            // When - first reading to initialize
            kalmanHeadingUseCase(
                KalmanHeadingUseCase.Params(
                    gyroscopeData = gyroData1
                )
            )
            
            // Given - second reading with gyro and rotation vector
            val gyroData2 = SensorData.Gyroscope(
                x = 0f,
                y = 0f,
                z = -0.1f,  // -0.1 rad/s around z-axis (clockwise)
                timestamp = 2000000000L  // 1 second later
            )
            
            // Rotation vector representing 90 degrees east
            val rotVectorData2 = SensorData.RotationVector(
                x = 0f,
                y = 0f,
                z = 0.7071f,  // sin(π/4)
                w = 0.7071f,  // cos(π/4)
                timestamp = 2000000000L
            )
            
            // When - second reading with Kalman update
            val result = kalmanHeadingUseCase(
                KalmanHeadingUseCase.Params(
                    gyroscopeData = gyroData2,
                    rotationVectorData = rotVectorData2,
                    rotVectorNoise = 0.01f  // Low noise = high trust in rotation vector
                )
            )
            
            // Then
            // Note: We can't test the exact value since SensorManager is not available in unit tests
            // But we can verify that the variance decreased due to the measurement update
            assertTrue(result.variance < 10f)  // Should be less than initial variance
        }
    }
}