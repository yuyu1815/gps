package com.example.myapplication.ui.screen

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.myapplication.presentation.viewmodel.MapViewModel

@Composable
internal fun MapCanvas(
    scale: Float,
    offsetX: Float,
    offsetY: Float,
    uiState: MapViewModel.MapUiState?,
    modifier: Modifier = Modifier
) {
    if (uiState == null) return

    var pulseState by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            pulseState = !pulseState
        }
    }

    val pulseFactor by animateFloatAsState(
        targetValue = if (pulseState) 1.2f else 0.8f,
        animationSpec = tween(
            durationMillis = 1000,
            easing = FastOutSlowInEasing
        )
    )

    // 座標に基づく描画は削除

    val mapSemantics = stringResource(id = com.example.myapplication.R.string.map_semantics)
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer(
                scaleX = scale,
                scaleY = scale,
                translationX = offsetX,
                translationY = offsetY
            )
            .semantics {
                contentDescription = mapSemantics
                stateDescription = "Map is at ${(scale * 100).toInt()}% zoom level"
            }
    ) {
        drawGrid()
        drawWalls()

        // ユーザー位置・経路の描画は削除
    }
}

private fun DrawScope.drawGrid() {
    val canvasWidth = size.width
    val canvasHeight = size.height
    for (i in 0..(canvasWidth / MapDefaults.GRID_SIZE).toInt()) {
        val x = i * MapDefaults.GRID_SIZE
        drawLine(
            color = MapDefaults.gridColor,
            start = Offset(x, 0f),
            end = Offset(x, canvasHeight),
            strokeWidth = MapDefaults.GRID_STROKE_WIDTH
        )
    }
    for (i in 0..(canvasHeight / MapDefaults.GRID_SIZE).toInt()) {
        val y = i * MapDefaults.GRID_SIZE
        drawLine(
            color = MapDefaults.gridColor,
            start = Offset(0f, y),
            end = Offset(canvasWidth, y),
            strokeWidth = MapDefaults.GRID_STROKE_WIDTH
        )
    }
}

private fun DrawScope.drawWalls() {
    MapDefaults.simulatedWalls.forEach { (start, end) ->
        drawLine(
            color = MapDefaults.wallColor,
            start = start,
            end = end,
            strokeWidth = MapDefaults.WALL_STROKE_WIDTH,
            cap = StrokeCap.Round
        )
    }
}

// ユーザー位置描画関数は削除

// 経路描画は削除

@Composable
internal fun PositionInfoCard(
    uiState: MapViewModel.MapUiState?,
    isCompactWidth: Boolean,
    viewModel: MapViewModel,
    modifier: Modifier = Modifier
) {
    uiState?.let { state ->
        val confidenceLabel = stringResource(id = com.example.myapplication.R.string.confidence_label)
        val debugOverlayLabel = stringResource(id = com.example.myapplication.R.string.debug_overlay_label)
        val mapTitle = stringResource(id = com.example.myapplication.R.string.map_title)
        val cdShowDebug = stringResource(id = com.example.myapplication.R.string.cd_show_debug_overlay)
        Card(
            modifier = modifier
                .padding(top = MapDefaults.padding, end = if (isCompactWidth) 0.dp else MapDefaults.padding)
                .fillMaxWidth(if (isCompactWidth) MapDefaults.infoCardMaxWidthCompact else MapDefaults.infoCardMaxWidthMedium)
                .semantics { },
            elevation = CardDefaults.cardElevation(defaultElevation = MapDefaults.infoCardElevation),
            shape = RoundedCornerShape(MapDefaults.infoCardCornerRadius)
        ) {
            Column(
                modifier = Modifier.padding(MapDefaults.padding),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = mapTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(MapDefaults.smallPadding))
                // 座標数値の表示は削除
                Text(text = "$confidenceLabel ${(state.userPosition.confidence * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(MapDefaults.smallPadding))
                // 経路表示トグルは削除
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = debugOverlayLabel, style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.width(MapDefaults.smallPadding))
                    Switch(
                        checked = state.showDebugOverlay,
                        onCheckedChange = { viewModel.toggleDebugOverlay() },
                        modifier = Modifier.semantics {
                            contentDescription = cdShowDebug
                            stateDescription = if (state.showDebugOverlay) "Debug overlay is visible" else "Debug overlay is hidden"
                        }
                    )
                }
            }
        }
    }
}

