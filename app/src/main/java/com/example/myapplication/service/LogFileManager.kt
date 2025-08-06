package com.example.myapplication.service

import android.content.Context
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Centralized manager for log file operations.
 *
 * This class provides a unified interface for managing log files from different sources
 * (sensors, BLE scans, etc.) for easier access and management.
 */
class LogFileManager(private val context: Context) {

    // Maximum age of log files (7 days in milliseconds)
    private val maxLogAge = 7 * 24 * 60 * 60 * 1000L
    
    // Maximum total size of log files (100 MB)
    private val maxTotalLogSize = 100 * 1024 * 1024L
    
    // Log directories
    private val rootLogDirectory: File by lazy {
        File(context.getExternalFilesDir(null), "logs").apply {
            if (!exists()) {
                mkdirs()
            }
        }
    }
    
    private val sensorLogDirectory: File by lazy {
        File(context.getExternalFilesDir(null), "sensor_logs").apply {
            if (!exists()) {
                mkdirs()
            }
        }
    }
    
    private val bleLogDirectory: File by lazy {
        File(context.getExternalFilesDir(null), "ble_logs").apply {
            if (!exists()) {
                mkdirs()
            }
        }
    }
    
    /**
     * Enum representing different types of log files.
     */
    enum class LogType {
        SENSOR,
        BLE,
        ALL
    }
    
    /**
     * Data class representing a log file with metadata.
     */
    data class LogFileInfo(
        val file: File,
        val type: LogType,
        val timestamp: Long,
        val sizeBytes: Long
    ) {
        val formattedDate: String
            get() = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(timestamp))
        
