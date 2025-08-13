package com.example.myapplication.service

import com.example.myapplication.data.repository.IPositionRepository
import com.example.myapplication.domain.model.Motion
import com.example.myapplication.domain.model.SensorData
import com.example.myapplication.domain.model.UserPosition
import com.example.myapplication.domain.usecase.fusion.FuseSensorDataUseCase
import com.example.myapplication.domain.usecase.pdr.DetectStepUseCase
import com.example.myapplication.domain.usecase.pdr.EstimateHeadingUseCase
import com.example.myapplication.domain.usecase.pdr.EstimateStepLengthUseCase
import com.example.myapplication.domain.usecase.pdr.KalmanHeadingUseCase
import com.example.myapplication.domain.usecase.pdr.UpdatePdrPositionUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Coordinates the Pedestrian Dead Reckoning (PDR) components to track user position.
 * This class integrates step detection, heading estimation, step length estimation,
 * and position updates to provide continuous position tracking.
 */
class PdrTracker(
    private val sensorMonitor: SensorMonitor,
    private val positionRepository: IPositionRepository,
    private val detectStepUseCase: DetectStepUseCase,
    private val estimateHeadingUseCase: KalmanHeadingUseCase,
    private val estimateStepLengthUseCase: EstimateStepLengthUseCase,
    private val updatePdrPositionUseCase: UpdatePdrPositionUseCase,
    private val fuseSensorDataUseCase: FuseSensorDataUseCase,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {
    // PDR state
    private val _pdrState = MutableStateFlow(PdrState())
    val pdrState: StateFlow<PdrState> = _pdrState

    // User settings
    private var userHeight = 1.75f // Default height in meters
    private var stepLengthCalibrationFactor = 1.0f // Default calibration factor

    // Tracking state
    private var isTracking = false
    private var lastStepTimestamp = 0L
    private var stepCount = 0
    private var lastHeading: Float? = null

    /**
     * Starts PDR tracking.
     */
    fun startTracking() {
        if (isTracking) {
            Timber.d("PDR tracking already in progress")
            return
        }

        Timber.d("Starting PDR tracking")
        isTracking = true

        // Start sensor monitoring if not already started
        sensorMonitor.startMonitoring()

        // Reset state
        resetState()

        // Start collecting sensor data
        coroutineScope.launch {
            sensorMonitor.sensorData.collect { sensorData ->
                processSensorData(sensorData)
            }
        }
    }

    /**
     * Stops PDR tracking.
     */
    fun stopTracking() {
        if (!isTracking) {
            return
        }

        Timber.d("Stopping PDR tracking")
        isTracking = false

        // Note: We don't stop sensor monitoring here as other components might be using it
    }

    /**
     * Sets the user's height for step length estimation.
     */
    fun setUserHeight(height: Float) {
        userHeight = height
        Timber.d("User height set to $userHeight meters")
    }

    /**
     * Sets the calibration factor for step length estimation.
     */
    fun setStepLengthCalibrationFactor(factor: Float) {
        stepLengthCalibrationFactor = factor
        Timber.d("Step length calibration factor set to $stepLengthCalibrationFactor")
    }

    /**
     * Resets the PDR tracking state.
     */
    fun resetState() {
        detectStepUseCase.reset()
        estimateHeadingUseCase.reset()
        estimateStepLengthUseCase.reset()
        updatePdrPositionUseCase.reset()
        lastStepTimestamp = 0L
        stepCount = 0
        lastHeading = null
        _pdrState.value = PdrState()
    }

    /**
     * Sets the initial position for PDR tracking.
     */
    fun setInitialPosition(position: UserPosition) {
        _pdrState.value = _pdrState.value.copy(
            currentPosition = position,
            initialPosition = position
        )
        Timber.d("Initial position set to (${position.x}, ${position.y})")
    }

    /**
     * Processes new sensor data for PDR tracking.
     */
    private suspend fun processSensorData(sensorData: SensorData.Combined) {
        if (!isTracking) {
            return
        }

        // Get sensor status and positioning confidence
        val sensorStatus = sensorMonitor.getSensorStatus()
        val positioningConfidence = sensorMonitor.getPositioningConfidence()
        
        // Log sensor availability for debugging
        if (positioningConfidence < 0.7f) {
            Timber.w("Reduced positioning confidence: $positioningConfidence. " +
                    "Available sensors: " +
                    "accelerometer=${sensorStatus.accelerometer}, " +
                    "gyroscope=${sensorStatus.gyroscope}, " +
                    "magnetometer=${sensorStatus.magnetometer}")
        }

        // Create SensorData objects for use cases
        val accelerometerData = SensorData.Accelerometer(
            x = sensorData.accelerometer.x,
            y = sensorData.accelerometer.y,
            z = sensorData.accelerometer.z,
            timestamp = sensorData.timestamp
        )
        val gyroscopeData = SensorData.Gyroscope(
            x = sensorData.gyroscope.x,
            y = sensorData.gyroscope.y,
            z = sensorData.gyroscope.z,
            timestamp = sensorData.timestamp
        )
        val magnetometerData = SensorData.Magnetometer(
            x = sensorData.magnetometer.x,
            y = sensorData.magnetometer.y,
            z = sensorData.magnetometer.z,
            timestamp = sensorData.timestamp
        )

        // Get heading with graceful degradation
        val headingResult = if (sensorStatus.gyroscope && (sensorStatus.accelerometer || sensorStatus.magnetometer)) {
            // We have enough sensors for heading estimation
            estimateHeadingUseCase.invoke(
                KalmanHeadingUseCase.Params(
                    gyroscopeData = gyroscopeData,
                    accelerometerData = accelerometerData,
                    magnetometerData = magnetometerData,
                    rotationVectorData = sensorData.rotationVector
                )
            )
        } else {
            // Not enough sensors for heading estimation, use last known heading
            Timber.w("Insufficient sensors for heading estimation, using last known heading")
            val lastHeadingValue = lastHeading ?: 0f
            KalmanHeadingUseCase.Result(
                heading = lastHeadingValue,
                headingRate = 0f,
                variance = 10f,
                headingAccuracy = EstimateHeadingUseCase.HeadingAccuracy.LOW,
                timestamp = sensorData.timestamp
            )
        }

        // Detect step with graceful degradation
        val stepResult = if (sensorStatus.accelerometer) {
            // We have accelerometer for step detection
            detectStepUseCase.invoke(
                DetectStepUseCase.Params(accelerometerData)
            )
        } else {
            // No accelerometer, can't detect steps
            Timber.w("Accelerometer unavailable, step detection disabled")
            DetectStepUseCase.Result(
                stepDetected = false,
                stepCount = stepCount,
                filteredAcceleration = 0f,
                timestamp = sensorData.timestamp
            )
        }

        // Update step count
        if (stepResult.stepDetected) {
            val timeDelta = (stepResult.timestamp - lastStepTimestamp) / 1000.0
            stepCount = stepResult.stepCount
            lastStepTimestamp = stepResult.timestamp

            // Estimate step length
            val stepLength = estimateStepLengthUseCase.invoke(
                EstimateStepLengthUseCase.Params(
                    sensorData = sensorData,
                    userHeight = userHeight,
                    calibrationFactor = stepLengthCalibrationFactor
                )
            )

            // Calculate PDR motion
            val velocity = if (timeDelta > 0) stepLength / timeDelta else 0.0
            val angularVelocity = if (lastHeading != null && timeDelta > 0) {
                (headingResult.heading - lastHeading!!) / timeDelta
            } else {
                0.0
            }
            lastHeading = headingResult.heading

            val pdrMotion = Motion(velocity = velocity, angularVelocity = angularVelocity)

            // TODO: Get actual SLAM motion and Wi-Fi position
            val slamMotion = Motion(0.0, 0.0)
            val wifiPosition = UserPosition.invalid()

            // Fuse data
            val fusedPosition = fuseSensorDataUseCase.invoke(
                FuseSensorDataUseCase.Params(
                    slamMotion = slamMotion,
                    wifiPosition = wifiPosition,
                    pdrMotion = pdrMotion
                )
            )

            // Update PDR state
            _pdrState.value = _pdrState.value.copy(
                stepCount = stepCount,
                lastStepTimestamp = lastStepTimestamp,
                currentHeading = headingResult.heading,
                lastStepLength = stepLength,
                currentPosition = fusedPosition, // Use fused position
                isMoving = true
            )
        } else {
            // Check if user has stopped moving
            val isMoving = (System.currentTimeMillis() - lastStepTimestamp) < MOVEMENT_TIMEOUT_MS

            // Update PDR state without position change
            _pdrState.value = _pdrState.value.copy(
                currentHeading = headingResult.heading,
                isMoving = isMoving
            )
            lastHeading = headingResult.heading
        }
    }

    /**
     * Represents the current state of PDR tracking.
     */
    data class PdrState(
        val stepCount: Int = 0,
        val lastStepTimestamp: Long = 0L,
        val currentHeading: Float = 0f,
        val lastStepLength: Float = 0f,
        val initialPosition: UserPosition? = null,
        val currentPosition: UserPosition? = null,
        val isMoving: Boolean = false
    )

    companion object {
        // Time without steps after which user is considered stationary
        private const val MOVEMENT_TIMEOUT_MS = 2000L
    }
}