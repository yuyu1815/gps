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
import androidx.compose.material3.HorizontalDivider
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
import com.example.myapplication.domain.model.SensorData
import com.example.myapplication.domain.model.UserPosition
import com.example.myapplication.domain.model.PositionSource
import com.example.myapplication.service.PerformanceMetricsCollector
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * A debug overlay that displays real-time data for debugging purposes.
 * This includes sensor data, positioning information, and performance metrics.
 *
 * @param sensorData Current sensor data from device sensors
 * @param userPosition Current calculated user position
 * @param performanceMetrics Current performance metrics (optional)
 * @param modifier Modifier for customizing the layout
 */
@Composable
fun DebugOverlay(
    sensorData: SensorData.Combined?,
    userPosition: UserPosition?,
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
                text = androidx.compose.ui.res.stringResource(id = com.example.myapplication.R.string.dbg_info_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Text(
                text = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date()),
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace
            )
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            
            // Positioning Information
            SectionHeader(androidx.compose.ui.res.stringResource(id = com.example.myapplication.R.string.dbg_positioning))
            
            userPosition?.let {
                DataRow(androidx.compose.ui.res.stringResource(id = com.example.myapplication.R.string.dbg_position), String.format("(%.2f, %.2f)", it.x, it.y))
                DataRow(androidx.compose.ui.res.stringResource(id = com.example.myapplication.R.string.dbg_accuracy), String.format("±%.2f m", it.accuracy))
                DataRow(androidx.compose.ui.res.stringResource(id = com.example.myapplication.R.string.dbg_source), it.source.name)
                DataRow(androidx.compose.ui.res.stringResource(id = com.example.myapplication.R.string.dbg_confidence), String.format("%.2f", it.confidence))
                DataRow(androidx.compose.ui.res.stringResource(id = com.example.myapplication.R.string.dbg_timestamp), SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date(it.timestamp)))
            } ?: Text(
                text = androidx.compose.ui.res.stringResource(id = com.example.myapplication.R.string.dbg_no_position),
                style = MaterialTheme.typography.bodySmall,
                color = Color.Red
            )
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            
            // Sensor Data
            SectionHeader(androidx.compose.ui.res.stringResource(id = com.example.myapplication.R.string.dbg_sensor_data))
            
            sensorData?.let {
                DataRow(androidx.compose.ui.res.stringResource(id = com.example.myapplication.R.string.dbg_accelerometer), String.format("(%.2f, %.2f, %.2f)", 
                    it.accelerometer.x, it.accelerometer.y, it.accelerometer.z))
                DataRow(androidx.compose.ui.res.stringResource(id = com.example.myapplication.R.string.dbg_gyroscope), String.format("(%.2f, %.2f, %.2f)", 
                    it.gyroscope.x, it.gyroscope.y, it.gyroscope.z))
                DataRow(androidx.compose.ui.res.stringResource(id = com.example.myapplication.R.string.dbg_magnetometer), String.format("(%.2f, %.2f, %.2f)", 
                    it.magnetometer.x, it.magnetometer.y, it.magnetometer.z))
                DataRow(androidx.compose.ui.res.stringResource(id = com.example.myapplication.R.string.dbg_step_detected), it.stepDetected.toString())
                DataRow(androidx.compose.ui.res.stringResource(id = com.example.myapplication.R.string.dbg_step_length), String.format("%.2f cm", it.stepLength))
                DataRow(androidx.compose.ui.res.stringResource(id = com.example.myapplication.R.string.dbg_heading), String.format("%.1f°", it.heading))
            } ?: Text(
                text = androidx.compose.ui.res.stringResource(id = com.example.myapplication.R.string.dbg_no_sensor),
                style = MaterialTheme.typography.bodySmall,
                color = Color.Red
            )
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            
            // Performance Metrics
            SectionHeader(androidx.compose.ui.res.stringResource(id = com.example.myapplication.R.string.dbg_performance))
            
            performanceMetrics?.let {
                DataRow(androidx.compose.ui.res.stringResource(id = com.example.myapplication.R.string.dbg_frame_rate), String.format("%.1f fps", it.frameRate))
                DataRow(androidx.compose.ui.res.stringResource(id = com.example.myapplication.R.string.dbg_battery_drain), String.format("%.2f%%/hr", it.batteryDrainRate))
                DataRow(androidx.compose.ui.res.stringResource(id = com.example.myapplication.R.string.dbg_memory_usage), formatMemorySize(it.memoryUsage))
                DataRow(androidx.compose.ui.res.stringResource(id = com.example.myapplication.R.string.dbg_position_updates), String.format("%.1f/sec", it.positionUpdateFrequency))
                DataRow(androidx.compose.ui.res.stringResource(id = com.example.myapplication.R.string.dbg_position_latency), String.format("%d ms", it.positioningLatency))
                DataRow(androidx.compose.ui.res.stringResource(id = com.example.myapplication.R.string.dbg_scan_frequency), String.format("%.1f/min", it.scanFrequency))
            } ?: run {
                DataRow(androidx.compose.ui.res.stringResource(id = com.example.myapplication.R.string.dbg_frame_rate), "-- fps")
                DataRow(androidx.compose.ui.res.stringResource(id = com.example.myapplication.R.string.dbg_battery_impact), androidx.compose.ui.res.stringResource(id = com.example.myapplication.R.string.dbg_unknown))
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