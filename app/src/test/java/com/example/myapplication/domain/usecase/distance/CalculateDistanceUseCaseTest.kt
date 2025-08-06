package com.example.myapplication.domain.usecase.distance

import com.example.myapplication.data.repository.IBeaconRepository
import com.example.myapplication.domain.model.Beacon
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.junit.Assert.*
import kotlin.math.pow

class CalculateDistanceUseCaseTest {

    private lateinit var calculateDistanceUseCase: CalculateDistanceUseCase
    private lateinit var mockBeaconRepository: IBeaconRepository
    
    @Before
    fun setUp() {
        mockBeaconRepository = Mockito.mock(IBeaconRepository::class.java)
        calculateDistanceUseCase = CalculateDistanceUseCase(mockBeaconRepository)
    }
    
    @Test
    fun testDistanceCalculationWithValidRSSI() {
        runBlocking {
            // Given
            val txPower = -70
            val rssi = -80
            val environmentalFactor = 2.0f
            val expectedDistance = 10.0.pow((txPower - rssi) / (10.0 * environmentalFactor)).toFloat()
            
            val beacon = Beacon(
                macAddress = "AA:BB:CC:DD:EE:FF",
                x = 10f,
                y = 20f,
                txPower = txPower,
                name = "Test Beacon"
            )
            beacon.updateRssi(rssi, System.currentTimeMillis())
            
            // When
            val result = calculateDistanceUseCase(
                CalculateDistanceUseCase.Params(
                    beacon = beacon,
                    environmentalFactor = environmentalFactor,
                    useFilteredRssi = true
                )
            )
            
            // Then
            assertEquals(expectedDistance, result.distance, 0.01f)
        }
    }
    
    @Test
    fun testDistanceCalculationWithZeroRSSI() {
        runBlocking {
            // Given
            val beacon = Beacon(
                macAddress = "AA:BB:CC:DD:EE:FF",
                x = 10f,
                y = 20f,
                txPower = -70,
                name = "Test Beacon"
            )
            beacon.lastRssi = 0 // Zero RSSI
            
            // When
            val result = calculateDistanceUseCase(
                CalculateDistanceUseCase.Params(
                    beacon = beacon,
                    environmentalFactor = 2.0f,
                    useFilteredRssi = true
                )
            )
            
            // Then
            assertEquals(Float.MAX_VALUE, result.distance)
            assertEquals(0f, result.confidence)
        }
    }
    
    @Test
    fun testDistanceCalculationWithDifferentEnvironmentalFactors() {
        runBlocking {
            // Given
            val txPower = -70
            val rssi = -85
            val beacon = Beacon(
                macAddress = "AA:BB:CC:DD:EE:FF",
                x = 10f,
                y = 20f,
                txPower = txPower,
                name = "Test Beacon"
            )
            beacon.updateRssi(rssi, System.currentTimeMillis())
            
            // When - with environmental factor 2.0
            val result1 = calculateDistanceUseCase(
                CalculateDistanceUseCase.Params(
                    beacon = beacon,
                    environmentalFactor = 2.0f,
                    useFilteredRssi = true
                )
            )
            
            // When - with environmental factor 3.0
            val result2 = calculateDistanceUseCase(
                CalculateDistanceUseCase.Params(
                    beacon = beacon,
                    environmentalFactor = 3.0f,
                    useFilteredRssi = true
                )
            )
            
            // Then
            val expectedDistance1 = 10.0.pow((txPower - rssi) / (10.0 * 2.0)).toFloat()
            val expectedDistance2 = 10.0.pow((txPower - rssi) / (10.0 * 3.0)).toFloat()
            
            assertEquals(expectedDistance1, result1.distance, 0.01f)
            assertEquals(expectedDistance2, result2.distance, 0.01f)
            
            // Environmental factor 3.0 should result in a shorter distance than factor 2.0
            assertTrue(result2.distance < result1.distance)
        }
    }
    
    @Test
    fun testDistanceCalculationWithFilteredVsRawRSSI() {
        runBlocking {
            // Given
            val txPower = -70
            val lastRssi = -90
            val beacon = Beacon(
                macAddress = "AA:BB:CC:DD:EE:FF",
                x = 10f,
                y = 20f,
                txPower = txPower,
                name = "Test Beacon"
            )
            
            // Add multiple RSSI values to create a filtered value
            val currentTime = System.currentTimeMillis()
            beacon.updateRssi(-80, currentTime - 4000)
            beacon.updateRssi(-82, currentTime - 3000)
            beacon.updateRssi(-84, currentTime - 2000)
            beacon.updateRssi(-86, currentTime - 1000)
            beacon.updateRssi(lastRssi, currentTime) // Last RSSI is -90
            
            // When - using filtered RSSI
            val resultFiltered = calculateDistanceUseCase(
                CalculateDistanceUseCase.Params(
                    beacon = beacon,
                    environmentalFactor = 2.0f,
                    useFilteredRssi = true
                )
            )
            
            // When - using raw RSSI
            val resultRaw = calculateDistanceUseCase(
                CalculateDistanceUseCase.Params(
                    beacon = beacon,
                    environmentalFactor = 2.0f,
                    useFilteredRssi = false
                )
            )
            
            // Then
            // Filtered RSSI should be the average of the 5 values: (-80-82-84-86-90)/5 = -84.4 ≈ -84
            val expectedFilteredRssi = -84
            val expectedFilteredDistance = 10.0.pow((txPower - expectedFilteredRssi) / (10.0 * 2.0)).toFloat()
            
            val expectedRawDistance = 10.0.pow((txPower - lastRssi) / (10.0 * 2.0)).toFloat()
            
            assertEquals(expectedFilteredDistance, resultFiltered.distance, 0.1f)
            assertEquals(expectedRawDistance, resultRaw.distance, 0.01f)
            
            // Raw RSSI (-90) should result in a larger distance than filtered RSSI (-84)
            assertTrue(resultRaw.distance > resultFiltered.distance)
        }
    }
    
    @Test
    fun testConfidenceCalculationBasedOnRecency() {
        runBlocking {
            // Given
            val txPower = -70
            val rssi = -80
            val currentTime = System.currentTimeMillis()
            
            val recentBeacon = Beacon(
                macAddress = "AA:BB:CC:DD:EE:FF",
                x = 10f,
                y = 20f,
                txPower = txPower,
                name = "Recent Beacon"
            )
            recentBeacon.updateRssi(rssi, currentTime)
            
            val oldBeacon = Beacon(
                macAddress = "11:22:33:44:55:66",
                x = 15f,
                y = 25f,
                txPower = txPower,
                name = "Old Beacon"
            )
            // Update with an old timestamp (4 seconds ago)
            oldBeacon.updateRssi(rssi, currentTime - 4000)
            
            // When
            val recentResult = calculateDistanceUseCase(
                CalculateDistanceUseCase.Params(
                    beacon = recentBeacon,
                    environmentalFactor = 2.0f,
                    useFilteredRssi = true,
                    currentTime = currentTime
                )
            )
            
            val oldResult = calculateDistanceUseCase(
                CalculateDistanceUseCase.Params(
                    beacon = oldBeacon,
                    environmentalFactor = 2.0f,
                    useFilteredRssi = true,
                    currentTime = currentTime
                )
            )
            
            // Then
            // Recent beacon should have higher confidence
            assertTrue(recentResult.confidence > oldResult.confidence)
        }
    }
}