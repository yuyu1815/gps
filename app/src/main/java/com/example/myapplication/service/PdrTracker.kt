package com.example.myapplication.service

import com.example.myapplication.data.repository.IPositionRepository
import com.example.myapplication.domain.model.SensorData
import com.example.myapplication.domain.model.UserPosition
import com.example.myapplication.domain.usecase.pdr.DetectStepUseCase
import com.example.myapplication.domain.usecase.pdr.EstimateHeadingUseCase
import com.example.myapplication.domain.usecase.pdr.EstimateStepLengthUseCase
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
    private val estimateHeadingUseCase: EstimateHeadingUseCase,
    private val estimateStepLengthUseCase: EstimateStepLengthUseCase,
    private val updatePdrPositionUseCase: UpdatePdrPositionUseCase,
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
        estimateStepLengthUseCase.reset()
        updatePdrPositionUseCase.reset()
        lastStepTimestamp = 0L
        stepCount = 0
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

        // Convert to accelerometer data for step detection
        val accelerometerData = SensorData.Accelerometer(
            x = sensorData.accelerometer.x,
            y = sensorData.accelerometer.y,
            z = sensorData.accelerometer.z,
            timestamp = sensorData.timestamp
        )

        // Detect step
        val stepResult = detectStepUseCase.invoke(
            DetectStepUseCase.Params(accelerometerData)
        )

        // Update step count
        if (stepResult.stepDetected) {
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

            // Get heading
            val heading = sensorData.heading

            // Update position
            val newPosition = updatePdrPositionUseCase.invoke(
                UpdatePdrPositionUseCase.Params(
                    stepDetected = true,
                    stepLength = stepLength,
                    heading = heading,
                    initialPosition = _pdrState.value.initialPosition,
                    updateRepository = true
                )
            )

            // Update PDR state
            _pdrState.value = _pdrState.value.copy(
                stepCount = stepCount,
                lastStepTimestamp = lastStepTimestamp,
                currentHeading = heading,
                lastStepLength = stepLength,
                currentPosition = newPosition,
                isMoving = true
            )
        } else {
            // Check if user has stopped moving
            val isMoving = (System.currentTimeMillis() - lastStepTimestamp) < MOVEMENT_TIMEOUT_MS

            // Update PDR state without position change
            _pdrState.value = _pdrState.value.copy(
                currentHeading = sensorData.heading,
                isMoving = isMoving
            )
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