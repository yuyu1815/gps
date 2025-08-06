package com.example.myapplication.service

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.example.myapplication.domain.model.SensorData
import com.example.myapplication.domain.model.Vector3D
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import timber.log.Timber

/**
 * Lifecycle-aware sensor monitor component.
 * Handles sensor data collection and automatically manages the sensor listeners
 * based on the lifecycle of the associated component.
 * Includes device position detection for orientation handling in different carrying positions.
 * Implements static detection to reduce sensor sampling frequency when device is not moving.
 */
class SensorMonitor(
    private val context: Context
) : DefaultLifecycleObserver, SensorEventListener {
    
    // List of static state listeners
    private val staticStateListeners = mutableListOf<StaticStateListener>()
    
    private val sensorManager: SensorManager by lazy {
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }
    
    // Sensors
    private var accelerometer: Sensor? = null
    private var gyroscope: Sensor? = null
    private var magnetometer: Sensor? = null
    private var linearAcceleration: Sensor? = null
    private var gravity: Sensor? = null
    
    // Sensor data
    private val _sensorData = MutableStateFlow<SensorData.Combined>(
        SensorData.Combined(
            timestamp = System.currentTimeMillis(),
            accelerometer = Vector3D(),
            gyroscope = Vector3D(),
            magnetometer = Vector3D(),
            linearAcceleration = Vector3D(),
            gravity = Vector3D(),
            orientation = Vector3D(),
            heading = 0f
        )
    )
    val sensorData: StateFlow<SensorData.Combined> = _sensorData
    
    // Individual sensor data flows
    private val _accelerometerFlow = MutableStateFlow<SensorData.Accelerometer?>(null)
    val accelerometerFlow: StateFlow<SensorData.Accelerometer?> = _accelerometerFlow
    
    private val _gyroscopeFlow = MutableStateFlow<SensorData.Gyroscope?>(null)
    val gyroscopeFlow: StateFlow<SensorData.Gyroscope?> = _gyroscopeFlow
    
    private val _magnetometerFlow = MutableStateFlow<SensorData.Magnetometer?>(null)
    val magnetometerFlow: StateFlow<SensorData.Magnetometer?> = _magnetometerFlow
    
    // Sensor data buffers
    private var accelerometerData = Vector3D()
    private var gyroscopeData = Vector3D()
    private var magnetometerData = Vector3D()
    private var linearAccelerationData = Vector3D()
    private var gravityData = Vector3D()
    
    // Orientation calculation
    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)
    
    // Device position detector
    private val devicePositionDetector = DevicePositionDetector()
    
    // Static state detector for optimizing sensor sampling rate
    private val staticDetector = StaticDetector()
    
    // Handler for delayed sampling rate adjustments
    private val handler = Handler(Looper.getMainLooper())
    
    // Sampling rates (in microseconds)
    private val highSamplingRate = SensorManager.SENSOR_DELAY_GAME      // ~20ms for active movement
    private val normalSamplingRate = SensorManager.SENSOR_DELAY_UI      // ~60ms for normal use
    private val lowSamplingRate = SensorManager.SENSOR_DELAY_NORMAL     // ~200ms for static periods
    private val veryLowSamplingRate = 500000                            // 500ms for long static periods
    private var currentSamplingRate = normalSamplingRate
    
    // Monitoring state
    private var isMonitoring = false
    private var isStatic = false
    private var isLongStatic = false
    
    // Lifecycle callbacks
    override fun onResume(owner: LifecycleOwner) {
        Timber.d("SensorMonitor: onResume")
        if (isMonitoring) {
            startMonitoring()
        }
    }
    
    override fun onPause(owner: LifecycleOwner) {
        Timber.d("SensorMonitor: onPause")
        stopMonitoring()
    }
    
    override fun onDestroy(owner: LifecycleOwner) {
        Timber.d("SensorMonitor: onDestroy")
        stopMonitoring()
    }
    
    /**
     * Starts sensor monitoring.
     */
    fun startMonitoring() {
        if (isMonitoring) {
            Timber.d("Sensor monitoring already in progress")
            return
        }
        
        Timber.d("Starting sensor monitoring")
        
        // Initialize sensors
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        linearAcceleration = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        gravity = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
        
        // Register listeners with current sampling rate
        registerSensors(currentSamplingRate)
        
        isMonitoring = true
        
        // Reset static detector
        staticDetector.reset()
        isStatic = false
        isLongStatic = false
    }
    
    /**
     * Stops sensor monitoring.
     */
    fun stopMonitoring() {
        if (!isMonitoring) {
            return
        }
        
        Timber.d("Stopping sensor monitoring")
        
        // Unregister listeners
        unregisterSensors()
        
        isMonitoring = false
    }
    
    /**
     * Registers sensor listeners with the specified sampling rate.
     * 
     * @param samplingPeriodUs The sampling period in microseconds
     */
    private fun registerSensors(samplingPeriodUs: Int) {
        Timber.d("Registering sensors with sampling rate: ${samplingPeriodUs}us")
        
        accelerometer?.let {
            sensorManager.registerListener(this, it, samplingPeriodUs)
        }
        gyroscope?.let {
            sensorManager.registerListener(this, it, samplingPeriodUs)
        }
        magnetometer?.let {
            sensorManager.registerListener(this, it, samplingPeriodUs)
        }
        linearAcceleration?.let {
            sensorManager.registerListener(this, it, samplingPeriodUs)
        }
        gravity?.let {
            sensorManager.registerListener(this, it, samplingPeriodUs)
        }
    }
    
    /**
     * Unregisters all sensor listeners.
     */
    private fun unregisterSensors() {
        sensorManager.unregisterListener(this)
    }
    
    /**
     * Registers a listener to be notified of static state changes.
     * 
     * @param listener The listener to register
     */
    fun addStaticStateListener(listener: StaticStateListener) {
        if (!staticStateListeners.contains(listener)) {
            staticStateListeners.add(listener)
            Timber.d("Static state listener added, total listeners: ${staticStateListeners.size}")
            
            // Immediately notify the new listener of the current state
            if (isMonitoring) {
                listener.onStaticStateChanged(
                    isStatic,
                    isLongStatic,
                    staticDetector.getStaticConfidence(),
                    staticDetector.getStaticDuration()
                )
            }
        }
    }
    
    /**
     * Unregisters a static state listener.
     * 
     * @param listener The listener to unregister
     */
    fun removeStaticStateListener(listener: StaticStateListener) {
        if (staticStateListeners.remove(listener)) {
            Timber.d("Static state listener removed, remaining listeners: ${staticStateListeners.size}")
        }
    }
    
    /**
     * Notifies all registered listeners of a static state change.
     * 
     * @param isStatic Whether the device is currently static
     * @param isLongStatic Whether the device has been static for a long period
     * @param confidence The confidence level in the static state detection
     * @param durationMs The duration of the current static state in milliseconds
     */
    private fun notifyStaticStateListeners(
        isStatic: Boolean,
        isLongStatic: Boolean,
        confidence: Float,
        durationMs: Long
    ) {
        staticStateListeners.forEach { listener ->
            try {
                listener.onStaticStateChanged(isStatic, isLongStatic, confidence, durationMs)
            } catch (e: Exception) {
                Timber.e("Error notifying static state listener: ${e.message}")
            }
        }
    }
    
    /**
     * Sets the sensor sampling rate.
     * 
     * @param samplingPeriodUs The sampling period in microseconds
     * @param force If true, forces the sampling rate change even if it would normally be managed dynamically
     */
    fun setSamplingRate(samplingPeriodUs: Int, force: Boolean = false) {
        if (force) {
            currentSamplingRate = samplingPeriodUs
            Timber.d("Forced sampling rate change to ${samplingPeriodUs}us")
        }
        
        if (isMonitoring) {
            unregisterSensors()
            registerSensors(currentSamplingRate)
        }
    }
    
    /**
     * Adjusts the sensor sampling rate based on the device's static state.
     * This helps reduce battery consumption when the device is not moving.
     */
    private fun adjustSamplingRate() {
        val newRate = when {
            isLongStatic -> {
                // Device has been static for a long time, use very low sampling rate
                veryLowSamplingRate
            }
            isStatic -> {
                // Device is static but not for long, use low sampling rate
                lowSamplingRate
            }
            devicePositionDetector.getCurrentPosition() == DevicePositionDetector.CarryingPosition.HAND -> {
                // Device is in hand and moving, use high sampling rate for better responsiveness
                highSamplingRate
            }
            else -> {
                // Default to normal sampling rate
                normalSamplingRate
            }
        }
        
        // Only change sampling rate if it's different from current rate
        if (newRate != currentSamplingRate) {
            Timber.d("Adjusting sampling rate from ${currentSamplingRate}us to ${newRate}us " +
                    "(static: $isStatic, longStatic: $isLongStatic)")
            
            currentSamplingRate = newRate
            
            // Apply the new sampling rate
            unregisterSensors()
            registerSensors(currentSamplingRate)
        }
    }
    
    // SensorEventListener implementation
    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                accelerometerData = Vector3D(
                    event.values[0],
                    event.values[1],
                    event.values[2]
                )
                
                // Update accelerometer flow
                _accelerometerFlow.value = SensorData.Accelerometer(
                    x = event.values[0],
                    y = event.values[1],
                    z = event.values[2],
                    timestamp = event.timestamp
                )
            }
            Sensor.TYPE_GYROSCOPE -> {
                gyroscopeData = Vector3D(
                    event.values[0],
                    event.values[1],
                    event.values[2]
                )
                
                // Update gyroscope flow
                _gyroscopeFlow.value = SensorData.Gyroscope(
                    x = event.values[0],
                    y = event.values[1],
                    z = event.values[2],
                    timestamp = event.timestamp
                )
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                magnetometerData = Vector3D(
                    event.values[0],
                    event.values[1],
                    event.values[2]
                )
                
                // Update magnetometer flow
                _magnetometerFlow.value = SensorData.Magnetometer(
                    x = event.values[0],
                    y = event.values[1],
                    z = event.values[2],
                    timestamp = event.timestamp
                )
                
                // Update orientation when magnetometer data changes
                updateOrientation()
            }
            Sensor.TYPE_LINEAR_ACCELERATION -> {
                linearAccelerationData = Vector3D(
                    event.values[0],
                    event.values[1],
                    event.values[2]
                )
            }
            Sensor.TYPE_GRAVITY -> {
                gravityData = Vector3D(
                    event.values[0],
                    event.values[1],
                    event.values[2]
                )
                
                // Update orientation when gravity data changes
                updateOrientation()
            }
        }
        
        // Create combined sensor data
        val combinedData = SensorData.Combined(
            timestamp = System.currentTimeMillis(),
            accelerometer = accelerometerData,
            gyroscope = gyroscopeData,
            magnetometer = magnetometerData,
            linearAcceleration = linearAccelerationData,
            gravity = gravityData,
            orientation = calculateOrientation(),
            heading = calculateHeading()
        )
        
        // Detect device position and apply corrections
        val devicePosition = devicePositionDetector.detectPosition(combinedData)
        val (accelFactor, gyroFactor, headingOffset) = devicePositionDetector.getOrientationCorrectionFactors()
        
        // Detect static state for sampling rate optimization
        val previousStatic = isStatic
        val previousLongStatic = isLongStatic
        isStatic = staticDetector.detectStaticState(combinedData)
        isLongStatic = staticDetector.isLongStaticState()
        
        // Log static state changes
        if (previousStatic != isStatic || previousLongStatic != isLongStatic) {
            val staticDuration = staticDetector.getStaticDuration()
            val staticConfidence = staticDetector.getStaticConfidence()
            
            Timber.d("Static state changed: static=$isStatic, longStatic=$isLongStatic, " +
                    "confidence=$staticConfidence, " +
                    "duration=${staticDuration/1000}s")
            
            // Notify all registered listeners
            notifyStaticStateListeners(isStatic, isLongStatic, staticConfidence, staticDuration)
            
            // Schedule sampling rate adjustment with a slight delay to avoid rapid changes
            handler.removeCallbacksAndMessages(null) // Remove any pending adjustments
            handler.postDelayed({ adjustSamplingRate() }, 1000)
        }
        
        // Apply corrections based on device position
        val correctedData = combinedData.copy(
            // Apply acceleration correction
            linearAcceleration = combinedData.linearAcceleration * accelFactor,
            // Apply gyroscope correction
            gyroscope = combinedData.gyroscope * gyroFactor,
            // Apply heading correction
            heading = (combinedData.heading + headingOffset + 360f) % 360f
        )
        
        // Log device position changes
        if (devicePositionDetector.getCurrentPosition() != DevicePositionDetector.CarryingPosition.UNKNOWN) {
            Timber.d("Device position: ${devicePositionDetector.getCurrentPosition()}, " +
                    "confidence: ${devicePositionDetector.getPositionConfidence()}")
        }
        
        // Update sensor data flow with corrected data
        _sensorData.value = correctedData
    }
    
    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        // Not used
    }
    
    /**
     * Updates the orientation angles using gravity and magnetometer data.
     */
    private fun updateOrientation() {
        // Skip if we don't have both gravity and magnetometer data
        if (gravityData.magnitude() < 0.1f || magnetometerData.magnitude() < 0.1f) {
            return
        }
        
        // Convert Vector3D to FloatArray
        val gravityValues = floatArrayOf(gravityData.x, gravityData.y, gravityData.z)
        val magneticValues = floatArrayOf(magnetometerData.x, magnetometerData.y, magnetometerData.z)
        
        // Calculate rotation matrix
        SensorManager.getRotationMatrix(
            rotationMatrix,
            null,
            gravityValues,
            magneticValues
        )
        
        // Calculate orientation angles
        SensorManager.getOrientation(rotationMatrix, orientationAngles)
    }
    
    /**
     * Calculates the orientation vector from the orientation angles.
     */
    private fun calculateOrientation(): Vector3D {
        // Convert radians to degrees
        val azimuth = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
        val pitch = Math.toDegrees(orientationAngles[1].toDouble()).toFloat()
        val roll = Math.toDegrees(orientationAngles[2].toDouble()).toFloat()
        
        return Vector3D(
            x = roll,
            y = pitch,
            z = azimuth
        )
    }
    
    /**
     * Calculates the heading in degrees (0-359).
     */
    private fun calculateHeading(): Float {
        // Get azimuth from orientation angles (in radians)
        val azimuth = orientationAngles[0]
        
        // Convert to degrees and normalize to 0-359
        val heading = (Math.toDegrees(azimuth.toDouble()).toFloat() + 360) % 360
        
        return heading
    }
}