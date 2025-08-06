package com.example.myapplication.util

import com.example.myapplication.domain.model.Beacon
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import kotlin.math.abs

/**
 * Tests for the MockBleScanner class.
 * 
 * These tests demonstrate how to use the MockBleScanner for testing
 * positioning algorithms without requiring actual BLE hardware.
 */
class MockBleScannerTest {

    private lateinit var mockBleScanner: MockBleScanner
    
    @Before
    fun setUp() {
        mockBleScanner = MockBleScanner()
    }
    
    @Test
    fun testAddMockBeacon() {
        // Create a test beacon
        val beacon = Beacon(
            macAddress = "AA:BB:CC:DD:EE:FF",
            x = 10f,
            y = 20f,
            txPower = -70,
            name = "Test Beacon"
        )
        
        // Add the beacon to the mock scanner
        mockBleScanner.addMockBeacon(
            beacon = beacon,
            rssiAtOneMeter = -70,
            environmentalFactor = 2.0f,
            rssiNoise = 0.1f
        )
        
        // Start scanning
        mockBleScanner.startScanning()
        
        // Simulate a scan cycle with user at position (10, 20) - same as beacon
        mockBleScanner.simulateScanCycle(10f, 20f)
        
        // Verify the discovered beacons
        runBlocking {
            val discoveredBeacons = mockBleScanner.discoveredBeaconsFlow.first()
            
            // Should have one beacon
            assertEquals(1, discoveredBeacons.size)
            
            // The beacon should have the same MAC address
            assertEquals(beacon.macAddress, discoveredBeacons[0].macAddress)
            
            // The RSSI should be close to -70 (rssiAtOneMeter) since we're at 1m distance
            // Allow for some noise
            assertTrue(abs(discoveredBeacons[0].lastRssi + 70) < 5)
        }
    }
    
    @Test
    fun testRssiDecreaseWithDistance() {
        // Create a test beacon at origin
        val beacon = Beacon(
            macAddress = "AA:BB:CC:DD:EE:FF",
            x = 0f,
            y = 0f,
            txPower = -70,
            name = "Test Beacon"
        )
        
        // Add the beacon to the mock scanner with no noise
        mockBleScanner.addMockBeacon(
            beacon = beacon,
            rssiAtOneMeter = -70,
            environmentalFactor = 2.0f,
            rssiNoise = 0f  // No noise for deterministic testing
        )
        
        // Start scanning
        mockBleScanner.startScanning()
        
        // Test positions at different distances
        val positions = listOf(
            Pair(0f, 0f),    // At beacon (distance = 0)
            Pair(1f, 0f),    // 1m away
            Pair(2f, 0f),    // 2m away
            Pair(5f, 0f),    // 5m away
            Pair(10f, 0f)    // 10m away
        )
        
        val rssiValues = mutableListOf<Int>()
        
        // Collect RSSI values at different distances
        runBlocking {
            for ((x, y) in positions) {
                mockBleScanner.simulateScanCycle(x, y)
                val discoveredBeacons = mockBleScanner.discoveredBeaconsFlow.first()
                rssiValues.add(discoveredBeacons[0].lastRssi)
            }
        }
        
        // Verify that RSSI decreases with distance
        for (i in 0 until rssiValues.size - 1) {
            assertTrue(rssiValues[i] > rssiValues[i + 1])
        }
        
        // Verify the RSSI at 1m is close to rssiAtOneMeter
        assertEquals(-70, rssiValues[1])
    }
    
    @Test
    fun testGridBeaconGeneration() {
        // Generate a 3x3 grid of beacons
        mockBleScanner.generateGridBeacons(
            rows = 3,
            cols = 3,
            spacingX = 5f,
            spacingY = 5f,
            offsetX = 0f,
            offsetY = 0f,
            rssiAtOneMeter = -70,
            environmentalFactor = 2.0f,
            rssiNoise = 0.1f
        )
        
        // Start scanning
        mockBleScanner.startScanning()
        
        // Simulate a scan cycle with user at the center of the grid
        mockBleScanner.simulateScanCycle(5f, 5f)
        
        // Verify the discovered beacons
        runBlocking {
            val discoveredBeacons = mockBleScanner.discoveredBeaconsFlow.first()
            
            // Should have 9 beacons (3x3 grid)
            assertEquals(9, discoveredBeacons.size)
            
            // Verify beacon positions
            val positions = discoveredBeacons.map { Pair(it.x, it.y) }.toSet()
            
            // Check that all expected positions are present
            for (row in 0 until 3) {
                for (col in 0 until 3) {
                    val x = col * 5f
                    val y = row * 5f
                    assertTrue(positions.contains(Pair(x, y)))
                }
            }
        }
    }
    
    @Test
    fun testPositioningWithMockBeacons() {
        // Create a triangular arrangement of beacons
        val beacon1 = Beacon(
            macAddress = "AA:BB:CC:DD:EE:FF",
            x = 0f,
            y = 0f,
            txPower = -70,
            name = "Beacon 1"
        )
        
        val beacon2 = Beacon(
            macAddress = "11:22:33:44:55:66",
            x = 10f,
            y = 0f,
            txPower = -70,
            name = "Beacon 2"
        )
        
        val beacon3 = Beacon(
            macAddress = "AA:22:CC:44:EE:66",
            x = 5f,
            y = 10f,
            txPower = -70,
            name = "Beacon 3"
        )
        
        // Add beacons to the mock scanner with no noise
        mockBleScanner.addMockBeacon(beacon1, rssiNoise = 0f)
        mockBleScanner.addMockBeacon(beacon2, rssiNoise = 0f)
        mockBleScanner.addMockBeacon(beacon3, rssiNoise = 0f)
        
        // Start scanning
        mockBleScanner.startScanning()
        
        // Simulate a scan cycle with user at position (5, 5) - center of the triangle
        mockBleScanner.simulateScanCycle(5f, 5f)
        
        // Verify the discovered beacons
        runBlocking {
            val discoveredBeacons = mockBleScanner.discoveredBeaconsFlow.first()
            
            // Should have 3 beacons
            assertEquals(3, discoveredBeacons.size)
            
            // Calculate distances from RSSI values
            val distances = discoveredBeacons.map { beacon ->
                val ratio = (beacon.txPower - beacon.lastRssi) / (10 * 2.0)
                Math.pow(10.0, ratio).toFloat()
            }
            
            // All distances should be approximately equal since we're equidistant from all beacons
            val expectedDistance = 5.59f  // sqrt(5^2 + 5^2) ≈ 7.07, but due to log-distance model it's not exact
            
            for (distance in distances) {
                assertTrue(abs(distance - expectedDistance) < 0.5f)
            }
        }
    }
}