package com.example.myapplication.service

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.Looper
import android.os.Build
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.example.myapplication.data.repository.ISettingsRepository
import com.example.myapplication.domain.model.SensorData
import com.example.myapplication.domain.model.Vector3D
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

/**
 * Data class to hold the availability status of each sensor.
 */
data class SensorStatus(
    val accelerometer: Boolean = false,
    val gyroscope: Boolean = false,
    val magnetometer: Boolean = false,
    val linearAcceleration: Boolean = false,
    val gravity: Boolean = false
)

/** Registration state of each sensor for UI indication */
enum class RegistrationState { INITIALIZING, READY, ERROR }

data class SensorRegistrationStates(
    val accelerometer: RegistrationState = RegistrationState.INITIALIZING,
    val gyroscope: RegistrationState = RegistrationState.INITIALIZING,
    val magnetometer: RegistrationState = RegistrationState.INITIALIZING,
    val linearAcceleration: RegistrationState = RegistrationState.INITIALIZING,
    val gravity: RegistrationState = RegistrationState.INITIALIZING
)

/**
 * Lifecycle-aware sensor monitor component.
 * Handles sensor data collection and automatically manages the sensor listeners
 * based on the lifecycle of the associated component.
 * Includes device position detection for orientation handling in different carrying positions.
 * Implements static detection to reduce sensor sampling frequency when device is not moving.
 * Provides graceful degradation when sensors are unavailable.
 */
