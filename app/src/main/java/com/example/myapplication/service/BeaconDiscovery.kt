package com.example.myapplication.service

import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.os.ParcelUuid
import com.example.myapplication.data.repository.IBeaconRepository
import com.example.myapplication.domain.model.Beacon
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Handles beacon discovery and filtering from BLE scan results.
 * Implements filtering, RSSI smoothing, and staleness management.
 */
class BeaconDiscovery(
    private val beaconRepository: IBeaconRepository
) {
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    
    // Known beacon UUIDs for filtering
    private val knownBeaconUuids = mutableListOf<UUID>()
    
    // Beacon manufacturers for filtering
    private val knownManufacturerIds = mutableListOf(
        0x004C, // Apple (iBeacon)
        0x0059, // Nordic Semiconductor
        0x0499  // Ruuvi
    )
    
    // Cache of recently seen beacons for quick access and filtering
    private val beaconCache = ConcurrentHashMap<String, Beacon>()
    
    // Staleness timeout in milliseconds (default: 5 seconds)
    private var stalenessTimeoutMs = Beacon.DEFAULT_STALENESS_TIMEOUT_MS
    
    // Signal strength threshold for filtering very weak signals (in dBm)
    private var rssiThreshold = -90
    
    /**
     * Adds a known beacon UUID for filtering.
     */
    fun addKnownBeaconUuid(uuid: UUID) {
        knownBeaconUuids.add(uuid)
    }
    
    /**
     * Adds a known manufacturer ID for filtering.
     */
    fun addKnownManufacturerId(manufacturerId: Int) {
        knownManufacturerIds.add(manufacturerId)
    }
    
    /**
     * Sets the staleness timeout in milliseconds.
     * Beacons not seen for this duration will be considered stale.
     */
    fun setStalenessTimeout(timeoutMs: Long) {
        stalenessTimeoutMs = timeoutMs
    }
    
    /**
     * Sets the RSSI threshold for filtering weak signals.
     * Beacons with RSSI below this threshold will be ignored.
     */
    fun setRssiThreshold(threshold: Int) {
        rssiThreshold = threshold
    }
    
    /**
     * Creates scan filters for known beacon types.
     */
    fun createScanFilters(): List<ScanFilter> {
        val filters = mutableListOf<ScanFilter>()
        
        // Add filters for known UUIDs
        knownBeaconUuids.forEach { uuid ->
            val filter = ScanFilter.Builder()
                .setServiceUuid(ParcelUuid(uuid))
                .build()
            filters.add(filter)
        }
        
        // Add filters for known manufacturers
        // Note: Manufacturer-specific data filtering is more complex and may require
        // additional processing in the processBeacon method
        
        return filters
    }
    
    /**
     * Checks all cached beacons for staleness and updates their status.
     * Should be called periodically to maintain accurate beacon status.
     */
    fun checkBeaconStaleness() {
        val currentTime = System.currentTimeMillis()
        
        coroutineScope.launch {
            // Check staleness for all cached beacons
            beaconCache.forEach { (_, beacon) ->
                if (beacon.checkStaleness(currentTime, stalenessTimeoutMs)) {
                    Timber.d("Beacon ${beacon.macAddress} marked as stale")
                    
                    // Update the beacon in the repository
                    beaconRepository.update(beacon)
                }
            }
            
            // Remove stale beacons from cache if they've been stale for too long (3x timeout)
            val veryStaleThreshold = stalenessTimeoutMs * 3
            val staleBeacons = beaconCache.filter { (_, beacon) -> 
                (currentTime - beacon.lastSeenTimestamp) > veryStaleThreshold 
            }
            
            staleBeacons.forEach { (address, _) ->
                beaconCache.remove(address)
                Timber.d("Very stale beacon $address removed from cache")
            }
        }
    }
    
    /**
     * Processes a scan result to extract beacon information.
     * Implements filtering based on signal strength and beacon type.
     * Updates the beacon cache and repository with the latest data.
     */
    fun processBeacon(scanResult: ScanResult) {
        val device = scanResult.device
        val rssi = scanResult.rssi
        val scanRecord = scanResult.scanRecord ?: return
        val currentTime = System.currentTimeMillis()
        
        // Filter out weak signals
        if (rssi < rssiThreshold) {
            Timber.v("Beacon ${device.address} filtered out due to weak signal: $rssi dBm")
            return
        }
        
        Timber.d("Processing beacon: ${device.address}, RSSI: $rssi dBm")
        
        // Extract beacon data
        val serviceUuids = scanRecord.serviceUuids
        val manufacturerData = scanRecord.manufacturerSpecificData
        
        // Get device name safely (avoiding permission issues)
        val deviceName = try {
            scanRecord.deviceName ?: device.name ?: ""
        } catch (e: SecurityException) {
            Timber.w("Permission denied when accessing device name: ${e.message}")
            ""
        }
        
        // Check if this is a known beacon type
        val isKnownBeacon = isKnownBeacon(serviceUuids, manufacturerData)
        
        if (!isKnownBeacon) {
            Timber.d("Unknown beacon type, ignoring")
            return
        }
        
        // Extract beacon-specific data
        val beaconData = extractBeaconData(scanRecord, device.address)
        
        // Check if we already have this beacon in our cache
        val cachedBeacon = beaconCache[device.address]
        
        coroutineScope.launch {
            if (cachedBeacon != null) {
                // Update existing beacon in cache
                cachedBeacon.updateRssi(rssi, currentTime)
                beaconCache[device.address] = cachedBeacon
                
                // Update in repository
                beaconRepository.update(cachedBeacon)
                Timber.d("Updated existing beacon: ${device.address}, filtered RSSI: ${cachedBeacon.filteredRssi}")
            } else {
                // Check repository for existing beacon
                val existingBeacon = beaconRepository.getById(device.address)
                
                if (existingBeacon != null) {
                    // Update existing beacon from repository
                    existingBeacon.updateRssi(rssi, currentTime)
                    beaconCache[device.address] = existingBeacon
                    beaconRepository.update(existingBeacon)
                    Timber.d("Updated beacon from repository: ${device.address}")
                } else {
                    // Create new beacon
                    val beacon = Beacon(
                        macAddress = device.address,
                        name = deviceName,
                        x = beaconData.x,
                        y = beaconData.y,
                        txPower = beaconData.txPower,
                        lastRssi = rssi,
                        filteredRssi = rssi,
                        lastSeenTimestamp = currentTime
                    )
                    
                    // Initialize RSSI history with first value
                    beacon.updateRssi(rssi, currentTime)
                    
                    // Add to cache and repository
                    beaconCache[device.address] = beacon
                    beaconRepository.save(beacon)
                    Timber.d("Created new beacon: ${device.address}")
                }
            }
        }
    }
    
    /**
     * Checks if a beacon is a known type based on service UUIDs and manufacturer data.
     */
    private fun isKnownBeacon(
        serviceUuids: List<ParcelUuid>?,
        manufacturerData: android.util.SparseArray<ByteArray>?
    ): Boolean {
        // Check service UUIDs
        if (serviceUuids != null) {
            for (serviceUuid in serviceUuids) {
                if (knownBeaconUuids.contains(serviceUuid.uuid)) {
                    return true
                }
            }
        }
        
        // Check manufacturer data
        if (manufacturerData != null) {
            for (i in 0 until manufacturerData.size()) {
                val manufacturerId = manufacturerData.keyAt(i)
                if (knownManufacturerIds.contains(manufacturerId)) {
                    return true
                }
            }
        }
        
        return false
    }
    
    /**
     * Extracts beacon-specific data from scan record.
     */
    private fun extractBeaconData(
        scanRecord: android.bluetooth.le.ScanRecord,
        macAddress: String
    ): BeaconData {
        // Default values
        var x = 0f
        var y = 0f
        var txPower = -59 // Default txPower at 1m distance
        
        // Try to extract txPower from scan record
        val recordTxPower = scanRecord.txPowerLevel
        if (recordTxPower != Integer.MIN_VALUE) {
            txPower = recordTxPower
        }
        
        // For iBeacon, extract from manufacturer data
        val manufacturerData = scanRecord.manufacturerSpecificData
        if (manufacturerData != null && manufacturerData.size() > 0) {
            val appleData = manufacturerData.get(0x004C) // Apple
            if (appleData != null && appleData.size >= 23) {
                // iBeacon format
                if (appleData[0] == 0x02.toByte() && appleData[1] == 0x15.toByte()) {
                    // Extract txPower from iBeacon format (calibrated RSSI at 1m)
                    txPower = appleData[22].toInt()
                    if (txPower > 127) txPower -= 256 // Convert to signed byte
                }
            }
        }
        
        // In a real application, you would look up the beacon's position from a configuration
        // or database. For now, we'll use placeholder values.
        
        return BeaconData(x, y, txPower)
    }
    
    /**
     * Data class to hold extracted beacon data.
     */
    data class BeaconData(
        val x: Float,
        val y: Float,
        val txPower: Int
    )
}