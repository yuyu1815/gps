package com.example.myapplication.data.repository

import com.example.myapplication.domain.model.Beacon
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap

/**
 * Implementation of the IBeaconRepository interface.
 * Manages BLE beacons in the indoor positioning system.
 */
class BeaconRepository(private val context: android.content.Context) : IBeaconRepository {
    
    // Using ConcurrentHashMap for thread safety
    private val beacons = ConcurrentHashMap<String, Beacon>()
    
    // StateFlow for observing beacon changes
    private val _beaconsFlow = MutableStateFlow<List<Beacon>>(emptyList())
    
    init {
        Timber.d("BeaconRepository initialized with context: ${context.packageName}")
    }
    
    /**
     * Updates the beacons flow with the current list of beacons.
     */
    private fun updateBeaconsFlow() {
        _beaconsFlow.value = beacons.values.toList()
    }
    
    override fun getBeaconsFlow(): Flow<List<Beacon>> {
        return _beaconsFlow.asStateFlow()
    }
    
    override suspend fun getAll(): List<Beacon> {
        return beacons.values.toList()
    }
    
    override suspend fun getBeacons(): List<Beacon> {
        return getAll()
    }
    
    override suspend fun addOrUpdateBeacon(beacon: Beacon): Boolean {
        return if (beacons.containsKey(beacon.macAddress)) {
            update(beacon)
        } else {
            save(beacon)
        }
    }
    
    override suspend fun getById(id: String): Beacon? {
        return beacons.values.find { it.id == id }
    }
    
    override suspend fun save(item: Beacon): Boolean {
        try {
            beacons[item.macAddress] = item
            updateBeaconsFlow()
            Timber.d("Saved beacon: ${item.macAddress}")
            return true
        } catch (e: Exception) {
            Timber.e(e, "Error saving beacon: ${item.macAddress}")
            return false
        }
    }
    
    override suspend fun update(item: Beacon): Boolean {
        return if (beacons.containsKey(item.macAddress)) {
            beacons[item.macAddress] = item
            updateBeaconsFlow()
            Timber.d("Updated beacon: ${item.macAddress}")
            true
        } else {
            Timber.w("Beacon not found for update: ${item.macAddress}")
            false
        }
    }
    
    override suspend fun delete(id: String): Boolean {
        // In this implementation, we're using MAC address as the key
        // so we need to find the beacon with the given ID first
        val beacon = beacons.values.find { it.id == id }
        return if (beacon != null) {
            beacons.remove(beacon.macAddress)
            updateBeaconsFlow()
            Timber.d("Deleted beacon with ID: $id")
            true
        } else {
            Timber.w("Beacon not found for deletion with ID: $id")
            false
        }
    }
    
    override suspend fun getBeacon(macAddress: String): Beacon? {
        return beacons[macAddress]
    }
    
    override suspend fun getBeaconsByMacAddresses(macAddresses: List<String>): List<Beacon> {
        return beacons.values.filter { it.macAddress in macAddresses }
    }
    
    override suspend fun updateRssi(macAddress: String, rssi: Int, timestamp: Long): Boolean {
        val beacon = beacons[macAddress]
        return if (beacon != null) {
            beacon.updateRssi(rssi, timestamp)
            beacons[macAddress] = beacon
            updateBeaconsFlow()
            Timber.d("Updated RSSI for beacon: $macAddress, RSSI: $rssi")
            true
        } else {
            Timber.w("Beacon not found for RSSI update: $macAddress")
            false
        }
    }
    
    override suspend fun updateDistance(macAddress: String, distance: Float): Boolean {
        val beacon = beacons[macAddress]
        return if (beacon != null) {
            beacon.estimatedDistance = distance
            beacons[macAddress] = beacon
            updateBeaconsFlow()
            Timber.d("Updated distance for beacon: $macAddress, distance: $distance")
            true
        } else {
            Timber.w("Beacon not found for distance update: $macAddress")
            false
        }
    }
    
    override suspend fun updateStaleness(timeoutMs: Long): Int {
        val currentTime = System.currentTimeMillis()
        var staleCount = 0
        
        beacons.values.forEach { beacon ->
            if (beacon.checkStaleness(currentTime, timeoutMs)) {
                staleCount++
            }
        }
        
        if (staleCount > 0) {
            updateBeaconsFlow()
            Timber.d("Updated staleness, $staleCount beacons marked as stale")
        }
        
        return staleCount
    }
    
    override suspend fun getNonStaleBeacons(): List<Beacon> {
        return beacons.values.filter { !it.isStale }
    }
    
    override suspend fun getBeaconsInRadius(x: Float, y: Float, radiusMeters: Float): List<Beacon> {
        val radiusSquared = radiusMeters * radiusMeters
        
        return beacons.values.filter { beacon ->
            val dx = beacon.x - x
            val dy = beacon.y - y
            (dx * dx + dy * dy) <= radiusSquared
        }
    }
    
    override suspend fun updateTxPower(macAddress: String, txPower: Int): Boolean {
        val beacon = beacons[macAddress]
        return if (beacon != null) {
            // Since txPower is a val in the Beacon class, we need to create a new instance
            val updatedBeacon = beacon.copy(txPower = txPower)
            beacons[macAddress] = updatedBeacon
            updateBeaconsFlow()
            Timber.d("Updated TxPower for beacon: $macAddress, TxPower: $txPower")
            true
        } else {
            Timber.w("Beacon not found for TxPower update: $macAddress")
            false
        }
    }
    
    override suspend fun updateBatteryLevel(macAddress: String, batteryLevel: Int): Boolean {
        val beacon = beacons[macAddress]
        return if (beacon != null) {
            beacon.updateBatteryLevel(batteryLevel)
            beacons[macAddress] = beacon
            updateBeaconsFlow()
            Timber.d("Updated battery level for beacon: $macAddress, Level: $batteryLevel%")
            true
        } else {
            Timber.w("Beacon not found for battery level update: $macAddress")
            false
        }
    }
    
    override suspend fun recordFailedDetection(macAddress: String): Boolean {
        val beacon = beacons[macAddress]
        return if (beacon != null) {
            beacon.recordFailedDetection()
            beacons[macAddress] = beacon
            updateBeaconsFlow()
            Timber.d("Recorded failed detection for beacon: $macAddress, Count: ${beacon.failedDetectionCount}")
            true
        } else {
            Timber.w("Beacon not found for failed detection record: $macAddress")
            false
        }
    }
    
    override suspend fun getBeaconsWithAlerts(): List<Beacon> {
        return beacons.values.filter { it.hasAlert }
    }
    
    override suspend fun evaluateBeaconHealth(): Int {
        var criticalCount = 0
        
        beacons.values.forEach { beacon ->
            beacon.evaluateHealthStatus()
            if (beacon.healthStatus == com.example.myapplication.domain.model.BeaconHealthStatus.CRITICAL) {
                criticalCount++
            }
        }
        
        if (criticalCount > 0 || beacons.values.any { it.hasAlert }) {
            updateBeaconsFlow()
            Timber.d("Evaluated beacon health, $criticalCount beacons in critical state")
        }
        
        return criticalCount
    }
}