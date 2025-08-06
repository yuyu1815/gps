package com.example.myapplication.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.domain.model.Beacon
import com.example.myapplication.domain.model.SensorData
import com.example.myapplication.domain.model.UserPosition
import com.example.myapplication.presentation.viewmodel.PerformanceMetricsViewModel
import com.example.myapplication.service.PerformanceMetricsCollector
import org.koin.androidx.compose.koinViewModel

/**
 * A controller component that allows toggling the debug overlay visibility.
 * 
 * @param sensorData Current sensor data from device sensors
 * @param userPosition Current calculated user position
 * @param beacons List of detected beacons
 * @param modifier Modifier for customizing the layout
 */
@Composable
fun DebugOverlayController(
    sensorData: SensorData.Combined?,
    userPosition: UserPosition?,
    beacons: List<Beacon>,
    modifier: Modifier = Modifier,
    performanceMetricsViewModel: PerformanceMetricsViewModel = koinViewModel()
) {
    var showDebugOverlay by remember { mutableStateOf(false) }
    
    // Collect performance metrics
    val performanceMetrics by performanceMetricsViewModel.metrics.collectAsState()
    
    // Record frame rendering for performance tracking
    performanceMetricsViewModel.recordFrameRendered()
    
    Box(modifier = modifier) {
        // Debug button
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f))
                .clickable { showDebugOverlay = !showDebugOverlay },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "D",
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        }
        
        // Debug overlay
        if (showDebugOverlay) {
            DebugOverlay(
                sensorData = sensorData,
                userPosition = userPosition,
                beacons = beacons,
                performanceMetrics = performanceMetrics
            )
        }
    }
}