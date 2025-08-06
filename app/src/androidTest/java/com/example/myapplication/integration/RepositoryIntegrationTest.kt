package com.example.myapplication.integration

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.myapplication.data.repository.BeaconRepository
import com.example.myapplication.data.repository.IBeaconRepository
import com.example.myapplication.data.repository.PositionRepository
import com.example.myapplication.domain.model.Beacon
import com.example.myapplication.domain.model.UserPosition
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration test for repository interactions.
 * 
 * This test verifies that the repositories correctly interact with each other
 * and maintain consistent state.
 */
@RunWith(AndroidJUnit4::class)
class RepositoryIntegrationTest {

    private lateinit var context: Context
    private lateinit var beaconRepository: IBeaconRepository
    private lateinit var positionRepository: PositionRepository
    
    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        beaconRepository = BeaconRepository(context)
        positionRepository = PositionRepository(context)
    }
    
    @After
    fun tearDown() {
        // Clean up any test data
        runBlocking {
            // Clear repositories if they have clear methods
            // This depends on the actual implementation
        }
    }
    
    @Test
    fun testBeaconRepositoryStoresAndRetrievesBeacons() = runBlocking {
        // Create test beacons
        val beacon1 = Beacon(
            macAddress = "AA:BB:CC:DD:EE:FF",
            x = 10f,
            y = 20f,
            txPower = -70,
            name = "Test Beacon 1"
        )
        
        val beacon2 = Beacon(
            macAddress = "11:22:33:44:55:66",
            x = 15f,
            y = 25f,
            txPower = -75,
            name = "Test Beacon 2"
        )
        
        // Add beacons to repository
        beaconRepository.addOrUpdateBeacon(beacon1)
        beaconRepository.addOrUpdateBeacon(beacon2)
        
        // Retrieve all beacons
        val beacons = beaconRepository.getBeacons()
        
        // Verify beacons were stored correctly
        assertEquals(2, beacons.size)
        
        // Find our test beacons
        val foundBeacon1 = beacons.find { it.macAddress == beacon1.macAddress }
        val foundBeacon2 = beacons.find { it.macAddress == beacon2.macAddress }
        
        // Verify beacon properties
        assertNotNull(foundBeacon1)
        assertNotNull(foundBeacon2)
        assertEquals(beacon1.name, foundBeacon1?.name)
        assertEquals(beacon2.name, foundBeacon2?.name)
        assertEquals(beacon1.x, foundBeacon1?.x)
        assertEquals(beacon2.y, foundBeacon2?.y)
    }
    
    @Test
    fun testPositionRepositoryStoresAndRetrievesPositions() = runBlocking {
        // Create test positions
        val position1 = UserPosition(
            x = 5f,
            y = 10f,
            accuracy = 2.0f,
            confidence = 0.8f,
            timestamp = System.currentTimeMillis()
        )
        
        val position2 = UserPosition(
            x = 7f,
            y = 12f,
            accuracy = 1.5f,
            confidence = 0.9f,
            timestamp = System.currentTimeMillis() + 1000
        )
        
        // Add positions to repository
        positionRepository.updatePosition(position1)
        positionRepository.updatePosition(position2)
        
        // Get current position (should be the most recent one)
        val currentPosition = positionRepository.getCurrentPosition()
        
        // Verify current position is the most recent one
        assertNotNull(currentPosition)
        assertEquals(position2.x, currentPosition?.x)
        assertEquals(position2.y, currentPosition?.y)
        
        // Get position history
        val history = positionRepository.getPositionHistory()
        
        // Verify history contains both positions
        assertEquals(2, history.size)
        assertTrue(history.any { it.x == position1.x && it.y == position1.y })
        assertTrue(history.any { it.x == position2.x && it.y == position2.y })
    }
    
    @Test
    fun testRepositoryInteraction() = runBlocking {
        // Create test beacons
        val beacon1 = Beacon(
            macAddress = "AA:BB:CC:DD:EE:FF",
            x = 10f,
            y = 20f,
            txPower = -70,
            name = "Test Beacon 1"
        )
        
        val beacon2 = Beacon(
            macAddress = "11:22:33:44:55:66",
            x = 15f,
            y = 25f,
            txPower = -75,
            name = "Test Beacon 2"
        )
        
        // Add beacons to repository
        beaconRepository.addOrUpdateBeacon(beacon1)
        beaconRepository.addOrUpdateBeacon(beacon2)
        
        // Update beacon distances (simulating distance calculation)
        beaconRepository.updateDistance(beacon1.macAddress, 5.0f)
        beaconRepository.updateDistance(beacon2.macAddress, 7.0f)
        
        // Create a position based on beacon positions (simulating triangulation)
        val calculatedPosition = UserPosition(
            x = (beacon1.x + beacon2.x) / 2,  // Simple averaging for test
            y = (beacon1.y + beacon2.y) / 2,
            accuracy = 3.0f,
            confidence = 0.85f,
            timestamp = System.currentTimeMillis()
        )
        
        // Update position repository
        positionRepository.updatePosition(calculatedPosition)
        
        // Verify position was stored
        val currentPosition = positionRepository.getCurrentPosition()
        assertNotNull(currentPosition)
        assertEquals(calculatedPosition.x, currentPosition?.x)
        assertEquals(calculatedPosition.y, currentPosition?.y)
        
        // Verify beacons have updated distances
        val updatedBeacons = beaconRepository.getBeacons()
        val updatedBeacon1 = updatedBeacons.find { it.macAddress == beacon1.macAddress }
        val updatedBeacon2 = updatedBeacons.find { it.macAddress == beacon2.macAddress }
        
        assertNotNull(updatedBeacon1)
        assertNotNull(updatedBeacon2)
        assertEquals(5.0f, updatedBeacon1?.estimatedDistance)
        assertEquals(7.0f, updatedBeacon2?.estimatedDistance)
    }
    
    @Test
    fun testRepositoryFlows() = runBlocking {
        // Create test data
        val beacon = Beacon(
            macAddress = "AA:BB:CC:DD:EE:FF",
            x = 10f,
            y = 20f,
            txPower = -70,
            name = "Test Beacon"
        )
        
        val position = UserPosition(
            x = 5f,
            y = 10f,
            accuracy = 2.5f,
            confidence = 0.8f,
            timestamp = System.currentTimeMillis()
        )
        
        // Add data to repositories
        beaconRepository.addOrUpdateBeacon(beacon)
        positionRepository.updatePosition(position)
        
        // Collect from flows
        val beaconsFlow = beaconRepository.getBeaconsFlow()
        val positionFlow = positionRepository.getCurrentPositionFlow()
        
        // Verify initial values
        val beacons = beaconsFlow.first()
        val currentPosition = positionFlow.first()
        
        assertEquals(1, beacons.size)
        assertEquals(beacon.macAddress, beacons[0].macAddress)
        
        assertNotNull(currentPosition)
        assertEquals(position.x, currentPosition?.x)
        assertEquals(position.y, currentPosition?.y)
        
        // Update data
        val updatedBeacon = beacon.copy(lastRssi = -65)
        beaconRepository.addOrUpdateBeacon(updatedBeacon)
        
        val updatedPosition = UserPosition(
            x = 6f,
            y = 11f,
            accuracy = 1.8f,
            confidence = 0.9f,
            timestamp = System.currentTimeMillis()
        )
        positionRepository.updatePosition(updatedPosition)
        
        // Verify updated values
        val updatedBeacons = beaconsFlow.first()
        val updatedCurrentPosition = positionFlow.first()
        
        assertEquals(1, updatedBeacons.size)
        assertEquals(-65, updatedBeacons[0].lastRssi)
        
        assertNotNull(updatedCurrentPosition)
        assertEquals(updatedPosition.x, updatedCurrentPosition?.x)
        assertEquals(updatedPosition.y, updatedCurrentPosition?.y)
    }
}