package com.example.myapplication.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.myapplication.R
import com.example.myapplication.service.SensorMonitor
import com.example.myapplication.service.SensorStatus
import com.example.myapplication.presentation.viewmodel.SensorDiagnosticViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SensorDiagnosticScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sensorDiagnosticViewModel: SensorDiagnosticViewModel = koinViewModel()
    val uiState by sensorDiagnosticViewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.sensor_diagnostic)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = stringResource(id = R.string.navigate_back)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { sensorDiagnosticViewModel.refreshSensorStatus() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(id = R.string.refresh)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.surface)
                .verticalScroll(rememberScrollState())
        ) {
            // Sensor Status Overview
            SensorStatusOverview(
                sensorStatus = uiState?.sensorStatus ?: SensorStatus(),
                modifier = Modifier.padding(16.dp)
            )

            // Real-time Sensor Data
            RealTimeSensorData(
                sensorData = uiState?.sensorData,
                modifier = Modifier.padding(16.dp)
            )

            // Diagnostics (new)
            DiagnosticsSection(
                diagnostics = uiState?.diagnostics ?: emptyList(),
                modifier = Modifier.padding(16.dp)
            )

            // Sensor Details
            SensorDetailsSection(
                sensorStatus = uiState?.sensorStatus ?: SensorStatus(),
                positioningConfidence = uiState?.positioningConfidence ?: 0.0f,
                modifier = Modifier.padding(16.dp)
            )

            // Troubleshooting Tips
            TroubleshootingSection(
                sensorStatus = uiState?.sensorStatus ?: SensorStatus(),
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Composable
fun DiagnosticsSection(
    diagnostics: List<String>,
    modifier: Modifier = Modifier
) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Sensor Registration Diagnostics",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                TextButton(
                    enabled = diagnostics.isNotEmpty(),
                    onClick = {
                        val text = diagnostics.joinToString("\n")
                        clipboard.setText(AnnotatedString(text))
                        android.widget.Toast.makeText(
                            context,
                            context.getString(R.string.copied_to_clipboard),
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(text = stringResource(id = R.string.copy))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (diagnostics.isEmpty()) {
                Text(
                    text = stringResource(id = R.string.no_sensor_data),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                diagnostics.takeLast(50).forEach { line ->
                    Text(
                        text = line,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
fun SensorStatusOverview(
    sensorStatus: SensorStatus,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(id = R.string.sensor_status_overview),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Sensor status grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SensorStatusItem(
                    name = stringResource(id = R.string.accelerometer),
                    isAvailable = sensorStatus.accelerometer,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                SensorStatusItem(
                    name = stringResource(id = R.string.gyroscope),
                    isAvailable = sensorStatus.gyroscope,
                    modifier = Modifier.weight(1f)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SensorStatusItem(
                    name = stringResource(id = R.string.magnetometer),
                    isAvailable = sensorStatus.magnetometer,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                SensorStatusItem(
                    name = stringResource(id = R.string.linear_acceleration),
                    isAvailable = sensorStatus.linearAcceleration,
                    modifier = Modifier.weight(1f)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SensorStatusItem(
                    name = stringResource(id = R.string.gravity),
                    isAvailable = sensorStatus.gravity,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                // Empty space for alignment
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun SensorStatusItem(
    name: String,
    isAvailable: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (isAvailable) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Colored dot: we will show availability only here; detailed state shown in Sensors tab via registration states
            val dotColor = if (isAvailable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(
                        if (isAvailable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = name,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            
            Text(
                text = if (isAvailable) stringResource(id = R.string.available) else stringResource(id = R.string.unavailable),
                style = MaterialTheme.typography.bodySmall,
                color = if (isAvailable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
fun RealTimeSensorData(
    sensorData: com.example.myapplication.domain.model.SensorData.Combined?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(id = R.string.realtime_sensor_data),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (sensorData != null) {
                SensorDataRow(
                    label = stringResource(id = R.string.accelerometer),
                    values = listOf(
                        "%.3f".format(sensorData.accelerometer.x),
                        "%.3f".format(sensorData.accelerometer.y),
                        "%.3f".format(sensorData.accelerometer.z)
                    ),
                    unit = "m/s²"
                )
                
                SensorDataRow(
                    label = stringResource(id = R.string.gyroscope),
                    values = listOf(
                        "%.3f".format(sensorData.gyroscope.x),
                        "%.3f".format(sensorData.gyroscope.y),
                        "%.3f".format(sensorData.gyroscope.z)
                    ),
                    unit = "rad/s"
                )
                
                SensorDataRow(
                    label = stringResource(id = R.string.magnetometer),
                    values = listOf(
                        "%.3f".format(sensorData.magnetometer.x),
                        "%.3f".format(sensorData.magnetometer.y),
                        "%.3f".format(sensorData.magnetometer.z)
                    ),
                    unit = "µT"
                )
                
                SensorDataRow(
                    label = stringResource(id = R.string.heading),
                    values = listOf("%.1f".format(sensorData.heading)),
                    unit = "°"
                )
            } else {
                Text(
                    text = stringResource(id = R.string.no_sensor_data),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun SensorDataRow(
    label: String,
    values: List<String>,
    unit: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(100.dp)
        )
        
        Text(
            text = "[${values.joinToString(", ")}] $unit",
            style = MaterialTheme.typography.bodySmall,
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
        )
    }
}

@Composable
fun SensorDetailsSection(
    sensorStatus: SensorStatus,
    positioningConfidence: Float,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(id = R.string.sensor_details),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Positioning confidence
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(id = R.string.positioning_confidence),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "%.1f%%".format(positioningConfidence * 100),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        positioningConfidence >= 0.8f -> MaterialTheme.colorScheme.primary
                        positioningConfidence >= 0.5f -> Color(0xFFFF9800) // Orange
                        else -> MaterialTheme.colorScheme.error
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Available sensors count
            val availableSensors = listOf(
                sensorStatus.accelerometer,
                sensorStatus.gyroscope,
                sensorStatus.magnetometer,
                sensorStatus.linearAcceleration,
                sensorStatus.gravity
            ).count { it }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(id = R.string.available_sensors),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "$availableSensors/5",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Critical sensors status
            val criticalSensorsAvailable = sensorStatus.accelerometer && 
                                         sensorStatus.gyroscope && 
                                         sensorStatus.magnetometer
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(id = R.string.critical_sensors),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = if (criticalSensorsAvailable) 
                        stringResource(id = R.string.available) 
                    else 
                        stringResource(id = R.string.unavailable),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (criticalSensorsAvailable) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun TroubleshootingSection(
    sensorStatus: SensorStatus,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(id = R.string.troubleshooting),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Generate troubleshooting tips based on sensor status
            val tips = mutableListOf<String>()
            
            if (!sensorStatus.accelerometer) {
                tips.add(stringResource(id = R.string.tip_accelerometer_missing))
            }
            
            if (!sensorStatus.gyroscope) {
                tips.add(stringResource(id = R.string.tip_gyroscope_missing))
            }
            
            if (!sensorStatus.magnetometer) {
                tips.add(stringResource(id = R.string.tip_magnetometer_missing))
            }
            
            if (tips.isEmpty()) {
                Text(
                    text = stringResource(id = R.string.all_sensors_working),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                tips.forEach { tip ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "• ",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = tip,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}
