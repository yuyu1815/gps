package com.example.myapplication.wifi

import android.util.Log
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import java.util.concurrent.ConcurrentHashMap

/**
 * Represents a Wi-Fi fingerprint for a specific location.
 * Contains signal strength information for multiple access points.
 */
@JsonClass(generateAdapter = true)
data class WifiFingerprint(
    val locationId: String,
    val accessPoints: Map<String, Int>, // BSSID to RSSI mapping
    val timestamp: Long = System.currentTimeMillis(),
    val metadata: Map<String, String> = emptyMap()
)

/**
 * Manages the generation and storage of Wi-Fi fingerprints for indoor positioning.
 */
class WifiFingerprintManager(private val wifiScanner: WifiScanner) {
    companion object {
        private const val TAG = "WifiFingerprintManager"
        private const val MIN_SAMPLES_FOR_FINGERPRINT = 5
        private const val MAX_FINGERPRINT_AGE_MS = 24 * 60 * 60 * 1000L // 24 hours
    }

    // Store fingerprints by location ID
    private val fingerprintMap = ConcurrentHashMap<String, WifiFingerprint>()
    
    /**
     * Creates a fingerprint for the current location by collecting multiple Wi-Fi scans.
     * 
     * @param locationId Identifier for the current location
     * @param numSamples Number of scan samples to collect (default: 5)
     * @param metadata Additional information about the location
     * @return true if fingerprint was successfully created, false otherwise
     */
    suspend fun createFingerprint(
        locationId: String,
        numSamples: Int = MIN_SAMPLES_FOR_FINGERPRINT,
        metadata: Map<String, String> = emptyMap()
    ): Boolean {
        if (!wifiScanner.hasRequiredPermissions.first()) {
            Log.w(TAG, "Cannot create fingerprint: missing required permissions")
            return false
        }
        
        // Collect scan samples
        val samples = mutableListOf<List<ScanResult>>()
        
        // Start scanning
        wifiScanner.startPeriodicScanning()
        
        try {
            // Collect the specified number of samples
            repeat(numSamples) {
                val scanResults = wifiScanner.scanResults.first()
                if (scanResults.isNotEmpty()) {
                    samples.add(scanResults)
                    Log.d(TAG, "Collected sample ${it + 1}/$numSamples with ${scanResults.size} APs")
                }
            }
        } finally {
            // Stop scanning
            wifiScanner.stopScan()
        }
        
        if (samples.isEmpty()) {
            Log.w(TAG, "Failed to create fingerprint: no samples collected")
            return false
        }
        
        // Process samples to create a fingerprint
        val accessPointMap = processScans(samples)
        
        if (accessPointMap.isEmpty()) {
            Log.w(TAG, "Failed to create fingerprint: no access points found")
            return false
        }
        
        // Create and store the fingerprint
        val fingerprint = WifiFingerprint(
            locationId = locationId,
            accessPoints = accessPointMap,
            timestamp = System.currentTimeMillis(),
            metadata = metadata
        )
        
        fingerprintMap[locationId] = fingerprint
        Log.d(TAG, "Created fingerprint for location $locationId with ${accessPointMap.size} access points")
        
        return true
    }
    
    /**
     * Processes multiple scan samples to create a stable fingerprint.
     * 
     * @param samples List of scan result samples
     * @return Map of BSSID to average RSSI values
     */
    private fun processScans(samples: List<List<ScanResult>>): Map<String, Int> {
        // Collect all BSSIDs across all samples
        val allBssids = mutableSetOf<String>()
        samples.forEach { sample ->
            sample.forEach { result ->
                allBssids.add(result.bssid)
            }
        }
        
        // Calculate average RSSI for each BSSID
        val result = mutableMapOf<String, Int>()
        
        for (bssid in allBssids) {
            var totalRssi = 0
            var count = 0
            
            // Sum RSSI values across all samples
            samples.forEach { sample ->
                sample.find { it.bssid == bssid }?.let {
                    totalRssi += it.rssi
                    count++
                }
            }
            
            // Only include APs that were present in at least half of the samples
            if (count >= samples.size / 2) {
                result[bssid] = totalRssi / count
            }
        }
        
        return result
    }
    
    /**
     * Gets a fingerprint for a specific location.
     * 
     * @param locationId Identifier for the location
     * @return The fingerprint or null if not found
     */
    fun getFingerprint(locationId: String): WifiFingerprint? {
        return fingerprintMap[locationId]
    }
    
    /**
     * Gets all stored fingerprints.
     * 
     * @return List of all fingerprints
     */
    fun getAllFingerprints(): List<WifiFingerprint> {
        return fingerprintMap.values.toList()
    }
    
    /**
     * Removes a fingerprint for a specific location.
     * 
     * @param locationId Identifier for the location
     */
    fun removeFingerprint(locationId: String) {
        fingerprintMap.remove(locationId)
    }
    
    /**
     * Clears all stored fingerprints.
     */
    fun clearFingerprints() {
        fingerprintMap.clear()
    }
    
    /**
     * Removes fingerprints older than the specified age.
     * 
     * @param maxAgeMs Maximum age in milliseconds (default: 24 hours)
     * @return Number of fingerprints removed
     */
    fun removeStaleFingerprints(maxAgeMs: Long = MAX_FINGERPRINT_AGE_MS): Int {
        val currentTime = System.currentTimeMillis()
        val staleKeys = mutableListOf<String>()
        
        // Find stale fingerprints
        fingerprintMap.forEach { (locationId, fingerprint) ->
            if (currentTime - fingerprint.timestamp > maxAgeMs) {
                staleKeys.add(locationId)
            }
        }
        
        // Remove stale fingerprints
        staleKeys.forEach { locationId ->
            fingerprintMap.remove(locationId)
        }
        
        return staleKeys.size
    }
    
    /**
     * Exports fingerprints to a JSON string for storage.
     * 
     * @return JSON string representation of fingerprints
     */
    fun exportFingerprintsToJson(): String {
        val moshi = Moshi.Builder().build()
        val type = Types.newParameterizedType(List::class.java, WifiFingerprint::class.java)
        val adapter = moshi.adapter<List<WifiFingerprint>>(type)
        return adapter.toJson(fingerprintMap.values.toList())
    }
    
    /**
     * Imports fingerprints from a JSON string.
     * 
     * @param json JSON string representation of fingerprints
     * @return Number of fingerprints imported
     */
    fun importFingerprintsFromJson(json: String): Int {
        return try {
            val moshi = Moshi.Builder().build()
            val type = Types.newParameterizedType(List::class.java, WifiFingerprint::class.java)
            val adapter = moshi.adapter<List<WifiFingerprint>>(type)
            val list = adapter.fromJson(json).orEmpty()
            list.forEach { fingerprintMap[it.locationId] = it }
            list.size
        } catch (e: Exception) {
            Log.e(TAG, "Failed to import fingerprints", e)
            0
        }
    }

    fun saveToFile(filePath: String): Boolean {
        return try {
            val json = exportFingerprintsToJson()
            java.io.File(filePath).writeText(json)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save fingerprints to $filePath", e)
            false
        }
    }

    fun loadFromFile(filePath: String): Int {
        return try {
            val file = java.io.File(filePath)
            if (!file.exists()) return 0
            val json = file.readText()
            importFingerprintsFromJson(json)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load fingerprints from $filePath", e)
            0
        }
    }
}