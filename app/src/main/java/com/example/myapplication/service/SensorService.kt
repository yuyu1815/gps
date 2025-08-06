package com.example.myapplication.service

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.example.myapplication.domain.model.SensorData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import java.io.File

/**
 * Service for managing sensor data collection and processing.
 * 
 * This service handles the registration and unregistration of sensor listeners,
 * and provides access to sensor data through StateFlows.
 */
class SensorService(private val context: Context) {
    
    // Android sensor manager
    private val sensorManager: SensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    
    // Sensors
    private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gyroscope: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private val magnetometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
    private val rotationVector: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    
    // StateFlows for sensor data
    private val _accelerometerData = MutableStateFlow(SensorData.Accelerometer())
    private val _gyroscopeData = MutableStateFlow(SensorData.Gyroscope())
    private val _magnetometerData = MutableStateFlow(SensorData.Magnetometer())
    private val _rotationVectorData = MutableStateFlow(SensorData.RotationVector())
    
    // Public StateFlows
    val accelerometerData: StateFlow<SensorData.Accelerometer> = _accelerometerData.asStateFlow()
    val gyroscopeData: StateFlow<SensorData.Gyroscope> = _gyroscopeData.asStateFlow()
    val magnetometerData: StateFlow<SensorData.Magnetometer> = _magnetometerData.asStateFlow()
    val rotationVectorData: StateFlow<SensorData.RotationVector> = _rotationVectorData.asStateFlow()
    
    // Sensor data logger
    private val sensorDataLogger = SensorDataLogger(context)
    
