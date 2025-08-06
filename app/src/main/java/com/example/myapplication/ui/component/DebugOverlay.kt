package com.example.myapplication.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.domain.model.Beacon
import com.example.myapplication.domain.model.SensorData
import com.example.myapplication.domain.model.UserPosition
import com.example.myapplication.domain.model.PositionSource
import com.example.myapplication.service.PerformanceMetricsCollector
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * A debug overlay that displays real-time data for debugging purposes.
 * This includes sensor data, positioning information, beacon details, and performance metrics.
 *
 * @param sensorData Current sensor data from device sensors
 * @param userPosition Current calculated user position
 * @param beacons List of detected beacons
 * @param performanceMetrics Current performance metrics (optional)
 * @param modifier Modifier for customizing the layout
 */
@Composable
fun DebugOverlay(
    sensorData: SensorData.Combined?,
    userPosition: UserPosition?,
    beacons: List<Beacon>,
    performanceMetrics: PerformanceMetricsCollector.PerformanceMetrics? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .width(300.dp)
                .background(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(12.dp)
        ) {
            // Header
            Text(
                text = "Debug Information",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Text(
                text = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date()),
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace
            )
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            // Positioning Information
            SectionHeader("Positioning")
            
            userPosition?.let {
                DataRow("Position", String.format("(%.2f, %.2f)", it.x, it.y))
                DataRow("Accuracy", String.format("±%.2f m", it.accuracy))
                DataRow("Source", it.source.name)
                DataRow("Confidence", String.format("%.2f", it.confidence))
                DataRow("Timestamp", SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date(it.timestamp)))
            } ?: Text(
                text = "No position data available",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Red
            )
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            // Sensor Data
            SectionHeader("Sensor Data")
            
            sensorData?.let {
                DataRow("Accelerometer", String.format("(%.2f, %.2f, %.2f)", 
                    it.accelerometer.x, it.accelerometer.y, it.accelerometer.z))
                DataRow("Gyroscope", String.format("(%.2f, %.2f, %.2f)", 
                    it.gyroscope.x, it.gyroscope.y, it.gyroscope.z))
                DataRow("Magnetometer", String.format("(%.2f, %.2f, %.2f)", 
                    it.magnetometer.x, it.magnetometer.y, it.magnetometer.z))
                DataRow("Step Detected", it.stepDetected.toString())
                DataRow("Step Length", String.format("%.2f cm", it.stepLength))
                DataRow("Heading", String.format("%.1f°", it.heading))
            } ?: Text(
                text = "No sensor data available",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Red
            )
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            // Beacon Information
            SectionHeader("Beacons (${beacons.size})")
            
            if (beacons.isNotEmpty()) {
                // Sort beacons by filtered RSSI (strongest first)
                val sortedBeacons = beacons.sortedByDescending { it.filteredRssi }
                
                // Show top 5 beacons
                sortedBeacons.take(5).forEach { beacon ->
                    BeaconDataRow(beacon)
                    Spacer(modifier = Modifier.height(4.dp))
                }
                
                if (sortedBeacons.size > 5) {
                    Text(
                        text = "... and ${sortedBeacons.size - 5} more",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.alpha(0.7f)
                    )
                }
            } else {
                Text(
                    text = "No beacons detected",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Red
                )
            }
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            
            // Performance Metrics
            SectionHeader("Performance")
            
            performanceMetrics?.let {
                DataRow("Frame Rate", String.format("%.1f fps", it.frameRate))
                DataRow("Battery Drain", String.format("%.2f%%/hr", it.batteryDrainRate))
                DataRow("Memory Usage", formatMemorySize(it.memoryUsage))
                DataRow("Position Updates", String.format("%.1f/sec", it.positionUpdateFrequency))
                DataRow("Position Latency", String.format("%d ms", it.positioningLatency))
                DataRow("Scan Frequency", String.format("%.1f/min", it.scanFrequency))
                DataRow("Active Beacons", it.activeBeaconCount.toString())
            } ?: run {
                DataRow("Frame Rate", "-- fps")
                DataRow("Battery Impact", "Unknown")
            }
        }
    }
}

/**
 * Displays a section header in the debug overlay.
 */
@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.secondary,
        modifier = Modifier.padding(bottom = 4.dp)
    )
}

/**
 * Displays a row with a label and value in the debug overlay.
 */
@Composable
private fun DataRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(100.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp
            )
        )
    }
}

/**
 * Formats memory size in bytes to a human-readable string with appropriate units.
 * 
 * @param bytes Memory size in bytes
 * @return Formatted string with appropriate unit (B, KB, MB, GB)
 */
private fun formatMemorySize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
        bytes < 1024 * 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
        else -> String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
    }
}

/**
 * Displays beacon information in the debug overlay.
 */
@Composable
private fun BeaconDataRow(beacon: Beacon) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                shape = RoundedCornerShape(4.dp)
            )
            .padding(4.dp)
    ) {
        // Beacon ID and RSSI
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = beacon.id.takeLast(8),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "${beacon.filteredRssi} dBm",
                style = MaterialTheme.typography.bodySmall,
                color = when {
                    beacon.filteredRssi > -70 -> Color.Green
                    beacon.filteredRssi > -90 -> Color(0xFFFFA000) // Amber
                    else -> Color.Red
                }
            )
        }
        
        // Distance and confidence
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = String.format("%.2f m", beacon.estimatedDistance),
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = String.format("Conf: %.2f", beacon.distanceConfidence),
                style = MaterialTheme.typography.bodySmall,
                color = when {
                    beacon.distanceConfidence > 0.7 -> Color.Green
                    beacon.distanceConfidence > 0.4 -> Color(0xFFFFA000) // Amber
                    else -> Color.Red
                }
            )
        }
    }
}