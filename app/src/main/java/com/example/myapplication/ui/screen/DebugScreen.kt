package com.example.myapplication.ui.screen

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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Debug screen that provides tools for monitoring and debugging the positioning system.
 * Includes real-time data visualization, logging controls, and system diagnostics.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugScreen(
    onNavigateBack: () -> Unit = {}
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var isLoggingEnabled by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Debug Tools") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
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
                listOf("Real-time", "Beacons", "Sensors", "Logs").forEachIndexed { index, title ->
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
                1 -> BeaconsDebugTab()
                2 -> SensorsDebugTab()
                3 -> LoggingDebugTab(
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
        // Position information
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
                    text = "Current Position",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "X: 30 m",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    Text(
                        text = "Y: 40 m",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    Text(
                        text = "Confidence: 85%",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
        
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
                    text = "System Status",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                StatusRow("BLE Scanner", "Active")
                StatusRow("Sensor Monitor", "Active")
                StatusRow("Position Updates", "10 Hz")
                StatusRow("Memory Usage", "45 MB")
                StatusRow("Battery Impact", "Low")
            }
        }
        
        // Visualization placeholder
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .padding(bottom = 16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Map Visualization",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Visualization will be displayed here",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
fun BeaconsDebugTab() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Detected Beacons (4)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // Sample beacon cards
        BeaconCard(
            name = "Beacon 1",
            id = "B1",
            rssi = -65,
            position = "10, 10",
            lastSeen = "16:58:32",
            distance = 2.5f
        )
        
        BeaconCard(
            name = "Beacon 2",
            id = "B2",
            rssi = -70,
            position = "50, 10",
            lastSeen = "16:58:30",
            distance = 3.2f
        )
        
        BeaconCard(
            name = "Beacon 3",
            id = "B3",
            rssi = -75,
            position = "50, 50",
            lastSeen = "16:58:28",
            distance = 4.1f
        )
        
        BeaconCard(
            name = "Beacon 4",
            id = "B4",
            rssi = -68,
            position = "10, 50",
            lastSeen = "16:58:31",
            distance = 2.8f
        )
    }
}

@Composable
fun BeaconCard(
    name: String,
    id: String,
    rssi: Int,
    position: String,
    lastSeen: String,
    distance: Float
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "ID: $id",
                    style = MaterialTheme.typography.bodySmall
                )
                
                Text(
                    text = "RSSI: $rssi dBm",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Position: $position m",
                    style = MaterialTheme.typography.bodySmall
                )
                
                Text(
                    text = "Last seen: $lastSeen",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = "Estimated distance: ${String.format("%.2f", distance)} m",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun SensorsDebugTab() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Accelerometer data
        SensorDataCard(
            title = "Accelerometer",
            values = mapOf(
                "X" to "0.12 m/s²",
                "Y" to "-0.08 m/s²",
                "Z" to "9.81 m/s²"
            ),
            status = "Active"
        )
        
        // Gyroscope data
        SensorDataCard(
            title = "Gyroscope",
            values = mapOf(
                "X" to "0.01 rad/s",
                "Y" to "0.02 rad/s",
                "Z" to "0.00 rad/s"
            ),
            status = "Active"
        )
        
        // Magnetometer data
        SensorDataCard(
            title = "Magnetometer",
            values = mapOf(
                "X" to "22.5 µT",
                "Y" to "40.3 µT",
                "Z" to "-13.7 µT"
            ),
            status = "Active"
        )
        
        // Step detection
        SensorDataCard(
            title = "Step Detection",
            values = mapOf(
                "Steps" to "247",
                "Step Length" to "0.75 m",
                "Step Frequency" to "1.8 Hz"
            ),
            status = "Active"
        )
        
        // Heading
        SensorDataCard(
            title = "Heading",
            values = mapOf(
                "Angle" to "127.5°",
                "Accuracy" to "±2.1°"
            ),
            status = "Active"
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
                    color = if (status == "Active") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
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
                    text = "Logging Controls",
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
                        text = "Enable Data Logging",
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
                        Text("New Session")
                    }
                    
                    Button(
                        onClick = { /* Export logs */ },
                        enabled = isLoggingEnabled
                    ) {
                        Text("Export Logs")
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
                    text = "Log Files",
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
                    text = "Log Statistics",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                StatusRow("Total Log Files", "4")
                StatusRow("Total Size", "6.6 MB")
                StatusRow("Oldest Log", "2025-08-05 14:25:17")
                StatusRow("Newest Log", "2025-08-05 15:30:22")
                StatusRow("Storage Available", "14.2 GB")
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
    
    Divider(
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
    
    Divider(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    )
}