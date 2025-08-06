package com.example.myapplication.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.myapplication.domain.model.Beacon
import com.example.myapplication.domain.model.BeaconHealthStatus
import com.example.myapplication.presentation.viewmodel.MapViewModel
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/**
 * A component that displays the health status of all beacons in the system.
 * Shows information such as signal strength, health status, battery level, and alerts.
 */
@Composable
fun BeaconHealthMonitor(
    modifier: Modifier = Modifier,
    viewModel: MapViewModel = koinViewModel()
) {
    val beacons by viewModel.beacons.collectAsState(initial = emptyList())
    val beaconsWithAlerts by viewModel.beaconsWithAlerts.collectAsState()
    val dateFormatter = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Beacon Health Monitor",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Health summary
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Active beacons
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${beacons.count { !it.isStale }}",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.Green
                    )
                    Text(
                        text = "Active",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                
                // Warning beacons
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${beacons.count { it.healthStatus == BeaconHealthStatus.WARNING }}",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFFFFA500) // Orange
                    )
                    Text(
                        text = "Warning",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                
                // Critical beacons
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${beacons.count { it.healthStatus == BeaconHealthStatus.CRITICAL }}",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.Red
                    )
                    Text(
                        text = "Critical",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                
                // Total beacons
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${beacons.size}",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Total",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            
            // Alerts section
            if (beaconsWithAlerts.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Alerts (${beaconsWithAlerts.size})",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.Red,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Divider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    color = Color.Red.copy(alpha = 0.5f)
                )
                
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                ) {
                    items(beaconsWithAlerts) { beacon ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(Color.Red, CircleShape)
                            )
                            
                            Text(
                                text = beacon.name.ifEmpty { beacon.macAddress.takeLast(8) },
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(start = 8.dp, end = 16.dp)
                            )
                            
                            Text(
                                text = beacon.alertMessage,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Beacon",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(2f)
                )
                Text(
                    text = "Health",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "RSSI",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Confidence",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1.5f),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Last Seen",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1.5f),
                    textAlign = TextAlign.Center
                )
            }
            
            Divider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )
            
            // Beacon list
            LazyColumn(
                modifier = Modifier.fillMaxWidth()
            ) {
                items(beacons.sortedWith(
                    compareBy<Beacon> { 
                        when (it.healthStatus) {
                            BeaconHealthStatus.CRITICAL -> 0
                            BeaconHealthStatus.WARNING -> 1
                            BeaconHealthStatus.GOOD -> 2
                            BeaconHealthStatus.UNKNOWN -> 3
                        }
                    }.thenBy { it.isStale }
                )) { beacon ->
                    BeaconHealthItem(
                        beacon = beacon,
                        dateFormatter = dateFormatter
                    )
                    
                    Divider(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

/**
 * A single item in the beacon health list.
 */
@Composable
fun BeaconHealthItem(
    beacon: Beacon,
    dateFormatter: SimpleDateFormat
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Beacon name/ID
            Text(
                text = beacon.name.ifEmpty { beacon.macAddress.takeLast(8) },
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(2f)
            )
            
            // Health status indicator
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                val statusColor = when (beacon.healthStatus) {
                    BeaconHealthStatus.GOOD -> Color.Green
                    BeaconHealthStatus.WARNING -> Color(0xFFFFA500) // Orange
                    BeaconHealthStatus.CRITICAL -> Color.Red
                    BeaconHealthStatus.UNKNOWN -> Color.Gray
                }
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(statusColor, CircleShape)
                        .border(1.dp, Color.Gray.copy(alpha = 0.5f), CircleShape)
                )
            }
            
            // RSSI value
            Text(
                text = "${beacon.filteredRssi} dBm",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
            
            // Confidence level
            ConfidenceIndicator(
                confidence = beacon.distanceConfidence,
                modifier = Modifier.weight(1.5f)
            )
            
            // Last seen timestamp
            Text(
                text = if (beacon.lastSeenTimestamp > 0) {
                    dateFormatter.format(Date(beacon.lastSeenTimestamp))
                } else {
                    "Never"
                },
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1.5f),
                textAlign = TextAlign.Center
            )
        }
        
        // Battery level indicator (if available)
        if (beacon.batteryLevel >= 0) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Battery:",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 16.dp, end = 8.dp)
                )
                
                val batteryColor = when {
                    beacon.batteryLevel > 50 -> Color.Green
                    beacon.batteryLevel > 20 -> Color(0xFFFFA500) // Orange
                    else -> Color.Red
                }
                
                Box(
                    modifier = Modifier
                        .width(100.dp)
                        .height(8.dp)
                        .background(Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .width(100.dp * (beacon.batteryLevel / 100f))
                            .height(8.dp)
                            .background(batteryColor, RoundedCornerShape(4.dp))
                    )
                }
                
                Text(
                    text = "${beacon.batteryLevel}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = batteryColor,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
        
        // Alert message (if any)
        if (beacon.hasAlert) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .background(Color.Red.copy(alpha = 0.2f), CircleShape)
                        .border(1.dp, Color.Red, CircleShape)
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "!",
                        color = Color.Red,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Text(
                    text = beacon.alertMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Red,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}

/**
 * A visual indicator of the confidence level.
 */
@Composable
fun ConfidenceIndicator(
    confidence: Float,
    modifier: Modifier = Modifier
) {
    val confidencePercentage = (confidence * 100).roundToInt()
    val confidenceColor = when {
        confidence >= 0.7f -> Color.Green
        confidence >= 0.4f -> Color(0xFFFFA500) // Orange
        else -> Color.Red
    }
    
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "$confidencePercentage%",
            style = MaterialTheme.typography.bodyMedium,
            color = confidenceColor
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Row(
            modifier = Modifier
                .width(60.dp)
                .height(6.dp)
                .background(Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(3.dp))
        ) {
            Box(
                modifier = Modifier
                    .width(60.dp * confidence)
                    .height(6.dp)
                    .background(confidenceColor, RoundedCornerShape(3.dp))
            )
        }
    }
}