package com.example.myapplication.ui.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.myapplication.R
import kotlin.math.min
import org.koin.androidx.compose.getKoin
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size

/**
 * Debug screen that provides tools for monitoring and debugging the positioning system.
 * Includes real-time data visualization, logging controls, and system diagnostics.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToSensorDiagnostic: () -> Unit = {},
    onNavigateToSensor3DTest: () -> Unit = {},
    onNavigateToAccelerometerCompass: () -> Unit = {},
    onNavigateToSlamCamera: () -> Unit = {}
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var isLoggingEnabled by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.debug_tools)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tab selection
            TabRow(selectedTabIndex = selectedTabIndex) {
                listOf(
                    stringResource(id = R.string.debug_realtime),
                    stringResource(id = R.string.debug_sensors),
                    stringResource(id = R.string.debug_logs)
                ).forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title) }
                    )
                }
            }
            
            // Tab content
            when (selectedTabIndex) {
                0 -> RealTimeDebugTab()
                1 -> SensorsDebugTab(
                    onNavigateToSensorDiagnostic = onNavigateToSensorDiagnostic,
                    onNavigateToSensor3DTest = onNavigateToSensor3DTest,
                    onNavigateToAccelerometerCompass = onNavigateToAccelerometerCompass,
                    onNavigateToSlamCamera = onNavigateToSlamCamera
                )
                2 -> LoggingDebugTab(
                    isLoggingEnabled = isLoggingEnabled,
                    onToggleLogging = { isLoggingEnabled = it }
                )
            }
        }
    }
}

@Composable
fun RealTimeDebugTab() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // 位置の数値表示は削除
        
        // System status
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.debug_system_status),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                StatusRow(stringResource(id = R.string.debug_ble_scanner), stringResource(id = R.string.debug_active))
                StatusRow(stringResource(id = R.string.debug_sensor_monitor), stringResource(id = R.string.debug_active))
                StatusRow(stringResource(id = R.string.debug_position_updates), "10 Hz")
                StatusRow(stringResource(id = R.string.debug_memory_usage), "45 MB")
                StatusRow(stringResource(id = R.string.debug_battery_impact), "Low")
            }
        }
        
        // 可視化（座標軸/数値の直接表示はしない簡易図）
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .padding(bottom = 16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                // シンプルなグリッドのみ（数値ラベルなし）
                val step = 10
                for (i in 0..10) {
                    drawLine(
                        color = Color.LightGray,
                        start = Offset(i * size.width / step, 0f),
                        end = Offset(i * size.width / step, size.height)
                    )
                    drawLine(
                        color = Color.LightGray,
                        start = Offset(0f, i * size.height / step),
                        end = Offset(size.width, i * size.height / step)
                    )
                }
                // 中心に点だけ描画
                drawCircle(color = Color.Red, radius = 12f, center = Offset(size.width / 2, size.height / 2))
            }
        }
    }
}


@Composable
fun SensorsDebugTab(
    onNavigateToSensorDiagnostic: () -> Unit = {},
    onNavigateToSensor3DTest: () -> Unit = {},
    onNavigateToAccelerometerCompass: () -> Unit = {},
    onNavigateToSlamCamera: () -> Unit = {}
) {
    // センサー登録ダイアグノスティクスは不要のため取得しない

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // センサー登録ダイアグノスティクスは削除

        // Sensor Diagnostic Button
        Button(
            onClick = onNavigateToSensorDiagnostic,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Sensors,
                contentDescription = null,
                modifier = Modifier.padding(end = 8.dp)
            )
            Text(stringResource(id = R.string.sensor_diagnostic))
        }

        // SLAM Camera Button
        Button(
            onClick = onNavigateToSlamCamera,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Text("SLAMカメラ表示")
        }

        // Sensor 3D Test Button
        Button(
            onClick = onNavigateToSensor3DTest,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Text("3Dセンサーブロック表示")
        }

        // Accelerometer Compass Button
        Button(
            onClick = onNavigateToAccelerometerCompass,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Text("加速度コンパス表示")
        }
        
        // Accelerometer data
        SensorDataCard(
            title = stringResource(id = R.string.debug_accelerometer),
            values = mapOf(
                "X" to "0.12 m/s²",
                "Y" to "-0.08 m/s²",
                "Z" to "9.81 m/s²"
            ),
            status = stringResource(id = R.string.debug_active)
        )
        
        // Gyroscope data
        SensorDataCard(
            title = stringResource(id = R.string.debug_gyroscope),
            values = mapOf(
                "X" to "0.01 rad/s",
                "Y" to "0.02 rad/s",
                "Z" to "0.00 rad/s"
            ),
            status = stringResource(id = R.string.debug_active)
        )
        
        // Magnetometer data
        SensorDataCard(
            title = stringResource(id = R.string.debug_magnetometer),
            values = mapOf(
                "X" to "22.5 µT",
                "Y" to "40.3 µT",
                "Z" to "-13.7 µT"
            ),
            status = stringResource(id = R.string.debug_active)
        )
        
        // Step detection
        SensorDataCard(
            title = stringResource(id = R.string.debug_step_detection),
            values = mapOf(
                "Steps" to "247",
                "Step Length" to "0.75 m",
                "Step Frequency" to "1.8 Hz"
            ),
            status = stringResource(id = R.string.debug_active)
        )
        
        // Heading
        SensorDataCard(
            title = stringResource(id = R.string.debug_heading),
            values = mapOf(
                "Angle" to "127.5°",
                "Accuracy" to "±2.1°"
            ),
            status = stringResource(id = R.string.debug_active)
        )
    }
}

@Composable
fun SensorDataCard(
    title: String,
    values: Map<String, String>,
    status: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
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
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = status,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (status == stringResource(id = R.string.debug_active)) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            values.forEach { (key, value) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = key,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
fun LoggingDebugTab(
    isLoggingEnabled: Boolean,
    onToggleLogging: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Logging controls
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.debug_logging_controls),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(id = R.string.debug_enable_data_logging),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    Switch(
                        checked = isLoggingEnabled,
                        onCheckedChange = onToggleLogging
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = { /* Start new log session */ },
                        enabled = isLoggingEnabled
                    ) {
                        Text(stringResource(id = R.string.debug_new_session))
                    }
                    
                    Button(
                        onClick = { /* Export logs */ },
                        enabled = isLoggingEnabled
                    ) {
                        Text(stringResource(id = R.string.debug_export_logs))
                    }
                }
            }
        }
        
        // Log files
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.debug_log_files),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Sample log files
                LogFileItem(
                    name = "positioning_log_20250805_153022.csv",
                    size = "1.2 MB",
                    date = "2025-08-05 15:30:22"
                )
                
                LogFileItem(
                    name = "sensor_log_20250805_153022.csv",
                    size = "3.5 MB",
                    date = "2025-08-05 15:30:22"
                )
                
                LogFileItem(
                    name = "beacon_log_20250805_153022.csv",
                    size = "0.8 MB",
                    date = "2025-08-05 15:30:22"
                )
                
                LogFileItem(
                    name = "positioning_log_20250805_142517.csv",
                    size = "1.1 MB",
                    date = "2025-08-05 14:25:17"
                )
            }
        }
        
        // Log statistics
        Card(
            modifier = Modifier
                .fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.debug_log_statistics),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                StatusRow(stringResource(id = R.string.debug_total_log_files), "4")
                StatusRow(stringResource(id = R.string.debug_total_size), "6.6 MB")
                StatusRow(stringResource(id = R.string.debug_oldest_log), "2025-08-05 14:25:17")
                StatusRow(stringResource(id = R.string.debug_newest_log), "2025-08-05 15:30:22")
                StatusRow(stringResource(id = R.string.debug_storage_available), "14.2 GB")
            }
        }
    }
}

@Composable
fun StatusRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium
        )
        
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
    
    HorizontalDivider(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    )
}

@Composable
fun LogFileItem(
    name: String,
    size: String,
    date: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = size,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Text(
                text = date,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    
    HorizontalDivider(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    )
}
