package com.example.myapplication.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.example.myapplication.MainActivity
import com.example.myapplication.data.repository.IBeaconRepository
import com.example.myapplication.domain.usecase.ble.UpdateBeaconRssiUseCase
import com.example.myapplication.domain.usecase.ble.UpdateBeaconStalenessUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import timber.log.Timber
import java.io.File
import com.example.myapplication.service.PerformanceMetricsCollector

/**
 * Foreground service for continuous BLE scanning in the background.
 * This service allows the app to continue scanning for beacons even when
 * the app is not in the foreground.
 * Implements StaticStateListener to adjust scanning frequency based on device movement.
 */
class BleScanService : Service(), StaticStateListener {

    // Inject dependencies using Koin
    private val beaconRepository: IBeaconRepository by inject()
    private val updateBeaconRssiUseCase: UpdateBeaconRssiUseCase by inject()
    private val updateBeaconStalenessUseCase: UpdateBeaconStalenessUseCase by inject()
    private val beaconDiscovery: BeaconDiscovery by inject()
    private val performanceMetricsCollector: PerformanceMetricsCollector by inject()
    private val sensorMonitor: SensorMonitor by inject()
    
    // Battery optimizer for dynamic scanning intervals
    private lateinit var batteryOptimizer: BatteryOptimizer

    // BLE scanner instance
    private lateinit var bleScanner: BleScanner

