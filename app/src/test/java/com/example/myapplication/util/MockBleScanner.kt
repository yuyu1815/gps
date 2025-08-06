package com.example.myapplication.util

import com.example.myapplication.domain.model.Beacon
import com.example.myapplication.service.BleScanner
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import kotlin.random.Random

/**
 * Mock implementation of BLE scanner for testing without hardware.
 * 
 * This class simulates BLE beacon scanning by generating mock scan results
 * based on predefined beacon configurations. It can be used for testing
 * positioning algorithms without requiring actual BLE hardware.
 */
class MockBleScanner {

    // List of mock beacons with their configurations
    private val mockBeacons = mutableListOf<MockBeacon>()
    
    // Current scan state
    private var isScanning = false
    
    // Flow of discovered beacons
    private val _discoveredBeaconsFlow = MutableStateFlow<List<Beacon>>(emptyList())
    val discoveredBeaconsFlow: Flow<List<Beacon>> = _discoveredBeaconsFlow.asStateFlow()
    
    /**
     * Adds a mock beacon configuration.
     * 
     * @param beacon The beacon configuration to add
     * @param rssiAtOneMeter The RSSI value at one meter distance (typically around -70 dBm)
     * @param environmentalFactor The environmental factor for signal propagation (typically 2.0-4.0)
     * @param rssiNoise The amount of random noise to add to RSSI values (0.0-1.0)
     */
    fun addMockBeacon(
        beacon: Beacon,
        rssiAtOneMeter: Int = -70,
        environmentalFactor: Float = 2.0f,
        rssiNoise: Float = 0.1f
    ) {
        mockBeacons.add(
            MockBeacon(
                beacon = beacon,
                rssiAtOneMeter = rssiAtOneMeter,
                environmentalFactor = environmentalFactor,
                rssiNoise = rssiNoise
            )
        )
    }
    
    /**
     * Starts the mock BLE scanning process.
     */
    fun startScanning() {
        if (isScanning) return
        isScanning = true
    }
    
    /**
     * Stops the mock BLE scanning process.
     */
    fun stopScanning() {
        if (!isScanning) return
        isScanning = false
    }
    
    /**
     * Simulates a scan cycle, generating mock scan results based on the
     * configured beacons and the provided user position.
     * 
     * @param userX User's X coordinate in meters
     * @param userY User's Y coordinate in meters
     */
    fun simulateScanCycle(userX: Float, userY: Float) {
        if (!isScanning) return
        
        val discoveredBeacons = mutableListOf<Beacon>()
        
        for (mockBeacon in mockBeacons) {
            // Calculate distance from user to beacon
            val dx = mockBeacon.beacon.x - userX
            val dy = mockBeacon.beacon.y - userY
            val distance = kotlin.math.sqrt(dx * dx + dy * dy)
            
            // Calculate RSSI based on distance using the log-distance path loss model
            // RSSI = RSSI_at_1m - 10 * n * log10(d)
            // where n is the environmental factor
            val rssiMean = mockBeacon.rssiAtOneMeter - 
                           (10 * mockBeacon.environmentalFactor * kotlin.math.log10(
                               kotlin.math.max(distance, 0.1f)
                           )).toInt()
            
            // Add random noise to RSSI
            val rssiNoise = (Random.nextFloat() * 2 - 1) * mockBeacon.rssiNoise * 10
            val rssi = rssiMean + rssiNoise.toInt()
            
            // Create a copy of the beacon with updated RSSI
            val discoveredBeacon = mockBeacon.beacon.copy()
            discoveredBeacon.updateRssi(rssi, System.currentTimeMillis())
            
            // Add to discovered beacons
            discoveredBeacons.add(discoveredBeacon)
        }
        
        // Update the flow with discovered beacons
        _discoveredBeaconsFlow.value = discoveredBeacons
    }
    
    /**
     * Clears all mock beacon configurations.
     */
    fun clearMockBeacons() {
        mockBeacons.clear()
        _discoveredBeaconsFlow.value = emptyList()
    }
    
    /**
     * Generates a set of mock beacons arranged in a grid pattern.
     * 
     * @param rows Number of rows in the grid
     * @param cols Number of columns in the grid
     * @param spacingX Horizontal spacing between beacons in meters
     * @param spacingY Vertical spacing between beacons in meters
     * @param offsetX X coordinate offset for the grid
     * @param offsetY Y coordinate offset for the grid
     * @param rssiAtOneMeter The RSSI value at one meter distance
     * @param environmentalFactor The environmental factor for signal propagation
     * @param rssiNoise The amount of random noise to add to RSSI values
     */
    fun generateGridBeacons(
        rows: Int,
        cols: Int,
        spacingX: Float = 5f,
        spacingY: Float = 5f,
        offsetX: Float = 0f,
        offsetY: Float = 0f,
        rssiAtOneMeter: Int = -70,
        environmentalFactor: Float = 2.0f,
        rssiNoise: Float = 0.1f
    ) {
        for (row in 0 until rows) {
            for (col in 0 until cols) {
                val x = offsetX + col * spacingX
                val y = offsetY + row * spacingY
                
                val beacon = Beacon(
                    macAddress = generateMockMacAddress(),
                    x = x,
                    y = y,
                    txPower = rssiAtOneMeter,
                    name = "Beacon_${row}_${col}"
                )
                
                addMockBeacon(
                    beacon = beacon,
                    rssiAtOneMeter = rssiAtOneMeter,
                    environmentalFactor = environmentalFactor,
                    rssiNoise = rssiNoise
                )
            }
        }
    }
    
    /**
     * Generates a random MAC address for mock beacons.
     * 
     * @return A random MAC address string
     */
    private fun generateMockMacAddress(): String {
        val bytes = ByteArray(6)
        Random.nextBytes(bytes)
        
        return bytes.joinToString(":") { byte ->
            byte.toUByte().toString(16).padStart(2, '0').uppercase()
        }
    }
    
    /**
     * Data class representing a mock beacon configuration.
     */
    data class MockBeacon(
        val beacon: Beacon,
        val rssiAtOneMeter: Int,
        val environmentalFactor: Float,
        val rssiNoise: Float
    )
}