    // Sensor event listeners
    private val accelerometerListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]
                val timestamp = event.timestamp
                
                _accelerometerData.value = SensorData.Accelerometer(x, y, z, timestamp)
            }
        }
        
        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
            Timber.d("Accelerometer accuracy changed: $accuracy")
        }
    }
    
    private val gyroscopeListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (event.sensor.type == Sensor.TYPE_GYROSCOPE) {
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]
                val timestamp = event.timestamp
                
                _gyroscopeData.value = SensorData.Gyroscope(x, y, z, timestamp)
            }
        }
        
        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
            Timber.d("Gyroscope accuracy changed: $accuracy")
        }
    }
    
    private val magnetometerListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]
                val timestamp = event.timestamp
                
                _magnetometerData.value = SensorData.Magnetometer(x, y, z, timestamp)
            }
        }
        
        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
            Timber.d("Magnetometer accuracy changed: $accuracy")
        }
    }
    
    private val rotationVectorListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]
                val w = if (event.values.size >= 4) event.values[3] else 0f
                val timestamp = event.timestamp
                
                _rotationVectorData.value = SensorData.RotationVector(x, y, z, w, timestamp)
            }
        }
        
        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
            Timber.d("Rotation vector accuracy changed: $accuracy")
        }
    }
    
    /**
     * Starts collecting sensor data.
     * 
     * @param samplingPeriodUs Sampling period in microseconds
     * @return True if all sensors were registered successfully, false otherwise
     */
    fun startSensors(samplingPeriodUs: Int = SensorManager.SENSOR_DELAY_GAME): Boolean {
        var success = true
        
        // Register accelerometer
        if (accelerometer != null) {
            val registered = sensorManager.registerListener(
                accelerometerListener,
                accelerometer,
                samplingPeriodUs
            )
            if (!registered) {
                Timber.e("Failed to register accelerometer listener")
                success = false
            }
        } else {
            Timber.e("Accelerometer not available on this device")
            success = false
        }
        
        // Register gyroscope
        if (gyroscope != null) {
            val registered = sensorManager.registerListener(
                gyroscopeListener,
                gyroscope,
                samplingPeriodUs
            )
            if (!registered) {
                Timber.e("Failed to register gyroscope listener")
                success = false
            }
        } else {
            Timber.e("Gyroscope not available on this device")
            success = false
        }
        
        // Register magnetometer
        if (magnetometer != null) {
            val registered = sensorManager.registerListener(
                magnetometerListener,
                magnetometer,
                samplingPeriodUs
            )
            if (!registered) {
                Timber.e("Failed to register magnetometer listener")
                success = false
            }
        } else {
            Timber.e("Magnetometer not available on this device")
            success = false
        }
        
        // Register rotation vector
        if (rotationVector != null) {
            val registered = sensorManager.registerListener(
                rotationVectorListener,
                rotationVector,
                samplingPeriodUs
            )
            if (!registered) {
                Timber.e("Failed to register rotation vector listener")
                success = false
            }
        } else {
            Timber.e("Rotation vector sensor not available on this device")
            success = false
        }
        
        return success
    }
    
    /**
     * Stops collecting sensor data.
     */
    fun stopSensors() {
        sensorManager.unregisterListener(accelerometerListener)
        sensorManager.unregisterListener(gyroscopeListener)
        sensorManager.unregisterListener(magnetometerListener)
        sensorManager.unregisterListener(rotationVectorListener)
        
        Timber.d("All sensor listeners unregistered")
    }
    
    /**
     * Checks if the required sensors are available on the device.
     * 
     * @return Map of sensor types to their availability
     */
    fun checkSensorsAvailability(): Map<String, Boolean> {
        return mapOf(
            "accelerometer" to (accelerometer != null),
            "gyroscope" to (gyroscope != null),
            "magnetometer" to (magnetometer != null),
            "rotationVector" to (rotationVector != null)
        )
    }
    
    /**
     * Gets the sampling rates supported by the sensors.
     * 
     * @return Map of sensor types to their supported sampling rates in microseconds
     */
    fun getSupportedSamplingRates(): Map<String, List<Int>> {
        val rates = mutableMapOf<String, List<Int>>()
        
        // Standard sampling rates
        val standardRates = listOf(
            SensorManager.SENSOR_DELAY_FASTEST,
            SensorManager.SENSOR_DELAY_GAME,
            SensorManager.SENSOR_DELAY_UI,
            SensorManager.SENSOR_DELAY_NORMAL
        )
        
        if (accelerometer != null) {
            rates["accelerometer"] = standardRates
        }
        
        if (gyroscope != null) {
            rates["gyroscope"] = standardRates
        }
        
        if (magnetometer != null) {
            rates["magnetometer"] = standardRates
        }
        
        if (rotationVector != null) {
            rates["rotationVector"] = standardRates
        }
        
        return rates
    }
    
    /**
     * Starts logging sensor data to CSV files.
     * 
     * @return True if logging started successfully, false otherwise
     */
    fun startLogging(): Boolean {
        return sensorDataLogger.startLogging(
            accelerometerData,
            gyroscopeData,
            magnetometerData,
            rotationVectorData
        )
    }
    
    /**
     * Stops logging sensor data.
     */
    fun stopLogging() {
        sensorDataLogger.stopLogging()
    }
    
    /**
     * Checks if sensor data logging is in progress.
     * 
     * @return True if logging is in progress, false otherwise
     */
    fun isLoggingInProgress(): Boolean {
        return sensorDataLogger.isLoggingInProgress()
    }
    
    /**
     * Gets a list of all sensor data log files.
     * 
     * @return List of log files
     */
    fun getLogFiles(): List<File> {
        return sensorDataLogger.getLogFiles()
    }
    
    /**
     * Deletes a sensor data log file.
     * 
     * @param file The file to delete
     * @return True if the file was deleted successfully, false otherwise
     */
    fun deleteLogFile(file: File): Boolean {
        return sensorDataLogger.deleteLogFile(file)
    }
    
    /**
     * Deletes all sensor data log files.
     * 
     * @return The number of files deleted
     */
    fun deleteAllLogFiles(): Int {
        return sensorDataLogger.deleteAllLogFiles()
    }
}