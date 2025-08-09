package com.example.myapplication.wifi

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.mockito.Mockito.times

@ExperimentalCoroutinesApi
class WifiFingerprintManagerTest {

    private lateinit var mockWifiScanner: WifiScanner
    private lateinit var fingerprintManager: WifiFingerprintManager
    private lateinit var mockScanResults: MutableStateFlow<List<ScanResult>>
    private lateinit var mockPermissions: MutableStateFlow<Boolean>

    @Before
    fun setup() {
        // Create mock objects
        mockWifiScanner = mock(WifiScanner::class.java)
        mockScanResults = MutableStateFlow(emptyList())
        mockPermissions = MutableStateFlow(true)
        
        // Set up mock behavior
        `when`(mockWifiScanner.scanResults).thenReturn(mockScanResults)
        `when`(mockWifiScanner.hasRequiredPermissions).thenReturn(mockPermissions)
        `when`(mockWifiScanner.startPeriodicScanning()).thenReturn(true)
        
        // Create the manager with mock scanner
        fingerprintManager = WifiFingerprintManager(mockWifiScanner)
    }
    
    @Test
    fun testGetFingerprintReturnsNullForNonExistentLocation() {
        // When
        val result = fingerprintManager.getFingerprint("nonexistent")
        
        // Then
        assertNull(result)
    }
    
    @Test
    fun testGetAllFingerprintsReturnsEmptyListWhenNoFingerprints() {
        // When
        val result = fingerprintManager.getAllFingerprints()
        
        // Then
        assertTrue(result.isEmpty())
    }
    
    @Test
    fun testRemoveFingerprintDoesNotThrowForNonExistentLocation() {
        // When/Then - should not throw
        fingerprintManager.removeFingerprint("nonexistent")
    }
    
    @Test
    fun testClearFingerprintsRemovesAllFingerprints() = runTest {
        // Given - create a fingerprint
        mockScanResults.value = createMockScanResults()
        fingerprintManager.createFingerprint("test-location")
        
        // Verify we have fingerprints
        assertTrue(fingerprintManager.getAllFingerprints().isNotEmpty())
        
        // When
        fingerprintManager.clearFingerprints()
        
        // Then
        assertTrue(fingerprintManager.getAllFingerprints().isEmpty())
    }
    
    @Test
    fun testCreateFingerprintReturnsFalseWhenPermissionsDenied() = runTest {
        // Given
        mockPermissions.value = false
        
        // When
        val result = fingerprintManager.createFingerprint("test-location")
        
        // Then
        assertFalse(result)
    }
    
    @Test
    fun testCreateFingerprintReturnsFalseWhenNoScanResults() = runTest {
        // Given
        mockScanResults.value = emptyList()
        
        // When
        val result = fingerprintManager.createFingerprint("test-location")
        
        // Then
        assertFalse(result)
    }
    
    @Test
    fun testCreateFingerprintReturnsTrueWithValidScanResults() = runTest {
        // Given
        mockScanResults.value = createMockScanResults()
        
        // When
        val result = fingerprintManager.createFingerprint("test-location")
        
        // Then
        assertTrue(result)
        
        // Verify fingerprint was stored
        val fingerprint = fingerprintManager.getFingerprint("test-location")
        assertNotNull(fingerprint)
        assertEquals("test-location", fingerprint?.locationId)
        assertTrue(fingerprint?.accessPoints?.isNotEmpty() == true)
    }
    
    @Test
    fun testRemoveStaleFingerprints() = runTest {
        // Given - create fingerprints with different timestamps
        mockScanResults.value = createMockScanResults()
        
        // Create fingerprint and manually set an old timestamp
        fingerprintManager.createFingerprint("old-location")
        val oldFingerprint = fingerprintManager.getFingerprint("old-location")
        val oldFingerprintWithOldTimestamp = WifiFingerprint(
            locationId = oldFingerprint!!.locationId,
            accessPoints = oldFingerprint.accessPoints,
            timestamp = System.currentTimeMillis() - 48 * 60 * 60 * 1000L, // 48 hours old
            metadata = oldFingerprint.metadata
        )
        
        // Use reflection to update the fingerprint map
        val field = WifiFingerprintManager::class.java.getDeclaredField("fingerprintMap")
        field.isAccessible = true
        val fingerprintMap = field.get(fingerprintManager) as java.util.concurrent.ConcurrentHashMap<String, WifiFingerprint>
        fingerprintMap["old-location"] = oldFingerprintWithOldTimestamp
        
        // Create a new fingerprint with current timestamp
        fingerprintManager.createFingerprint("new-location")
        
        // When
        val removedCount = fingerprintManager.removeStaleFingerprints(24 * 60 * 60 * 1000L) // 24 hours
        
        // Then
        assertEquals(1, removedCount)
        assertNull(fingerprintManager.getFingerprint("old-location"))
        assertNotNull(fingerprintManager.getFingerprint("new-location"))
    }
    
    @Test
    fun testExportFingerprintsToJson() = runTest {
        // Given
        mockScanResults.value = createMockScanResults()
        fingerprintManager.createFingerprint("test-location", metadata = mapOf("floor" to "1"))
        
        // When
        val json = fingerprintManager.exportFingerprintsToJson()
        
        // Then
        assertTrue(json.contains("\"locationId\": \"test-location\""))
        assertTrue(json.contains("\"floor\": \"1\""))
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
}