    // Coroutine scope for background operations
    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())

    // Wake lock to keep CPU running during scanning
    private var wakeLock: PowerManager.WakeLock? = null

    // Binder for client communication
    private val binder = LocalBinder()

    // Service state
    private var isRunning = false
    
    // Static state tracking
    private var isDeviceStatic = false
    private var isLongStatic = false

    // Notification constants
    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "ble_scan_channel"
        private const val CHANNEL_NAME = "BLE Scanning"
        private const val WAKE_LOCK_TAG = "BleScanService:WakeLock"

        // Default scan settings
        private const val DEFAULT_SCAN_PERIOD_MS = 4000L
        private const val DEFAULT_SCAN_INTERVAL_MS = 5000L
        private const val DEFAULT_STALENESS_TIMEOUT_MS = 5000L
        
        // Static state scan settings
        private const val STATIC_SCAN_PERIOD_MS = 3000L
        private const val STATIC_SCAN_INTERVAL_MS = 10000L  // Less frequent when static
        private const val LONG_STATIC_SCAN_PERIOD_MS = 2000L
        private const val LONG_STATIC_SCAN_INTERVAL_MS = 20000L  // Very infrequent when long static
    }
    
    /**
     * Implements StaticStateListener interface.
     * Adjusts BLE scanning frequency based on device movement state.
     */
    override fun onStaticStateChanged(isStatic: Boolean, isLongStatic: Boolean, confidence: Float, durationMs: Long) {
        Timber.d("Static state changed in BleScanService: static=$isStatic, longStatic=$isLongStatic, " +
                "confidence=$confidence, duration=${durationMs/1000}s")
        
        // Update state
        this.isDeviceStatic = isStatic
        this.isLongStatic = isLongStatic
        
        // Record app usage for usage pattern optimization
        batteryOptimizer.recordAppUsage()
        
        // Adjust scan settings based on battery state and static state
        updateScanSettingsBasedOnBatteryState()
    }
    
    /**
     * Updates scan settings based on battery state, device movement, and usage patterns.
     * This optimizes battery usage while maintaining appropriate positioning accuracy.
     */
    private fun updateScanSettingsBasedOnBatteryState() {
        if (!isRunning) return
        
        // Get current activity level from BleScanner if available
        val activityLevel = try {
            val field = BleScanner::class.java.getDeclaredField("activityLevel")
            field.isAccessible = true
            field.get(bleScanner) as ActivityLevel
        } catch (e: Exception) {
            ActivityLevel.NORMAL // Default if we can't access the field
        }
        
        // Get scan configuration from battery optimizer
        val scanConfig = batteryOptimizer.getScanConfig(
            isStatic = isDeviceStatic,
            isLongStatic = isLongStatic,
            activityLevel = activityLevel,
            defaultScanMode = ScanSettings.SCAN_MODE_BALANCED,
            lowPowerScanMode = ScanSettings.SCAN_MODE_LOW_POWER,
            highPrecisionScanMode = ScanSettings.SCAN_MODE_LOW_LATENCY
        )
        
        // Update scan settings
        updateScanSettings(
            scanPeriodMs = scanConfig.scanPeriodMs,
            scanIntervalMs = scanConfig.scanIntervalMs
        )
        
        // Set scan mode using reflection since it's private in BleScanner
        try {
            val field = BleScanner::class.java.getDeclaredField("currentScanMode")
            field.isAccessible = true
            field.set(bleScanner, scanConfig.scanMode)
        } catch (e: Exception) {
            Timber.e("Failed to set scan mode: ${e.message}")
        }
        
        Timber.i("Adjusted BLE scan settings based on battery optimization: " +
                "${scanConfig.description}, period=${scanConfig.scanPeriodMs}ms, " +
                "interval=${scanConfig.scanIntervalMs}ms, battery=${batteryOptimizer.getBatteryLevel()}%, " +
                "charging=${batteryOptimizer.isCharging()}")
    }

    /**
     * Binder class for client communication
     */
    inner class LocalBinder : Binder() {
        fun getService(): BleScanService = this@BleScanService
    }

    override fun onCreate() {
        super.onCreate()
        Timber.d("BleScanService created")

        // Initialize BLE scanner
        bleScanner = BleScanner(
            context = applicationContext,
            beaconRepository = beaconRepository,
            updateBeaconRssiUseCase = updateBeaconRssiUseCase,
            updateBeaconStalenessUseCase = updateBeaconStalenessUseCase,
            beaconDiscovery = beaconDiscovery
        )

        // Initialize battery optimizer
        batteryOptimizer = BatteryOptimizer(applicationContext)
        batteryOptimizer.updateBatteryState()
        
        // Configure scanner with default settings
        bleScanner.setScanPeriod(DEFAULT_SCAN_PERIOD_MS)
        bleScanner.setScanInterval(DEFAULT_SCAN_INTERVAL_MS)
        bleScanner.setStalenessTimeout(DEFAULT_STALENESS_TIMEOUT_MS)
        
        // Register as a static state listener to optimize scanning frequency
        sensorMonitor.addStaticStateListener(this)
        Timber.d("Registered as static state listener")
        
        // Record app usage for usage pattern optimization
        batteryOptimizer.recordAppUsage()
        
        // Schedule periodic battery state updates
        serviceScope.launch {
            while (isRunning) {
                // Update battery state every minute
                if (batteryOptimizer.updateBatteryState()) {
                    // If battery state changed, adjust scan settings
                    updateScanSettingsBasedOnBatteryState()
                }
                delay(60000) // 1 minute
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.d("BleScanService started")

        // Create notification channel for Android O and above
        createNotificationChannel()

        // Start as a foreground service with notification
        startForeground(NOTIFICATION_ID, createNotification())

        // Acquire wake lock to keep CPU running
        acquireWakeLock()

        // Start BLE scanning
        startScanning()

        // Return sticky so service restarts if killed
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.d("BleScanService destroyed")

        // Stop scanning
        stopScanning()

        // Release wake lock
        releaseWakeLock()
        
        // Unregister as static state listener
        sensorMonitor.removeStaticStateListener(this)
        Timber.d("Unregistered as static state listener")

        // Cancel all coroutines
        serviceScope.cancel()
    }

    /**
     * Starts BLE scanning
     */
    fun startScanning() {
        if (!isRunning) {
            Timber.d("Starting BLE scanning")
            bleScanner.startScan()
            isRunning = true
        }
    }

    /**
     * Stops BLE scanning
     */
    fun stopScanning() {
        if (isRunning) {
            Timber.d("Stopping BLE scanning")
            bleScanner.stopScan()
            isRunning = false
        }
    }

    /**
     * Updates scan settings
     */
    fun updateScanSettings(
        scanPeriodMs: Long = DEFAULT_SCAN_PERIOD_MS,
        scanIntervalMs: Long = DEFAULT_SCAN_INTERVAL_MS,
        stalenessTimeoutMs: Long = DEFAULT_STALENESS_TIMEOUT_MS
    ) {
        bleScanner.setScanPeriod(scanPeriodMs)
        bleScanner.setScanInterval(scanIntervalMs)
        bleScanner.setStalenessTimeout(stalenessTimeoutMs)
        
        // Also update the beacon discovery staleness timeout
        beaconDiscovery.setStalenessTimeout(stalenessTimeoutMs)
        
        Timber.d("Updated scan settings: period=$scanPeriodMs, interval=$scanIntervalMs, timeout=$stalenessTimeoutMs")
    }
    
    /**
     * Sets the long stationary threshold - the time after which the device is considered
     * to be in a long stationary state, which triggers power-saving scan mode.
     * 
     * @param thresholdMs The threshold time in milliseconds
     */
    fun setLongStationaryThreshold(thresholdMs: Long) {
        if (::bleScanner.isInitialized) {
            serviceScope.launch {
                try {
                    // Use reflection to set the property since it's private
                    val field = BleScanner::class.java.getDeclaredField("longStationaryThresholdMs")
                    field.isAccessible = true
                    field.set(bleScanner, thresholdMs)
                    
                    Timber.d("Updated long stationary threshold: $thresholdMs ms")
                } catch (e: Exception) {
                    Timber.e("Failed to set long stationary threshold: ${e.message}")
                }
            }
        }
    }
    
    /**
     * Sets the movement threshold - the acceleration change required to consider
     * the device as moving.
     * 
     * @param threshold The movement threshold value
     */
    fun setMovementThreshold(threshold: Float) {
        if (::bleScanner.isInitialized) {
            serviceScope.launch {
                try {
                    // Use reflection to set the property since it's private
                    val field = BleScanner::class.java.getDeclaredField("movementThreshold")
                    field.isAccessible = true
                    field.set(bleScanner, threshold)
                    
                    Timber.d("Updated movement threshold: $threshold")
                } catch (e: Exception) {
                    Timber.e("Failed to set movement threshold: ${e.message}")
                }
            }
        }
    }
    
    /**
     * Sets the battery thresholds for scan optimization.
     * 
     * @param lowThreshold The threshold for low battery mode (percentage)
     * @param criticalThreshold The threshold for critical battery mode (percentage)
     */
    fun setBatteryThresholds(lowThreshold: Int, criticalThreshold: Int) {
        if (::bleScanner.isInitialized) {
            serviceScope.launch {
                try {
                    // Set low battery threshold
                    val lowField = BleScanner::class.java.getDeclaredField("lowBatteryThreshold")
                    lowField.isAccessible = true
                    lowField.set(bleScanner, lowThreshold)
                    
                    // Set critical battery threshold
                    val criticalField = BleScanner::class.java.getDeclaredField("criticalBatteryThreshold")
                    criticalField.isAccessible = true
                    criticalField.set(bleScanner, criticalThreshold)
                    
                    Timber.d("Updated battery thresholds: low=$lowThreshold%, critical=$criticalThreshold%")
                } catch (e: Exception) {
                    Timber.e("Failed to set battery thresholds: ${e.message}")
                }
            }
        }
    }

    /**
     * Creates the notification channel required for Android O and above
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Used for BLE scanning in the background"
                setShowBadge(false)
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Creates the notification for the foreground service
     */
    private fun createNotification(): Notification {
        // Create intent to open the app when notification is tapped
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Indoor Positioning Active")
            .setContentText("Scanning for BLE beacons")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    /**
     * Acquires a wake lock to keep the CPU running during scanning
     */
    private fun acquireWakeLock() {
        if (wakeLock == null) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                WAKE_LOCK_TAG
            )
            wakeLock?.acquire(10 * 60 * 1000L) // 10 minutes timeout
            Timber.d("Wake lock acquired")
        }
    }

    /**
     * Releases the wake lock
     */
    private fun releaseWakeLock() {
        if (wakeLock != null && wakeLock!!.isHeld) {
            wakeLock!!.release()
            wakeLock = null
            Timber.d("Wake lock released")
        }
    }
    
    /**
     * Starts logging BLE scan results to CSV files.
     * 
     * @return True if logging started successfully, false otherwise
     */
    fun startLogging(): Boolean {
        if (!::bleScanner.isInitialized) {
            Timber.e("BleScanner not initialized")
            return false
        }
        
        val result = bleScanner.startLogging()
        if (result) {
            Timber.i("BLE scan logging started")
        } else {
            Timber.e("Failed to start BLE scan logging")
        }
        
        return result
    }
    
    /**
     * Stops logging BLE scan results.
     */
    fun stopLogging() {
        if (!::bleScanner.isInitialized) {
            Timber.e("BleScanner not initialized")
            return
        }
        
        bleScanner.stopLogging()
        Timber.i("BLE scan logging stopped")
    }
    
    /**
     * Checks if BLE scan logging is in progress.
     * 
     * @return True if logging is in progress, false otherwise
     */
    fun isLoggingInProgress(): Boolean {
        if (!::bleScanner.isInitialized) {
            return false
        }
        
        return bleScanner.isLoggingInProgress()
    }
    
    /**
     * Gets a list of all BLE scan log files.
     * 
     * @return List of log files
     */
    fun getLogFiles(): List<File> {
        if (!::bleScanner.isInitialized) {
            return emptyList()
        }
        
        return bleScanner.getLogFiles()
    }
    
    /**
     * Deletes a BLE scan log file.
     * 
     * @param file The file to delete
     * @return True if the file was deleted successfully, false otherwise
     */
    fun deleteLogFile(file: File): Boolean {
        if (!::bleScanner.isInitialized) {
            return false
        }
        
        return bleScanner.deleteLogFile(file)
    }
    
    /**
     * Deletes all BLE scan log files.
     * 
     * @return The number of files deleted
     */
    fun deleteAllLogFiles(): Int {
        if (!::bleScanner.isInitialized) {
            return 0
        }
        
        return bleScanner.deleteAllLogFiles()
    }
}