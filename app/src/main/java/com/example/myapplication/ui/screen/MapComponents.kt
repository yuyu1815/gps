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

    val animatedUserX by animateFloatAsState(
        targetValue = MapDefaults.USER_POSITION_X_OFFSET + uiState.userPosition.x * MapDefaults.USER_POSITION_MULTIPLIER,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )
    val animatedUserY by animateFloatAsState(
        targetValue = MapDefaults.USER_POSITION_Y_OFFSET + uiState.userPosition.y * MapDefaults.USER_POSITION_MULTIPLIER,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )
    val animatedConfidenceRadius by animateFloatAsState(
        targetValue = uiState.userPosition.confidence * MapDefaults.USER_CONFIDENCE_RADIUS_MULTIPLIER,
        animationSpec = tween(
            durationMillis = 500,
            easing = FastOutSlowInEasing
        )
    )

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

        val userPosition = Offset(animatedUserX, animatedUserY)

        val heading = uiState.debugData.pdrHeading
        drawUserPosition(userPosition, animatedConfidenceRadius, pulseFactor, heading)

        if (uiState.showPath && uiState.pathPoints.isNotEmpty()) {
            val historicalPath = uiState.pathPoints.map {
                Offset(
                    MapDefaults.USER_POSITION_X_OFFSET + it.x * MapDefaults.USER_POSITION_MULTIPLIER,
                    MapDefaults.USER_POSITION_Y_OFFSET + it.y * MapDefaults.USER_POSITION_MULTIPLIER
                )
            }
            drawPath(historicalPath + userPosition)
        }
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

private fun DrawScope.drawUserPosition(
    userPosition: Offset,
    confidenceRadius: Float,
    pulseFactor: Float = 1.0f,
    heading: Float = 0f
) {
    val normalizedRadius = confidenceRadius.coerceIn(MapDefaults.MIN_CONFIDENCE_RADIUS, MapDefaults.MAX_CONFIDENCE_RADIUS)
    val confidenceLevel = (MapDefaults.MAX_CONFIDENCE_RADIUS - normalizedRadius) /
            (MapDefaults.MAX_CONFIDENCE_RADIUS - MapDefaults.MIN_CONFIDENCE_RADIUS)
                .coerceIn(0f, 1f)

    val confidenceColor = when {
        confidenceLevel >= 0.7f -> androidx.compose.ui.graphics.Color(
            red = 0f,
            green = 0.8f - (1f - confidenceLevel) * 0.3f,
            blue = 0.2f + (1f - confidenceLevel) * 0.8f,
            alpha = 1f
        )
        confidenceLevel >= 0.4f -> androidx.compose.ui.graphics.Color(
            red = (0.7f - confidenceLevel) * 1.5f,
            green = 0.7f,
            blue = (confidenceLevel - 0.4f) * 1.5f,
            alpha = 1f
        )
        else -> androidx.compose.ui.graphics.Color(
            red = 0.9f,
            green = confidenceLevel * 2f,
            blue = 0f,
            alpha = 1f
        )
    }

    drawCircle(color = confidenceColor.copy(alpha = 0.1f), radius = confidenceRadius, center = userPosition)
    drawCircle(color = confidenceColor.copy(alpha = 0.15f), radius = confidenceRadius * 0.8f, center = userPosition)
    drawCircle(color = confidenceColor.copy(alpha = 0.2f), radius = confidenceRadius * 0.6f, center = userPosition)

    val dashPathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
    drawCircle(
        color = confidenceColor.copy(alpha = 0.6f),
        radius = confidenceRadius * pulseFactor,
        center = userPosition,
        style = Stroke(width = 2f, pathEffect = dashPathEffect)
    )

    drawCircle(
        color = confidenceColor.copy(alpha = 0.7f),
        radius = confidenceRadius * 0.4f,
        center = userPosition,
        style = Stroke(width = 1.5f)
    )

    drawCircle(
        color = MapDefaults.userPositionDotColor,
        radius = MapDefaults.USER_POSITION_DOT_RADIUS * (0.8f + (pulseFactor - 1f) * 0.5f),
        center = userPosition
    )

    val arrowLength = MapDefaults.USER_POSITION_DOT_RADIUS * 3f
    val headingRadians = Math.toRadians(heading.toDouble()).toFloat()
    val arrowEnd = Offset(
        x = userPosition.x + arrowLength * kotlin.math.sin(headingRadians),
        y = userPosition.y - arrowLength * kotlin.math.cos(headingRadians)
    )
    drawLine(color = MapDefaults.userPositionDotColor, start = userPosition, end = arrowEnd, strokeWidth = 3f, cap = StrokeCap.Round)
    val arrowHeadSize = MapDefaults.USER_POSITION_DOT_RADIUS * 1.2f
    val arrowLeftPoint = Offset(
        x = arrowEnd.x + arrowHeadSize * kotlin.math.sin(headingRadians - 2.5f),
        y = arrowEnd.y - arrowHeadSize * kotlin.math.cos(headingRadians - 2.5f)
    )
    val arrowRightPoint = Offset(
        x = arrowEnd.x + arrowHeadSize * kotlin.math.sin(headingRadians + 2.5f),
        y = arrowEnd.y - arrowHeadSize * kotlin.math.cos(headingRadians + 2.5f)
    )
    drawLine(color = MapDefaults.userPositionDotColor, start = arrowEnd, end = arrowLeftPoint, strokeWidth = 3f, cap = StrokeCap.Round)
    drawLine(color = MapDefaults.userPositionDotColor, start = arrowEnd, end = arrowRightPoint, strokeWidth = 3f, cap = StrokeCap.Round)
    drawCircle(color = MapDefaults.userPositionDotColor.copy(alpha = (1.2f - pulseFactor) * 0.3f), radius = MapDefaults.USER_POSITION_DOT_RADIUS * 2f * pulseFactor, center = userPosition)
}

