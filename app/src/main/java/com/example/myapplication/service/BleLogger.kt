package com.example.myapplication.service

import android.bluetooth.le.ScanResult
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Service for logging BLE scan results to CSV files.
 *
 * This service logs BLE scan results to CSV files for offline analysis
 * and algorithm improvement.
 */
class BleLogger(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.IO)
    private var loggingJob: Job? = null
    private var isLogging = false
    
    // Directory for storing log files
    private val logDirectory: File by lazy {
        File(context.getExternalFilesDir(null), "ble_logs").apply {
            if (!exists()) {
                mkdirs()
            }
        }
    }
    
    // Current log file
    private val bleLogFile: File by lazy { createLogFile() }
    
    /**
     * Creates a log file with a timestamp in the filename.
     *
     * @return A File object representing the log file
     */
    private fun createLogFile(): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return File(logDirectory, "ble_scan_${timestamp}.csv")
    }
    
    // Reference to LogFileManager for automatic cleanup
    private val logFileManager: LogFileManager by lazy {
        LogFileManager(context)
    }
    
    /**
     * Starts logging BLE scan results to CSV files.
     *
     * @return True if logging started successfully, false otherwise
     */
    fun startLogging(): Boolean {
        if (isLogging) {
            Timber.w("BLE scan logging is already in progress")
            return false
        }
        
        try {
            // Perform automatic cleanup to prevent excessive disk usage
            logFileManager.performAutoCleanup(LogFileManager.LogType.BLE)
            
            // Write header to CSV file
            writeHeader(bleLogFile, "timestamp,mac_address,rssi,tx_power,device_name,manufacturer_data,service_uuids")
            
            isLogging = true
            Timber.i("BLE scan logging started")
            return true
        } catch (e: IOException) {
            Timber.e(e, "Failed to start BLE scan logging")
            return false
        }
    }
    
    /**
     * Stops logging BLE scan results.
     */
    fun stopLogging() {
        if (!isLogging) {
            Timber.w("BLE scan logging is not in progress")
            return
        }
        
        loggingJob?.cancel()
        loggingJob = null
        isLogging = false
        
        Timber.i("BLE scan logging stopped")
    }
    
    /**
     * Writes a header line to a CSV file.
     *
     * @param file The file to write to
     * @param header The header line
     */
    private fun writeHeader(file: File, header: String) {
        try {
            FileOutputStream(file, false).use { outputStream ->
                outputStream.write("$header\n".toByteArray())
            }
        } catch (e: IOException) {
            Timber.e(e, "Failed to write header to ${file.name}")
        }
    }
    
    /**
     * Appends a line of data to a CSV file.
     *
     * @param file The file to write to
     * @param line The line of data
     */
    private fun appendLine(file: File, line: String) {
        try {
            FileOutputStream(file, true).use { outputStream ->
                outputStream.write("$line\n".toByteArray())
            }
        } catch (e: IOException) {
            Timber.e(e, "Failed to append line to ${file.name}")
        }
    }
    
    /**
     * Logs a BLE scan result to the CSV file.
     *
     * @param result The ScanResult to log
     */
    fun logScanResult(result: ScanResult) {
        if (!isLogging) {
            return
        }
        
        scope.launch {
            val device = result.device
            val timestamp = System.currentTimeMillis()
            val macAddress = device.address
            val rssi = result.rssi
            val txPower = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                result.txPower
            } else {
                -1 // Not available on older Android versions
            }
            
            // Safely get device name with permission check
            val deviceName = try {
                device.name ?: "Unknown"
            } catch (e: SecurityException) {
                Timber.w("Permission denied for accessing device name")
                "Permission Denied"
            }
            
            // Get manufacturer data as hex string
            val manufacturerData = if (result.scanRecord?.manufacturerSpecificData != null) {
                val data = StringBuilder()
                for (i in 0 until result.scanRecord!!.manufacturerSpecificData.size()) {
                    val manufacturerId = result.scanRecord!!.manufacturerSpecificData.keyAt(i)
                    val manufacturerBytes = result.scanRecord!!.manufacturerSpecificData.get(manufacturerId)
                    data.append("$manufacturerId:")
                    manufacturerBytes?.forEach { byte ->
                        data.append(String.format("%02X", byte))
                    }
                    data.append(";")
                }
                data.toString()
            } else {
                ""
            }
            
            // Get service UUIDs as string
            val serviceUuids = result.scanRecord?.serviceUuids?.joinToString(";") { it.toString() } ?: ""
            
            // Escape any commas in fields to prevent CSV parsing issues
            val escapedDeviceName = deviceName.replace(",", "\\,")
            val escapedManufacturerData = manufacturerData.replace(",", "\\,")
            val escapedServiceUuids = serviceUuids.replace(",", "\\,")
            
            val line = "$timestamp,$macAddress,$rssi,$txPower,$escapedDeviceName,$escapedManufacturerData,$escapedServiceUuids"
            appendLine(bleLogFile, line)
        }
    }
    
    /**
     * Logs a batch of BLE scan results to the CSV file.
     *
     * @param results The list of ScanResults to log
     */
    fun logBatchScanResults(results: List<ScanResult>) {
        if (!isLogging) {
            return
        }
        
        results.forEach { result ->
            logScanResult(result)
        }
    }
    
    /**
     * Gets a list of all log files.
     *
     * @return List of log files
     */
    fun getLogFiles(): List<File> {
        return logDirectory.listFiles()?.toList() ?: emptyList()
    }
    
    /**
     * Deletes a log file.
     *
     * @param file The file to delete
     * @return True if the file was deleted successfully, false otherwise
     */
    fun deleteLogFile(file: File): Boolean {
        return if (file.exists() && file.isFile) {
            val deleted = file.delete()
            if (deleted) {
                Timber.i("Deleted log file: ${file.name}")
            } else {
                Timber.e("Failed to delete log file: ${file.name}")
            }
            deleted
        } else {
            Timber.e("Cannot delete non-existent file: ${file.name}")
            false
        }
    }
    
    /**
     * Deletes all log files.
     *
     * @return The number of files deleted
     */
    fun deleteAllLogFiles(): Int {
        var count = 0
        logDirectory.listFiles()?.forEach { file ->
            if (file.delete()) {
                count++
            }
        }
        Timber.i("Deleted $count log files")
        return count
    }
    
    /**
     * Checks if logging is currently in progress.
     *
     * @return True if logging is in progress, false otherwise
     */
    fun isLoggingInProgress(): Boolean {
        return isLogging
    }
}