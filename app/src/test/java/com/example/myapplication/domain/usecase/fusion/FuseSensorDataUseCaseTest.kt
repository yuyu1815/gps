package com.example.myapplication.domain.usecase.fusion

import com.example.myapplication.data.repository.IPositionRepository
import com.example.myapplication.domain.model.Motion
import com.example.myapplication.domain.model.PositionSource
import com.example.myapplication.domain.model.UserPosition
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*
import org.junit.Assert.*
import org.mockito.ArgumentMatchers.any

class FuseSensorDataUseCaseTest {

    private lateinit var fuseSensorDataUseCase: FuseSensorDataUseCase
    private lateinit var mockPositionRepository: IPositionRepository

    @Before
    fun setUp() {
        mockPositionRepository = mock(IPositionRepository::class.java)
        fuseSensorDataUseCase = FuseSensorDataUseCase(mockPositionRepository)
    }

    @Test
    fun testInitializeEkfWithValidWifiPosition() = runBlocking {
        // Given
        val wifiPosition = UserPosition(
            x = 10.0f,
            y = 20.0f,
            accuracy = 3.0f,
            timestamp = System.currentTimeMillis(),
            source = PositionSource.BLE, // Using BLE as a substitute for WiFi
            confidence = 0.8f
        )
        
        val slamMotion = Motion(
            velocity = 0.0,
            angularVelocity = 0.0
        )
        
        val params = FuseSensorDataUseCase.Params(
            slamMotion = slamMotion,
            wifiPosition = wifiPosition,
            updateRepository = true
        )
        
        // When
        val result = fuseSensorDataUseCase.invoke(params)
        
        // Then
        assertEquals(wifiPosition.x, result.x, 0.001f)
        assertEquals(wifiPosition.y, result.y, 0.001f)
        assertEquals(wifiPosition.source, PositionSource.BLE)
        verify(mockPositionRepository, never()).updatePosition(any())
    }
    
    @Test
    fun testReturnInvalidPositionWhenWifiPositionIsInvalidAndEkfNotInitialized() = runBlocking {
        // Given
        val invalidWifiPosition = UserPosition.invalid()
        
        val slamMotion = Motion(
            velocity = 0.0,
            angularVelocity = 0.0
        )
        
        val params = FuseSensorDataUseCase.Params(
            slamMotion = slamMotion,
            wifiPosition = invalidWifiPosition,
            updateRepository = true
        )
        
        // When
        val result = fuseSensorDataUseCase.invoke(params)
        
        // Then
        assertFalse(UserPosition.isValid(result))
        verify(mockPositionRepository, never()).updatePosition(any())
    }
    
    @Test
    fun testFusePositionDataFromMultipleSources() = runBlocking {
        // First initialize the EKF
        val initialWifiPosition = UserPosition(
            x = 10.0f,
            y = 20.0f,
            accuracy = 3.0f,
            timestamp = System.currentTimeMillis(),
            source = PositionSource.BLE, // Using BLE as a substitute for WiFi
            confidence = 0.8f
        )
        
        val initialSlamMotion = Motion(
            velocity = 0.0,
            angularVelocity = 0.0
        )
        
        val initialParams = FuseSensorDataUseCase.Params(
            slamMotion = initialSlamMotion,
            wifiPosition = initialWifiPosition,
            updateRepository = false
        )
        
        fuseSensorDataUseCase.invoke(initialParams)
        
        // Now perform fusion with new data
        val newWifiPosition = UserPosition(
            x = 12.0f,
            y = 22.0f,
            accuracy = 2.5f,
            timestamp = System.currentTimeMillis() + 1000,
            source = PositionSource.BLE, // Using BLE as a substitute for WiFi
            confidence = 0.85f
        )
        
        val newSlamMotion = Motion(
            velocity = 1.0,
            angularVelocity = 0.1
        )
        
        val pdrMotion = Motion(
            velocity = 0.8,
            angularVelocity = 0.05
        )
        
        val fusionParams = FuseSensorDataUseCase.Params(
            slamMotion = newSlamMotion,
            wifiPosition = newWifiPosition,
            pdrMotion = pdrMotion,
            motionConfidence = 0.7f,
            updateRepository = true
        )
        
        // When
        val result = fuseSensorDataUseCase.invoke(fusionParams)
        
        // Then
        assertEquals(PositionSource.FUSION, result.source)
        assertTrue(result.x in 10.0f..12.0f) // Should be between initial and new position
        assertTrue(result.y in 20.0f..22.0f) // Should be between initial and new position
        verify(mockPositionRepository, times(1)).updatePosition(any())
    }
    
