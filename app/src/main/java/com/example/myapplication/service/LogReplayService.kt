package com.example.myapplication.service

import android.content.Context
import com.example.myapplication.domain.model.Beacon
import com.example.myapplication.domain.model.SensorData
import com.example.myapplication.service.LogFileManager.LogType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Service for replaying recorded log files for algorithm testing.
 *
 * This service reads log files and replays the data events with their original timing,
 * allowing for testing and visualization of positioning algorithms with recorded data.
 */
class LogReplayService(private val context: Context) {

    // Replay state
    private val _isReplaying = MutableStateFlow(false)
    val isReplaying: StateFlow<Boolean> = _isReplaying

    private val _replayProgress = MutableStateFlow(0f)
    val replayProgress: StateFlow<Float> = _replayProgress

    private val _replaySpeed = MutableStateFlow(1.0f)
    val replaySpeed: StateFlow<Float> = _replaySpeed

    private val _currentLogFile = MutableStateFlow<File?>(null)
    val currentLogFile: StateFlow<File?> = _currentLogFile

    // Replay data
    private val _sensorData = MutableStateFlow<SensorData?>(null)
    val sensorData: StateFlow<SensorData?> = _sensorData

    private val _beaconData = MutableStateFlow<List<Beacon>>(emptyList())
    val beaconData: StateFlow<List<Beacon>> = _beaconData

    // Replay job
    private var replayJob: Job? = null

    /**
     * Sets the replay speed.
     *
     * @param speed The replay speed (1.0 = normal, 2.0 = double speed, etc.)
     */
    fun setReplaySpeed(speed: Float) {
        if (speed > 0) {
            _replaySpeed.value = speed
        }
    }

