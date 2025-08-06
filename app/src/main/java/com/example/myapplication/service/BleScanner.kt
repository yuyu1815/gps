package com.example.myapplication.service

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.example.myapplication.data.repository.IBeaconRepository
import com.example.myapplication.domain.usecase.ble.UpdateBeaconRssiUseCase
import com.example.myapplication.domain.usecase.ble.UpdateBeaconStalenessUseCase
import com.example.myapplication.presentation.viewmodel.PerformanceMetricsViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import java.io.File
import java.util.UUID
import kotlin.math.abs

/**
 * Enum representing different activity levels for scan optimization
 */
enum class ActivityLevel {
    LOW,      // Very little movement, likely stationary
    NORMAL,   // Regular movement patterns
    HIGH      // Frequent movement, requires more accurate positioning
}

/**
 * Lifecycle-aware BLE scanner component.
 * Handles BLE scanning operations and automatically manages the scanning state
 * based on the lifecycle of the associated component.
 */
class BleScanner(
    private val context: Context,
    private val beaconRepository: IBeaconRepository,
    private val updateBeaconRssiUseCase: UpdateBeaconRssiUseCase,
    private val updateBeaconStalenessUseCase: UpdateBeaconStalenessUseCase,
    private val beaconDiscovery: BeaconDiscovery
) : DefaultLifecycleObserver, SensorEventListener, KoinComponent {
    
    // Inject PerformanceMetricsViewModel for metrics collection
    private val performanceMetricsViewModel: PerformanceMetricsViewModel by inject()
    
    // BLE logger for CSV logging
    private val bleLogger = BleLogger(context)
    
    private val bluetoothManager: BluetoothManager by lazy {
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    }
    
    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        bluetoothManager.adapter
    }
    
    private val sensorManager: SensorManager by lazy {
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }
    
    private var accelerometer: Sensor? = null
    
    private var bluetoothLeScanner: BluetoothLeScanner? = null
    private val handler = Handler(Looper.getMainLooper())
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    
    // Scanning parameters
    private var scanPeriodMs: Long = 10000
    private var scanIntervalMs: Long = 5000
    private var stalenessTimeoutMs: Long = 5000
    
    // Battery saving parameters
    private var lowBatteryScanMode = ScanSettings.SCAN_MODE_LOW_POWER
    private var normalScanMode = ScanSettings.SCAN_MODE_BALANCED
    private var highPrecisionScanMode = ScanSettings.SCAN_MODE_LOW_LATENCY
    private var currentScanMode = normalScanMode
    
    // Movement detection parameters
    private var isMoving = false
    private var lastX = 0f
    private var lastY = 0f
    private var lastZ = 0f
    private var movementThreshold = 1.0f
    private var lastMovementTime = 0L
    private var movementCheckIntervalMs = 1000L
    private var stationaryDuration = 0L
    private var longStationaryThresholdMs = 60000L // 1 minute of no movement
    private var isLongStationary = false
    
    // Activity pattern detection
    private var activityLevel = ActivityLevel.NORMAL
    private var movementSamples = mutableListOf<Boolean>()
    private val maxMovementSamples = 10
    
    // Battery level monitoring
    private var batteryLevel = 100
    private var lowBatteryThreshold = 20
    private var criticalBatteryThreshold = 10
    
    /**
     * Helper function to get the name of a scan mode for logging
     */
    private fun getScanModeName(scanMode: Int): String {
        return when (scanMode) {
            ScanSettings.SCAN_MODE_LOW_POWER -> "SCAN_MODE_LOW_POWER"
            ScanSettings.SCAN_MODE_BALANCED -> "SCAN_MODE_BALANCED"
            ScanSettings.SCAN_MODE_LOW_LATENCY -> "SCAN_MODE_LOW_LATENCY"
            else -> "UNKNOWN_SCAN_MODE"
        }
    }
    
    // Scanning state
    private var isScanning = false
    private var isPaused = false
    
    // Scan callback
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            val rssi = result.rssi
            
            Timber.d("BLE scan result: ${device.address}, RSSI: $rssi dBm")
            
            // Process beacon using BeaconDiscovery
            beaconDiscovery.processBeacon(result)
            
            // Log scan result to CSV file
            bleLogger.logScanResult(result)
            
            // Record metrics for single scan result
            CoroutineScope(Dispatchers.IO).launch {
                val activeBeaconCount = beaconRepository.getBeacons().size
                performanceMetricsViewModel.recordBleScan(activeBeaconCount)
            }
        }
        
        override fun onBatchScanResults(results: List<ScanResult>) {
            Timber.d("BLE batch scan results: ${results.size} results")
            
            // Process each beacon using BeaconDiscovery
            results.forEach { result ->
                beaconDiscovery.processBeacon(result)
            }
            
            // Log batch scan results to CSV file
            bleLogger.logBatchScanResults(results)
            
            // Record metrics for batch scan results
            CoroutineScope(Dispatchers.IO).launch {
                val activeBeaconCount = beaconRepository.getBeacons().size
                performanceMetricsViewModel.recordBleScan(activeBeaconCount)
            }
        }
        
        override fun onScanFailed(errorCode: Int) {
            Timber.e("BLE scan failed with error code: $errorCode")
            stopScan()
        }
    }
    
    // Lifecycle callbacks
    override fun onResume(owner: LifecycleOwner) {
        Timber.d("BleScanner: onResume")
        isPaused = false
        registerSensors()
        updateBatteryLevel()
        if (isScanning) {
            startScanCycle()
        }
    }
    
    override fun onPause(owner: LifecycleOwner) {
        Timber.d("BleScanner: onPause")
        isPaused = true
        unregisterSensors()
        stopScan()
    }
    
    /**
     * Registers sensor listeners for movement detection.
     */
    private fun registerSensors() {
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (accelerometer != null) {
            sensorManager.registerListener(
                this,
                accelerometer,
                SensorManager.SENSOR_DELAY_NORMAL
            )
            Timber.d("Accelerometer sensor registered")
        } else {
            Timber.w("No accelerometer sensor available")
        }
    }
    
    /**
     * Unregisters sensor listeners.
     */
    private fun unregisterSensors() {
        if (accelerometer != null) {
            sensorManager.unregisterListener(this)
            Timber.d("Sensors unregistered")
        }
    }
    
    /**
     * Updates the current battery level.
     */
    private fun updateBatteryLevel() {
        val batteryIntent = context.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )
        
        batteryIntent?.let {
            val level = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = it.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            
            if (level != -1 && scale != -1) {
                batteryLevel = (level * 100 / scale.toFloat()).toInt()
                Timber.d("Battery level: $batteryLevel%")
                
                // Adjust scan parameters based on battery level
                adjustScanParameters()
            }
        }
    }
    
    /**
     * Adjusts scan parameters based on movement patterns, activity level, and battery level.
     * Implements dynamic scan intervals to optimize battery usage while maintaining
     * positioning accuracy appropriate for the current user activity.
     */
    private fun adjustScanParameters() {
        // Update activity level based on movement patterns
        updateActivityLevel()
        
        // Determine optimal scan mode based on battery level, movement, and activity level
        currentScanMode = when {
            // Critical battery - use ultra low power mode regardless of movement
            batteryLevel <= criticalBatteryThreshold -> {
                Timber.d("Critical battery mode: using ultra low power scanning")
                scanPeriodMs = 3000
                scanIntervalMs = 20000 // Very infrequent scanning
                lowBatteryScanMode
            }
            // Low battery - use low power mode with adaptive intervals
            batteryLevel <= lowBatteryThreshold -> {
                Timber.d("Low battery mode: using low power scanning")
                scanPeriodMs = 4000
                scanIntervalMs = 12000
                lowBatteryScanMode
            }
            // Long stationary period - use very low power scanning
            isLongStationary -> {
                Timber.d("Long stationary period: using very low power scanning")
                scanPeriodMs = 3000
                scanIntervalMs = 15000 // Very infrequent scanning when stationary for long periods
                lowBatteryScanMode
            }
            // Activity level based scanning
            else -> when (activityLevel) {
                ActivityLevel.HIGH -> {
                    Timber.d("High activity: using high precision scanning")
                    scanPeriodMs = 6000
                    scanIntervalMs = 2000 // More frequent scanning for high activity
                    highPrecisionScanMode
                }
                ActivityLevel.NORMAL -> {
                    Timber.d("Normal activity: using balanced scanning")
                    scanPeriodMs = 5000
                    scanIntervalMs = 5000
                    normalScanMode
                }
                ActivityLevel.LOW -> {
                    Timber.d("Low activity: using low power scanning")
                    scanPeriodMs = 4000
                    scanIntervalMs = 10000
                    lowBatteryScanMode
                }
            }
        }
        
        Timber.d("Adjusted scan parameters: mode=${getScanModeName(currentScanMode)}, " +
                "period=${scanPeriodMs}ms, interval=${scanIntervalMs}ms, " +
                "activity=$activityLevel, battery=$batteryLevel%, " +
                "longStationary=$isLongStationary")
    }
    
    /**
     * Updates the activity level based on recent movement patterns.
     * This helps optimize scanning based on user behavior.
     */
    private fun updateActivityLevel() {
        // Keep only the most recent samples
        while (movementSamples.size > maxMovementSamples) {
            movementSamples.removeAt(0)
        }
        
        // Calculate the percentage of movement samples
        val movementRatio = if (movementSamples.isNotEmpty()) {
            movementSamples.count { it } / movementSamples.size.toFloat()
        } else {
            0.5f // Default to medium activity if no samples
        }
        
        // Determine activity level based on movement ratio
        activityLevel = when {
            movementRatio > 0.7f -> ActivityLevel.HIGH   // Mostly moving
            movementRatio > 0.3f -> ActivityLevel.NORMAL // Mixed movement
            else -> ActivityLevel.LOW                    // Mostly stationary
        }
    }
    
    override fun onDestroy(owner: LifecycleOwner) {
        Timber.d("BleScanner: onDestroy")
        stopScan()
        handler.removeCallbacksAndMessages(null)
        unregisterSensors()
    }
    
    /**
     * Handles sensor data changes for movement detection and activity tracking.
     * Tracks movement patterns, stationary periods, and updates scan parameters accordingly.
     */
    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            val currentTime = System.currentTimeMillis()
            
            // Only check for movement periodically to save battery
            if (currentTime - lastMovementTime > movementCheckIntervalMs) {
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]
                
                // Calculate movement based on acceleration changes
                val deltaX = abs(lastX - x)
                val deltaY = abs(lastY - y)
                val deltaZ = abs(lastZ - z)
                
                // Check if movement exceeds threshold
                val isCurrentlyMoving = (deltaX > movementThreshold || 
                                        deltaY > movementThreshold || 
                                        deltaZ > movementThreshold)
                
                // Add to movement samples for activity level calculation
                movementSamples.add(isCurrentlyMoving)
                
                // Update stationary duration tracking
                if (!isCurrentlyMoving) {
                    if (isMoving) {
                        // Just became stationary, start tracking duration
                        stationaryDuration = 0
                    } else {
                        // Continue tracking stationary duration
                        stationaryDuration += (currentTime - lastMovementTime)
                        
                        // Check if we've been stationary for a long time
                        val wasLongStationary = isLongStationary
                        isLongStationary = stationaryDuration > longStationaryThresholdMs
                        
                        // Log when long stationary state changes
                        if (wasLongStationary != isLongStationary) {
                            Timber.d("Long stationary state changed: $isLongStationary (duration: ${stationaryDuration/1000}s)")
                        }
                    }
                } else {
                    // Reset stationary duration when moving
                    stationaryDuration = 0
                    isLongStationary = false
                }
                
                // Update movement state if changed
                if (isCurrentlyMoving != isMoving) {
                    isMoving = isCurrentlyMoving
                    Timber.d("Movement state changed: ${if (isMoving) "moving" else "stationary"}")
                    
                    // Adjust scan parameters based on movement
                    adjustScanParameters()
                } else if (isLongStationary) {
                    // Even if movement state hasn't changed, we might need to adjust parameters
                    // if we've entered a long stationary period
                    adjustScanParameters()
                }
                
                // Update last values
                lastX = x
                lastY = y
                lastZ = z
                lastMovementTime = currentTime
            }
        }
    }
    
    /**
     * Handles sensor accuracy changes.
     */
    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        // Not used in this implementation
    }
    
    /**
     * Starts BLE scanning.
     */
    fun startScan() {
        if (isScanning) {
            Timber.d("BLE scanning already in progress")
            return
        }
        
        isScanning = true
        if (!isPaused) {
            startScanCycle()
        }
    }
    
    /**
     * Stops BLE scanning.
     */
    fun stopScan() {
        if (!isScanning) {
            return
        }
        
        isScanning = false
        stopScanCycle()
    }
    
    /**
     * Sets the scan period (how long each scan runs).
     */
    fun setScanPeriod(periodMs: Long) {
        scanPeriodMs = periodMs
    }
    
    /**
     * Sets the scan interval (time between scan cycles).
     */
    fun setScanInterval(intervalMs: Long) {
        scanIntervalMs = intervalMs
    }
    
    /**
     * Sets the staleness timeout for beacons.
     */
    fun setStalenessTimeout(timeoutMs: Long) {
        stalenessTimeoutMs = timeoutMs
    }
    
    /**
     * Starts a scan cycle (scan for a period, then pause, then repeat).
     */
    private fun startScanCycle() {
        if (bluetoothAdapter == null || !bluetoothAdapter!!.isEnabled) {
            Timber.e("Bluetooth is not enabled")
            return
        }
        
        bluetoothLeScanner = bluetoothAdapter!!.bluetoothLeScanner
        if (bluetoothLeScanner == null) {
            Timber.e("BluetoothLeScanner is null")
            return
        }
        
        Timber.d("Starting BLE scan cycle")
        
        // Update battery level before starting scan
        updateBatteryLevel()
        
        // Create scan settings with dynamic scan mode based on movement and battery
        val settings = ScanSettings.Builder()
            .setScanMode(currentScanMode)
            .build()
            
        Timber.d("Starting scan with mode: ${getScanModeName(currentScanMode)}")
        
        // Start scanning
        try {
            // Check for required permissions
            if (!hasRequiredPermissions()) {
                Timber.e("Missing required Bluetooth permissions")
                return
            }
            
            bluetoothLeScanner?.startScan(null, settings, scanCallback)
            
            // Schedule scan stop after scan period
            handler.postDelayed({
                stopScanCycle()
                
                // Schedule next scan cycle after interval
                if (isScanning && !isPaused) {
                    handler.postDelayed({ startScanCycle() }, scanIntervalMs)
                }
            }, scanPeriodMs)
            
            // Update beacon staleness using both the use case and the enhanced discovery functionality
            coroutineScope.launch {
                // Use the existing use case for backward compatibility
                val staleCount = updateBeaconStalenessUseCase(
                    UpdateBeaconStalenessUseCase.Params(stalenessTimeoutMs)
                )
                
                // Use the enhanced staleness management in BeaconDiscovery
                beaconDiscovery.setStalenessTimeout(stalenessTimeoutMs)
                beaconDiscovery.checkBeaconStaleness()
                
                Timber.d("Updated beacon staleness: $staleCount beacons marked as stale")
            }
        } catch (e: SecurityException) {
            Timber.e("Bluetooth permission denied: ${e.message}")
        } catch (e: Exception) {
            Timber.e("Failed to start BLE scan: ${e.message}")
        }
    }
    
    /**
     * Stops the current scan cycle.
     */
    private fun stopScanCycle() {
        Timber.d("Stopping BLE scan cycle")
        try {
            // Check for required permissions
            if (!hasRequiredPermissions()) {
                Timber.e("Missing required Bluetooth permissions")
                return
            }
            
            bluetoothLeScanner?.stopScan(scanCallback)
        } catch (e: SecurityException) {
            Timber.e("Bluetooth permission denied: ${e.message}")
        } catch (e: Exception) {
            Timber.e("Failed to stop BLE scan: ${e.message}")
        }
    }
    
    /**
     * Checks if the app has the required Bluetooth permissions.
     */
    private fun hasRequiredPermissions(): Boolean {
        // For Android 12+ (API 31+), we need BLUETOOTH_SCAN permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_SCAN
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return false
            }
        }
        // For older versions, we need BLUETOOTH and BLUETOOTH_ADMIN permissions
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH
                ) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_ADMIN
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return false
            }
        }
        
        // We also need location permission for BLE scanning
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
        
        return true
    }
    
    /**
     * Starts logging BLE scan results to CSV files.
     * 
     * @return True if logging started successfully, false otherwise
     */
    fun startLogging(): Boolean {
        return bleLogger.startLogging()
    }
    
    /**
     * Stops logging BLE scan results.
     */
    fun stopLogging() {
        bleLogger.stopLogging()
    }
    
    /**
     * Checks if BLE scan logging is in progress.
     * 
     * @return True if logging is in progress, false otherwise
     */
    fun isLoggingInProgress(): Boolean {
        return bleLogger.isLoggingInProgress()
    }
    
    /**
     * Gets a list of all BLE scan log files.
     * 
     * @return List of log files
     */
    fun getLogFiles(): List<File> {
        return bleLogger.getLogFiles()
    }
    
    /**
     * Deletes a BLE scan log file.
     * 
     * @param file The file to delete
     * @return True if the file was deleted successfully, false otherwise
     */
    fun deleteLogFile(file: File): Boolean {
        return bleLogger.deleteLogFile(file)
    }
    
    /**
     * Deletes all BLE scan log files.
     * 
     * @return The number of files deleted
     */
    fun deleteAllLogFiles(): Int {
        return bleLogger.deleteAllLogFiles()
    }
}