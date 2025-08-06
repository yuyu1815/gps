package com.example.myapplication.data.repository

import com.example.myapplication.domain.model.Beacon
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing BLE beacons.
 */
interface IBeaconRepository : IRepository<Beacon> {
    /**
     * Gets a flow of all beacons, which emits updates when the beacon data changes.
     * @return Flow of beacon list
     */
    fun getBeaconsFlow(): Flow<List<Beacon>>
    
    /**
     * Gets all beacons.
     * @return List of all beacons
     */
    suspend fun getBeacons(): List<Beacon>
    
    /**
     * Adds a new beacon or updates an existing one.
     * @param beacon The beacon to add or update
     * @return True if the operation was successful, false otherwise
     */
    suspend fun addOrUpdateBeacon(beacon: Beacon): Boolean
    
    /**
     * Gets a beacon by its MAC address.
     * @param macAddress MAC address of the beacon
     * @return The beacon if found, null otherwise
     */
    suspend fun getBeacon(macAddress: String): Beacon?
    
    /**
     * Gets beacons by their MAC addresses.
     * @param macAddresses List of MAC addresses to filter by
     * @return List of beacons matching the MAC addresses
     */
    suspend fun getBeaconsByMacAddresses(macAddresses: List<String>): List<Beacon>
    
    /**
     * Updates the RSSI value for a beacon.
     * @param macAddress MAC address of the beacon
     * @param rssi New RSSI value
     * @param timestamp Timestamp of the measurement
     * @return True if the update was successful, false otherwise
     */
    suspend fun updateRssi(macAddress: String, rssi: Int, timestamp: Long): Boolean
    
    /**
     * Updates the estimated distance for a beacon.
     * @param macAddress MAC address of the beacon
     * @param distance Estimated distance in meters
     * @return True if the update was successful, false otherwise
     */
    suspend fun updateDistance(macAddress: String, distance: Float): Boolean
    
    /**
     * Updates the staleness status for all beacons based on a timeout threshold.
     * @param timeoutMs Timeout threshold in milliseconds
     * @return Number of beacons marked as stale
     */
    suspend fun updateStaleness(timeoutMs: Long): Int
    
    /**
     * Gets all non-stale beacons.
     * @return List of non-stale beacons
     */
    suspend fun getNonStaleBeacons(): List<Beacon>
    
    /**
     * Gets beacons within a specified radius from a position.
     * @param x X coordinate in meters
     * @param y Y coordinate in meters
     * @param radiusMeters Radius in meters
     * @return List of beacons within the radius
     */
    suspend fun getBeaconsInRadius(x: Float, y: Float, radiusMeters: Float): List<Beacon>
    
    /**
     * Updates the transmit power for a beacon.
     * @param macAddress MAC address of the beacon
     * @param txPower New transmit power value
     * @return True if the update was successful, false otherwise
     */
    suspend fun updateTxPower(macAddress: String, txPower: Int): Boolean
    
    /**
     * Updates the battery level for a beacon.
     * @param macAddress MAC address of the beacon
     * @param batteryLevel Battery level in percentage (0-100)
     * @return True if the update was successful, false otherwise
     */
    suspend fun updateBatteryLevel(macAddress: String, batteryLevel: Int): Boolean
    
    /**
     * Records a failed detection attempt for a beacon.
     * @param macAddress MAC address of the beacon
     * @return True if the update was successful, false otherwise
     */
    suspend fun recordFailedDetection(macAddress: String): Boolean
    
    /**
     * Gets all beacons that have active alerts.
     * @return List of beacons with active alerts
     */
    suspend fun getBeaconsWithAlerts(): List<Beacon>
    
    /**
     * Evaluates the health status of all beacons.
     * @return Number of beacons with critical health status
     */
    suspend fun evaluateBeaconHealth(): Int
}