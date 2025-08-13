package com.example.myapplication.wifi

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import java.util.concurrent.ConcurrentHashMap

/**
 * Represents a calibration point with collected samples.
 */
data class CalibrationPoint(
    val locationId: String,
    val x: Double,
    val y: Double,
    val samples: List<List<ScanResult>>,
    val timestamp: Long = System.currentTimeMillis(),
    val metadata: Map<String, String> = emptyMap()
)

/**
 * Represents a calibration session with multiple calibration points.
 */
data class CalibrationSession(
    val sessionId: String,
    val points: List<CalibrationPoint>,
    val timestamp: Long = System.currentTimeMillis(),
    val metadata: Map<String, String> = emptyMap()
)

/**
 * Tool for calibrating Wi-Fi signal mapping.
 * Provides functionality for collecting samples at specific locations,
 * managing calibration sessions, and generating fingerprints from calibration data.
 */
class WifiCalibrationTool(
    private val wifiScanner: WifiScanner,
    private val fingerprintManager: WifiFingerprintManager
) {
    companion object {
        private const val TAG = "WifiCalibrationTool"
        private const val DEFAULT_SAMPLES_PER_POINT = 5
        private const val DEFAULT_SCAN_INTERVAL_MS = 1000L
    }

    // Store calibration points by location ID
    private val calibrationPoints = ConcurrentHashMap<String, CalibrationPoint>()
    
    // Store calibration sessions by session ID
    private val calibrationSessions = ConcurrentHashMap<String, CalibrationSession>()
    
    // Current calibration state
    private val _calibrationState = MutableStateFlow<CalibrationState>(CalibrationState.Idle)
    val calibrationState: StateFlow<CalibrationState> = _calibrationState
    
    /**
     * Collects Wi-Fi samples at a specific location.
     * 
     * @param locationId Identifier for the location
     * @param x X-coordinate of the location
     * @param y Y-coordinate of the location
     * @param numSamples Number of samples to collect (default: 5)
     * @param scanIntervalMs Interval between scans in milliseconds (default: 1000)
     * @param metadata Additional information about the location
     * @return The collected calibration point or null if collection failed
     */
    suspend fun collectSamples(
        locationId: String,
        x: Double,
        y: Double,
        numSamples: Int = DEFAULT_SAMPLES_PER_POINT,
        scanIntervalMs: Long = DEFAULT_SCAN_INTERVAL_MS,
        metadata: Map<String, String> = emptyMap()
    ): CalibrationPoint? {
        // Check permissions
        if (!wifiScanner.hasRequiredPermissions.first()) {
            Log.w(TAG, "Cannot collect samples: missing required permissions")
            _calibrationState.value = CalibrationState.Error("Missing required permissions")
            return null
        }
        
        // Update state
        _calibrationState.value = CalibrationState.Collecting(locationId, 0, numSamples)
        
        // Collect samples
        val samples = mutableListOf<List<ScanResult>>()
        
        try {
            // Configure scanner
            wifiScanner.startPeriodicScanning(scanIntervalMs)
            
            // Collect the specified number of samples
            repeat(numSamples) { sampleIndex ->
                // Update state
                _calibrationState.value = CalibrationState.Collecting(locationId, sampleIndex + 1, numSamples)
                
                // Get scan results
                val scanResults = wifiScanner.scanResults.first()
                
                if (scanResults.isNotEmpty()) {
                    samples.add(scanResults)
                    Log.d(TAG, "Collected sample ${sampleIndex + 1}/$numSamples with ${scanResults.size} APs")
                } else {
                    Log.w(TAG, "Empty scan results for sample ${sampleIndex + 1}/$numSamples")
                }
                
                // Wait for next scan
                kotlinx.coroutines.delay(scanIntervalMs)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error collecting samples", e)
            _calibrationState.value = CalibrationState.Error("Error collecting samples: ${e.message}")
            return null
        } finally {
            // Stop scanning
            wifiScanner.stopScan()
        }
        
        if (samples.isEmpty()) {
            Log.w(TAG, "No samples collected")
            _calibrationState.value = CalibrationState.Error("No samples collected")
            return null
        }
        
        // Create calibration point
        val calibrationPoint = CalibrationPoint(
            locationId = locationId,
            x = x,
            y = y,
            samples = samples,
            timestamp = System.currentTimeMillis(),
            metadata = metadata
        )
        
        // Store calibration point
        calibrationPoints[locationId] = calibrationPoint
        
        // Update state
        _calibrationState.value = CalibrationState.Idle
        
        return calibrationPoint
    }
    
    /**
     * Creates a calibration session from collected calibration points.
     * 
     * @param sessionId Identifier for the session
     * @param locationIds List of location IDs to include in the session
     * @param metadata Additional information about the session
     * @return The created calibration session or null if creation failed
     */
    fun createCalibrationSession(
        sessionId: String,
        locationIds: List<String>,
        metadata: Map<String, String> = emptyMap()
    ): CalibrationSession? {
        // Get calibration points
        val points = locationIds.mapNotNull { calibrationPoints[it] }
        
        if (points.isEmpty()) {
            Log.w(TAG, "No calibration points found for session $sessionId")
            return null
        }
        
        // Create session
        val session = CalibrationSession(
            sessionId = sessionId,
            points = points,
            timestamp = System.currentTimeMillis(),
            metadata = metadata
        )
        
        // Store session
        calibrationSessions[sessionId] = session
        
        return session
    }
    
    /**
     * Generates fingerprints from a calibration session.
     * 
     * @param sessionId Identifier for the session
     * @return Number of fingerprints generated
     */
    suspend fun generateFingerprintsFromSession(sessionId: String): Int {
        val session = calibrationSessions[sessionId] ?: run {
            Log.w(TAG, "Calibration session not found: $sessionId")
            return 0
        }
        
        return generateFingerprintsFromPoints(session.points)
    }
    
    /**
     * Generates fingerprints from calibration points.
     * 
     * @param points List of calibration points
     * @return Number of fingerprints generated
     */
    suspend fun generateFingerprintsFromPoints(points: List<CalibrationPoint>): Int {
        var generatedCount = 0
        
        for (point in points) {
            // Process samples to create a fingerprint
            val accessPointMap = processCalibrationSamples(point.samples)
            
            if (accessPointMap.isEmpty()) {
                Log.w(TAG, "No access points found for location ${point.locationId}")
                continue
            }
            
            // Create metadata with coordinates
            val metadata = point.metadata.toMutableMap().apply {
                put("x", point.x.toString())
                put("y", point.y.toString())
            }
            
            // Create fingerprint
            val fingerprint = WifiFingerprint(
                locationId = point.locationId,
                accessPoints = accessPointMap,
                timestamp = System.currentTimeMillis(),
                metadata = metadata
            )
            
            // Store fingerprint
            fingerprintManager.getFingerprint(point.locationId)?.let {
                Log.d(TAG, "Replacing existing fingerprint for location ${point.locationId}")
            }
            
            // Use reflection to access the private fingerprintMap in WifiFingerprintManager
            // Use a safer approach with explicit type checking
            val field = WifiFingerprintManager::class.java.getDeclaredField("fingerprintMap")
            field.isAccessible = true
            val fieldValue = field.get(fingerprintManager)
            
            // Check if the field is of the expected type before casting
            if (fieldValue is ConcurrentHashMap<*, *>) {
                @Suppress("UNCHECKED_CAST")
                val fingerprintMap = fieldValue as ConcurrentHashMap<String, WifiFingerprint>
                fingerprintMap[point.locationId] = fingerprint
            } else {
                Log.e(TAG, "Failed to access fingerprintMap: unexpected type ${fieldValue?.javaClass}")
            }
            
            generatedCount++
        }
        
        return generatedCount
    }
    
    /**
     * Processes calibration samples to create a fingerprint.
     * 
     * @param samples List of scan result samples
     * @return Map of BSSID to average RSSI values
     */
    private fun processCalibrationSamples(samples: List<List<ScanResult>>): Map<String, Int> {
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
     * Gets a calibration point by location ID.
     * 
     * @param locationId Identifier for the location
     * @return The calibration point or null if not found
     */
    fun getCalibrationPoint(locationId: String): CalibrationPoint? {
        return calibrationPoints[locationId]
    }
    
    /**
     * Gets all calibration points.
     * 
     * @return List of all calibration points
     */
    fun getAllCalibrationPoints(): List<CalibrationPoint> {
        return calibrationPoints.values.toList()
    }
    
    /**
     * Gets a calibration session by session ID.
     * 
     * @param sessionId Identifier for the session
     * @return The calibration session or null if not found
     */
    fun getCalibrationSession(sessionId: String): CalibrationSession? {
        return calibrationSessions[sessionId]
    }
    
    /**
     * Gets all calibration sessions.
     * 
     * @return List of all calibration sessions
     */
    fun getAllCalibrationSessions(): List<CalibrationSession> {
        return calibrationSessions.values.toList()
    }
    
    /**
     * Removes a calibration point.
     * 
     * @param locationId Identifier for the location
     */
    fun removeCalibrationPoint(locationId: String) {
        calibrationPoints.remove(locationId)
    }
    
    /**
     * Removes a calibration session.
     * 
     * @param sessionId Identifier for the session
     */
    fun removeCalibrationSession(sessionId: String) {
        calibrationSessions.remove(sessionId)
    }
    
    /**
     * Clears all calibration points and sessions.
     */
    fun clearCalibrationData() {
        calibrationPoints.clear()
        calibrationSessions.clear()
    }
    
    /**
     * Exports calibration data to a JSON string.
     * 
     * @return JSON string representation of calibration data
     */
    fun exportCalibrationDataToJson(): String {
        // Simple JSON serialization for demonstration
        val sb = StringBuilder()
        sb.append("{\n")
        sb.append("  \"calibrationPoints\": [\n")
        
        calibrationPoints.values.forEachIndexed { index, point ->
            sb.append("    {\n")
            sb.append("      \"locationId\": \"${point.locationId}\",\n")
            sb.append("      \"x\": ${point.x},\n")
            sb.append("      \"y\": ${point.y},\n")
            sb.append("      \"timestamp\": ${point.timestamp},\n")
            sb.append("      \"metadata\": {\n")
            
            point.metadata.entries.forEachIndexed { metaIndex, (key, value) ->
                sb.append("        \"$key\": \"$value\"")
                if (metaIndex < point.metadata.size - 1) {
                    sb.append(",")
                }
                sb.append("\n")
            }
            
            sb.append("      },\n")
            sb.append("      \"samples\": [\n")
            
            point.samples.forEachIndexed { sampleIndex, sample ->
                sb.append("        [\n")
                
                sample.forEachIndexed { resultIndex, result ->
                    sb.append("          {\n")
                    sb.append("            \"bssid\": \"${result.bssid}\",\n")
                    sb.append("            \"rssi\": ${result.rssi},\n")
                    sb.append("            \"timestamp\": ${result.timestamp}\n")
                    sb.append("          }")
                    
                    if (resultIndex < sample.size - 1) {
                        sb.append(",")
                    }
                    sb.append("\n")
                }
                
                sb.append("        ]")
                
                if (sampleIndex < point.samples.size - 1) {
                    sb.append(",")
                }
                sb.append("\n")
            }
            
            sb.append("      ]\n")
            sb.append("    }")
            
            if (index < calibrationPoints.size - 1) {
                sb.append(",")
            }
            sb.append("\n")
        }
        
        sb.append("  ],\n")
        sb.append("  \"calibrationSessions\": [\n")
        
        calibrationSessions.values.forEachIndexed { index, session ->
            sb.append("    {\n")
            sb.append("      \"sessionId\": \"${session.sessionId}\",\n")
            sb.append("      \"timestamp\": ${session.timestamp},\n")
            sb.append("      \"metadata\": {\n")
            
            session.metadata.entries.forEachIndexed { metaIndex, (key, value) ->
                sb.append("        \"$key\": \"$value\"")
                if (metaIndex < session.metadata.size - 1) {
                    sb.append(",")
                }
                sb.append("\n")
            }
            
            sb.append("      },\n")
            sb.append("      \"pointIds\": [\n")
            
            session.points.forEachIndexed { pointIndex, point ->
                sb.append("        \"${point.locationId}\"")
                
                if (pointIndex < session.points.size - 1) {
                    sb.append(",")
                }
                sb.append("\n")
            }
            
            sb.append("      ]\n")
            sb.append("    }")
            
            if (index < calibrationSessions.size - 1) {
                sb.append(",")
            }
            sb.append("\n")
        }
        
        sb.append("  ]\n")
        sb.append("}")
        
        return sb.toString()
    }
}

/**
 * Represents the state of the calibration process.
 */
sealed class CalibrationState {
    /**
     * Calibration is idle.
     */
    object Idle : CalibrationState()
    
    /**
     * Calibration is collecting samples.
     * 
     * @param locationId Identifier for the location
     * @param currentSample Current sample index (1-based)
     * @param totalSamples Total number of samples to collect
     */
    data class Collecting(
        val locationId: String,
        val currentSample: Int,
        val totalSamples: Int
    ) : CalibrationState()
    
    /**
     * Calibration encountered an error.
     * 
     * @param message Error message
     */
    data class Error(val message: String) : CalibrationState()
}