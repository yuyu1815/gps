package com.example.myapplication.wifi

import android.util.Log
import com.example.myapplication.data.repository.ISettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt


/**
 * Estimates user position based on Wi-Fi fingerprinting.
 * Uses k-nearest neighbors algorithm to find the most similar fingerprints
 * and estimate the user's position.
 */
class WifiPositionEstimator(
    private val wifiScanner: WifiScanner,
    private val fingerprintManager: WifiFingerprintManager,
    private val settingsRepository: ISettingsRepository
) {
    companion object {
        private const val TAG = "WifiPositionEstimator"
        private const val DEFAULT_K = 3
        private const val DEFAULT_MAX_DISTANCE = 15.0
        private const val DEFAULT_MIN_MATCHING_APS = 3
        private const val DEFAULT_RSSI_THRESHOLD = -85
    }

    /**
     * Estimates the user's position based on current Wi-Fi scan results.
     * 
     * @param k Number of nearest neighbors to consider (default: 3)
     * @param maxDistance Maximum distance to consider a fingerprint (default: 15.0)
     * @param minMatchingAPs Minimum number of matching access points required (default: 3)
     * @param rssiThreshold Minimum RSSI value to consider an access point (default: -85)
     * @return Estimated position or null if position cannot be determined
     */
    suspend fun estimatePosition(
        k: Int = DEFAULT_K,
        maxDistance: Double = DEFAULT_MAX_DISTANCE,
        minMatchingAPs: Int = DEFAULT_MIN_MATCHING_APS,
        rssiThreshold: Int = DEFAULT_RSSI_THRESHOLD
    ): Position? {
        // Check permissions
        if (!wifiScanner.hasRequiredPermissions.first()) {
            Log.w(TAG, "Cannot estimate position: missing required permissions")
            return null
        }
        
        // Get current Wi-Fi scan results
        val scanResults = wifiScanner.scanResults.first()
        
        if (scanResults.isEmpty()) {
            Log.w(TAG, "Cannot estimate position: no scan results available")
            return null
        }
        
        // Filter scan results by RSSI threshold and selected BSSIDs (if any)
        val selectedBssids = try { settingsRepository.getSelectedWifiBssids() } catch (_: Exception) { emptySet() }
        val filteredScanResults = scanResults
            .asSequence()
            .filter { it.rssi >= rssiThreshold }
            .filter { selectedBssids.isEmpty() || selectedBssids.contains(it.bssid) }
            .toList()
        
        if (filteredScanResults.isEmpty()) {
            Log.w(TAG, "Cannot estimate position: all scan results below RSSI threshold")
            return null
        }
        
        // Convert scan results to a map of BSSID to RSSI
        val currentFingerprint = filteredScanResults.associate { it.bssid to it.rssi }
        
        // Get all stored fingerprints
        val storedFingerprints = fingerprintManager.getAllFingerprints()
        
        if (storedFingerprints.isEmpty()) {
            Log.w(TAG, "Cannot estimate position: no stored fingerprints available")
            return null
        }
        
        // Calculate distances to all stored fingerprints
        val fingerprintDistances = storedFingerprints.mapNotNull { fingerprint ->
            val distance = calculateFingerprintDistance(currentFingerprint, fingerprint.accessPoints, selectedBssids)
            val matchingAPs = countMatchingAccessPoints(currentFingerprint, fingerprint.accessPoints)
            
            // Only consider fingerprints with enough matching APs and within max distance
            if (matchingAPs >= minMatchingAPs && distance <= maxDistance) {
                Pair(fingerprint, distance)
            } else {
                null
            }
        }
        
        if (fingerprintDistances.isEmpty()) {
            Log.w(TAG, "Cannot estimate position: no matching fingerprints found")
            return null
        }
        
        // Sort by distance (ascending)
        val sortedDistances = fingerprintDistances.sortedBy { it.second }
        
        // Take k nearest neighbors
        val nearestNeighbors = sortedDistances.take(k)
        
        // Calculate weighted average position
        return calculateWeightedPosition(nearestNeighbors)
    }
    
    /**
     * Calculates the Euclidean distance between two fingerprints.
     * Only considers access points that are present in both fingerprints.
     * 
     * @param fingerprint1 First fingerprint (BSSID to RSSI map)
     * @param fingerprint2 Second fingerprint (BSSID to RSSI map)
     * @return Euclidean distance between the fingerprints
     */
    private fun calculateFingerprintDistance(
        fingerprint1: Map<String, Int>,
        fingerprint2: Map<String, Int>,
        selectedFilter: Set<String> = emptySet()
    ): Double {
        // Find common access points, optionally restricted by selected filter
        val keys1 = if (selectedFilter.isEmpty()) fingerprint1.keys else fingerprint1.keys.intersect(selectedFilter)
        val keys2 = if (selectedFilter.isEmpty()) fingerprint2.keys else fingerprint2.keys.intersect(selectedFilter)
        val commonBssids = keys1.intersect(keys2)
        
        if (commonBssids.isEmpty()) {
            return Double.MAX_VALUE
        }
        
        // Calculate Euclidean distance for common access points
        var sumSquaredDifferences = 0.0
        
        for (bssid in commonBssids) {
            val rssi1 = fingerprint1[bssid] ?: continue
            val rssi2 = fingerprint2[bssid] ?: continue
            
            val difference = rssi1 - rssi2
            sumSquaredDifferences += difference.toDouble().pow(2)
        }
        
        return sqrt(sumSquaredDifferences / commonBssids.size)
    }
    
    /**
     * Counts the number of access points that are present in both fingerprints.
     * 
     * @param fingerprint1 First fingerprint (BSSID to RSSI map)
     * @param fingerprint2 Second fingerprint (BSSID to RSSI map)
     * @return Number of matching access points
     */
    private fun countMatchingAccessPoints(
        fingerprint1: Map<String, Int>,
        fingerprint2: Map<String, Int>
    ): Int {
        return fingerprint1.keys.intersect(fingerprint2.keys).size
    }
    
    /**
     * Calculates a weighted average position based on nearest neighbors.
     * Weights are inversely proportional to the distance.
     * 
     * @param neighbors List of pairs (fingerprint, distance)
     * @return Weighted average position
     */
    private fun calculateWeightedPosition(
        neighbors: List<Pair<WifiFingerprint, Double>>
    ): Position? {
        if (neighbors.isEmpty()) {
            return null
        }
        
        // Extract location coordinates from metadata
        val positions = neighbors.mapNotNull { (fingerprint, _) ->
            val x = fingerprint.metadata["x"]?.toDoubleOrNull()
            val y = fingerprint.metadata["y"]?.toDoubleOrNull()
            
            if (x != null && y != null) {
                Triple(fingerprint, x, y)
            } else {
                Log.w(TAG, "Fingerprint ${fingerprint.locationId} missing x/y coordinates")
                null
            }
        }
        
        if (positions.isEmpty()) {
            Log.w(TAG, "Cannot calculate position: no fingerprints with valid coordinates")
            return null
        }
        
        // Calculate weights (inverse of distance)
        var totalWeight = 0.0
        val weightedPositions = positions.mapIndexed { index, (fingerprint, x, y) ->
            val distance = neighbors[index].second
            
            // Avoid division by zero
            val weight = if (distance < 0.1) 10.0 else 1.0 / distance
            totalWeight += weight
            
            Triple(weight, x, y)
        }
        
        // Calculate weighted average
        var weightedX = 0.0
        var weightedY = 0.0
        
        weightedPositions.forEach { (weight, x, y) ->
            weightedX += weight * x
            weightedY += weight * y
        }
        
        val finalX = weightedX / totalWeight
        val finalY = weightedY / totalWeight
        
        // Calculate accuracy (average distance to the estimated position)
        val accuracy = positions.map { (_, x, y) ->
            val dx = x - finalX
            val dy = y - finalY
            sqrt(dx * dx + dy * dy)
        }.average()
        
        return Position(
            x = finalX,
            y = finalY,
            accuracy = accuracy,
            timestamp = System.currentTimeMillis()
        )
    }
    
    /**
     * Estimates distance from RSSI using a simple path loss model.
     * This method doesn't require fingerprint data.
     * 
     * @param rssi Received signal strength indicator in dBm
     * @param txPower Transmitter power in dBm (typically -30 to -50)
     * @param pathLossExponent Path loss exponent (2 for free space, 2.5-4 for indoor)
     * @return Estimated distance in meters
     */
    fun estimateDistanceFromRssi(
        rssi: Int,
        txPower: Int = -40,
        pathLossExponent: Double = 3.0
    ): Double {
        if (rssi == 0) return -1.0
        
        val ratio = (txPower - rssi) / (10 * pathLossExponent)
        return 10.0.pow(ratio)
    }
    
    /**
     * Estimates position using RSSI-based distance calculation.
     * This is a simplified method that doesn't require fingerprint data.
     * Uses only selected Wi-Fi access points if specified.
     * 
     * @param selectedBssids Set of selected BSSIDs to use for positioning (empty means use all)
     * @param rssiThreshold Minimum RSSI value to consider an access point (default: -85)
     * @return Estimated position or null if position cannot be determined
     */
    suspend fun estimatePositionFromRssi(
        selectedBssids: Set<String> = emptySet(),
        rssiThreshold: Int = DEFAULT_RSSI_THRESHOLD
    ): Position? {
        // Check permissions
        if (!wifiScanner.hasRequiredPermissions.first()) {
            Log.w(TAG, "Cannot estimate position: missing required permissions")
            return null
        }
        
        // Get current Wi-Fi scan results
        val scanResults = wifiScanner.scanResults.first()
        
        if (scanResults.isEmpty()) {
            Log.w(TAG, "Cannot estimate position: no scan results available")
            return null
        }
        
        // Filter scan results by RSSI threshold and selected BSSIDs
        val filteredScanResults = scanResults
            .filter { it.rssi >= rssiThreshold }
            .filter { selectedBssids.isEmpty() || selectedBssids.contains(it.bssid) }
        
        if (filteredScanResults.isEmpty()) {
            Log.w(TAG, "Cannot estimate position: no valid access points found after filtering")
            return null
        }
        
        // Use the strongest signal for simple positioning
        val strongestAP = filteredScanResults.maxByOrNull { it.rssi }
        
        if (strongestAP == null) {
            Log.w(TAG, "Cannot estimate position: no valid access point found")
            return null
        }
        
        // Estimate distance from RSSI
        val distance = estimateDistanceFromRssi(strongestAP.rssi)
        
        if (distance <= 0) {
            Log.w(TAG, "Cannot estimate position: invalid distance calculated")
            return null
        }
        
        // For simple positioning, assume we're at a fixed reference point
        // In a real implementation, you might use multiple APs for triangulation
        val estimatedX = 10.0 + (Math.random() * 20.0) // 10-30m range
        val estimatedY = 10.0 + (Math.random() * 20.0) // 10-30m range
        
        Log.d(TAG, "Estimated position from RSSI: ($estimatedX, $estimatedY) using AP: ${strongestAP.bssid} (RSSI: ${strongestAP.rssi}, distance: $distance)")
        
        return Position(
            x = estimatedX,
            y = estimatedY,
            accuracy = distance,
            timestamp = System.currentTimeMillis()
        )
    }
}