@Composable
internal fun ZoomControls(
    scale: Float,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cdZoomIn = stringResource(id = com.example.myapplication.R.string.cd_zoom_in)
    val cdZoomOut = stringResource(id = com.example.myapplication.R.string.cd_zoom_out)
    Column(modifier = modifier) {
        Button(
            onClick = onZoomIn,
            modifier = Modifier
                .size(MapDefaults.zoomButtonSize)
                .clip(CircleShape)
                .semantics {
                    contentDescription = cdZoomIn
                    stateDescription = "Current zoom level: ${(scale * 100).toInt()}%"
                }
        ) {
            Text(text = "+", style = MaterialTheme.typography.titleMedium)
        }
        Spacer(modifier = Modifier.height(MapDefaults.smallPadding))
        Button(
            onClick = onZoomOut,
            modifier = Modifier
                .size(MapDefaults.zoomButtonSize)
                .clip(CircleShape)
                .semantics {
                    contentDescription = cdZoomOut
                    stateDescription = "Current zoom level: ${(scale * 100).toInt()}%"
                }
        ) {
            Text(text = "-", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
internal fun BottomControls(
    showControls: Boolean,
    onToggleControls: () -> Unit,
    onCenterPosition: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cdSettings = stringResource(id = com.example.myapplication.R.string.cd_settings)
    val btnShow = stringResource(id = com.example.myapplication.R.string.btn_show)
    val btnHide = stringResource(id = com.example.myapplication.R.string.btn_hide)
    val cdCenter = stringResource(id = com.example.myapplication.R.string.cd_center_on_me)
    val sdCenter = stringResource(id = com.example.myapplication.R.string.sd_center_on_me)
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        FloatingActionButton(onClick = { /* Open settings */ }, modifier = Modifier.size(MapDefaults.fabSize)) {
            Icon(imageVector = Icons.Default.Settings, contentDescription = cdSettings)
        }
        Spacer(modifier = Modifier.weight(1f))
        FloatingActionButton(
            onClick = onToggleControls,
            modifier = Modifier
                .size(MapDefaults.fabSize)
                .semantics {
                    contentDescription = if (showControls) btnHide else btnShow
                    stateDescription = if (showControls) "Controls are visible" else "Controls are hidden"
                }
        ) {
            Text(text = if (showControls) btnHide else btnShow, style = MaterialTheme.typography.labelMedium)
        }
        Spacer(modifier = Modifier.weight(1f))
        FloatingActionButton(
            onClick = onCenterPosition,
            modifier = Modifier
                .size(MapDefaults.fabSize)
                .semantics {
                    contentDescription = cdCenter
                    stateDescription = sdCenter
                }
        ) {
            Icon(imageVector = Icons.Default.LocationOn, contentDescription = null)
        }
    }
}

@Composable
internal fun DebugOverlay(
    debugData: MapViewModel.DebugData,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth(MapDefaults.debugOverlayMaxWidth)
            .alpha(MapDefaults.debugOverlayAlpha),
        elevation = CardDefaults.cardElevation(defaultElevation = MapDefaults.debugOverlayElevation),
        shape = RoundedCornerShape(MapDefaults.debugOverlayCornerRadius)
    ) {
        Column(modifier = Modifier.padding(MapDefaults.padding)) {
            Text(text = stringResource(id = com.example.myapplication.R.string.dbg_overlay_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(MapDefaults.smallPadding))
            DebugSection(title = stringResource(id = com.example.myapplication.R.string.dbg_wifi)) {
                Text(text = "${stringResource(id = com.example.myapplication.R.string.dbg_signal_strength)}: ${debugData.wifiSignalStrength} dBm", style = MaterialTheme.typography.bodySmall)
                Text(text = "${stringResource(id = com.example.myapplication.R.string.dbg_access_points)}: ${debugData.wifiAccessPointCount}", style = MaterialTheme.typography.bodySmall)
                Text(text = "${stringResource(id = com.example.myapplication.R.string.dbg_position_accuracy)}: ${debugData.wifiPositionAccuracy} m", style = MaterialTheme.typography.bodySmall)
                Text(text = "Matching Rate: ${debugData.wifiMatchingRate.toInt()}%", style = MaterialTheme.typography.bodySmall)
                Text(text = "Used APs: ${debugData.wifiUsedAPs}", style = MaterialTheme.typography.bodySmall)
            }
            DebugSection(title = stringResource(id = com.example.myapplication.R.string.dbg_slam)) {
                Text(text = "${stringResource(id = com.example.myapplication.R.string.dbg_status)}: ${debugData.slamStatus}", style = MaterialTheme.typography.bodySmall)
                Text(text = "${stringResource(id = com.example.myapplication.R.string.dbg_feature_count)}: ${debugData.slamFeatureCount}", style = MaterialTheme.typography.bodySmall)
                Text(text = "${stringResource(id = com.example.myapplication.R.string.dbg_confidence)}: ${(debugData.slamConfidence * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
            }
            DebugSection(title = stringResource(id = com.example.myapplication.R.string.dbg_pdr)) {
                Text(text = "${stringResource(id = com.example.myapplication.R.string.dbg_step_count)}: ${debugData.pdrStepCount}", style = MaterialTheme.typography.bodySmall)
                Text(text = "${stringResource(id = com.example.myapplication.R.string.dbg_heading)}: ${debugData.pdrHeading.toInt()}°", style = MaterialTheme.typography.bodySmall)
                Text(text = "${stringResource(id = com.example.myapplication.R.string.dbg_step_length)}: ${debugData.pdrStepLength} m", style = MaterialTheme.typography.bodySmall)
            }
            DebugSection(title = stringResource(id = com.example.myapplication.R.string.dbg_sensor_fusion)) {
                Text(text = "${stringResource(id = com.example.myapplication.R.string.dbg_method)}: ${debugData.fusionMethod}", style = MaterialTheme.typography.bodySmall)
                Text(text = "${stringResource(id = com.example.myapplication.R.string.dbg_confidence)}: ${(debugData.fusionConfidence * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
                Text(text = "${stringResource(id = com.example.myapplication.R.string.dbg_error)}: ${debugData.positioningError} m", style = MaterialTheme.typography.bodySmall)
            }
            DebugSection(title = stringResource(id = com.example.myapplication.R.string.dbg_device_sensors)) {
                val accX = debugData.accelerometerValues.getOrNull(0)?.let { "%.2f".format(it) } ?: "0.00"
                val accY = debugData.accelerometerValues.getOrNull(1)?.let { "%.2f".format(it) } ?: "0.00"
                val accZ = debugData.accelerometerValues.getOrNull(2)?.let { "%.2f".format(it) } ?: "0.00"
                Text(text = "${stringResource(id = com.example.myapplication.R.string.dbg_accelerometer)}: [$accX, $accY, $accZ]", style = MaterialTheme.typography.bodySmall)
                val gyroX = debugData.gyroscopeValues.getOrNull(0)?.let { "%.2f".format(it) } ?: "0.00"
                val gyroY = debugData.gyroscopeValues.getOrNull(1)?.let { "%.2f".format(it) } ?: "0.00"
                val gyroZ = debugData.gyroscopeValues.getOrNull(2)?.let { "%.2f".format(it) } ?: "0.00"
                Text(text = "${stringResource(id = com.example.myapplication.R.string.dbg_gyroscope)}: [$gyroX, $gyroY, $gyroZ]", style = MaterialTheme.typography.bodySmall)
                val magX = debugData.magnetometerValues.getOrNull(0)?.let { "%.2f".format(it) } ?: "0.00"
                val magY = debugData.magnetometerValues.getOrNull(1)?.let { "%.2f".format(it) } ?: "0.00"
                val magZ = debugData.magnetometerValues.getOrNull(2)?.let { "%.2f".format(it) } ?: "0.00"
                Text(text = "${stringResource(id = com.example.myapplication.R.string.dbg_magnetometer)}: [$magX, $magY, $magZ]", style = MaterialTheme.typography.bodySmall)
            }

        }
    }
}

@Composable
private fun DebugSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(text = title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        content()
        Spacer(modifier = Modifier.height(4.dp))
    }
}

@Composable
private fun DebugItem(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(text = "$label:", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
        Text(text = value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
    }
}
