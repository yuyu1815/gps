package com.example.myapplication.service

import com.example.myapplication.data.repository.IBeaconRepository
import com.example.myapplication.domain.model.Beacon
import com.example.myapplication.domain.model.BeaconHealthStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.UUID

/**
 * Tool for managing and maintaining beacons in the indoor positioning system.
 * Provides functionality for beacon diagnostics, maintenance scheduling, and health reporting.
 */
class BeaconMaintenanceTool(
    private val beaconRepository: IBeaconRepository,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    /**
     * Enum representing the maintenance priority of a beacon.
     */
    enum class MaintenancePriority {
        /**
         * Beacon requires immediate attention due to critical issues.
         */
        URGENT,
        
        /**
         * Beacon has issues that should be addressed soon but are not critical.
         */
        HIGH,
        
        /**
         * Beacon has minor issues that should be addressed during routine maintenance.
         */
        MEDIUM,
        
        /**
         * Beacon is functioning normally and does not require maintenance.
         */
        LOW
    }

    /**
     * Data class representing a maintenance task for a beacon.
     */
    data class MaintenanceTask(
        val id: String = UUID.randomUUID().toString(),
        val beaconId: String,
        val beaconMacAddress: String,
        val beaconName: String,
        val priority: MaintenancePriority,
        val description: String,
        val createdTimestamp: Long = System.currentTimeMillis(),
        var completedTimestamp: Long? = null,
        var notes: String = ""
    )

    private val _maintenanceTasks = MutableStateFlow<List<MaintenanceTask>>(emptyList())
    val maintenanceTasks = _maintenanceTasks.asStateFlow()

    private val _beaconDiagnosticReport = MutableStateFlow<Map<String, BeaconDiagnosticInfo>>(emptyMap())
    val beaconDiagnosticReport = _beaconDiagnosticReport.asStateFlow()

    /**
     * Data class containing diagnostic information for a beacon.
     */
    data class BeaconDiagnosticInfo(
        val beaconId: String,
        val macAddress: String,
        val name: String,
        val healthStatus: BeaconHealthStatus,
        val signalQuality: Int, // 0-100
        val batteryLevel: Int, // 0-100 or -1 if unknown
        val failedDetectionCount: Int,
        val lastSeenTimestamp: Long,
        val isStale: Boolean,
        val estimatedDistance: Float,
        val distanceConfidence: Float,
        val maintenancePriority: MaintenancePriority,
        val recommendedActions: List<String>
    )

    init {
        // Initialize maintenance tasks from repository data
        refreshMaintenanceTasks()
    }

    /**
     * Refreshes the maintenance tasks based on current beacon health status.
     * This should be called periodically to keep the maintenance tasks up to date.
     */
    fun refreshMaintenanceTasks() {
        coroutineScope.launch {
            try {
                // Get beacons with alerts
                val beaconsWithAlerts = beaconRepository.getBeaconsWithAlerts()
                
                // Create maintenance tasks for beacons with alerts
                val tasks = beaconsWithAlerts.map { beacon ->
                    createMaintenanceTaskForBeacon(beacon)
                }
                
                _maintenanceTasks.value = tasks
                
                Timber.d("Refreshed maintenance tasks: ${tasks.size} tasks created")
            } catch (e: Exception) {
                Timber.e(e, "Error refreshing maintenance tasks")
            }
        }
    }

    /**
     * Creates a maintenance task for a beacon based on its health status.
     */
    private fun createMaintenanceTaskForBeacon(beacon: Beacon): MaintenanceTask {
        val priority = when (beacon.healthStatus) {
            BeaconHealthStatus.CRITICAL -> MaintenancePriority.URGENT
            BeaconHealthStatus.WARNING -> MaintenancePriority.HIGH
            BeaconHealthStatus.GOOD -> MaintenancePriority.LOW
            BeaconHealthStatus.UNKNOWN -> MaintenancePriority.MEDIUM
        }
        
        return MaintenanceTask(
            beaconId = beacon.id,
            beaconMacAddress = beacon.macAddress,
            beaconName = beacon.name.ifEmpty { beacon.macAddress },
            priority = priority,
            description = beacon.alertMessage.ifEmpty { "Beacon requires maintenance" }
        )
    }

    /**
     * Runs a diagnostic on all beacons and generates a report.
     */
    fun runDiagnostics() {
        coroutineScope.launch {
            try {
                val beacons = beaconRepository.getAll()
                val diagnosticReport = mutableMapOf<String, BeaconDiagnosticInfo>()
                
                beacons.forEach { beacon ->
                    val diagnosticInfo = generateDiagnosticInfo(beacon)
                    diagnosticReport[beacon.id] = diagnosticInfo
                }
                
                _beaconDiagnosticReport.value = diagnosticReport
                
                Timber.d("Diagnostic report generated for ${beacons.size} beacons")
            } catch (e: Exception) {
                Timber.e(e, "Error running beacon diagnostics")
            }
        }
    }

    /**
     * Generates diagnostic information for a beacon.
     */
    private fun generateDiagnosticInfo(beacon: Beacon): BeaconDiagnosticInfo {
        // Calculate signal quality as a percentage
        val signalQuality = when {
            beacon.filteredRssi > -60 -> 90 // Excellent
            beacon.filteredRssi > -70 -> 75 // Good
            beacon.filteredRssi > -80 -> 50 // Fair
            beacon.filteredRssi > -90 -> 25 // Poor
            else -> 10 // Very poor
        }
        
        // Determine maintenance priority
        val priority = when {
            beacon.healthStatus == BeaconHealthStatus.CRITICAL -> MaintenancePriority.URGENT
            beacon.healthStatus == BeaconHealthStatus.WARNING -> MaintenancePriority.HIGH
            beacon.failedDetectionCount > 0 -> MaintenancePriority.MEDIUM
            else -> MaintenancePriority.LOW
        }
        
        // Generate recommended actions
        val recommendedActions = mutableListOf<String>()
        
        if (beacon.isStale) {
            recommendedActions.add("Check if beacon is powered on and in range")
        }
        
        if (beacon.batteryLevel in 0..20) {
            recommendedActions.add("Replace battery (current level: ${beacon.batteryLevel}%)")
        }
        
        if (beacon.filteredRssi < -85) {
            recommendedActions.add("Check for obstructions or interference near beacon")
        }
        
        if (beacon.failedDetectionCount > 3) {
            recommendedActions.add("Check beacon connectivity and signal strength")
        }
        
        if (beacon.distanceConfidence < 0.4f) {
            recommendedActions.add("Recalibrate beacon TxPower")
        }
        
        if (recommendedActions.isEmpty()) {
            recommendedActions.add("No maintenance required")
        }
        
        return BeaconDiagnosticInfo(
            beaconId = beacon.id,
            macAddress = beacon.macAddress,
            name = beacon.name.ifEmpty { beacon.macAddress },
            healthStatus = beacon.healthStatus,
            signalQuality = signalQuality,
            batteryLevel = beacon.batteryLevel,
            failedDetectionCount = beacon.failedDetectionCount,
            lastSeenTimestamp = beacon.lastSeenTimestamp,
            isStale = beacon.isStale,
            estimatedDistance = beacon.estimatedDistance,
            distanceConfidence = beacon.distanceConfidence,
            maintenancePriority = priority,
            recommendedActions = recommendedActions
        )
    }

    /**
     * Adds a maintenance task for a beacon.
     */
    fun addMaintenanceTask(
        beaconId: String,
        description: String,
        priority: MaintenancePriority = MaintenancePriority.MEDIUM
    ) {
        coroutineScope.launch {
            try {
                val beacon = beaconRepository.getById(beaconId)
                
                if (beacon != null) {
                    val task = MaintenanceTask(
                        beaconId = beacon.id,
                        beaconMacAddress = beacon.macAddress,
                        beaconName = beacon.name.ifEmpty { beacon.macAddress },
                        priority = priority,
                        description = description
                    )
                    
                    val currentTasks = _maintenanceTasks.value.toMutableList()
                    currentTasks.add(task)
                    _maintenanceTasks.value = currentTasks
                    
                    Timber.d("Added maintenance task for beacon ${beacon.macAddress}: $description")
                } else {
                    Timber.w("Cannot add maintenance task: Beacon with ID $beaconId not found")
                }
            } catch (e: Exception) {
                Timber.e(e, "Error adding maintenance task")
            }
        }
    }

    /**
     * Marks a maintenance task as completed.
     */
    fun completeMaintenanceTask(taskId: String, notes: String = "") {
        val currentTasks = _maintenanceTasks.value.toMutableList()
        val taskIndex = currentTasks.indexOfFirst { it.id == taskId }
        
        if (taskIndex >= 0) {
            val task = currentTasks[taskIndex].copy(
                completedTimestamp = System.currentTimeMillis(),
                notes = notes
            )
            
            currentTasks[taskIndex] = task
            _maintenanceTasks.value = currentTasks
            
            Timber.d("Marked maintenance task as completed: $taskId")
        } else {
            Timber.w("Cannot complete task: Task with ID $taskId not found")
        }
    }

    /**
     * Gets all maintenance tasks for a specific beacon.
     */
    fun getMaintenanceTasksForBeacon(beaconId: String): List<MaintenanceTask> {
        return _maintenanceTasks.value.filter { it.beaconId == beaconId }
    }

    /**
     * Gets all maintenance tasks with a specific priority.
     */
    fun getMaintenanceTasksByPriority(priority: MaintenancePriority): List<MaintenanceTask> {
        return _maintenanceTasks.value.filter { it.priority == priority }
    }

    /**
     * Gets all incomplete maintenance tasks.
     */
    fun getIncompleteMaintenanceTasks(): List<MaintenanceTask> {
        return _maintenanceTasks.value.filter { it.completedTimestamp == null }
    }

    /**
     * Gets the diagnostic information for a specific beacon.
     */
    fun getBeaconDiagnosticInfo(beaconId: String): BeaconDiagnosticInfo? {
        return _beaconDiagnosticReport.value[beaconId]
    }

    /**
     * Recalibrates a beacon's TxPower based on measured RSSI at 1 meter.
     * This should be called when the device is exactly 1 meter away from the beacon.
     */
    fun calibrateBeaconTxPower(beaconId: String, measuredRssiAt1m: Int) {
        coroutineScope.launch {
            try {
                val beacon = beaconRepository.getById(beaconId)
                
                if (beacon != null) {
                    // Update the beacon's TxPower
                    beaconRepository.updateTxPower(beacon.macAddress, measuredRssiAt1m)
                    
                    Timber.d("Calibrated TxPower for beacon ${beacon.macAddress}: $measuredRssiAt1m dBm")
                } else {
                    Timber.w("Cannot calibrate TxPower: Beacon with ID $beaconId not found")
                }
            } catch (e: Exception) {
                Timber.e(e, "Error calibrating beacon TxPower")
            }
        }
    }

    /**
     * Updates a beacon's battery level.
     * This should be called when the battery level is manually checked or reported by the beacon.
     */
    fun updateBeaconBatteryLevel(beaconId: String, batteryLevel: Int) {
        coroutineScope.launch {
            try {
                val beacon = beaconRepository.getById(beaconId)
                
                if (beacon != null) {
                    // Update the beacon's battery level
                    beaconRepository.updateBatteryLevel(beacon.macAddress, batteryLevel)
                    
                    Timber.d("Updated battery level for beacon ${beacon.macAddress}: $batteryLevel%")
                } else {
                    Timber.w("Cannot update battery level: Beacon with ID $beaconId not found")
                }
            } catch (e: Exception) {
                Timber.e(e, "Error updating beacon battery level")
            }
        }
    }

    /**
     * Generates a maintenance schedule for all beacons based on their health status.
     * @return A map of beacon IDs to recommended maintenance intervals in days
     */
    fun generateMaintenanceSchedule(): Map<String, Int> {
        val schedule = mutableMapOf<String, Int>()
        
        _beaconDiagnosticReport.value.forEach { (beaconId, diagnosticInfo) ->
            val maintenanceInterval = when (diagnosticInfo.maintenancePriority) {
                MaintenancePriority.URGENT -> 1 // Check daily
                MaintenancePriority.HIGH -> 7 // Check weekly
                MaintenancePriority.MEDIUM -> 30 // Check monthly
                MaintenancePriority.LOW -> 90 // Check quarterly
            }
            
            schedule[beaconId] = maintenanceInterval
        }
        
        return schedule
    }
}