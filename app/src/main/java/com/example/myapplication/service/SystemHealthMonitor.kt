package com.example.myapplication.service

import android.content.Context
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import com.example.myapplication.data.repository.IBeaconRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Service for monitoring the health of the system and application components.
 * Provides information about device resources, beacon health, and application performance.
 */
class SystemHealthMonitor(
    private val context: Context,
    private val beaconRepository: IBeaconRepository,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    /**
     * Data class representing the system health status.
     */
    data class SystemHealthStatus(
        val timestamp: Long = System.currentTimeMillis(),
        val batteryLevel: Int = -1,
        val isCharging: Boolean = false,
        val availableStorage: Long = -1,
        val totalStorage: Long = -1,
        val availableMemory: Long = -1,
        val totalMemory: Long = -1,
        val cpuUsage: Float = -1f,
        val beaconHealthSummary: BeaconHealthSummary = BeaconHealthSummary(),
        val appUptime: Long = -1,
        val lastScanTime: Long = -1,
        val scanSuccessRate: Float = -1f,
        val positionUpdateRate: Float = -1f
    )

    /**
     * Data class representing a summary of beacon health.
     */
    data class BeaconHealthSummary(
        val totalBeacons: Int = 0,
        val activeBeacons: Int = 0,
        val staleBeacons: Int = 0,
        val criticalBeacons: Int = 0,
        val warningBeacons: Int = 0,
        val averageSignalStrength: Int = 0,
        val averageBatteryLevel: Int = -1
    )

    private val _systemHealthStatus = MutableStateFlow(SystemHealthStatus())
    val systemHealthStatus: StateFlow<SystemHealthStatus> = _systemHealthStatus.asStateFlow()

    private val _healthLogs = MutableStateFlow<List<SystemHealthStatus>>(emptyList())
    val healthLogs: StateFlow<List<SystemHealthStatus>> = _healthLogs.asStateFlow()

    private val startTime = System.currentTimeMillis()
    private var lastScanTime = 0L
    private var scanCount = 0
    private var scanSuccessCount = 0
    private var positionUpdateCount = 0
    private var lastPositionUpdateTime = 0L

    init {
        // Initialize with current status
        refreshSystemHealthStatus()
    }

    /**
     * Refreshes the system health status.
     */
    fun refreshSystemHealthStatus() {
        coroutineScope.launch {
            try {
                val batteryStatus = getBatteryStatus()
                val storageInfo = getStorageInfo()
                val memoryInfo = getMemoryInfo()
                val cpuUsage = getCpuUsage()
                val beaconHealthSummary = getBeaconHealthSummary()
                val appUptime = System.currentTimeMillis() - startTime
                
                val scanSuccessRate = if (scanCount > 0) {
                    (scanSuccessCount.toFloat() / scanCount) * 100
                } else {
                    -1f
                }
                
                val positionUpdateRate = if (appUptime > 0) {
                    (positionUpdateCount.toFloat() / (appUptime / 1000f)) * 60 // Updates per minute
                } else {
                    -1f
                }
                
                val status = SystemHealthStatus(
                    timestamp = System.currentTimeMillis(),
                    batteryLevel = batteryStatus.first,
                    isCharging = batteryStatus.second,
                    availableStorage = storageInfo.first,
                    totalStorage = storageInfo.second,
                    availableMemory = memoryInfo.first,
                    totalMemory = memoryInfo.second,
                    cpuUsage = cpuUsage,
                    beaconHealthSummary = beaconHealthSummary,
                    appUptime = appUptime,
                    lastScanTime = lastScanTime,
                    scanSuccessRate = scanSuccessRate,
                    positionUpdateRate = positionUpdateRate
                )
                
                _systemHealthStatus.value = status
                
                // Add to logs
                val currentLogs = _healthLogs.value.toMutableList()
                currentLogs.add(status)
                
                // Keep only the last 100 logs
                if (currentLogs.size > 100) {
                    _healthLogs.value = currentLogs.takeLast(100)
                } else {
                    _healthLogs.value = currentLogs
                }
                
                Timber.d("System health status refreshed")
            } catch (e: Exception) {
                Timber.e(e, "Error refreshing system health status")
            }
        }
    }

    /**
     * Records a successful BLE scan.
     */
    fun recordSuccessfulScan() {
        lastScanTime = System.currentTimeMillis()
        scanCount++
        scanSuccessCount++
    }

    /**
     * Records a failed BLE scan.
     */
    fun recordFailedScan() {
        scanCount++
    }

    /**
     * Records a position update.
     */
    fun recordPositionUpdate() {
        lastPositionUpdateTime = System.currentTimeMillis()
        positionUpdateCount++
    }

    /**
     * Gets the battery status.
     * @return Pair of battery level (0-100) and charging status
     */
    private fun getBatteryStatus(): Pair<Int, Boolean> {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        
        val batteryLevel = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        val isCharging = batteryManager.isCharging
        
        return Pair(batteryLevel, isCharging)
    }

    /**
     * Gets the storage information.
     * @return Pair of available storage and total storage in bytes
     */
    private fun getStorageInfo(): Pair<Long, Long> {
        val stat = StatFs(Environment.getExternalStorageDirectory().path)
        
        val availableBytes = stat.availableBytes
        val totalBytes = stat.totalBytes
        
        return Pair(availableBytes, totalBytes)
    }

    /**
     * Gets the memory information.
     * @return Pair of available memory and total memory in bytes
     */
    private fun getMemoryInfo(): Pair<Long, Long> {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        val memoryInfo = android.app.ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        
        val availableMemory = memoryInfo.availMem
        val totalMemory = memoryInfo.totalMem
        
        return Pair(availableMemory, totalMemory)
    }

    /**
     * Gets the CPU usage.
     * This is a simplified implementation and may not be accurate.
     * @return CPU usage as a percentage (0-100)
     */
    private fun getCpuUsage(): Float {
        try {
            val process = Runtime.getRuntime().exec("top -n 1")
            process.waitFor()
            
            val reader = process.inputStream.bufferedReader()
            val output = reader.use { it.readText() }
            
            // Parse CPU usage from top output
            // This is a simplified approach and may not work on all devices
            val cpuLine = output.lines().firstOrNull { it.contains("CPU:") || it.contains("Cpu") }
            
            return if (cpuLine != null) {
                val regex = "(\\d+\\.?\\d*)%".toRegex()
                val matchResult = regex.find(cpuLine)
                
                matchResult?.groupValues?.get(1)?.toFloatOrNull() ?: -1f
            } else {
                -1f
            }
        } catch (e: Exception) {
            Timber.e(e, "Error getting CPU usage")
            return -1f
        }
    }

    /**
     * Gets a summary of beacon health.
     * @return BeaconHealthSummary
     */
    private suspend fun getBeaconHealthSummary(): BeaconHealthSummary {
        try {
            val beacons = beaconRepository.getAll()
            
            val totalBeacons = beacons.size
            val activeBeacons = beacons.count { !it.isStale }
            val staleBeacons = beacons.count { it.isStale }
            
            val criticalBeacons = beacons.count { it.healthStatus == com.example.myapplication.domain.model.BeaconHealthStatus.CRITICAL }
            val warningBeacons = beacons.count { it.healthStatus == com.example.myapplication.domain.model.BeaconHealthStatus.WARNING }
            
            val rssiSum = beacons.sumOf { it.filteredRssi.toDouble() }
            val averageSignalStrength = if (activeBeacons > 0) {
                (rssiSum / activeBeacons).roundToInt()
            } else {
                0
            }
            
            val beaconsWithBattery = beacons.filter { it.batteryLevel >= 0 }
            val batterySum = beaconsWithBattery.sumOf { it.batteryLevel.toDouble() }
            val averageBatteryLevel = if (beaconsWithBattery.isNotEmpty()) {
                (batterySum / beaconsWithBattery.size).roundToInt()
            } else {
                -1
            }
            
            return BeaconHealthSummary(
                totalBeacons = totalBeacons,
                activeBeacons = activeBeacons,
                staleBeacons = staleBeacons,
                criticalBeacons = criticalBeacons,
                warningBeacons = warningBeacons,
                averageSignalStrength = averageSignalStrength,
                averageBatteryLevel = averageBatteryLevel
            )
        } catch (e: Exception) {
            Timber.e(e, "Error getting beacon health summary")
            return BeaconHealthSummary()
        }
    }

    /**
     * Exports health logs to a CSV file.
     * @param file The file to export to
     * @return True if export was successful, false otherwise
     */
    fun exportHealthLogs(file: File): Boolean {
        return try {
            val logs = _healthLogs.value
            
            if (logs.isEmpty()) {
                Timber.w("No health logs to export")
                return false
            }
            
            val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            
            file.bufferedWriter().use { writer ->
                // Write header
                writer.write("Timestamp,Battery Level,Charging,Available Storage (MB),Total Storage (MB),Available Memory (MB),Total Memory (MB),CPU Usage (%),Total Beacons,Active Beacons,Stale Beacons,Critical Beacons,Warning Beacons,Average Signal Strength,Average Battery Level,App Uptime (min),Last Scan Time,Scan Success Rate (%),Position Update Rate (updates/min)")
                writer.newLine()
                
                // Write data
                logs.forEach { status ->
                    val timestamp = dateFormatter.format(Date(status.timestamp))
                    val availableStorageMB = status.availableStorage / (1024 * 1024)
                    val totalStorageMB = status.totalStorage / (1024 * 1024)
                    val availableMemoryMB = status.availableMemory / (1024 * 1024)
                    val totalMemoryMB = status.totalMemory / (1024 * 1024)
                    val uptimeMinutes = status.appUptime / (1000 * 60)
                    val lastScanTimeStr = if (status.lastScanTime > 0) dateFormatter.format(Date(status.lastScanTime)) else "N/A"
                    
                    writer.write("$timestamp,${status.batteryLevel},${status.isCharging},$availableStorageMB,$totalStorageMB,$availableMemoryMB,$totalMemoryMB,${status.cpuUsage},${status.beaconHealthSummary.totalBeacons},${status.beaconHealthSummary.activeBeacons},${status.beaconHealthSummary.staleBeacons},${status.beaconHealthSummary.criticalBeacons},${status.beaconHealthSummary.warningBeacons},${status.beaconHealthSummary.averageSignalStrength},${status.beaconHealthSummary.averageBatteryLevel},$uptimeMinutes,$lastScanTimeStr,${status.scanSuccessRate},${status.positionUpdateRate}")
                    writer.newLine()
                }
            }
            
            Timber.d("Health logs exported to ${file.absolutePath}")
            true
        } catch (e: Exception) {
            Timber.e(e, "Error exporting health logs")
            false
        }
    }

    /**
     * Clears all health logs.
     */
    fun clearHealthLogs() {
        _healthLogs.value = emptyList()
        Timber.d("Health logs cleared")
    }
}