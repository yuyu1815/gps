package com.example.myapplication.wifi

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import java.util.concurrent.ConcurrentHashMap

@ExperimentalCoroutinesApi
class WifiCalibrationToolTest {

    private lateinit var mockWifiScanner: WifiScanner
    private lateinit var mockFingerprintManager: WifiFingerprintManager
    private lateinit var calibrationTool: WifiCalibrationTool
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
        `when`(mockWifiScanner.startPeriodicScanning(any(), any())).thenReturn(true)
        
        // Create the calibration tool with mock dependencies
        calibrationTool = WifiCalibrationTool(mockWifiScanner, mockFingerprintManager)
    }
    
    @Test
    fun testCollectSamplesReturnsNullWhenPermissionsDenied() = runTest {
        // Given
        mockPermissions.value = false
        
        // When
        val result = calibrationTool.collectSamples("test-location", 10.0, 20.0)
        
        // Then
        assertNull(result)
        
        // Verify state is Error
        val state = calibrationTool.calibrationState.first()
        assertTrue(state is CalibrationState.Error)
    }
    
    @Test
    fun testCollectSamplesReturnsNullWhenNoScanResults() = runTest {
        // Given
        mockScanResults.value = emptyList()
        
        // When
        val result = calibrationTool.collectSamples("test-location", 10.0, 20.0, numSamples = 1)
        
        // Then
        assertNull(result)
    }
    
    @Test
    fun testCollectSamplesReturnsCalibrationPointWithValidScanResults() = runTest {
        // Given
        mockScanResults.value = createMockScanResults()
        
        // When
        val result = calibrationTool.collectSamples("test-location", 10.0, 20.0, numSamples = 1)
        
        // Then
        assertNotNull(result)
        assertEquals("test-location", result?.locationId)
        assertEquals(10.0, result!!.x, 0.01)
        assertEquals(20.0, result.y, 0.01)
        assertEquals(1, result?.samples?.size)
        
        // Verify state is back to Idle
        val state = calibrationTool.calibrationState.first()
        assertTrue(state is CalibrationState.Idle)
    }
    
    @Test
    fun testCreateCalibrationSessionReturnsNullWhenNoCalibrationPoints() {
        // When
        val result = calibrationTool.createCalibrationSession("test-session", listOf("nonexistent"))
        
        // Then
        assertNull(result)
    }
    
    @Test
    fun testCreateCalibrationSessionReturnsSessionWithValidCalibrationPoints() = runTest {
        // Given - create a calibration point
        mockScanResults.value = createMockScanResults()
        calibrationTool.collectSamples("test-location", 10.0, 20.0, numSamples = 1)
        
        // When
        val result = calibrationTool.createCalibrationSession("test-session", listOf("test-location"))
        
        // Then
        assertNotNull(result)
        assertEquals("test-session", result?.sessionId)
        assertEquals(1, result?.points?.size)
        assertEquals("test-location", result?.points?.get(0)?.locationId)
    }
    
    @Test
    fun testGenerateFingerprintsFromSessionReturnsZeroWhenSessionNotFound() = runTest {
        // When
        val result = calibrationTool.generateFingerprintsFromSession("nonexistent")
        
        // Then
        assertEquals(0, result)
    }
    
    @Test
    fun testGenerateFingerprintsFromPointsReturnsCorrectCount() = runTest {
        // Given - create a calibration point
        mockScanResults.value = createMockScanResults()
        val point = calibrationTool.collectSamples("test-location", 10.0, 20.0, numSamples = 1)
        
        // Mock the fingerprintMap field in WifiFingerprintManager
        val fingerprintMap = ConcurrentHashMap<String, WifiFingerprint>()
        val field = WifiFingerprintManager::class.java.getDeclaredField("fingerprintMap")
        field.isAccessible = true
        field.set(mockFingerprintManager, fingerprintMap)
        
        // When
        val result = calibrationTool.generateFingerprintsFromPoints(listOf(point!!))
        
        // Then
        assertEquals(1, result)
        assertEquals(1, fingerprintMap.size)
        assertTrue(fingerprintMap.containsKey("test-location"))
    }
    
    @Test
    fun testGetCalibrationPointReturnsNullWhenNotFound() {
        // When
        val result = calibrationTool.getCalibrationPoint("nonexistent")
        
        // Then
        assertNull(result)
    }
    
    @Test
    fun testGetAllCalibrationPointsReturnsEmptyListWhenNoPoints() {
        // When
        val result = calibrationTool.getAllCalibrationPoints()
        
        // Then
        assertTrue(result.isEmpty())
    }
    
    @Test
    fun testGetCalibrationSessionReturnsNullWhenNotFound() {
        // When
        val result = calibrationTool.getCalibrationSession("nonexistent")
        
        // Then
        assertNull(result)
    }
    
    @Test
    fun testGetAllCalibrationSessionsReturnsEmptyListWhenNoSessions() {
        // When
        val result = calibrationTool.getAllCalibrationSessions()
        
        // Then
        assertTrue(result.isEmpty())
    }
    
    @Test
    fun testRemoveCalibrationPointRemovesPoint() = runTest {
        // Given - create a calibration point
        mockScanResults.value = createMockScanResults()
        calibrationTool.collectSamples("test-location", 10.0, 20.0, numSamples = 1)
        
        // Verify point exists
        assertNotNull(calibrationTool.getCalibrationPoint("test-location"))
        
        // When
        calibrationTool.removeCalibrationPoint("test-location")
        
        // Then
        assertNull(calibrationTool.getCalibrationPoint("test-location"))
    }
    
    @Test
    fun testRemoveCalibrationSessionRemovesSession() = runTest {
        // Given - create a calibration point and session
        mockScanResults.value = createMockScanResults()
        calibrationTool.collectSamples("test-location", 10.0, 20.0, numSamples = 1)
        calibrationTool.createCalibrationSession("test-session", listOf("test-location"))
        
        // Verify session exists
        assertNotNull(calibrationTool.getCalibrationSession("test-session"))
        
        // When
        calibrationTool.removeCalibrationSession("test-session")
        
        // Then
        assertNull(calibrationTool.getCalibrationSession("test-session"))
    }
    
    @Test
    fun testClearCalibrationDataRemovesAllData() = runTest {
        // Given - create a calibration point and session
        mockScanResults.value = createMockScanResults()
        calibrationTool.collectSamples("test-location", 10.0, 20.0, numSamples = 1)
        calibrationTool.createCalibrationSession("test-session", listOf("test-location"))
        
        // Verify data exists
        assertTrue(calibrationTool.getAllCalibrationPoints().isNotEmpty())
        assertTrue(calibrationTool.getAllCalibrationSessions().isNotEmpty())
        
        // When
        calibrationTool.clearCalibrationData()
        
        // Then
        assertTrue(calibrationTool.getAllCalibrationPoints().isEmpty())
        assertTrue(calibrationTool.getAllCalibrationSessions().isEmpty())
    }
    
    @Test
    fun testExportCalibrationDataToJsonReturnsValidJson() = runTest {
        // Given - create a calibration point and session
        mockScanResults.value = createMockScanResults()
        calibrationTool.collectSamples("test-location", 10.0, 20.0, numSamples = 1)
        calibrationTool.createCalibrationSession("test-session", listOf("test-location"))
        
        // When
        val json = calibrationTool.exportCalibrationDataToJson()
        
        // Then
        assertTrue(json.contains("\"locationId\": \"test-location\""))
        assertTrue(json.contains("\"x\": 10.0"))
        assertTrue(json.contains("\"y\": 20.0"))
        assertTrue(json.contains("\"sessionId\": \"test-session\""))
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
    
    private fun <T> any(): T {
        org.mockito.Mockito.any<T>()
        return null as T
    }
}