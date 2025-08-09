package com.example.myapplication.ui.screen

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Screen for calibrating the positioning system.
 * Provides tools for TxPower calibration and environmental factor adjustment.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalibrationScreen(
    onNavigateBack: () -> Unit = {}
) {
    var currentTab by remember { mutableStateOf(CalibrationTab.TX_POWER) }
    var isCalibrating by remember { mutableStateOf(false) }
    var calibrationProgress by remember { mutableFloatStateOf(0f) }
    var environmentalFactor by remember { mutableFloatStateOf(2.0f) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Calibration") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Calibration type tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                CalibrationTab.values().forEach { tab ->
                    Button(
                        onClick = { currentTab = tab },
                        modifier = Modifier.weight(1f),
                        enabled = !isCalibrating
                    ) {
                        Text(
                            text = when (tab) {
                                CalibrationTab.TX_POWER -> "TxPower"
                                CalibrationTab.ENVIRONMENTAL -> "Environment"
                                CalibrationTab.WIFI_FINGERPRINTING -> "Wi-Fi"
                            },
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                    
                    if (tab != CalibrationTab.values().last()) {
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                }
            }
            
            // Content based on selected tab
            when (currentTab) {
                CalibrationTab.TX_POWER -> {
                    TxPowerCalibration(
                        isCalibrating = isCalibrating,
                        progress = calibrationProgress,
                        onStartCalibration = {
                            isCalibrating = true
                            // Simulate calibration progress
                            // In a real app, this would be connected to actual calibration logic
                            calibrationProgress = 0f
                            // Simulate progress over time
                        },
                        onStopCalibration = {
                            isCalibrating = false
                            calibrationProgress = 0f
                        }
                    )
                }
                
                CalibrationTab.ENVIRONMENTAL -> {
                    EnvironmentalFactorCalibration(
                        environmentalFactor = environmentalFactor,
                        onEnvironmentalFactorChange = { environmentalFactor = it }
                    )
                }

                CalibrationTab.WIFI_FINGERPRINTING -> {
                    WifiFingerprintingCalibration()
                }
            }
        }
    }
}

@Composable
fun WifiFingerprintingCalibration() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Wi-Fi Fingerprinting",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun TxPowerCalibration(
    isCalibrating: Boolean,
    progress: Float,
    onStartCalibration: () -> Unit,
    onStopCalibration: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "TxPower Calibration",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Place your device exactly 1 meter away from the beacon and start calibration.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Calibration visualization
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (isCalibrating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(160.dp),
                        progress = { progress }
                    )
                }
                
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isCalibrating) "${(progress * 100).toInt()}%" else "Ready",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    
                    if (isCalibrating) {
                        Text(
                            text = "Calibrating...",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Calibration controls
            Button(
                onClick = if (isCalibrating) onStopCalibration else onStartCalibration,
                modifier = Modifier.fillMaxWidth(0.7f)
            ) {
                Text(
                    text = if (isCalibrating) "Stop Calibration" else "Start Calibration",
                    style = MaterialTheme.typography.labelLarge
                )
            }
            
            if (isCalibrating) {
                Spacer(modifier = Modifier.height(16.dp))
                
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(0.7f)
                )
            }
        }
    }
}

@Composable
fun EnvironmentalFactorCalibration(
    environmentalFactor: Float,
    onEnvironmentalFactorChange: (Float) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Environmental Factor Adjustment",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Adjust the environmental factor based on the physical environment:",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Environment type descriptions
            EnvironmentDescription(
                value = 2.0f,
                name = "Free Space",
                description = "Open areas with no obstacles"
            )
            
            EnvironmentDescription(
                value = 2.5f,
                name = "Office",
                description = "Typical office environment with furniture"
            )
            
            EnvironmentDescription(
                value = 3.0f,
                name = "Dense",
                description = "Dense environment with many obstacles"
            )
            
            EnvironmentDescription(
                value = 3.5f,
                name = "Very Dense",
                description = "Very dense environment with walls and metal objects"
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Environmental factor slider
            Text(
                text = "Environmental Factor: ${String.format("%.1f", environmentalFactor)}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Slider(
                value = environmentalFactor,
                onValueChange = onEnvironmentalFactorChange,
                valueRange = 2.0f..4.0f,
                steps = 20,
                modifier = Modifier.fillMaxWidth(0.9f)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(0.9f),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "2.0",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Text(
                    text = "4.0",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Apply button
            Button(
                onClick = { /* Apply environmental factor */ },
                modifier = Modifier.fillMaxWidth(0.7f)
            ) {
                Text(
                    text = "Apply Changes",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
fun EnvironmentDescription(
    value: Float,
    name: String,
    description: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = String.format("%.1f", value),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(40.dp)
        )
        
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = description,
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

enum class CalibrationTab {
    TX_POWER,
    ENVIRONMENTAL,
    WIFI_FINGERPRINTING
}
