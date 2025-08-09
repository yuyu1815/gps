package com.example.myapplication.wifi

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import kotlin.math.abs

@ExperimentalCoroutinesApi
class WifiPositionEstimatorTest {

    private lateinit var mockWifiScanner: WifiScanner
    private lateinit var mockFingerprintManager: WifiFingerprintManager
    private lateinit var positionEstimator: WifiPositionEstimator
    private lateinit var mockScanResults: MutableStateFlow<List<ScanResult>>
    private lateinit var mockPermissions: MutableStateFlow<Boolean>

    @Before
    fun setup() {
        // Create mock objects
        mockWifiScanner = mock(WifiScanner::class.java)
        mockFingerprintManager = mock(WifiFingerprintManager::class.java)
        mockScanResults = MutableStateFlow(emptyList())
        mockPermissions = MutableStateFlow(true)
        
        // Set up mock behavior
        `when`(mockWifiScanner.scanResults).thenReturn(mockScanResults)
        `when`(mockWifiScanner.hasRequiredPermissions).thenReturn(mockPermissions)
        
        // Create the estimator with mock dependencies
        positionEstimator = WifiPositionEstimator(mockWifiScanner, mockFingerprintManager)
    }
    
    @Test
    fun testEstimatePositionReturnsNullWhenPermissionsDenied() = runTest {
        // Given
        mockPermissions.value = false
        
        // When
        val result = positionEstimator.estimatePosition()
        
        // Then
        assertNull(result)
    }
    
    @Test
    fun testEstimatePositionReturnsNullWhenNoScanResults() = runTest {
        // Given
        mockScanResults.value = emptyList()
        
        // When
        val result = positionEstimator.estimatePosition()
        
        // Then
        assertNull(result)
    }
    
    @Test
    fun testEstimatePositionReturnsNullWhenNoStoredFingerprints() = runTest {
        // Given
        mockScanResults.value = createMockScanResults()
        `when`(mockFingerprintManager.getAllFingerprints()).thenReturn(emptyList())
        
        // When
        val result = positionEstimator.estimatePosition()
        
        // Then
        assertNull(result)
    }
    
    @Test
    fun testEstimatePositionReturnsCorrectPositionWithOneFingerprint() = runTest {
        // Given
        mockScanResults.value = createMockScanResults()
        
        val fingerprint = WifiFingerprint(
            locationId = "test-location",
            accessPoints = mapOf(
                "00:11:22:33:44:55" to -70,
                "AA:BB:CC:DD:EE:FF" to -80
            ),
            metadata = mapOf("x" to "10.0", "y" to "20.0")
        )
        
        `when`(mockFingerprintManager.getAllFingerprints()).thenReturn(listOf(fingerprint))
        
        // When
        val result = positionEstimator.estimatePosition()
        
        // Then
        assertNotNull(result)
        assertEquals(10.0, result!!.x, 0.01)
        assertEquals(20.0, result.y, 0.01)
    }
    
    @Test
    fun testEstimatePositionReturnsWeightedAverageWithMultipleFingerprints() = runTest {
        // Given
        mockScanResults.value = createMockScanResults()
        
        val fingerprints = listOf(
            WifiFingerprint(
                locationId = "location1",
                accessPoints = mapOf(
                    "00:11:22:33:44:55" to -70,
                    "AA:BB:CC:DD:EE:FF" to -80
                ),
                metadata = mapOf("x" to "10.0", "y" to "20.0")
            ),
            WifiFingerprint(
                locationId = "location2",
                accessPoints = mapOf(
                    "00:11:22:33:44:55" to -75,
                    "AA:BB:CC:DD:EE:FF" to -85
                ),
                metadata = mapOf("x" to "15.0", "y" to "25.0")
            ),
            WifiFingerprint(
                locationId = "location3",
                accessPoints = mapOf(
                    "00:11:22:33:44:55" to -80,
                    "AA:BB:CC:DD:EE:FF" to -90
                ),
                metadata = mapOf("x" to "20.0", "y" to "30.0")
            )
        )
        
        `when`(mockFingerprintManager.getAllFingerprints()).thenReturn(fingerprints)
        
        // When
        val result = positionEstimator.estimatePosition()
        
        // Then
        assertNotNull(result)
        // The result should be a weighted average, closer to location1 which has the most similar RSSI values
        assertTrue(result!!.x in 10.0..20.0)
        assertTrue(result.y in 20.0..30.0)
    }
    
    @Test
    fun testEstimatePositionSimpleReturnsClosestFingerprint() = runTest {
        // Given
        mockScanResults.value = createMockScanResults()
        
        val fingerprints = listOf(
            WifiFingerprint(
                locationId = "location1",
                accessPoints = mapOf(
                    "00:11:22:33:44:55" to -70,
                    "AA:BB:CC:DD:EE:FF" to -80
                ),
                metadata = mapOf("x" to "10.0", "y" to "20.0")
            ),
            WifiFingerprint(
                locationId = "location2",
                accessPoints = mapOf(
                    "00:11:22:33:44:55" to -75,
                    "AA:BB:CC:DD:EE:FF" to -85
                ),
                metadata = mapOf("x" to "15.0", "y" to "25.0")
            )
        )
        
        `when`(mockFingerprintManager.getAllFingerprints()).thenReturn(fingerprints)
        
        // When
        val result = positionEstimator.estimatePositionSimple()
        
        // Then
        assertNotNull(result)
        // Should return location1 as it's the closest match
        assertEquals(10.0, result!!.x, 0.01)
        assertEquals(20.0, result.y, 0.01)
    }
    
    // Helper methods
    
    private fun createMockScanResults(): List<ScanResult> {
        return listOf(
            ScanResult(
                bssid = "00:11:22:33:44:55",
                rssi = -70,
                timestamp = System.currentTimeMillis(),
                frequency = 2412,
                ssid = "TestAP1"
            ),
            ScanResult(
                bssid = "AA:BB:CC:DD:EE:FF",
                rssi = -80,
                timestamp = System.currentTimeMillis(),
                frequency = 5180,
                ssid = "TestAP2"
            )
        )
    }
    
    private fun assertTrue(condition: Boolean) {
        org.junit.Assert.assertTrue(condition)
    }
}