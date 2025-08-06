package com.example.myapplication.service

import android.content.Context
import com.example.myapplication.domain.model.SensorData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Service for logging sensor data to CSV files.
 *
 * This service collects sensor data from StateFlows and writes it to CSV files
 * for offline analysis and algorithm improvement.
 */
class SensorDataLogger(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.IO)
    private var loggingJob: Job? = null
    private var isLogging = false
    
    // Directory for storing log files
    private val logDirectory: File by lazy {
        File(context.getExternalFilesDir(null), "sensor_logs").apply {
            if (!exists()) {
                mkdirs()
            }
        }
    }
    
    // Files for each sensor type
    private val accelerometerFile: File by lazy { createLogFile("accelerometer") }
    private val gyroscopeFile: File by lazy { createLogFile("gyroscope") }
    private val magnetometerFile: File by lazy { createLogFile("magnetometer") }
    private val rotationVectorFile: File by lazy { createLogFile("rotation_vector") }
    
    /**
     * Creates a log file with a timestamp in the filename.
     *
     * @param sensorType The type of sensor data to be logged
     * @return A File object representing the log file
     */
    private fun createLogFile(sensorType: String): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return File(logDirectory, "${sensorType}_${timestamp}.csv")
    }
    
    // Reference to LogFileManager for automatic cleanup
    private val logFileManager: LogFileManager by lazy {
        LogFileManager(context)
    }
    
    /**
     * Starts logging sensor data to CSV files.
     *
     * @param accelerometerData StateFlow of accelerometer data
     * @param gyroscopeData StateFlow of gyroscope data
     * @param magnetometerData StateFlow of magnetometer data
     * @param rotationVectorData StateFlow of rotation vector data
     * @return True if logging started successfully, false otherwise
     */
    fun startLogging(
        accelerometerData: StateFlow<SensorData.Accelerometer>,
        gyroscopeData: StateFlow<SensorData.Gyroscope>,
        magnetometerData: StateFlow<SensorData.Magnetometer>,
        rotationVectorData: StateFlow<SensorData.RotationVector>
    ): Boolean {
        if (isLogging) {
            Timber.w("Sensor data logging is already in progress")
            return false
        }
        
        try {
            // Perform automatic cleanup to prevent excessive disk usage
            logFileManager.performAutoCleanup(LogFileManager.LogType.SENSOR)
            
            // Write headers to CSV files
            writeHeader(accelerometerFile, "timestamp,x,y,z")
            writeHeader(gyroscopeFile, "timestamp,x,y,z")
            writeHeader(magnetometerFile, "timestamp,x,y,z")
            writeHeader(rotationVectorFile, "timestamp,x,y,z,w")
            
            loggingJob = scope.launch {
                // Launch coroutines to collect and log each sensor type
                launch { collectAndLogAccelerometerData(accelerometerData) }
                launch { collectAndLogGyroscopeData(gyroscopeData) }
                launch { collectAndLogMagnetometerData(magnetometerData) }
                launch { collectAndLogRotationVectorData(rotationVectorData) }
            }
            
            isLogging = true
            Timber.i("Sensor data logging started")
            return true
        } catch (e: IOException) {
            Timber.e(e, "Failed to start sensor data logging")
            return false
        }
    }
    
    /**
     * Stops logging sensor data.
     */
    fun stopLogging() {
        if (!isLogging) {
            Timber.w("Sensor data logging is not in progress")
            return
        }
        
        loggingJob?.cancel()
        loggingJob = null
        isLogging = false
        
        Timber.i("Sensor data logging stopped")
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
     * Collects and logs accelerometer data.
     *
     * @param accelerometerData StateFlow of accelerometer data
     */
    private suspend fun collectAndLogAccelerometerData(accelerometerData: StateFlow<SensorData.Accelerometer>) {
        accelerometerData.collect { data ->
            val line = "${data.timestamp},${data.x},${data.y},${data.z}"
            appendLine(accelerometerFile, line)
        }
    }
    
    /**
     * Collects and logs gyroscope data.
     *
     * @param gyroscopeData StateFlow of gyroscope data
     */
    private suspend fun collectAndLogGyroscopeData(gyroscopeData: StateFlow<SensorData.Gyroscope>) {
        gyroscopeData.collect { data ->
            val line = "${data.timestamp},${data.x},${data.y},${data.z}"
            appendLine(gyroscopeFile, line)
        }
    }
    
    /**
     * Collects and logs magnetometer data.
     *
     * @param magnetometerData StateFlow of magnetometer data
     */
    private suspend fun collectAndLogMagnetometerData(magnetometerData: StateFlow<SensorData.Magnetometer>) {
        magnetometerData.collect { data ->
            val line = "${data.timestamp},${data.x},${data.y},${data.z}"
            appendLine(magnetometerFile, line)
        }
    }
    
    /**
     * Collects and logs rotation vector data.
     *
     * @param rotationVectorData StateFlow of rotation vector data
     */
    private suspend fun collectAndLogRotationVectorData(rotationVectorData: StateFlow<SensorData.RotationVector>) {
        rotationVectorData.collect { data ->
            val line = "${data.timestamp},${data.x},${data.y},${data.z},${data.w}"
            appendLine(rotationVectorFile, line)
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