        val formattedSize: String
            get() = when {
                sizeBytes < 1024 -> "$sizeBytes B"
                sizeBytes < 1024 * 1024 -> "${sizeBytes / 1024} KB"
                else -> "${sizeBytes / (1024 * 1024)} MB"
            }
    }
    
    /**
     * Gets a list of all log files with metadata.
     *
     * @param type The type of log files to get (default: ALL)
     * @return List of LogFileInfo objects
     */
    fun getLogFiles(type: LogType = LogType.ALL): List<LogFileInfo> {
        val logFiles = mutableListOf<LogFileInfo>()
        
        when (type) {
            LogType.SENSOR -> {
                addLogFilesFromDirectory(sensorLogDirectory, LogType.SENSOR, logFiles)
            }
            LogType.BLE -> {
                addLogFilesFromDirectory(bleLogDirectory, LogType.BLE, logFiles)
            }
            LogType.ALL -> {
                addLogFilesFromDirectory(sensorLogDirectory, LogType.SENSOR, logFiles)
                addLogFilesFromDirectory(bleLogDirectory, LogType.BLE, logFiles)
                // Add any other log directories here
            }
        }
        
        // Sort by timestamp (newest first)
        return logFiles.sortedByDescending { it.timestamp }
    }
    
    /**
     * Helper method to add log files from a directory to the list.
     *
     * @param directory The directory to scan for log files
     * @param type The type of log files
     * @param logFiles The list to add the log files to
     */
    private fun addLogFilesFromDirectory(directory: File, type: LogType, logFiles: MutableList<LogFileInfo>) {
        directory.listFiles()?.forEach { file ->
            if (file.isFile && file.name.endsWith(".csv")) {
                val timestamp = parseTimestampFromFilename(file.name) ?: file.lastModified()
                logFiles.add(
                    LogFileInfo(
                        file = file,
                        type = type,
                        timestamp = timestamp,
                        sizeBytes = file.length()
                    )
                )
            }
        }
    }
    
    /**
     * Parses a timestamp from a filename.
     * Expected format: *_yyyyMMdd_HHmmss.csv
     *
     * @param filename The filename to parse
     * @return The timestamp in milliseconds, or null if parsing failed
     */
    private fun parseTimestampFromFilename(filename: String): Long? {
        val regex = ".*_(\\d{8}_\\d{6})\\.csv$".toRegex()
        val matchResult = regex.find(filename)
        
        return matchResult?.groupValues?.get(1)?.let { timestampStr ->
            try {
                val date = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).parse(timestampStr)
                date?.time
            } catch (e: Exception) {
                Timber.e(e, "Failed to parse timestamp from filename: $filename")
                null
            }
        }
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
     * Deletes all log files of a specific type.
     *
     * @param type The type of log files to delete (default: ALL)
     * @return The number of files deleted
     */
    fun deleteAllLogFiles(type: LogType = LogType.ALL): Int {
        var count = 0
        
        when (type) {
            LogType.SENSOR -> {
                count += deleteFilesInDirectory(sensorLogDirectory)
            }
            LogType.BLE -> {
                count += deleteFilesInDirectory(bleLogDirectory)
            }
            LogType.ALL -> {
                count += deleteFilesInDirectory(sensorLogDirectory)
                count += deleteFilesInDirectory(bleLogDirectory)
                // Add any other log directories here
            }
        }
        
        Timber.i("Deleted $count log files")
        return count
    }
    
    /**
     * Helper method to delete all files in a directory.
     *
     * @param directory The directory to delete files from
     * @return The number of files deleted
     */
    private fun deleteFilesInDirectory(directory: File): Int {
        var count = 0
        directory.listFiles()?.forEach { file ->
            if (file.isFile && file.delete()) {
                count++
            }
        }
        return count
    }
    
    /**
     * Creates a new log file for a specific type.
     *
     * @param type The type of log file to create
     * @param name An optional name prefix for the log file
     * @return The created log file
     */
    fun createLogFile(type: LogType, name: String = ""): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val directory = when (type) {
            LogType.SENSOR -> sensorLogDirectory
            LogType.BLE -> bleLogDirectory
            LogType.ALL -> rootLogDirectory
        }
        
        val prefix = if (name.isNotEmpty()) "${name}_" else ""
        val filename = "${prefix}${timestamp}.csv"
        
        return File(directory, filename)
    }
    
    /**
     * Gets the total size of all log files of a specific type.
     *
     * @param type The type of log files to measure (default: ALL)
     * @return The total size in bytes
     */
    fun getTotalLogSize(type: LogType = LogType.ALL): Long {
        var totalSize = 0L
        
        getLogFiles(type).forEach { logFileInfo ->
            totalSize += logFileInfo.sizeBytes
        }
        
        return totalSize
    }
    
    /**
     * Gets the formatted total size of all log files of a specific type.
     *
     * @param type The type of log files to measure (default: ALL)
     * @return The formatted total size (e.g., "1.2 MB")
     */
    fun getFormattedTotalLogSize(type: LogType = LogType.ALL): String {
        val totalBytes = getTotalLogSize(type)
        
        return when {
            totalBytes < 1024 -> "$totalBytes B"
            totalBytes < 1024 * 1024 -> "${totalBytes / 1024} KB"
            totalBytes < 1024 * 1024 * 1024 -> String.format("%.1f MB", totalBytes / (1024.0 * 1024.0))
            else -> String.format("%.2f GB", totalBytes / (1024.0 * 1024.0 * 1024.0))
        }
    }
    
    /**
     * Performs automatic cleanup of log files to reduce disk usage.
     * This method:
     * 1. Removes log files older than maxLogAge
     * 2. If total size still exceeds maxTotalLogSize, removes oldest files until size is under limit
     *
     * @param type The type of log files to clean up (default: ALL)
     * @return The number of files deleted
     */
    fun performAutoCleanup(type: LogType = LogType.ALL): Int {
        var deletedCount = 0
        val currentTime = System.currentTimeMillis()
        
        // Get all log files sorted by timestamp (oldest first)
        val logFiles = getLogFiles(type).sortedBy { it.timestamp }
        
        // Step 1: Remove files older than maxLogAge
        logFiles.filter { currentTime - it.timestamp > maxLogAge }.forEach { logFile ->
            if (deleteLogFile(logFile.file)) {
                deletedCount++
                Timber.d("Auto-cleanup: Deleted old log file: ${logFile.file.name}")
            }
        }
        
        // Step 2: If total size still exceeds maxTotalLogSize, remove oldest files
        var currentTotalSize = getTotalLogSize(type)
        if (currentTotalSize > maxTotalLogSize) {
            Timber.d("Auto-cleanup: Total log size ($currentTotalSize bytes) exceeds limit ($maxTotalLogSize bytes)")
            
            // Get updated list of files after age-based cleanup
            val remainingFiles = getLogFiles(type).sortedBy { it.timestamp }
            
            for (logFile in remainingFiles) {
                if (currentTotalSize <= maxTotalLogSize) {
                    break
                }
                
                if (deleteLogFile(logFile.file)) {
                    currentTotalSize -= logFile.sizeBytes
                    deletedCount++
                    Timber.d("Auto-cleanup: Deleted log file to reduce size: ${logFile.file.name}")
                }
            }
        }
        
        Timber.i("Auto-cleanup: Deleted $deletedCount log files")
        return deletedCount
    }
}