private fun DrawScope.drawPath(pathPoints: List<Offset>) {
    if (pathPoints.size < 2) return
    for (i in 0 until pathPoints.size - 1) {
        drawLine(color = MapDefaults.pathColor, start = pathPoints[i], end = pathPoints[i + 1], strokeWidth = MapDefaults.PATH_STROKE_WIDTH)
    }
}

@Composable
internal fun PositionInfoCard(
    uiState: MapViewModel.MapUiState?,
    isCompactWidth: Boolean,
    viewModel: MapViewModel,
    modifier: Modifier = Modifier
) {
    uiState?.let { state ->
        val positionLabel = stringResource(id = com.example.myapplication.R.string.position_label)
        val confidenceLabel = stringResource(id = com.example.myapplication.R.string.confidence_label)
        val pathLabel = stringResource(id = com.example.myapplication.R.string.path_label)
        val debugOverlayLabel = stringResource(id = com.example.myapplication.R.string.debug_overlay_label)
        val mapTitle = stringResource(id = com.example.myapplication.R.string.map_title)
        val cdShowPath = stringResource(id = com.example.myapplication.R.string.cd_show_path)
        val cdShowDebug = stringResource(id = com.example.myapplication.R.string.cd_show_debug_overlay)
        Card(
            modifier = modifier
                .padding(top = MapDefaults.padding, end = if (isCompactWidth) 0.dp else MapDefaults.padding)
                .fillMaxWidth(if (isCompactWidth) MapDefaults.infoCardMaxWidthCompact else MapDefaults.infoCardMaxWidthMedium)
                .semantics {
                    contentDescription = "Position information card"
                    stateDescription =
                        "$positionLabel (${state.userPosition.x.toInt()}, ${state.userPosition.y.toInt()}), $confidenceLabel ${(state.userPosition.confidence * 100).toInt()}%"
                },
            elevation = CardDefaults.cardElevation(defaultElevation = MapDefaults.infoCardElevation),
            shape = RoundedCornerShape(MapDefaults.infoCardCornerRadius)
        ) {
            Column(
                modifier = Modifier.padding(MapDefaults.padding),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = mapTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(MapDefaults.smallPadding))
                Text(text = "$positionLabel (${state.userPosition.x.toInt()}, ${state.userPosition.y.toInt()})", style = MaterialTheme.typography.bodyMedium)
                Text(text = "$confidenceLabel ${(state.userPosition.confidence * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(MapDefaults.smallPadding))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = pathLabel, style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.width(MapDefaults.smallPadding))
                    Switch(
                        checked = state.showPath,
                        onCheckedChange = { viewModel.togglePathDisplay() },
                        modifier = Modifier.semantics {
                            contentDescription = cdShowPath
                            stateDescription = if (state.showPath) "Path is visible" else "Path is hidden"
                        }
                    )
                }
                Spacer(modifier = Modifier.height(MapDefaults.smallPadding))
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
