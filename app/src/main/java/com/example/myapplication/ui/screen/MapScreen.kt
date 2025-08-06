package com.example.myapplication.ui.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import com.example.myapplication.presentation.viewmodel.MapViewModel
import org.koin.androidx.compose.koinViewModel

/**
 * Map screen that displays the indoor map and user position.
 * Provides controls for zooming, panning, and toggling map features.
 */
@Composable
fun MapScreen(
    viewModel: MapViewModel = koinViewModel(),
    isDebugMode: Boolean = false,
    windowSizeClass: WindowSizeClass? = null
) {
    // Determine if we should use a different layout based on screen width
    val isCompactWidth = windowSizeClass?.widthSizeClass == WindowWidthSizeClass.Compact || windowSizeClass == null
    val uiState by viewModel.uiState.collectAsState()
    val beacons by viewModel.beacons.collectAsState(initial = emptyList())
    
    // State for map zoom and pan
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    var showControls by remember { mutableStateOf(true) }
    
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(0.5f, 3f)
                        offsetX += pan.x
                        offsetY += pan.y
                    }
                }
        ) {
            // Map view with simulated grid
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offsetX,
                        translationY = offsetY
                    )
                    .semantics {
                        contentDescription = "Indoor map with user position"
                        stateDescription = "Map is at ${(scale * 100).toInt()}% zoom level"
                    }
            ) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                val gridSize = 50f
                
                // Draw grid
                val gridColor = Color.LightGray.copy(alpha = 0.3f)
                
                // Vertical lines
                for (i in 0..(canvasWidth / gridSize).toInt()) {
                    val x = i * gridSize
                    drawLine(
                        color = gridColor,
                        start = Offset(x, 0f),
                        end = Offset(x, canvasHeight),
                        strokeWidth = 1f
                    )
                }
                
                // Horizontal lines
                for (i in 0..(canvasHeight / gridSize).toInt()) {
                    val y = i * gridSize
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end = Offset(canvasWidth, y),
                        strokeWidth = 1f
                    )
                }
                
                // Draw simulated walls
                val wallColor = Color.DarkGray
                drawLine(
                    color = wallColor,
                    start = Offset(100f, 100f),
                    end = Offset(300f, 100f),
                    strokeWidth = 5f,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = wallColor,
                    start = Offset(300f, 100f),
                    end = Offset(300f, 400f),
                    strokeWidth = 5f,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = wallColor,
                    start = Offset(300f, 400f),
                    end = Offset(100f, 400f),
                    strokeWidth = 5f,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = wallColor,
                    start = Offset(100f, 400f),
                    end = Offset(100f, 100f),
                    strokeWidth = 5f,
                    cap = StrokeCap.Round
                )
                
                // Draw simulated beacons if enabled
                uiState?.let { state ->
                    if (state.showBeacons) {
                        val beaconColor = Color.Blue
                        val beaconRadius = 10f
                        
                        // Simulated beacon positions
                        val beaconPositions = listOf(
                            Offset(100f, 100f),
                            Offset(300f, 100f),
                            Offset(300f, 400f),
                            Offset(100f, 400f)
                        )
                        
                        beaconPositions.forEach { position ->
                            drawCircle(
                                color = beaconColor,
                                radius = beaconRadius,
                                center = position
                            )
                            
                            // Draw beacon range
                            drawCircle(
                                color = beaconColor.copy(alpha = 0.1f),
                                radius = 100f,
                                center = position
                            )
                            
                            drawCircle(
                                color = beaconColor,
                                radius = beaconRadius,
                                center = position,
                                style = Stroke(width = 2f)
                            )
                        }
                    }
                    
                    // Draw user position
                    val userPosition = Offset(
                        200f + state.userPosition.x * 10, 
                        250f + state.userPosition.y * 10
                    )
                    
                    // User position confidence circle
                    drawCircle(
                        color = Color.Green.copy(alpha = 0.2f),
                        radius = 30f * state.userPosition.confidence,
                        center = userPosition
                    )
                    
                    // User position dot
                    drawCircle(
                        color = Color.Green,
                        radius = 10f,
                        center = userPosition
                    )
                    
                    // Draw path if enabled
                    if (state.showPath && state.pathPoints.isNotEmpty()) {
                        val pathColor = Color.Red.copy(alpha = 0.7f)
                        
                        // Simulated path points
                        val pathPoints = listOf(
                            Offset(150f, 150f),
                            Offset(200f, 200f),
                            Offset(250f, 220f),
                            userPosition
                        )
                        
                        // Draw path lines
                        for (i in 0 until pathPoints.size - 1) {
                            drawLine(
                                color = pathColor,
                                start = pathPoints[i],
                                end = pathPoints[i + 1],
                                strokeWidth = 3f
                            )
                        }
                    }
                }
            }
            
            // Position info card
            uiState?.let { state ->
                if (showControls) {
                    Card(
                        modifier = Modifier
                            .align(if (isCompactWidth) Alignment.TopCenter else Alignment.TopEnd)
                            .padding(top = 16.dp, end = if (isCompactWidth) 0.dp else 16.dp)
                            .fillMaxWidth(if (isCompactWidth) 0.8f else 0.4f)
                            .semantics {
                                contentDescription = "Position information card"
                                stateDescription = "Position: (${state.userPosition.x.toInt()}, ${state.userPosition.y.toInt()}), Confidence: ${(state.userPosition.confidence * 100).toInt()}%"
                            },
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Indoor Positioning Map",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = "Position: (${state.userPosition.x.toInt()}, ${state.userPosition.y.toInt()})",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            
                            Text(
                                text = "Confidence: ${(state.userPosition.confidence * 100).toInt()}%",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Beacons:",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                
                                Spacer(modifier = Modifier.width(8.dp))
                                
                                Switch(
                                    checked = state.showBeacons,
                                    onCheckedChange = { viewModel.toggleBeaconDisplay() },
                                    modifier = Modifier.semantics {
                                        contentDescription = "Show beacons"
                                        stateDescription = if (state.showBeacons) "Beacons are visible" else "Beacons are hidden"
                                    }
                                )
                                
                                Spacer(modifier = Modifier.width(16.dp))
                                
                                Text(
                                    text = "Path:",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                
                                Spacer(modifier = Modifier.width(8.dp))
                                
                                Switch(
                                    checked = state.showPath,
                                    onCheckedChange = { viewModel.togglePathDisplay() },
                                    modifier = Modifier.semantics {
                                        contentDescription = "Show path"
                                        stateDescription = if (state.showPath) "Path is visible" else "Path is hidden"
                                    }
                                )
                            }
                        }
                    }
                }
            }
            
            // Zoom controls
            if (showControls) {
                Column(
                    modifier = Modifier
                        .align(if (isCompactWidth) Alignment.CenterEnd else Alignment.BottomEnd)
                        .padding(
                            end = 16.dp,
                            bottom = if (isCompactWidth) 0.dp else 80.dp
                        )
                ) {
                    Button(
                        onClick = { scale = (scale * 1.2f).coerceIn(0.5f, 3f) },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .semantics { 
                                contentDescription = "Zoom in"
                                stateDescription = "Current zoom level: ${(scale * 100).toInt()}%"
                            }
                    ) {
                        Text(
                            text = "+",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                
                    Spacer(modifier = Modifier.height(8.dp))
                
                    Button(
                        onClick = { scale = (scale / 1.2f).coerceIn(0.5f, 3f) },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .semantics { 
                                contentDescription = "Zoom out"
                                stateDescription = "Current zoom level: ${(scale * 100).toInt()}%"
                            }
                    ) {
                        Text(
                            text = "-",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
            
            // Bottom controls
            Row(
                modifier = Modifier
                    .align(if (isCompactWidth) Alignment.BottomCenter else Alignment.BottomStart)
                    .fillMaxWidth(if (isCompactWidth) 1f else 0.5f)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Settings button
                FloatingActionButton(
                    onClick = { /* Open settings */ },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings"
                    )
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                // Toggle controls visibility
                FloatingActionButton(
                    onClick = { showControls = !showControls },
                    modifier = Modifier
                        .size(48.dp)
                        .semantics { 
                            contentDescription = if (showControls) "Hide controls" else "Show controls"
                            stateDescription = if (showControls) "Controls are visible" else "Controls are hidden"
                        }
                ) {
                    Text(
                        text = if (showControls) "Hide" else "Show",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                // Center position button
                FloatingActionButton(
                    onClick = {
                        // Reset zoom and center on user
                        scale = 1f
                        offsetX = 0f
                        offsetY = 0f
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .semantics { 
                            contentDescription = "Center on my location"
                            stateDescription = "Resets zoom and centers the map on your current position"
                        }
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null // Content description is provided by parent
                    )
                }
            }
        }
    }
}