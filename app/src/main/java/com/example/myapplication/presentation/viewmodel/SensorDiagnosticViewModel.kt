package com.example.myapplication.presentation.viewmodel

import androidx.lifecycle.viewModelScope
import com.example.myapplication.domain.model.SensorData
import com.example.myapplication.service.SensorMonitor
import com.example.myapplication.service.SensorStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * ViewModel for the sensor diagnostic screen.
 * Manages sensor status and real-time sensor data for diagnostic purposes.
 */
class SensorDiagnosticViewModel : BaseViewModel<SensorDiagnosticViewModel.SensorDiagnosticUiState>(), KoinComponent {

    private val sensorMonitor: SensorMonitor by inject()

    init {
        Timber.d("SensorDiagnosticViewModel initialized")
        updateState(SensorDiagnosticUiState())
        // Start monitoring sensor data
        startSensorMonitoring()
    }

    private fun startSensorMonitoring() {
        // Monitor sensor status
        viewModelScope.launch(Dispatchers.IO) {
            sensorMonitor.sensorStatus.collectLatest { status ->
                withContext(Dispatchers.Main) {
                    uiState.value?.let { current ->
                        val updatedState = current.copy(
                            sensorStatus = status,
                            positioningConfidence = sensorMonitor.getPositioningConfidence()
                        )
                        updateState(updatedState)
                        Timber.d("Sensor status updated: $status")
                    }
                }
            }
        }

        // Monitor real-time sensor data
        viewModelScope.launch(Dispatchers.IO) {
            sensorMonitor.sensorData.collectLatest { data ->
                withContext(Dispatchers.Main) {
                    uiState.value?.let { current ->
                        val updatedState = current.copy(
                            sensorData = data
                        )
                        updateState(updatedState)
                        Timber.d("Sensor data updated: accelerometer=[${data.accelerometer.x}, ${data.accelerometer.y}, ${data.accelerometer.z}]")
                    }
                }
            }
        }

        // Monitor registration diagnostics
        viewModelScope.launch(Dispatchers.IO) {
            sensorMonitor.registrationDiagnostics.collectLatest { diags ->
                withContext(Dispatchers.Main) {
                    uiState.value?.let { current ->
                        updateState(current.copy(diagnostics = diags))
                    }
                }
            }
        }
    }

    /**
     * Refreshes the sensor status by restarting sensor monitoring.
     */
    fun refreshSensorStatus() {
        Timber.d("Refreshing sensor status")
        viewModelScope.launch(Dispatchers.IO) {
            sensorMonitor.stopMonitoring()
            sensorMonitor.startMonitoring()
            withContext(Dispatchers.Main) {
                Timber.d("Sensor monitoring restarted")
            }
        }
    }

    /**
     * Gets detailed sensor information for debugging.
     */
    fun getDetailedSensorInfo(): Map<String, Any> {
        val status = sensorMonitor.getSensorStatus()
        val confidence = sensorMonitor.getPositioningConfidence()
        return mapOf(
            "accelerometer" to status.accelerometer,
            "gyroscope" to status.gyroscope,
            "magnetometer" to status.magnetometer,
            "linearAcceleration" to status.linearAcceleration,
            "gravity" to status.gravity,
            "positioningConfidence" to confidence,
            "isMonitoring" to sensorMonitor.isMonitoring,
            "availableSensorsCount" to listOf(
                status.accelerometer,
                status.gyroscope,
                status.magnetometer,
                status.linearAcceleration,
                status.gravity
            ).count { it }
        )
    }

    /**
     * UI state for the sensor diagnostic screen.
     */
    data class SensorDiagnosticUiState(
        val sensorStatus: SensorStatus = SensorStatus(),
        val sensorData: SensorData.Combined? = null,
        val positioningConfidence: Float = 0.0f,
        val isLoading: Boolean = false,
        val error: String? = null,
        val diagnostics: List<String> = emptyList()
    )
}
