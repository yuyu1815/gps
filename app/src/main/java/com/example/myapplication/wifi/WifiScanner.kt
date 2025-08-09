package com.example.myapplication.wifi

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.ScanResult as NativeScanResult
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import java.util.ArrayList
import java.util.Iterator

/**
 * Wi-Fi scanner service that collects RSSI data from nearby access points.
 * Supports periodic scanning, signal filtering, and provides timestamped results.
 */
class WifiScanner(private val context: Context) {
    companion object {
        private const val TAG = "WifiScanner"
        private const val DEFAULT_SCAN_INTERVAL_MS = 5000L
        private const val DEFAULT_FILTER_WINDOW_SIZE = 5
        private const val STALE_THRESHOLD_MS = 30000L // 30 seconds
        
        // Required permissions for Wi-Fi scanning
        private val REQUIRED_PERMISSIONS = buildList {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.ACCESS_WIFI_STATE)
            add(Manifest.permission.CHANGE_WIFI_STATE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.NEARBY_WIFI_DEVICES)
            }
        }.toTypedArray()
    }

    private val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    private var scanJob: Job? = null
    private var isRegistered = false

    // Store raw scan results with timestamps
    private val rawScanResults = ConcurrentHashMap<String, ArrayList<TimestampedRssi>>()
    
    // Filtered results exposed to clients
    private val _scanResults = MutableStateFlow<List<ScanResult>>(emptyList())
    val scanResults: StateFlow<List<ScanResult>> = _scanResults

    // Processed access points with estimated distance exposed to clients
    private val _accessPoints = MutableStateFlow<List<com.example.myapplication.domain.model.WifiAccessPoint>>(emptyList())
    val accessPoints: StateFlow<List<com.example.myapplication.domain.model.WifiAccessPoint>> = _accessPoints

    // Scanning configuration
    private var scanIntervalMs = DEFAULT_SCAN_INTERVAL_MS
    private var filterWindowSize = DEFAULT_FILTER_WINDOW_SIZE
    private var isPeriodicScanningEnabled = false
    
    // Permission state
    private val _hasRequiredPermissions = MutableStateFlow(false)
    val hasRequiredPermissions: StateFlow<Boolean> = _hasRequiredPermissions

    // Scanning state
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning

    private val wifiScanReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == WifiManager.SCAN_RESULTS_AVAILABLE_ACTION) {
                val success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false)
                if (success) {
                    processWifiScanResults()
                } else {
                    Log.d(TAG, "Scan was not successful")
                }
                // Update scanning state
                _isScanning.value = false
            }
        }
    }

    init {
        // Check permissions on initialization
        updatePermissionState()
    }
    
    /**
     * Updates the permission state based on current granted permissions.
     * Should be called after permission requests are handled.
     */
    fun updatePermissionState() {
        _hasRequiredPermissions.value = hasPermissions()
    }
    
    /**
     * Checks if all required permissions are granted.
     * 
     * @return true if all permissions are granted, false otherwise
     */
    private fun hasPermissions(): Boolean {
        return REQUIRED_PERMISSIONS.all { permission ->
            ActivityCompat.checkSelfPermission(context, permission) == 
                    PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Starts a single Wi-Fi scan.
     * 
     * Note: This method uses startScan() which is deprecated in API level 28.
     * The ability for apps to trigger scan requests may be removed in a future release.
     * 
     * @return true if scan was started, false if permissions are missing
     */
    @Suppress("DEPRECATION")
    fun startScan(): Boolean {
        if (!hasPermissions()) {
            Log.w(TAG, "Cannot start Wi-Fi scan: missing required permissions")
            return false
        }
        
        if (!wifiManager.isWifiEnabled) {
            Log.w(TAG, "Cannot start Wi-Fi scan: Wi-Fi is disabled")
            return false
        }
        
        if (!isRegistered) {
            try {
                context.registerReceiver(
                    wifiScanReceiver,
                    IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
                )
                isRegistered = true
                Log.d(TAG, "Wi-Fi scan receiver registered")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to register Wi-Fi scan receiver", e)
                return false
            }
        }
        
        // Update scanning state
        _isScanning.value = true
        
        // Using deprecated API with suppression as there's no direct replacement
        val scanStarted = wifiManager.startScan()
        if (scanStarted) {
            Log.d(TAG, "Wi-Fi scan started successfully")
        } else {
            Log.w(TAG, "Failed to start Wi-Fi scan")
            _isScanning.value = false
        }
        return scanStarted
    }

    /**
     * Starts periodic Wi-Fi scanning at the specified interval.
     * 
     * @param intervalMs The interval between scans in milliseconds
     * @param filterSize The number of samples to use for signal smoothing
     * @return true if periodic scanning was started, false if permissions are missing
     */
    fun startPeriodicScanning(
        intervalMs: Long = DEFAULT_SCAN_INTERVAL_MS, 
        filterSize: Int = DEFAULT_FILTER_WINDOW_SIZE
    ): Boolean {
        if (!hasPermissions()) {
            Log.w(TAG, "Cannot start periodic Wi-Fi scanning: missing required permissions")
            return false
        }
        
        scanIntervalMs = intervalMs
        filterWindowSize = filterSize
        isPeriodicScanningEnabled = true
        
        if (scanJob?.isActive != true) {
            scanJob = coroutineScope.launch {
                while (isPeriodicScanningEnabled) {
                    startScan()
                    delay(scanIntervalMs)
                }
            }
        }
        
        return true
    }

    /**
     * Stops all scanning operations and releases resources.
     */
    fun stopScan() {
        isPeriodicScanningEnabled = false
        scanJob?.cancel()
        scanJob = null
        
        if (isRegistered) {
            try {
                context.unregisterReceiver(wifiScanReceiver)
                isRegistered = false
            } catch (e: IllegalArgumentException) {
                Log.e(TAG, "Receiver not registered", e)
            }
        }
    }

    /**
     * Processes Wi-Fi scan results, applies filtering, and updates the state flow.
     * Includes comprehensive error handling to ensure robustness.
     */
    private fun processWifiScanResults() {
        try {
            if (!hasPermissions()) {
                Log.w(TAG, "Cannot process Wi-Fi scan results: missing required permissions")
                return
            }
            
            val currentTime = System.currentTimeMillis()
            val nativeScanResults = getScanResults()
            
            Log.d(TAG, "Processing ${nativeScanResults.size} Wi-Fi scan results")
            
            if (nativeScanResults.isEmpty()) {
                Log.d(TAG, "No scan results to process")
                return
            }
            
            // Process new scan results
            try {
                for (nativeScanResult in nativeScanResults) {
                    val bssid = nativeScanResult.BSSID ?: continue // Skip if BSSID is null
                    val rssi = nativeScanResult.level
                    // Use getWifiSsid() for API 33+ and fall back to SSID for older versions
                    val ssid = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        @Suppress("NewApi")
                        nativeScanResult.getWifiSsid()?.toString()?.ifEmpty { "<hidden>" } ?: "<unknown>"
                    } else {
                        @Suppress("DEPRECATION")
                        nativeScanResult.SSID?.ifEmpty { "<hidden>" } ?: "<unknown>"
                    }
                    
                    Log.v(TAG, "Processing AP: $bssid ($ssid) RSSI: $rssi")
                    
                    // Add to raw results
                    var readings = rawScanResults[bssid]
                    if (readings == null) {
                        readings = ArrayList()
                        rawScanResults[bssid] = readings
                    }
                    readings.add(TimestampedRssi(rssi, currentTime))
                    
                    // Trim old readings
                    try {
                        val iterator = readings.iterator()
                        while (iterator.hasNext()) {
                            val reading = iterator.next()
                            if (reading.timestamp < currentTime - STALE_THRESHOLD_MS) {
                                iterator.remove()
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error trimming old readings for $bssid", e)
                        // Continue processing other access points
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing scan results", e)
                // Continue with existing data
            }
            
            // Remove stale access points (compatible with older API levels)
            try {
                val staleKeys = ArrayList<String>()
                for (entry in rawScanResults.entries) {
                    try {
                        val hasRecentReadings = entry.value.any { 
                            it.timestamp > currentTime - STALE_THRESHOLD_MS 
                        }
                        if (!hasRecentReadings) {
                            staleKeys.add(entry.key)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error checking staleness for ${entry.key}", e)
                        // Add to stale keys to be safe
                        staleKeys.add(entry.key)
                    }
                }
                for (key in staleKeys) {
                    rawScanResults.remove(key)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error removing stale access points", e)
                // Continue with existing data
            }
            
            // Apply filtering and create final results
            val filteredResults = ArrayList<ScanResult>()
            try {
                for (entry in rawScanResults.entries) {
                    try {
                        val bssid = entry.key
                        val readings = entry.value
                        
                        if (readings.isEmpty()) {
                            continue // Skip empty readings
                        }
                        
                        // Take the most recent readings up to the window size
                        val recentReadings = readings.sortedByDescending { it.timestamp }
                            .take(filterWindowSize)
                        
                        // Calculate average RSSI
                        val avgRssi = if (recentReadings.isNotEmpty()) {
                            recentReadings.sumOf { it.rssi } / recentReadings.size
                        } else {
                            0
                        }
                        
                        // Get the most recent timestamp
                        val latestTimestamp = recentReadings.maxOfOrNull { it.timestamp } ?: currentTime
                        
                        // Find additional metadata from native scan results
                        var frequency = 0
                        var ssid = "<unknown>"
                        
                        for (nativeScanResult in nativeScanResults) {
                            if (nativeScanResult.BSSID == bssid) {
                                frequency = nativeScanResult.frequency
                                // Use getWifiSsid() for API 33+ and fall back to SSID for older versions
                                ssid = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    @Suppress("NewApi")
                                    nativeScanResult.getWifiSsid()?.toString()?.ifEmpty { "<hidden>" } ?: "<unknown>"
                                } else {
                                    @Suppress("DEPRECATION")
                                    nativeScanResult.SSID?.ifEmpty { "<hidden>" } ?: "<unknown>"
                                }
                                break
                            }
                        }
                        
                        filteredResults.add(
                            ScanResult(
                                bssid = bssid,
                                rssi = avgRssi,
                                timestamp = latestTimestamp,
                                frequency = frequency,
                                ssid = ssid
                            )
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing access point ${entry.key}", e)
                        // Continue with other access points
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error creating filtered results", e)
                // Use empty results if we can't process
            }
            
            // Update the state flows with the results we have
            try {
                _scanResults.value = filteredResults
                Log.d(TAG, "Updated scan results: ${filteredResults.size} access points")
                // Map to domain model with distance estimation for consumers
                _accessPoints.value = filteredResults.map { sr ->
                    com.example.myapplication.domain.model.WifiAccessPoint(
                        bssid = sr.bssid,
                        ssid = sr.ssid,
                        rssi = sr.rssi,
                        frequency = sr.frequency,
                        timestamp = sr.timestamp,
                        capabilities = "",
                        distance = estimateDistance(sr.rssi),
                        distanceConfidence = calculateDistanceConfidence(sr.rssi)
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating scan results state flow", e)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Critical error in processWifiScanResults", e)
            // Ensure we don't crash the app
        }
    }
    
    /**
     * Gets the scan results with permission check and error handling.
     * 
     * @return List of scan results or empty list if permissions are not granted or an error occurs
     */
    @Suppress("MissingPermission")
    private fun getScanResults(): List<NativeScanResult> {
        if (!hasPermissions()) {
            Log.w(TAG, "Cannot get scan results: missing required permissions")
            return emptyList()
        }
        
        return try {
            // We've already checked permissions with hasPermissions()
            val results = wifiManager.scanResults
            
            if (results == null) {
                Log.e(TAG, "Scan results are null")
                emptyList()
            } else {
                Log.d(TAG, "Retrieved ${results.size} scan results from WifiManager")
                results
            }
        } catch (e: SecurityException) {
            // This can happen if permissions were revoked after our check
            Log.e(TAG, "Security exception when getting scan results", e)
            emptyList()
        } catch (e: NullPointerException) {
            // This can happen if the WifiManager is not available
            Log.e(TAG, "WifiManager is not available", e)
            emptyList()
        } catch (e: Exception) {
            // Catch any other unexpected exceptions
            Log.e(TAG, "Unexpected error when getting scan results", e)
            emptyList()
        }
    }
    
    /**
     * Clears all stored scan results.
     */
    fun clearScanResults() {
        rawScanResults.clear()
        _scanResults.value = emptyList()
    }
    
    /**
     * Returns the list of required permissions for Wi-Fi scanning.
     */
    fun getRequiredPermissions(): Array<String> {
        return REQUIRED_PERMISSIONS
    }
    
    /**
     * Performs a Wi-Fi scan and returns a list of WifiAccessPoint objects.
     * This is a convenience method for use with the environment classifier.
     * 
     * @return List of WifiAccessPoint objects representing detected access points
     */
    suspend fun scanWifiAccessPoints(): List<com.example.myapplication.domain.model.WifiAccessPoint> {
        // Start a scan if not already scanning periodically
        if (!isPeriodicScanningEnabled) {
            startScan()
            // Wait for scan to complete (typical scan takes 2-4 seconds)
            kotlinx.coroutines.delay(4000)
        }
        
        // Convert scan results to WifiAccessPoint objects
        return scanResults.value.map { scanResult ->
            com.example.myapplication.domain.model.WifiAccessPoint(
                bssid = scanResult.bssid,
                ssid = scanResult.ssid,
                rssi = scanResult.rssi,
                frequency = scanResult.frequency,
                timestamp = scanResult.timestamp,
                capabilities = "", // Not available in our ScanResult
                distance = estimateDistance(scanResult.rssi),
                distanceConfidence = calculateDistanceConfidence(scanResult.rssi)
            )
        }
    }
    
    /**
     * Estimates the distance to an access point based on RSSI.
     * Uses the log-distance path loss model: d = 10^((TxPower - RSSI)/(10 * n))
     * 
     * @param rssi The received signal strength in dBm
     * @return Estimated distance in meters
     */
    private fun estimateDistance(rssi: Int): Float {
        // Typical values: txPower = -40 dBm (signal strength at 1m), n = 2.7 (path loss exponent)
        val txPower = -40
        val pathLossExponent = 2.7
        
        return Math.pow(10.0, (txPower - rssi) / (10.0 * pathLossExponent)).toFloat()
    }
    
    /**
     * Calculates the confidence level for distance estimation based on RSSI.
     * Stronger signals provide more reliable distance estimates.
     * 
     * @param rssi The received signal strength in dBm
     * @return Confidence value between 0.0 and 1.0
     */
    private fun calculateDistanceConfidence(rssi: Int): Float {
        // RSSI values typically range from -30 (very close) to -90 (very far)
        return when {
            rssi > -50 -> 0.9f  // Very strong signal, high confidence
            rssi > -60 -> 0.8f  // Strong signal
            rssi > -70 -> 0.6f  // Moderate signal
            rssi > -80 -> 0.4f  // Weak signal
            rssi > -90 -> 0.2f  // Very weak signal
            else -> 0.1f        // Extremely weak signal, low confidence
        }
    }
}

/**
 * Represents a Wi-Fi access point scan result with signal strength and metadata.
 */
data class ScanResult(
    val bssid: String,
    val rssi: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val frequency: Int = 0,
    val ssid: String = "<unknown>"
)

/**
 * Internal data class for storing RSSI values with timestamps.
 */
private data class TimestampedRssi(val rssi: Int, val timestamp: Long)
