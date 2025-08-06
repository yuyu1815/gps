package com.example.myapplication.presentation.viewmodel

import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.repository.IBeaconRepository
import com.example.myapplication.data.repository.IMapRepository
import com.example.myapplication.data.repository.IPositionRepository
import com.example.myapplication.domain.model.Beacon
import com.example.myapplication.domain.model.BeaconHealthStatus
import com.example.myapplication.domain.model.UserPosition as DomainUserPosition
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * ViewModel for the map screen of the application.
 * Handles map display, user position, beacon monitoring, and interaction with the map.
 */
class MapViewModel(
    private val mapRepository: IMapRepository,
    private val positionRepository: IPositionRepository,
    private val beaconRepository: IBeaconRepository
) : BaseViewModel<MapViewModel.MapUiState>() {

    private val _mapInteractionState = MutableStateFlow<MapInteractionState>(MapInteractionState.Idle)
    val mapInteractionState: StateFlow<MapInteractionState> = _mapInteractionState.asStateFlow()
    
    // Expose beacons flow for UI components
    val beacons = beaconRepository.getBeaconsFlow()
    
    // Track beacons with alerts
    private val _beaconsWithAlerts = MutableStateFlow<List<Beacon>>(emptyList())
    val beaconsWithAlerts: StateFlow<List<Beacon>> = _beaconsWithAlerts.asStateFlow()
    
    // Health monitoring interval in milliseconds
    private val HEALTH_MONITORING_INTERVAL = 10000L

    init {
        Timber.d("MapViewModel initialized with repositories")
        updateState(MapUiState())
        
        // Observe position updates
        viewModelScope.launch {
            positionRepository.getCurrentPositionFlow().collectLatest { position ->
                updateUserPosition(position.x, position.y, position.confidence)
            }
        }
        
        // Observe active map
        viewModelScope.launch {
            try {
                val activeMap = mapRepository.getActiveMap()
                if (activeMap != null) {
                    Timber.d("Active map loaded: ${activeMap.id}")
                    // Update map center to the center of the active map
                    updateMapCenter(activeMap.widthMeters / 2, activeMap.heightMeters / 2)
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading active map")
                setError("Failed to load map: ${e.message}")
            }
        }
        
        // Start beacon health monitoring
        startBeaconHealthMonitoring()
    }
    
    /**
     * Starts periodic beacon health monitoring.
     */
    private fun startBeaconHealthMonitoring() {
        viewModelScope.launch {
            while (true) {
                try {
                    // Update staleness status (5000ms = 5 seconds timeout)
                    val staleCount = beaconRepository.updateStaleness(5000L)
                    
                    // Evaluate beacon health
                    val criticalCount = beaconRepository.evaluateBeaconHealth()
                    
                    // Update beacons with alerts
                    _beaconsWithAlerts.value = beaconRepository.getBeaconsWithAlerts()
                    
                    if (_beaconsWithAlerts.value.isNotEmpty()) {
                        Timber.d("Beacons with alerts: ${_beaconsWithAlerts.value.size}")
                    }
                    
                } catch (e: Exception) {
                    Timber.e(e, "Error during beacon health monitoring")
                }
                
                kotlinx.coroutines.delay(HEALTH_MONITORING_INTERVAL)
            }
        }
    }

    /**
     * Updates the user position on the map.
     */
    fun updateUserPosition(x: Float, y: Float, confidence: Float) {
        viewModelScope.launch {
            uiState.value?.let {
                val newPosition = UserPosition(x, y, confidence)
                Timber.d("Updating user position to $newPosition")
                updateState(it.copy(userPosition = newPosition))
            }
        }
    }

    /**
     * Updates the map zoom level.
     */
    fun updateZoomLevel(zoomLevel: Float) {
        viewModelScope.launch {
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
        viewModelScope.launch {
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
        viewModelScope.launch {
            Timber.d("Setting map interaction state to $state")
            _mapInteractionState.value = state
        }
    }

    /**
     * Toggles the display of beacons on the map.
     */
    fun toggleBeaconDisplay() {
        viewModelScope.launch {
            uiState.value?.let {
                val newState = !it.showBeacons
                Timber.d("Toggling beacon display to $newState")
                updateState(it.copy(showBeacons = newState))
            }
        }
    }

    /**
     * Toggles the display of the user's path on the map.
     */
    fun togglePathDisplay() {
        viewModelScope.launch {
            uiState.value?.let {
                val newState = !it.showPath
                Timber.d("Toggling path display to $newState")
                updateState(it.copy(showPath = newState))
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
        val showBeacons: Boolean = true,
        val showPath: Boolean = true,
        val pathPoints: List<UserPosition> = emptyList()
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
     * Represents the possible map interaction states.
     */
    sealed class MapInteractionState {
        object Idle : MapInteractionState()
        object Panning : MapInteractionState()
        object Zooming : MapInteractionState()
    }
}