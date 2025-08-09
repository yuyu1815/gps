package com.example.myapplication.presentation.viewmodel

import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.repository.IMapRepository
import com.example.myapplication.data.repository.IPositionRepository
import com.example.myapplication.domain.model.UserPosition as DomainUserPosition
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import com.example.myapplication.service.SensorMonitor
import com.example.myapplication.service.PdrTracker
import com.example.myapplication.wifi.WifiScanner
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlinx.coroutines.CancellationException

/**
 * ViewModel for the map screen of the application.
 * Handles map display, user position, and interaction with the map.
 */
class MapViewModel(
    private val mapRepository: IMapRepository,
    private val positionRepository: IPositionRepository,
    private val sensorMonitor: SensorMonitor,
    private val pdrTracker: PdrTracker,
    private val wifiScanner: WifiScanner
) : BaseViewModel<MapViewModel.MapUiState>(), KoinComponent {

    private val _mapInteractionState = MutableStateFlow<MapInteractionState>(MapInteractionState.Idle)
    val mapInteractionState: StateFlow<MapInteractionState> = _mapInteractionState.asStateFlow()

    // Position change detection
    private val positionChangeDetector = positionRepository.getPositionChangeDetector()
    private val _isMoving = MutableStateFlow(false)
    val isMoving: StateFlow<Boolean> = _isMoving.asStateFlow()
    
    private val _lastMovementDirection = MutableStateFlow<Float?>(null)
    val lastMovementDirection: StateFlow<Float?> = _lastMovementDirection.asStateFlow()
    
    private val _movementDistance = MutableStateFlow(0f)
    val movementDistance: StateFlow<Float> = _movementDistance.asStateFlow()
    
    init {
        Timber.d("MapViewModel initialized")
        updateState(MapUiState())
        
        // 軽量な初期化を即座に実行
        startAsyncDataCollection()
        
        // 重い初期化処理を非同期で実行
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Start observing position changes
                positionChangeDetector.isMovingFlow.collect { isMoving: Boolean ->
                    withContext(Dispatchers.Main) {
                        _isMoving.value = isMoving
                        Timber.d("Movement state changed: $isMoving")
                    }
                }
            } catch (e: CancellationException) {
                // normal on navigation/VM clear; ignore
            } catch (e: Exception) {
                Timber.e(e, "Error observing movement state")
            }
        }
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                positionChangeDetector.lastMovementDirection.collect { direction: Float? ->
                    withContext(Dispatchers.Main) {
                        _lastMovementDirection.value = direction
                        if (direction != null) {
                            Timber.d("Movement direction: ${direction}°")
                        }
                    }
                }
            } catch (e: CancellationException) {
                // ignore
            } catch (e: Exception) {
                Timber.e(e, "Error observing movement direction")
            }
        }
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                positionChangeDetector.movementDistance.collect { distance: Float ->
                    withContext(Dispatchers.Main) {
                        _movementDistance.value = distance
                        if (distance > 0f) {
                            Timber.d("Movement distance: ${distance}m")
                        }
                    }
                }
            } catch (e: CancellationException) {
                // ignore
            } catch (e: Exception) {
                Timber.e(e, "Error observing movement distance")
            }
        }
    }

    private fun startAsyncDataCollection() {
        // 位置情報の監視を非同期で開始
        viewModelScope.launch(Dispatchers.IO) {
            positionRepository.getCurrentPositionFlow().collectLatest { position ->
                withContext(Dispatchers.Main) {
                    updateUserPosition(position.x, position.y, position.confidence)
                    uiState.value?.let { current ->
                        val updatedDebug = current.debugData.copy(
                            fusionConfidence = position.confidence,
                            positioningError = position.accuracy
                        )
                        updateState(current.copy(debugData = updatedDebug))
                    }
                }
            }
        }

        // センサーデータの監視を非同期で開始
        viewModelScope.launch(Dispatchers.IO) {
            sensorMonitor.sensorData.collectLatest { data ->
                withContext(Dispatchers.Main) {
                    uiState.value?.let { current ->
                        val updatedDebug = current.debugData.copy(
                            accelerometerValues = listOf(data.accelerometer.x, data.accelerometer.y, data.accelerometer.z),
                            gyroscopeValues = listOf(data.gyroscope.x, data.gyroscope.y, data.gyroscope.z),
                            magnetometerValues = listOf(data.magnetometer.x, data.magnetometer.y, data.magnetometer.z)
                        )
                        updateState(current.copy(debugData = updatedDebug))
                    }
                }
            }
        }

        // PDRトラッカー状態の監視を非同期で開始
        viewModelScope.launch(Dispatchers.IO) {
            pdrTracker.pdrState.collectLatest { state ->
                withContext(Dispatchers.Main) {
                    uiState.value?.let { current ->
                        val updatedDebug = current.debugData.copy(
                            pdrStepCount = state.stepCount,
                            pdrHeading = state.currentHeading,
                            pdrStepLength = state.lastStepLength
                        )
                        updateState(current.copy(debugData = updatedDebug))
                    }
                }
            }
        }

        // Wi-Fiスキャン結果の監視を非同期で開始
        viewModelScope.launch(Dispatchers.IO) {
            wifiScanner.accessPoints.collectLatest { results ->
                withContext(Dispatchers.Main) {
                    uiState.value?.let { current ->
                        val avg = results.mapNotNull { it.distance }.let { if (it.isNotEmpty()) it.average().toFloat() else 0f }
                        val usedAPs = results.count { it.rssi >= -85 } // Count APs with good signal
                        val matchingRate = if (results.isNotEmpty()) {
                            // Calculate matching rate based on signal strength distribution
                            val strongSignals = results.count { it.rssi >= -70 }
                            (strongSignals.toFloat() / results.size) * 100f
                        } else 0f
                        
                        val updatedDebug = current.debugData.copy(
                            wifiSignalStrength = if (results.isNotEmpty()) results.map { it.rssi }.average().toFloat() else 0f,
                            wifiAccessPointCount = results.size,
                            wifiPositionAccuracy = avg,
                            wifiMatchingRate = matchingRate,
                            wifiUsedAPs = usedAPs
                        )
                        updateState(current.copy(debugData = updatedDebug))
                    }
                }
            }
        }
    }

    /**
     * Updates the user position on the map.
     */
    fun updateUserPosition(x: Float, y: Float, confidence: Float) {
        viewModelScope.launch(Dispatchers.Main) {
            uiState.value?.let {
                val newPosition = UserPosition(x, y, confidence)
                Timber.d("Updating user position to $newPosition")
                // Update debug data with the latest sensor and algorithm info
                val newDebugData = it.debugData.copy(
                    wifiSignalStrength = (Math.random() * -50 - 30).toFloat(), // Simulated
                    slamStatus = "Tracking", // Simulated
                    pdrStepCount = it.debugData.pdrStepCount + 1 // Simulated
                )
                updateState(it.copy(userPosition = newPosition, debugData = newDebugData))
            }
        }
    }

    /**
     * Updates the map zoom level.
     */
    fun updateZoomLevel(zoomLevel: Float) {
        viewModelScope.launch(Dispatchers.Main) {
            uiState.value?.let {
                Timber.d("Updating zoom level to $zoomLevel")
                updateState(it.copy(zoomLevel = zoomLevel))
            }
        }
    }

    /**
     * Updates the map center position.
     */
    fun updateMapCenter(x: Float, y: Float) {
        viewModelScope.launch(Dispatchers.Main) {
            uiState.value?.let {
                val newCenter = MapPosition(x, y)
                Timber.d("Updating map center to $newCenter")
                updateState(it.copy(mapCenter = newCenter))
            }
        }
    }

    /**
     * Sets the map interaction state.
     */
    fun setMapInteractionState(state: MapInteractionState) {
        viewModelScope.launch(Dispatchers.Main) {
            Timber.d("Setting map interaction state to $state")
            _mapInteractionState.value = state
        }
    }

    /**
     * Toggles the display of the user's path on the map.
     */
    fun togglePathDisplay() {
        viewModelScope.launch(Dispatchers.Main) {
            uiState.value?.let {
                val newState = !it.showPath
                Timber.d("Toggling path display to $newState")
                updateState(it.copy(showPath = newState))
            }
        }
    }

    /**
     * Toggles the display of the debug overlay.
     */
    fun toggleDebugOverlay() {
        viewModelScope.launch(Dispatchers.Main) {
            uiState.value?.let {
                val newState = !it.showDebugOverlay
                Timber.d("Toggling debug overlay to $newState")
                updateState(it.copy(showDebugOverlay = newState))
            }
        }
    }

    /**
     * Represents the UI state for the map screen.
     */
    data class MapUiState(
        val userPosition: UserPosition = UserPosition(0f, 0f, 0f),
        val mapCenter: MapPosition = MapPosition(0f, 0f),
        val zoomLevel: Float = 1.0f,
        val showPath: Boolean = true,
        val showDebugOverlay: Boolean = false,
        val pathPoints: List<UserPosition> = emptyList(),
        val debugData: DebugData = DebugData()
    )

    /**
     * Represents a position on the map.
     */
    data class MapPosition(val x: Float, val y: Float)

    /**
     * Represents the user's position on the map with confidence level.
     */
    data class UserPosition(val x: Float, val y: Float, val confidence: Float)

    /**
     * Holds real-time data for the debug overlay.
     */
    data class DebugData(
        // Wi-Fi data
        val wifiSignalStrength: Float = 0f,
        val wifiAccessPointCount: Int = 0,
        val wifiPositionAccuracy: Float = 0f,
        val wifiMatchingRate: Float = 0f, // New: WiFi fingerprint matching rate
        val wifiUsedAPs: Int = 0, // New: Number of APs used for positioning
        
        // SLAM data
        val slamStatus: String = "Initializing",
        val slamFeatureCount: Int = 0,
        val slamConfidence: Float = 0f,
        
        // PDR data
        val pdrStepCount: Int = 0,
        val pdrHeading: Float = 0f,
        val pdrStepLength: Float = 0f,
        
        // Sensor fusion data
        val fusionMethod: String = "EKF",
        val fusionConfidence: Float = 0f,
        val positioningError: Float = 0f,
        
        // Device sensors
        val accelerometerValues: List<Float> = listOf(0f, 0f, 0f),
        val gyroscopeValues: List<Float> = listOf(0f, 0f, 0f),
        val magnetometerValues: List<Float> = listOf(0f, 0f, 0f),
        
        // System status
        val batteryUsage: Float = 0f,
        val cpuUsage: Float = 0f,
        val updateFrequency: Float = 0f
    )

    /**
     * Represents the possible map interaction states.
     */
    sealed class MapInteractionState {
        object Idle : MapInteractionState()
        object Panning : MapInteractionState()
        object Zooming : MapInteractionState()
    }
}