    @Test
    fun testResetEkfState() = runBlocking {
        // First initialize the EKF
        val wifiPosition = UserPosition(
            x = 10.0f,
            y = 20.0f,
            accuracy = 3.0f,
            timestamp = System.currentTimeMillis(),
            source = PositionSource.BLE, // Using BLE as a substitute for WiFi
            confidence = 0.8f
        )
        
        val slamMotion = Motion(
            velocity = 0.0,
            angularVelocity = 0.0
        )
        
        val params = FuseSensorDataUseCase.Params(
            slamMotion = slamMotion,
            wifiPosition = wifiPosition,
            updateRepository = false
        )
        
        fuseSensorDataUseCase.invoke(params)
        
        // Reset the EKF
        fuseSensorDataUseCase.reset()
        
        // Try to use it with invalid WiFi position
        val invalidWifiPosition = UserPosition.invalid()
        
        val newParams = FuseSensorDataUseCase.Params(
            slamMotion = slamMotion,
            wifiPosition = invalidWifiPosition,
            updateRepository = false
        )
        
        // When
        val result = fuseSensorDataUseCase.invoke(newParams)
        
        // Then
        assertFalse(UserPosition.isValid(result))
    }
    
    @Test
    fun testAdjustProcessNoiseBasedOnMotionConfidence() = runBlocking {
        // First initialize the EKF
        val wifiPosition = UserPosition(
            x = 10.0f,
            y = 20.0f,
            accuracy = 3.0f,
            timestamp = System.currentTimeMillis(),
            source = PositionSource.BLE, // Using BLE as a substitute for WiFi
            confidence = 0.8f
        )
        
        val slamMotion = Motion(
            velocity = 0.0,
            angularVelocity = 0.0
        )
        
        val initialParams = FuseSensorDataUseCase.Params(
            slamMotion = slamMotion,
            wifiPosition = wifiPosition,
            updateRepository = false
        )
        
        fuseSensorDataUseCase.invoke(initialParams)
        
        // Now perform fusion with high confidence
        val highConfidenceParams = FuseSensorDataUseCase.Params(
            slamMotion = Motion(velocity = 1.0, angularVelocity = 0.1),
            wifiPosition = wifiPosition,
            motionConfidence = 0.9f,
            updateRepository = false
        )
        
        val highConfidenceResult = fuseSensorDataUseCase.invoke(highConfidenceParams)
        
        // Reset and initialize again
        fuseSensorDataUseCase.reset()
        fuseSensorDataUseCase.invoke(initialParams)
        
        // Now perform fusion with low confidence
        val lowConfidenceParams = FuseSensorDataUseCase.Params(
            slamMotion = Motion(velocity = 1.0, angularVelocity = 0.1),
            wifiPosition = wifiPosition,
            motionConfidence = 0.3f,
            updateRepository = false
        )
        
        val lowConfidenceResult = fuseSensorDataUseCase.invoke(lowConfidenceParams)
        
        // Then
        // With lower confidence, the position should rely more on the WiFi position
        // and less on the motion model, so it should be closer to the WiFi position
        val highConfidenceDistance = Math.sqrt(
            Math.pow((highConfidenceResult.x - wifiPosition.x).toDouble(), 2.0) +
            Math.pow((highConfidenceResult.y - wifiPosition.y).toDouble(), 2.0)
        )
        
        val lowConfidenceDistance = Math.sqrt(
            Math.pow((lowConfidenceResult.x - wifiPosition.x).toDouble(), 2.0) +
            Math.pow((lowConfidenceResult.y - wifiPosition.y).toDouble(), 2.0)
        )
        
        // Low confidence should result in position closer to WiFi position
        assertTrue(lowConfidenceDistance <= highConfidenceDistance)
    }
}