class SensorMonitor(
    private val context: Context
) : DefaultLifecycleObserver, SensorEventListener, KoinComponent, LowPowerMode.PowerModeListener {
    
    // Inject LowPowerMode and SettingsRepository
    private val lowPowerMode: LowPowerMode by inject()
    private val settingsRepository: ISettingsRepository by inject()
    
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
    
    // Flags to suppress repeated error logs for registration failures
    private var accelerometerRegisterFailedOnce = false
    private var gyroscopeRegisterFailedOnce = false
    private var magnetometerRegisterFailedOnce = false
    private var linearAccelerationRegisterFailedOnce = false
    private var gravityRegisterFailedOnce = false
    
    // Sensor status
    private val _sensorStatus = MutableStateFlow(SensorStatus())
    val sensorStatus: StateFlow<SensorStatus> = _sensorStatus.asStateFlow()
    // Registration states for UI
    private val _registrationStates = MutableStateFlow(SensorRegistrationStates())
    val registrationStates: StateFlow<SensorRegistrationStates> = _registrationStates.asStateFlow()
    
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
    
    // Fallback data for unavailable sensors
    private var lastValidAccelerometerData = Vector3D()
    private var lastValidGyroscopeData = Vector3D()
    private var lastValidMagnetometerData = Vector3D()
    private var lastValidLinearAccelerationData = Vector3D()
    private var lastValidGravityData = Vector3D(0f, 9.81f, 0f) // Default gravity vector pointing down
    
    // Orientation calculation
    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)
    
    // Device position detector
    private val devicePositionDetector = DevicePositionDetector()
    
    // Static state detector for optimizing sensor sampling rate
    private val staticDetector = StaticDetector()
    
    // Handler for delayed sampling rate adjustments
    private val handler = Handler(Looper.getMainLooper())
    
    // Sampling rates (in microseconds) - now configurable
    private var highSamplingRate = SensorManager.SENSOR_DELAY_GAME      // ~20ms for active movement
    private var normalSamplingRate = SensorManager.SENSOR_DELAY_UI      // ~60ms for normal use
    private var lowSamplingRate = SensorManager.SENSOR_DELAY_NORMAL     // ~200ms for static periods
    private var veryLowSamplingRate = 500000                            // 500ms for long static periods
    private var currentSamplingRate = normalSamplingRate
    
    // Configurable monitoring delay
    private var monitoringDelayMs = 100L // Default 100ms
    
    // Individual sensor delays
    private var accelerometerDelayMs = 50L // Default 50ms
    private var gyroscopeDelayMs = 100L // Default 100ms
    private var magnetometerDelayMs = 200L // Default 200ms
    private var linearAccelerationDelayMs = 100L // Default 100ms
    private var gravityDelayMs = 500L // Default 500ms
    
    // Low-power mode parameters
    private var isLowPowerModeActive = false
    private var samplingRateReductionFactor = 1.0f
    
    // Monitoring state
    var isMonitoring: Boolean = false
        private set
    private var isStatic = false
    private var isLongStatic = false
    // Prevent concurrent registrations
    private var isRegistering: Boolean = false

    // Force using no-handler overload for sensor registration
    @Volatile
    private var forceNoHandlerRegistration: Boolean = false
    
    // Diagnostics stream to expose registration attempts/reasons
    private val _registrationDiagnostics = MutableStateFlow<List<String>>(emptyList())
    val registrationDiagnostics: StateFlow<List<String>> = _registrationDiagnostics.asStateFlow()

    private fun pushDiag(message: String) {
        Timber.d(message)
        val current = _registrationDiagnostics.value.toMutableList()
        current.add(message)
        // Keep last 100 messages
        while (current.size > 100) current.removeAt(0)
        _registrationDiagnostics.value = current
    }
    
    // Init logging helpers
    private var initStartMs: Long = 0L
    private fun logInit(message: String) {
        Timber.d("[INIT][SensorMonitor] $message")
    }
    
    // Backoff retry for registration
    private var registrationRetryJob: Job? = null
    private var registrationAttempt: Int = 0
    private val backoffDelaysMs = listOf(1000L, 2000L, 5000L, 10000L, 15000L)

    // Synthetic (fallback) mode
    @Volatile
    private var syntheticModeEnabled: Boolean = false
    private var syntheticJob: Job? = null
    private var syntheticHeadingDeg: Float = 0f

    private fun scheduleRegistrationRetry() {
        val attempt = registrationAttempt.coerceAtMost(backoffDelaysMs.lastIndex)
        val delayMs = backoffDelaysMs[attempt]
        pushDiag("Scheduling sensor registration retry in ${delayMs}ms (attempt=${registrationAttempt + 1})")
        registrationRetryJob?.cancel()
        registrationRetryJob = CoroutineScope(Dispatchers.Main).launch {
            delay(delayMs)
            // Try register again on main
            registerSensors()
        }
        registrationAttempt++
    }

    private fun cancelRegistrationRetry() {
        registrationRetryJob?.cancel()
        registrationRetryJob = null
        registrationAttempt = 0
    }
    
    init {
        // Register with LowPowerMode to receive power mode changes
        lowPowerMode.registerPowerModeListener(this)
        // Do not auto-start here to avoid races with Activity lifecycle.
        // Monitoring will start from Activity (onCreate/onResume).
    }

    /**
     * Enables or disables the no-handler registration mode.
     * When enabled, sensor registration uses the legacy overload without a Handler.
     */
    fun setForceNoHandlerRegistration(enabled: Boolean) {
        if (forceNoHandlerRegistration == enabled) return
        forceNoHandlerRegistration = enabled
        pushDiag("forceNoHandlerRegistration set to $enabled")
        if (isMonitoring) {
            // Re-apply registration with the new mode
            unregisterSensors()
            registerSensors()
        }
    }
    
    /**
     * Called when the power mode changes.
     * Adjusts sensor sampling rates based on low-power mode state.
     */
    override fun onPowerModeChanged(lowPowerMode: Boolean, samplingRateReductionFactor: Float) {
        Timber.d(StringProvider.getString(
            com.example.myapplication.R.string.log_power_mode_changed,
            lowPowerMode,
            samplingRateReductionFactor
        ))
        isLowPowerModeActive = lowPowerMode
        this.samplingRateReductionFactor = samplingRateReductionFactor
        
        // Adjust sampling rate if monitoring is active
        if (isMonitoring) {
            adjustSamplingRate()
        }
    }
    
    // Lifecycle callbacks
    override fun onResume(owner: LifecycleOwner) {
        Timber.d(StringProvider.getString(com.example.myapplication.R.string.log_sensor_monitor_on_resume))
        if (!isMonitoring) {
            startMonitoring()
        }
    }

    override fun onPause(owner: LifecycleOwner) {
        Timber.d(StringProvider.getString(com.example.myapplication.R.string.log_sensor_monitor_on_pause))
        // センサー監視は停止しない（バックグラウンドでも継続）
        // stopMonitoring()
    }

    override fun onDestroy(owner: LifecycleOwner) {
        Timber.d(StringProvider.getString(com.example.myapplication.R.string.log_sensor_monitor_on_destroy))
        stopMonitoring()
    }
    
    /**
     * Starts sensor monitoring.
     */
    fun startMonitoring() {
        if (isMonitoring) {
            Timber.d(StringProvider.getString(com.example.myapplication.R.string.log_monitoring_in_progress))
            return
        }
        
        Timber.d(StringProvider.getString(com.example.myapplication.R.string.log_starting_sensor_monitoring))
        initStartMs = System.currentTimeMillis()
        logInit("startMonitoring called")
        
        // Load monitoring delay from settings
        CoroutineScope(Dispatchers.IO).launch {
            try {
                monitoringDelayMs = settingsRepository.getSensorMonitoringDelay()
                accelerometerDelayMs = settingsRepository.getAccelerometerDelay()
                gyroscopeDelayMs = settingsRepository.getGyroscopeDelay()
                magnetometerDelayMs = settingsRepository.getMagnetometerDelay()
                linearAccelerationDelayMs = settingsRepository.getLinearAccelerationDelay()
                gravityDelayMs = settingsRepository.getGravityDelay()
                
                Timber.d(StringProvider.getString(
                    com.example.myapplication.R.string.log_loaded_sensor_delays,
                    accelerometerDelayMs,
                    gyroscopeDelayMs,
                    magnetometerDelayMs,
                    linearAccelerationDelayMs,
                    gravityDelayMs
                ))
                
                // Adjust sampling rates based on monitoring delay
                adjustSamplingRatesForDelay()
                
                // Initialize sensors
                accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
                gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
                magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
                linearAcceleration = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
                gravity = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
                
                // Update sensor status
                val status = SensorStatus(
                    accelerometer = accelerometer != null,
                    gyroscope = gyroscope != null,
                    magnetometer = magnetometer != null,
                    linearAcceleration = linearAcceleration != null,
                    gravity = gravity != null
                )
                _sensorStatus.value = status

                Timber.d(StringProvider.getString(
                    com.example.myapplication.R.string.log_sensor_availability,
                    status.accelerometer,
                    status.gyroscope,
                    status.magnetometer,
                    status.linearAcceleration,
                    status.gravity
                ))
                logInit("availability acc=${status.accelerometer} gyr=${status.gyroscope} mag=${status.magnetometer} lin=${status.linearAcceleration} grv=${status.gravity}")
                
                if (!status.accelerometer) Timber.w(StringProvider.getString(com.example.myapplication.R.string.log_accelerometer_not_available))
                if (!status.gyroscope) Timber.w(StringProvider.getString(com.example.myapplication.R.string.log_gyroscope_not_available))
                if (!status.magnetometer) Timber.w(StringProvider.getString(com.example.myapplication.R.string.log_magnetometer_not_available))

                // Initialize visual registration states: available -> INITIALIZING, unavailable -> ERROR
                _registrationStates.value = SensorRegistrationStates(
                    accelerometer = if (status.accelerometer) RegistrationState.INITIALIZING else RegistrationState.ERROR,
                    gyroscope = if (status.gyroscope) RegistrationState.INITIALIZING else RegistrationState.ERROR,
                    magnetometer = if (status.magnetometer) RegistrationState.INITIALIZING else RegistrationState.ERROR,
                    linearAcceleration = if (status.linearAcceleration) RegistrationState.INITIALIZING else RegistrationState.ERROR,
                    gravity = if (status.gravity) RegistrationState.INITIALIZING else RegistrationState.ERROR
                )
                
                // Register listeners with current sampling rate on main thread
                withContext(Dispatchers.Main) {
                    registerSensors()
                }
                
                isMonitoring = true
                
                // Reset static detector
                staticDetector.reset()
                isStatic = false
                isLongStatic = false
                
                Timber.d(StringProvider.getString(com.example.myapplication.R.string.log_monitoring_started, isMonitoring))
                val tookMs = System.currentTimeMillis() - initStartMs
                logInit("monitoring started, took ${tookMs}ms from start")

                // Snapshot after 5s to see registration end-states when initialization stalls
                CoroutineScope(Dispatchers.Main).launch {
                    delay(5000)
                    val rs = _registrationStates.value
                    logInit("5s snapshot: ACC=${rs.accelerometer} GYR=${rs.gyroscope} MAG=${rs.magnetometer} LIN=${rs.linearAcceleration} GRV=${rs.gravity}")
                }
            } catch (e: Exception) {
                Timber.e(e, StringProvider.getString(com.example.myapplication.R.string.log_error_starting_monitoring))
                logInit("exception during startMonitoring: ${e.message}")
            }
        }
    }

    /**
     * Enables synthetic (fallback) mode that generates plausible sensor data
     * when real sensors cannot be registered (e.g., security restrictions).
     */
    fun enableSyntheticMode(reason: String = "") {
        if (syntheticModeEnabled) return
        syntheticModeEnabled = true
        logInit("Enabling synthetic mode${if (reason.isNotBlank()) ": $reason" else ""}")
        pushDiag("Synthetic mode enabled: $reason")

        // Stop any real registrations
        unregisterSensors()
        cancelRegistrationRetry()

        // Mark sensors as virtually available/ready for UI to proceed
        _sensorStatus.value = SensorStatus(
            accelerometer = true,
            gyroscope = true,
            magnetometer = true,
            linearAcceleration = true,
            gravity = true
        )
        _registrationStates.value = SensorRegistrationStates(
            accelerometer = RegistrationState.READY,
            gyroscope = RegistrationState.READY,
            magnetometer = RegistrationState.READY,
            linearAcceleration = RegistrationState.READY,
            gravity = RegistrationState.READY
        )

        // Start synthetic data loop
        syntheticJob?.cancel()
        syntheticJob = CoroutineScope(Dispatchers.Default).launch {
            val start = System.currentTimeMillis()
            var t = 0
            while (syntheticModeEnabled) {
                try {
                    // Simple synthetic signals
                    val ax = 0.02f * kotlin.math.sin(t / 20.0).toFloat()
                    val ay = 0.01f * kotlin.math.cos(t / 25.0).toFloat()
                    val az = 9.81f + 0.005f * kotlin.math.sin(t / 30.0).toFloat()
                    val gx = 0.001f * kotlin.math.sin(t / 18.0).toFloat()
                    val gy = 0.0015f * kotlin.math.cos(t / 22.0).toFloat()
                    val gz = 0.002f * kotlin.math.sin(t / 26.0).toFloat()
                    val mx = 30f + 0.1f * kotlin.math.sin(t / 40.0).toFloat()
                    val my = 5f + 0.1f * kotlin.math.cos(t / 35.0).toFloat()
                    val mz = -15f + 0.05f * kotlin.math.sin(t / 32.0).toFloat()

                    syntheticHeadingDeg = (syntheticHeadingDeg + 0.2f) % 360f

                    val combinedData = SensorData.Combined(
                        timestamp = System.currentTimeMillis(),
                        accelerometer = Vector3D(ax, ay, az),
                        gyroscope = Vector3D(gx, gy, gz),
                        magnetometer = Vector3D(mx, my, mz),
                        linearAcceleration = Vector3D(ax, ay, 0.0f),
                        gravity = Vector3D(0f, 9.81f, 0f),
                        orientation = Vector3D(0f, 0f, syntheticHeadingDeg),
                        heading = syntheticHeadingDeg
                    )
                    _sensorData.value = combinedData
                } catch (_: Exception) {
                }
                t += 1
                delay(50)
            }
        }
    }

    /**
     * Disables synthetic mode and attempts to go back to normal monitoring.
     */
    fun disableSyntheticMode() {
        if (!syntheticModeEnabled) return
        syntheticModeEnabled = false
        pushDiag("Synthetic mode disabled")
        syntheticJob?.cancel()
        syntheticJob = null
        // Leave current registration states as-is. Caller may re-register if needed
    }
    
    /**
     * Adjusts sampling rates based on the configured monitoring delay.
     */
    private fun adjustSamplingRatesForDelay() {
        // Convert monitoring delay to microseconds
        val delayUs = (monitoringDelayMs * 1000).toInt()
        
        // Adjust sampling rates based on delay
        when {
            delayUs <= 20000 -> { // 20ms or less - high frequency
                highSamplingRate = SensorManager.SENSOR_DELAY_GAME
                normalSamplingRate = SensorManager.SENSOR_DELAY_UI
                lowSamplingRate = SensorManager.SENSOR_DELAY_NORMAL
                veryLowSamplingRate = 500000
            }
            delayUs <= 60000 -> { // 60ms or less - normal frequency
                highSamplingRate = SensorManager.SENSOR_DELAY_UI
                normalSamplingRate = SensorManager.SENSOR_DELAY_NORMAL
                lowSamplingRate = 200000 // 200ms
                veryLowSamplingRate = 1000000 // 1 second
            }
            delayUs <= 200000 -> { // 200ms or less - low frequency
                highSamplingRate = SensorManager.SENSOR_DELAY_NORMAL
                normalSamplingRate = 200000 // 200ms
                lowSamplingRate = 500000 // 500ms
                veryLowSamplingRate = 2000000 // 2 seconds
            }
            else -> { // 200ms+ - very low frequency
                highSamplingRate = 200000 // 200ms
                normalSamplingRate = 500000 // 500ms
                lowSamplingRate = 1000000 // 1 second
                veryLowSamplingRate = 5000000 // 5 seconds
            }
        }
        
        Timber.d(StringProvider.getString(
            com.example.myapplication.R.string.log_adjusted_sampling_rates,
            monitoringDelayMs,
            highSamplingRate,
            normalSamplingRate,
            lowSamplingRate,
            veryLowSamplingRate
        ))
    }
    
    /**
     * Stops sensor monitoring.
     */
    fun stopMonitoring() {
        if (!isMonitoring) {
            return
        }
        
        Timber.d(StringProvider.getString(com.example.myapplication.R.string.log_stopping_sensor_monitoring))
        
        // Unregister listeners
        unregisterSensors()
        cancelRegistrationRetry()
        syntheticModeEnabled = false
        syntheticJob?.cancel()
        syntheticJob = null
        
        isMonitoring = false
    }
    
    /**
     * Registers sensor listeners with individual sampling rates for each sensor.
     */
    private fun registerSensors() {
        Timber.d(StringProvider.getString(com.example.myapplication.R.string.log_registering_sensors))
        if (isRegistering) {
            pushDiag("Registration already in progress. Skipping re-entry.")
            return
        }
        isRegistering = true
        var registeredCount = 0

        fun tryRegister(
            sensor: Sensor?,
            name: String,
            primaryDelayMs: Long
        ): Boolean {
            pushDiag("Attempt register $name primaryDelayMs=$primaryDelayMs useHandler=${!forceNoHandlerRegistration}")
            if (sensor == null) {
                pushDiag("$name unavailable (getDefaultSensor returned null)")
                return false
            }
            val primaryUs = getSamplingPeriodUsForDelay(primaryDelayMs)
            val usCandidates = linkedSetOf(
                primaryUs,
                200000,  // NORMAL ~200ms
                60000,   // UI ~60ms
                20000,   // GAME ~20ms
                0        // FASTEST
            )
            val constantCandidates = intArrayOf(
                SensorManager.SENSOR_DELAY_GAME,
                SensorManager.SENSOR_DELAY_UI,
                SensorManager.SENSOR_DELAY_NORMAL,
                SensorManager.SENSOR_DELAY_FASTEST
            )

            if (forceNoHandlerRegistration) {
                pushDiag("Force no-handler mode active for $name")
                // First, try 4-arg no-handler overload with maxReportLatencyUs=0
                for (periodUs in usCandidates) {
                    val ok = sensorManager.registerListener(this, sensor, periodUs, 0)
                    pushDiag("No-handler(4-arg) $name periodUs=$periodUs, maxLatencyUs=0 -> ${if (ok) "success" else "fail"}")
                    if (ok) return true
                }
                // Then, try 3-arg constants
                for (constant in constantCandidates) {
                    val ok = sensorManager.registerListener(this, sensor, constant)
                    pushDiag("No-handler(3-arg) $name delayConst=$constant -> ${if (ok) "success" else "fail"}")
                    if (ok) return true
                }
            } else {
                // Preferred: Handler overload with microsecond periods
                for (periodUs in usCandidates) {
                    val ok = sensorManager.registerListener(this, sensor, periodUs, handler)
                    pushDiag("Handler $name periodUs=$periodUs -> ${if (ok) "success" else "fail"}")
                    if (ok) return true
                }
                // Fallback: 4-arg no-handler with maxReportLatencyUs=0
                for (periodUs in usCandidates) {
                    val ok = sensorManager.registerListener(this, sensor, periodUs, 0)
                    pushDiag("Fallback no-handler(4-arg) $name periodUs=$periodUs, maxLatencyUs=0 -> ${if (ok) "success" else "fail"}")
                    if (ok) return true
                }
                // Fallback: 3-arg constants
                for (constant in constantCandidates) {
                    val ok = sensorManager.registerListener(this, sensor, constant)
                    pushDiag("Fallback no-handler(3-arg) $name delayConst=$constant -> ${if (ok) "success" else "fail"}")
                    if (ok) return true
                }
            }
            pushDiag("$name registration failed for all periods (handler and no-handler). Possible causes: sensor disabled by OS/power policy, device constraints, or hardware issue.")
            return false
        }

        // Accelerometer
        if (tryRegister(accelerometer, "Accelerometer", accelerometerDelayMs)) {
            registeredCount++
            accelerometerRegisterFailedOnce = false
            Timber.d(StringProvider.getString(com.example.myapplication.R.string.log_accelerometer_registered, accelerometerDelayMs))
            logInit("Accelerometer READY (delayMs=$accelerometerDelayMs)")
            _registrationStates.value = _registrationStates.value.copy(accelerometer = RegistrationState.READY)
        } else {
            if (!accelerometerRegisterFailedOnce) {
                Timber.w(StringProvider.getString(com.example.myapplication.R.string.log_failed_register_accelerometer))
                accelerometerRegisterFailedOnce = true
            } else {
                Timber.w(StringProvider.getString(com.example.myapplication.R.string.log_accelerometer_still_failing))
            }
            logInit("Accelerometer ERROR (all attempts failed)")
            _registrationStates.value = _registrationStates.value.copy(accelerometer = RegistrationState.ERROR)
        }

        // Gyroscope
        if (tryRegister(gyroscope, "Gyroscope", gyroscopeDelayMs)) {
            registeredCount++
            gyroscopeRegisterFailedOnce = false
            Timber.d(StringProvider.getString(com.example.myapplication.R.string.log_gyroscope_registered, gyroscopeDelayMs))
            logInit("Gyroscope READY (delayMs=$gyroscopeDelayMs)")
            _registrationStates.value = _registrationStates.value.copy(gyroscope = RegistrationState.READY)
        } else {
            if (!gyroscopeRegisterFailedOnce) {
                Timber.w(StringProvider.getString(com.example.myapplication.R.string.log_failed_register_gyroscope))
                gyroscopeRegisterFailedOnce = true
            } else {
                Timber.w(StringProvider.getString(com.example.myapplication.R.string.log_gyroscope_still_failing))
            }
            logInit("Gyroscope ERROR (all attempts failed)")
            _registrationStates.value = _registrationStates.value.copy(gyroscope = RegistrationState.ERROR)
        }

        // Magnetometer
        if (tryRegister(magnetometer, "Magnetometer", magnetometerDelayMs)) {
            registeredCount++
            magnetometerRegisterFailedOnce = false
            Timber.d(StringProvider.getString(com.example.myapplication.R.string.log_magnetometer_registered, magnetometerDelayMs))
            logInit("Magnetometer READY (delayMs=$magnetometerDelayMs)")
            _registrationStates.value = _registrationStates.value.copy(magnetometer = RegistrationState.READY)
        } else {
            if (!magnetometerRegisterFailedOnce) {
                Timber.w(StringProvider.getString(com.example.myapplication.R.string.log_failed_register_magnetometer))
                magnetometerRegisterFailedOnce = true
            } else {
                Timber.w(StringProvider.getString(com.example.myapplication.R.string.log_magnetometer_still_failing))
            }
            logInit("Magnetometer ERROR (all attempts failed)")
            _registrationStates.value = _registrationStates.value.copy(magnetometer = RegistrationState.ERROR)
        }

        // Linear Acceleration
        if (tryRegister(linearAcceleration, "LinearAcceleration", linearAccelerationDelayMs)) {
            registeredCount++
            linearAccelerationRegisterFailedOnce = false
            Timber.d(StringProvider.getString(com.example.myapplication.R.string.log_linear_acceleration_registered, linearAccelerationDelayMs))
            logInit("LinearAcceleration READY (delayMs=$linearAccelerationDelayMs)")
            _registrationStates.value = _registrationStates.value.copy(linearAcceleration = RegistrationState.READY)
        } else {
            if (!linearAccelerationRegisterFailedOnce) {
                Timber.w(StringProvider.getString(com.example.myapplication.R.string.log_failed_register_linear_acceleration))
                linearAccelerationRegisterFailedOnce = true
            } else {
                Timber.w(StringProvider.getString(com.example.myapplication.R.string.log_linear_acceleration_still_failing))
            }
            logInit("LinearAcceleration ERROR (all attempts failed)")
            _registrationStates.value = _registrationStates.value.copy(linearAcceleration = RegistrationState.ERROR)
        }

        // Gravity
        if (tryRegister(gravity, "Gravity", gravityDelayMs)) {
            registeredCount++
            gravityRegisterFailedOnce = false
            Timber.d(StringProvider.getString(com.example.myapplication.R.string.log_gravity_registered, gravityDelayMs))
            logInit("Gravity READY (delayMs=$gravityDelayMs)")
            _registrationStates.value = _registrationStates.value.copy(gravity = RegistrationState.READY)
        } else {
            if (!gravityRegisterFailedOnce) {
                Timber.w(StringProvider.getString(com.example.myapplication.R.string.log_failed_register_gravity))
                gravityRegisterFailedOnce = true
            } else {
                Timber.w(StringProvider.getString(com.example.myapplication.R.string.log_gravity_still_failing))
            }
            logInit("Gravity ERROR (all attempts failed)")
            _registrationStates.value = _registrationStates.value.copy(gravity = RegistrationState.ERROR)
        }

        if (registeredCount == 0) {
            scheduleRegistrationRetry()
        } else {
            cancelRegistrationRetry()
        }

        Timber.d(StringProvider.getString(com.example.myapplication.R.string.log_sensor_registration_complete, registeredCount))
        val rs = _registrationStates.value
        logInit("registration complete: count=$registeredCount ACC=${rs.accelerometer} GYR=${rs.gyroscope} MAG=${rs.magnetometer} LIN=${rs.linearAcceleration} GRV=${rs.gravity}")
        isRegistering = false
    }
    
    /**
     * Gets the appropriate sampling period in microseconds for a given delay in ms.
     * NOTE: registerListener with Handler expects microseconds, not SENSOR_DELAY_* constants.
     */
    private fun getSamplingPeriodUsForDelay(delayMs: Long): Int {
        val delayUs = (delayMs * 1000).toInt()
        return when {
            delayUs <= 20000 -> 20000        // GAME ~20ms
            delayUs <= 60000 -> 60000        // UI ~60ms
            delayUs <= 200000 -> 200000      // NORMAL ~200ms
            else -> 500000                   // very low ~500ms
        }
    }
    
    /**
     * Unregisters all sensor listeners.
     */
    private fun unregisterSensors() {
        // Cancel pending adjustments to avoid re-entrancy
        handler.removeCallbacksAndMessages(null)
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
            Timber.d(StringProvider.getString(com.example.myapplication.R.string.log_static_listener_added, staticStateListeners.size))
            
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
            Timber.d(StringProvider.getString(com.example.myapplication.R.string.log_static_listener_removed, staticStateListeners.size))
        }
    }
    
    /**
     * Gets the current sensor status.
     * 
     * @return Current sensor status
     */
    fun getSensorStatus(): SensorStatus {
        return _sensorStatus.value
    }
    
    /**
     * Checks if a specific sensor is available.
     * 
     * @param sensorType The sensor type to check
     * @return True if the sensor is available, false otherwise
     */
    fun isSensorAvailable(sensorType: Int): Boolean {
        return when (sensorType) {
            Sensor.TYPE_ACCELEROMETER -> _sensorStatus.value.accelerometer
            Sensor.TYPE_GYROSCOPE -> _sensorStatus.value.gyroscope
            Sensor.TYPE_MAGNETIC_FIELD -> _sensorStatus.value.magnetometer
            Sensor.TYPE_LINEAR_ACCELERATION -> _sensorStatus.value.linearAcceleration
            Sensor.TYPE_GRAVITY -> _sensorStatus.value.gravity
            else -> false
        }
    }
    
    /**
     * Gets the confidence level for positioning based on available sensors.
     * This can be used by other components to adjust their behavior based on sensor availability.
     * 
     * @return Confidence level between 0.0 (no sensors available) and 1.0 (all sensors available)
     */
    fun getPositioningConfidence(): Float {
        val status = _sensorStatus.value
        
        // Calculate confidence based on available sensors
        var confidence = 0.0f
        
        // Accelerometer is critical for step detection
        if (status.accelerometer) confidence += 0.4f
        
        // Gyroscope is important for heading estimation
        if (status.gyroscope) confidence += 0.3f
        
        // Magnetometer is important for absolute heading
        if (status.magnetometer) confidence += 0.3f
        
        // Linear acceleration and gravity are less critical
        if (status.linearAcceleration) confidence += 0.1f
        if (status.gravity) confidence += 0.1f
        
        // Normalize to 0.0-1.0 range (max 1.0 even if all sensors are available)
        return (confidence).coerceIn(0.0f, 1.0f)
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
                Timber.e(StringProvider.getString(com.example.myapplication.R.string.log_error_notifying_static_listener, e.message ?: "unknown"))
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
            Timber.d(StringProvider.getString(com.example.myapplication.R.string.log_forced_sampling_rate, samplingPeriodUs))
        }
        
        if (isMonitoring) {
            unregisterSensors()
            registerSensors()
        }
    }
    
    /**
     * Adjusts the sensor sampling rate based on the device's static state and power mode.
     * This helps reduce battery consumption when the device is not moving or when in low-power mode.
     */
    private fun adjustSamplingRate() {
        // Determine base sampling rate based on movement state
        val baseRate = when {
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
        
        // Apply low-power mode adjustment if active
        val adjustedRate = if (isLowPowerModeActive) {
            // Increase sampling period (reduce frequency) by the reduction factor
            (baseRate * samplingRateReductionFactor).toInt()
        } else {
            baseRate
        }
        
        // Only change sampling rate if it's different from current rate
        if (adjustedRate != currentSamplingRate) {
            Timber.d(StringProvider.getString(
                com.example.myapplication.R.string.log_adjust_sampling_rate,
                currentSamplingRate,
                adjustedRate,
                isStatic,
                isLongStatic,
                isLowPowerModeActive
            ))
            
            currentSamplingRate = adjustedRate
            
            // Apply the new sampling rate
            unregisterSensors()
            registerSensors()
        }
    }
    
    // SensorEventListener implementation
    override fun onSensorChanged(event: SensorEvent) {
        // Log sensor data reception for debugging
        Timber.d(StringProvider.getString(
            com.example.myapplication.R.string.log_sensor_data_received,
            event.sensor.name,
            event.sensor.type,
            event.values.joinToString(", ")
        ))
        
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                accelerometerData = Vector3D(
                    event.values[0],
                    event.values[1],
                    event.values[2]
                )
                
                // Store valid data for fallback
                lastValidAccelerometerData = accelerometerData
                
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
                
                // Store valid data for fallback
                lastValidGyroscopeData = gyroscopeData
                
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
                
                // Store valid data for fallback
                lastValidMagnetometerData = magnetometerData
                
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
                
                // Store valid data for fallback
                lastValidLinearAccelerationData = linearAccelerationData
            }
            Sensor.TYPE_GRAVITY -> {
                gravityData = Vector3D(
                    event.values[0],
                    event.values[1],
                    event.values[2]
                )
                
                // Store valid data for fallback
                lastValidGravityData = gravityData
                
                // Update orientation when gravity data changes
                updateOrientation()
            }
        }
        
        // Get sensor status
        val status = _sensorStatus.value
        
        // Create combined sensor data with fallbacks for unavailable sensors
        val combinedData = SensorData.Combined(
            timestamp = System.currentTimeMillis(),
            accelerometer = if (status.accelerometer) accelerometerData else lastValidAccelerometerData,
            gyroscope = if (status.gyroscope) gyroscopeData else lastValidGyroscopeData,
            magnetometer = if (status.magnetometer) magnetometerData else lastValidMagnetometerData,
            linearAcceleration = if (status.linearAcceleration) linearAccelerationData else lastValidLinearAccelerationData,
            gravity = if (status.gravity) gravityData else lastValidGravityData,
            orientation = calculateOrientation(),
            heading = calculateHeading()
        )
        
        // Log when using fallback data
        if (!status.accelerometer || !status.gyroscope || !status.magnetometer) {
            val missing = buildString {
                if (!status.accelerometer) append("accelerometer ")
                if (!status.gyroscope) append("gyroscope ")
                if (!status.magnetometer) append("magnetometer ")
            }.trim()
            Timber.d(StringProvider.getString(com.example.myapplication.R.string.log_using_fallback_for, missing))
        }
        
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
            
            Timber.d(StringProvider.getString(
                com.example.myapplication.R.string.log_static_state_changed,
                isStatic,
                isLongStatic,
                staticConfidence,
                (staticDuration / 1000).toInt()
            ))
            
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
            Timber.d(StringProvider.getString(
                com.example.myapplication.R.string.log_device_position,
                devicePositionDetector.getCurrentPosition().toString(),
                devicePositionDetector.getPositionConfidence()
            ))
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
