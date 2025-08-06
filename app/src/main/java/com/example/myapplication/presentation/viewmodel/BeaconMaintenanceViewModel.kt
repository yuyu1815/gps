package com.example.myapplication.presentation.viewmodel

import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.repository.IBeaconRepository
import com.example.myapplication.service.BeaconMaintenanceTool
import com.example.myapplication.service.BeaconMaintenanceTool.BeaconDiagnosticInfo
import com.example.myapplication.service.BeaconMaintenanceTool.MaintenanceTask
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * UI state for the beacon maintenance screen.
 */
data class BeaconMaintenanceUiState(
    val isRefreshing: Boolean = false,
    val isRunningDiagnostics: Boolean = false,
    val selectedBeaconId: String? = null
)

/**
 * ViewModel for beacon maintenance functionality.
 * Provides access to maintenance tasks, diagnostic reports, and maintenance scheduling.
 */
class BeaconMaintenanceViewModel(
    private val beaconRepository: IBeaconRepository,
    private val maintenanceTool: BeaconMaintenanceTool
) : BaseViewModel<BeaconMaintenanceUiState>() {

    private val _maintenanceTasks = MutableStateFlow<List<MaintenanceTask>>(emptyList())
    val maintenanceTasks: StateFlow<List<MaintenanceTask>> = _maintenanceTasks.asStateFlow()

    private val _diagnosticReport = MutableStateFlow<Map<String, BeaconDiagnosticInfo>>(emptyMap())
    val diagnosticReport: StateFlow<Map<String, BeaconDiagnosticInfo>> = _diagnosticReport.asStateFlow()

    private val _maintenanceSchedule = MutableStateFlow<Map<String, Int>>(emptyMap())
    val maintenanceSchedule: StateFlow<Map<String, Int>> = _maintenanceSchedule.asStateFlow()

    init {
        updateState(BeaconMaintenanceUiState())
        refreshData()
    }

    /**
     * Refreshes all data from the maintenance tool.
     */
    fun refreshData() {
        viewModelScope.launch {
            executeWithLoadingAndErrorHandling(
                block = {
                    updateState(uiState.value?.copy(isRefreshing = true) ?: BeaconMaintenanceUiState(isRefreshing = true))
                    
                    // Refresh maintenance tasks
                    maintenanceTool.refreshMaintenanceTasks()
                    _maintenanceTasks.value = maintenanceTool.maintenanceTasks.value
                    
                    // Run diagnostics if we don't have any diagnostic data
                    if (_diagnosticReport.value.isEmpty()) {
                        runDiagnostics()
                    }
                    
                    Timber.d("Maintenance data refreshed")
                },
                onSuccess = {
                    updateState(uiState.value?.copy(isRefreshing = false) ?: BeaconMaintenanceUiState())
                },
                onError = {
                    updateState(uiState.value?.copy(isRefreshing = false) ?: BeaconMaintenanceUiState())
                    Timber.e(it, "Error refreshing maintenance data")
                }
            )
        }
    }

    /**
     * Runs diagnostics on all beacons.
     */
    fun runDiagnostics() {
        viewModelScope.launch {
            executeWithLoadingAndErrorHandling(
                block = {
                    updateState(uiState.value?.copy(isRunningDiagnostics = true) ?: BeaconMaintenanceUiState(isRunningDiagnostics = true))
                    
                    maintenanceTool.runDiagnostics()
                    _diagnosticReport.value = maintenanceTool.beaconDiagnosticReport.value
                    
                    // Generate maintenance schedule based on diagnostics
                    generateMaintenanceSchedule()
                    
                    Timber.d("Diagnostics completed for ${_diagnosticReport.value.size} beacons")
                },
                onSuccess = {
                    updateState(uiState.value?.copy(isRunningDiagnostics = false) ?: BeaconMaintenanceUiState())
                },
                onError = {
                    updateState(uiState.value?.copy(isRunningDiagnostics = false) ?: BeaconMaintenanceUiState())
                    Timber.e(it, "Error running diagnostics")
                }
            )
        }
    }

    /**
     * Generates a maintenance schedule for all beacons.
     */
    fun generateMaintenanceSchedule() {
        viewModelScope.launch {
            executeWithLoadingAndErrorHandling(
                block = {
                    _maintenanceSchedule.value = maintenanceTool.generateMaintenanceSchedule()
                    Timber.d("Maintenance schedule generated for ${_maintenanceSchedule.value.size} beacons")
                },
                onError = {
                    Timber.e(it, "Error generating maintenance schedule")
                }
            )
        }
    }

    /**
     * Marks a maintenance task as completed.
     */
    fun completeMaintenanceTask(taskId: String, notes: String = "") {
        viewModelScope.launch {
            executeWithLoadingAndErrorHandling(
                block = {
                    maintenanceTool.completeMaintenanceTask(taskId, notes)
                    _maintenanceTasks.value = maintenanceTool.maintenanceTasks.value
                    Timber.d("Maintenance task completed: $taskId")
                },
                onError = {
                    Timber.e(it, "Error completing maintenance task")
                }
            )
        }
    }

    /**
     * Adds a new maintenance task for a beacon.
     */
    fun addMaintenanceTask(
        beaconId: String,
        description: String,
        priority: BeaconMaintenanceTool.MaintenancePriority = BeaconMaintenanceTool.MaintenancePriority.MEDIUM
    ) {
        viewModelScope.launch {
            executeWithLoadingAndErrorHandling(
                block = {
                    maintenanceTool.addMaintenanceTask(beaconId, description, priority)
                    _maintenanceTasks.value = maintenanceTool.maintenanceTasks.value
                    Timber.d("Maintenance task added for beacon $beaconId")
                },
                onError = {
                    Timber.e(it, "Error adding maintenance task")
                }
            )
        }
    }

    /**
     * Calibrates a beacon's TxPower.
     */
    fun calibrateBeaconTxPower(beaconId: String, measuredRssiAt1m: Int) {
        viewModelScope.launch {
            executeWithLoadingAndErrorHandling(
                block = {
                    maintenanceTool.calibrateBeaconTxPower(beaconId, measuredRssiAt1m)
                    Timber.d("Beacon TxPower calibrated: $beaconId")
                    // Return the beaconId so we can use it in onSuccess
                    beaconId
                },
                onSuccess = { id ->
                    // Update the selected beacon ID in the UI state
                    updateState(uiState.value?.copy(selectedBeaconId = id) ?: BeaconMaintenanceUiState(selectedBeaconId = id))
                    // Refresh diagnostics after calibration
                    runDiagnostics()
                },
                onError = {
                    Timber.e(it, "Error calibrating beacon TxPower")
                }
            )
        }
    }

    /**
     * Updates a beacon's battery level.
     */
    fun updateBeaconBatteryLevel(beaconId: String, batteryLevel: Int) {
        viewModelScope.launch {
            executeWithLoadingAndErrorHandling(
                block = {
                    maintenanceTool.updateBeaconBatteryLevel(beaconId, batteryLevel)
                    Timber.d("Beacon battery level updated: $beaconId")
                    // Return the beaconId so we can use it in onSuccess
                    beaconId
                },
                onSuccess = { id ->
                    // Update the selected beacon ID in the UI state
                    updateState(uiState.value?.copy(selectedBeaconId = id) ?: BeaconMaintenanceUiState(selectedBeaconId = id))
                    // Refresh diagnostics after update
                    runDiagnostics()
                },
                onError = {
                    Timber.e(it, "Error updating beacon battery level")
                }
            )
        }
    }

    /**
     * Gets diagnostic information for a specific beacon.
     */
    fun getBeaconInfo(beaconId: String): BeaconDiagnosticInfo? {
        return _diagnosticReport.value[beaconId]
    }

    /**
     * Gets all maintenance tasks for a specific beacon.
     */
    fun getMaintenanceTasksForBeacon(beaconId: String): List<MaintenanceTask> {
        return _maintenanceTasks.value.filter { it.beaconId == beaconId }
    }

    /**
     * Gets all incomplete maintenance tasks.
     */
    fun getIncompleteMaintenanceTasks(): List<MaintenanceTask> {
        return _maintenanceTasks.value.filter { it.completedTimestamp == null }
    }
}