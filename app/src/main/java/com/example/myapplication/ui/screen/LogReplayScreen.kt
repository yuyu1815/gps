package com.example.myapplication.ui.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.domain.model.Beacon
import com.example.myapplication.domain.model.UserPosition
import com.example.myapplication.presentation.viewmodel.LogReplayViewModel
import java.io.File

/**
 * Screen for visualizing log replay results.
 *
 * @param onNavigateBack Callback for navigating back
 * @param viewModel The view model for log replay
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogReplayScreen(
    onNavigateBack: () -> Unit,
    viewModel: LogReplayViewModel = viewModel()
) {
    val isReplaying by viewModel.isReplaying.collectAsState()
    val replayProgress by viewModel.replayProgress.collectAsState()
    val replaySpeed by viewModel.replaySpeed.collectAsState()
    val currentLogFile by viewModel.currentLogFile.collectAsState()
    val beacons by viewModel.beaconData.collectAsState()
    val sensorData by viewModel.sensorData.collectAsState()
    val calculatedPosition by viewModel.calculatedPosition.collectAsState()
    val selectedLogFile by viewModel.selectedLogFile.collectAsState()
    
    var showSpeedMenu by remember { mutableStateOf(false) }
    
    // Start replay when entering the screen if a log file is selected
    LaunchedEffect(selectedLogFile) {
        if (selectedLogFile != null && !isReplaying) {
            viewModel.startReplay()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Log Replay") },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.stopReplay()
                        onNavigateBack()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Log file info
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Replaying Log File",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = currentLogFile?.name ?: selectedLogFile?.file?.name ?: "No file selected",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Replay controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Play/Pause button
                IconButton(
                    onClick = {
                        if (isReplaying) {
                            viewModel.stopReplay()
                        } else {
                            viewModel.startReplay()
                        }
                    }
                ) {
                    Icon(
                        if (isReplaying) Icons.Default.ArrowBack else Icons.Default.PlayArrow,
                        contentDescription = if (isReplaying) "Stop" else "Play"
                    )
                }
                
                // Progress indicator
                LinearProgressIndicator(
                    progress = replayProgress,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp)
                )
                
                // Speed control
                Box {
                    IconButton(onClick = { showSpeedMenu = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Speed")
                    }
                    
                    DropdownMenu(
                        expanded = showSpeedMenu,
                        onDismissRequest = { showSpeedMenu = false }
                    ) {
                        viewModel.replaySpeedOptions.forEach { speed ->
                            DropdownMenuItem(
                                text = { Text("${speed}x") },
                                onClick = {
                                    viewModel.setReplaySpeed(speed)
                                    showSpeedMenu = false
                                }
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Visualization area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                // Draw the visualization
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height
                    
                    // Scale factor for visualization (adjust as needed)
                    val scale = 50f // 1 meter = 50 pixels
                    
                    // Center of the canvas
                    val centerX = canvasWidth / 2
                    val centerY = canvasHeight / 2
                    
                    // Draw beacons
                    beacons.forEach { beacon ->
                        // Convert beacon coordinates to canvas coordinates
                        val beaconX = centerX + beacon.x * scale
                        val beaconY = centerY + beacon.y * scale
                        
                        // Draw beacon
                        drawCircle(
                            color = Color.Blue,
                            radius = 10f,
                            center = Offset(beaconX, beaconY)
                        )
                        
                        // Draw beacon range (based on RSSI)
                        val rangeRadius = beacon.estimatedDistance * scale
                        drawCircle(
                            color = Color.Blue.copy(alpha = 0.2f),
                            radius = rangeRadius,
                            center = Offset(beaconX, beaconY),
                            style = Stroke(width = 2f)
                        )
                    }
                    
                    // Draw calculated position
                    calculatedPosition?.let { position ->
                        // Convert position coordinates to canvas coordinates
                        val posX = centerX + position.x * scale
                        val posY = centerY + position.y * scale
                        
                        // Draw position
                        drawCircle(
                            color = Color.Red,
                            radius = 15f,
                            center = Offset(posX, posY)
                        )
                        
                        // Draw accuracy circle
                        drawCircle(
                            color = Color.Red.copy(alpha = 0.3f),
                            radius = position.accuracy * scale,
                            center = Offset(posX, posY),
                            style = Stroke(width = 2f)
                        )
                    }
                }
                
                // Overlay with position info
                Column(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp)
                ) {
                    calculatedPosition?.let { position ->
                        Text(
                            text = "Position: (${String.format("%.2f", position.x)}m, ${String.format("%.2f", position.y)}m)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Accuracy: ${String.format("%.2f", position.accuracy)}m",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Confidence: ${String.format("%.2f", position.confidence * 100)}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } ?: Text(
                        text = "No position data",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Data summary
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Data Summary",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Beacons: ${beacons.size}")
                    sensorData?.let { data ->
                        when (data) {
                            is com.example.myapplication.domain.model.SensorData.Accelerometer -> {
                                Text("Accelerometer: (${String.format("%.2f", data.x)}, ${String.format("%.2f", data.y)}, ${String.format("%.2f", data.z)})")
                            }
                            is com.example.myapplication.domain.model.SensorData.Gyroscope -> {
                                Text("Gyroscope: (${String.format("%.2f", data.x)}, ${String.format("%.2f", data.y)}, ${String.format("%.2f", data.z)})")
                            }
                            is com.example.myapplication.domain.model.SensorData.Magnetometer -> {
                                Text("Magnetometer: (${String.format("%.2f", data.x)}, ${String.format("%.2f", data.y)}, ${String.format("%.2f", data.z)})")
                            }
                            else -> {
                                Text("Sensor data: ${data.javaClass.simpleName}")
                            }
                        }
                    } ?: Text("No sensor data")
                }
            }
        }
    }
}