    /**
     * Starts replaying a log file.
     *
     * @param file The log file to replay
     * @param scope The coroutine scope to launch the replay job in
     */
    fun startReplay(file: File, scope: CoroutineScope) {
        if (_isReplaying.value) {
            stopReplay()
        }

        _currentLogFile.value = file
        _isReplaying.value = true
        _replayProgress.value = 0f

        replayJob = scope.launch(Dispatchers.IO) {
            try {
                when {
                    file.path.contains("sensor_logs") -> replaySensorLog(file)
                    file.path.contains("ble_logs") -> replayBleLog(file)
                    else -> {
                        Timber.e("Unknown log file type: ${file.path}")
                        _isReplaying.value = false
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error replaying log file: ${file.name}")
                _isReplaying.value = false
            }
        }
    }

    /**
     * Stops the current replay.
     */
    fun stopReplay() {
        replayJob?.cancel()
        replayJob = null
        _isReplaying.value = false
        _replayProgress.value = 0f
        _currentLogFile.value = null
        _sensorData.value = null
        _beaconData.value = emptyList()
    }

    /**
     * Replays a sensor log file.
     *
     * @param file The sensor log file to replay
     */
    private suspend fun replaySensorLog(file: File) {
        BufferedReader(FileReader(file)).use { reader ->
            // Skip header line
            val header = reader.readLine()
            if (header == null) {
                Timber.e("Empty sensor log file: ${file.name}")
                return
            }

            // Read all lines to calculate total duration for progress tracking
            val lines = reader.readLines()
            if (lines.isEmpty()) {
                Timber.e("No data in sensor log file: ${file.name}")
                return
            }

            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
            var firstTimestamp: Long = -1
            var lastTimestamp: Long = -1

            // Parse first and last timestamps for duration calculation
            try {
                val firstLine = lines.first()
                val lastLine = lines.last()
                
                val firstFields = firstLine.split(",")
                val lastFields = lastLine.split(",")
                
                if (firstFields.size > 0 && lastFields.size > 0) {
                    firstTimestamp = dateFormat.parse(firstFields[0])?.time ?: -1
                    lastTimestamp = dateFormat.parse(lastFields[0])?.time ?: -1
                }
            } catch (e: Exception) {
                Timber.e(e, "Error parsing timestamps from sensor log file: ${file.name}")
            }

            val totalDuration = if (firstTimestamp > 0 && lastTimestamp > 0) {
                lastTimestamp - firstTimestamp
            } else {
                0L
            }

            var previousTimestamp: Long = -1

            // Process each line
            for ((index, line) in lines.withIndex()) {
                try {
                    val fields = line.split(",")
                    if (fields.size < 7) continue // Timestamp, sensor type, x, y, z, accuracy, timestamp_ns

                    val timestamp = dateFormat.parse(fields[0])?.time ?: continue
                    
                    // Wait for the appropriate time between events
                    if (previousTimestamp > 0) {
                        val timeDiff = timestamp - previousTimestamp
                        val adjustedDelay = (timeDiff / _replaySpeed.value).toLong()
                        delay(adjustedDelay)
                    }
                    
                    previousTimestamp = timestamp

                    // Update progress
                    if (totalDuration > 0) {
                        val progress = (timestamp - firstTimestamp).toFloat() / totalDuration
                        _replayProgress.value = progress
                    } else {
                        _replayProgress.value = index.toFloat() / lines.size
                    }

                    // Parse sensor data
                    val sensorType = fields[1]
                    val x = fields[2].toFloatOrNull() ?: 0f
                    val y = fields[3].toFloatOrNull() ?: 0f
                    val z = fields[4].toFloatOrNull() ?: 0f
                    val timestampNs = fields[6].toLongOrNull() ?: 0L

                    // Create sensor data object based on sensor type
                    val data = when (sensorType) {
                        "ACCELEROMETER" -> SensorData.Accelerometer(x, y, z, timestampNs)
                        "GYROSCOPE" -> SensorData.Gyroscope(x, y, z, timestampNs)
                        "MAGNETOMETER" -> SensorData.Magnetometer(x, y, z, timestampNs)
                        else -> null
                    }

                    // Emit sensor data
                    data?.let { _sensorData.value = it }

                } catch (e: Exception) {
                    Timber.e(e, "Error processing sensor log line: $line")
                }
            }

            // Replay complete
            _replayProgress.value = 1f
            delay(1000) // Show completed state briefly
            _isReplaying.value = false
        }
    }

    /**
     * Replays a BLE log file.
     *
     * @param file The BLE log file to replay
     */
    private suspend fun replayBleLog(file: File) {
        BufferedReader(FileReader(file)).use { reader ->
            // Skip header line
            val header = reader.readLine()
            if (header == null) {
                Timber.e("Empty BLE log file: ${file.name}")
                return
            }

            // Read all lines to calculate total duration for progress tracking
            val lines = reader.readLines()
            if (lines.isEmpty()) {
                Timber.e("No data in BLE log file: ${file.name}")
                return
            }

            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
            var firstTimestamp: Long = -1
            var lastTimestamp: Long = -1

            // Parse first and last timestamps for duration calculation
            try {
                val firstLine = lines.first()
                val lastLine = lines.last()
                
                val firstFields = firstLine.split(",")
                val lastFields = lastLine.split(",")
                
                if (firstFields.size > 0 && lastFields.size > 0) {
                    firstTimestamp = dateFormat.parse(firstFields[0])?.time ?: -1
                    lastTimestamp = dateFormat.parse(lastFields[0])?.time ?: -1
                }
            } catch (e: Exception) {
                Timber.e(e, "Error parsing timestamps from BLE log file: ${file.name}")
            }

            val totalDuration = if (firstTimestamp > 0 && lastTimestamp > 0) {
                lastTimestamp - firstTimestamp
            } else {
                0L
            }

            var previousTimestamp: Long = -1
            val currentBeacons = mutableListOf<Beacon>()

            // Process each line
            for ((index, line) in lines.withIndex()) {
                try {
                    val fields = line.split(",")
                    if (fields.size < 5) continue // Timestamp, MAC, RSSI, TxPower, Name

                    val timestamp = dateFormat.parse(fields[0])?.time ?: continue
                    
                    // Wait for the appropriate time between events
                    if (previousTimestamp > 0) {
                        val timeDiff = timestamp - previousTimestamp
                        val adjustedDelay = (timeDiff / _replaySpeed.value).toLong()
                        delay(adjustedDelay)
                    }
                    
                    previousTimestamp = timestamp

                    // Update progress
                    if (totalDuration > 0) {
                        val progress = (timestamp - firstTimestamp).toFloat() / totalDuration
                        _replayProgress.value = progress
                    } else {
                        _replayProgress.value = index.toFloat() / lines.size
                    }

                    // Parse beacon data
                    val macAddress = fields[1]
                    val rssi = fields[2].toIntOrNull() ?: 0
                    val txPower = fields[3].toIntOrNull() ?: -65 // Default txPower if not available
                    val name = if (fields.size > 4) fields[4] else ""

                    // Create a new beacon or update existing one
                    // Note: For replay purposes, we're creating simplified beacons without x,y coordinates
                    // In a real implementation, these would be loaded from a configuration
                    val currentTime = System.currentTimeMillis()
                    
                    // Find existing beacon or create new one
                    val existingBeaconIndex = currentBeacons.indexOfFirst { it.macAddress == macAddress }
                    if (existingBeaconIndex >= 0) {
                        // Update existing beacon
                        val existingBeacon = currentBeacons[existingBeaconIndex]
                        // Since we can't modify the beacon directly (it's immutable), we need to create a new one
                        // with updated values and replace the old one in the list
                        currentBeacons[existingBeaconIndex] = Beacon(
                            id = existingBeacon.id,
                            macAddress = macAddress,
                            x = existingBeacon.x,
                            y = existingBeacon.y,
                            txPower = txPower,
                            name = name,
                            lastSeenTimestamp = currentTime,
                            lastRssi = rssi
                        )
                    } else {
                        // Add new beacon with default position (0,0)
                        currentBeacons.add(
                            Beacon(
                                macAddress = macAddress,
                                x = 0f,
                                y = 0f,
                                txPower = txPower,
                                name = name,
                                lastSeenTimestamp = currentTime,
                                lastRssi = rssi
                            )
                        )
                    }

                    // Remove stale beacons (not seen in the last 5 seconds)
                    currentBeacons.removeAll { beacon -> 
                        currentTime - beacon.lastSeenTimestamp > 5000
                    }

                    // Emit updated beacon list
                    _beaconData.value = currentBeacons.toList()

                } catch (e: Exception) {
                    Timber.e(e, "Error processing BLE log line: $line")
                }
            }

            // Replay complete
            _replayProgress.value = 1f
            delay(1000) // Show completed state briefly
            _isReplaying.value = false
        }
    }
}