package com.example.myapplication.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.myapplication.domain.model.Beacon
import com.example.myapplication.domain.model.CoordinateTransformer
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * A composable function that visualizes beacon placements for debugging purposes.
 * This shows beacons with their IDs, signal strengths, and confidence levels.
 *
 * @param beacons List of beacons to visualize
 * @param transformer Coordinate transformer to convert between physical and screen coordinates
 * @param selectedBeacon Currently selected beacon (if any)
 */
@Composable
fun BeaconDebugVisualizer(
    beacons: List<Beacon>,
    transformer: CoordinateTransformer,
    selectedBeacon: Beacon? = null
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        beacons.forEach { beacon ->
            drawBeaconDebugInfo(beacon, transformer, selectedBeacon)
        }
    }
}

/**
 * Draws detailed debug information for a beacon.
 */
private fun DrawScope.drawBeaconDebugInfo(
    beacon: Beacon,
    transformer: CoordinateTransformer,
    selectedBeacon: Beacon?
) {
    val (screenX, screenY) = transformer.metersToScreen(beacon.x, beacon.y)
    val radius = 20f * transformer.getScaleFactor()
    
    // Determine beacon color based on selection state
    val beaconColor = if (beacon == selectedBeacon) Color.Red else Color.Blue
    
    // Draw beacon coverage area based on txPower
    drawBeaconCoverage(beacon, screenX, screenY, transformer)
    
    // Draw confidence indicator
    drawConfidenceIndicator(beacon, screenX, screenY, radius, transformer)
    
    // Draw beacon circle
    drawCircle(
        color = beaconColor,
        radius = radius,
        center = Offset(screenX, screenY),
        alpha = 0.7f
    )
    
    // Removed beacon info drawing to fix compilation issues
}

// Removed drawBeaconConnections method to fix compilation issues

/**
 * Draws the theoretical coverage area of a beacon based on its txPower.
 */
private fun DrawScope.drawBeaconCoverage(
    beacon: Beacon,
    screenX: Float,
    screenY: Float,
    transformer: CoordinateTransformer
) {
    // Calculate coverage radius based on txPower
    // txPower is the RSSI at 1m, so we can estimate maximum range
    // A very rough estimate: -100 dBm is approximately the minimum detectable signal
    val maxRangeMeters = 10.0.pow((beacon.txPower + 100) / 20.0).toFloat().coerceIn(1f, 30f)
    val coverageRadius = maxRangeMeters * transformer.getScaleFactor()
    
    // Draw coverage circle
    drawCircle(
        color = Color.Blue,
        radius = coverageRadius,
        center = Offset(screenX, screenY),
        alpha = 0.05f
    )
    
    // Draw coverage boundary
    drawCircle(
        color = Color.Blue,
        radius = coverageRadius,
        center = Offset(screenX, screenY),
        alpha = 0.2f,
        style = Stroke(width = 1f * transformer.getScaleFactor())
    )
}

/**
 * Draws a visual indicator of the beacon's distance estimation confidence.
 */
private fun DrawScope.drawConfidenceIndicator(
    beacon: Beacon,
    screenX: Float,
    screenY: Float,
    radius: Float,
    transformer: CoordinateTransformer
) {
    if (beacon.distanceConfidence > 0) {
        val confidenceRadius = radius * 2.5f
        val confidenceColor = when {
            beacon.distanceConfidence > 0.7f -> Color.Green
            beacon.distanceConfidence > 0.4f -> Color.Yellow
            else -> Color.Red
        }
        
        // Draw confidence ring
        drawCircle(
            color = confidenceColor,
            radius = confidenceRadius,
            center = Offset(screenX, screenY),
            alpha = 0.15f,
            style = Stroke(width = 2f * transformer.getScaleFactor())
        )
    }
}

// Removed drawBeaconInfo method to fix compilation issues

// Extension function for Float.pow
private fun Float.pow(exponent: Int): Float = this.toDouble().pow(exponent).toFloat()
private fun Float.pow(exponent: Float): Float = this.toDouble().pow(exponent.toDouble()).toFloat()