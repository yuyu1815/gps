package com.example.myapplication.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.myapplication.domain.model.Beacon
import kotlin.math.min

/**
 * A composable that visualizes the distance uncertainty for a beacon.
 * 
 * This component draws a circle representing the estimated distance to the beacon,
 * with a semi-transparent area around it representing the uncertainty in the estimation.
 * The transparency of the uncertainty area is based on the confidence level of the distance estimation.
 * 
 * @param beacon The beacon to visualize
 * @param modifier Modifier for the component
 * @param distanceScale Scale factor for converting distance to pixels (pixels per meter)
 * @param beaconColor Color for the beacon
 * @param distanceColor Color for the distance circle
 * @param uncertaintyColor Color for the uncertainty area
 */
@Composable
fun DistanceUncertaintyVisualizer(
    beacon: Beacon,
    modifier: Modifier = Modifier,
    distanceScale: Float = 10f,
    beaconColor: Color = Color.Blue,
    distanceColor: Color = Color.Green,
    uncertaintyColor: Color = Color.Red
) {
    Box(
        modifier = modifier.size(500.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            
            // Draw the beacon at the center
            drawBeacon(center, beaconColor)
            
            // Calculate the radius for the distance circle
            val distanceRadius = beacon.estimatedDistance * distanceScale
            
            // Calculate the uncertainty radius based on confidence
            // Lower confidence means larger uncertainty
            val uncertaintyFactor = 1f - beacon.distanceConfidence
            val uncertaintyRadius = distanceRadius * (1f + uncertaintyFactor)
            
            // Draw the uncertainty area
            drawUncertaintyArea(center, distanceRadius, uncertaintyRadius, uncertaintyColor, beacon.distanceConfidence)
            
            // Draw the estimated distance circle
            drawDistanceCircle(center, distanceRadius, distanceColor)
        }
    }
}

/**
 * Draws a beacon at the specified position.
 */
private fun DrawScope.drawBeacon(center: Offset, color: Color) {
    // Draw a filled circle for the beacon
    drawCircle(
        color = color,
        radius = 10f,
        center = center
    )
}

/**
 * Draws a circle representing the estimated distance.
 */
private fun DrawScope.drawDistanceCircle(center: Offset, radius: Float, color: Color) {
    // Draw a stroked circle for the distance
    drawCircle(
        color = color,
        radius = radius,
        center = center,
        style = Stroke(width = 2f)
    )
}

/**
 * Draws a semi-transparent area representing the uncertainty in the distance estimation.
 */
private fun DrawScope.drawUncertaintyArea(
    center: Offset,
    innerRadius: Float,
    outerRadius: Float,
    color: Color,
    confidence: Float
) {
    // Calculate alpha based on confidence (higher confidence = more transparent uncertainty)
    val alpha = 0.3f * (1f - confidence)
    
    // Draw a semi-transparent filled circle for the uncertainty area
    drawCircle(
        color = color.copy(alpha = alpha),
        radius = outerRadius,
        center = center
    )
    
    // Draw a semi-transparent filled circle for the inner area
    drawCircle(
        color = color.copy(alpha = alpha / 2),
        radius = innerRadius,
        center = center
    )
}

/**
 * A composable that visualizes multiple beacons with their distance uncertainties.
 * 
 * @param beacons List of beacons to visualize
 * @param modifier Modifier for the component
 * @param distanceScale Scale factor for converting distance to pixels (pixels per meter)
 * @param beaconColors Map of beacon MAC addresses to colors
 * @param distanceColor Color for the distance circles
 * @param uncertaintyColor Color for the uncertainty areas
 */
@Composable
fun MultiBeaconUncertaintyVisualizer(
    beacons: List<Beacon>,
    modifier: Modifier = Modifier,
    distanceScale: Float = 10f,
    beaconColors: Map<String, Color> = emptyMap(),
    distanceColor: Color = Color.Green,
    uncertaintyColor: Color = Color.Red
) {
    Box(
        modifier = modifier.size(500.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            
            // Draw each beacon's uncertainty and distance
            beacons.forEach { beacon ->
                // Get color for this beacon, or use default
                val beaconColor = beaconColors[beacon.macAddress] ?: Color.Blue
                
                // Calculate the radius for the distance circle
                val distanceRadius = beacon.estimatedDistance * distanceScale
                
                // Calculate the uncertainty radius based on confidence
                val uncertaintyFactor = 1f - beacon.distanceConfidence
                val uncertaintyRadius = distanceRadius * (1f + uncertaintyFactor)
                
                // Draw the uncertainty area
                drawUncertaintyArea(center, distanceRadius, uncertaintyRadius, uncertaintyColor, beacon.distanceConfidence)
                
                // Draw the estimated distance circle
                drawDistanceCircle(center, distanceRadius, distanceColor)
            }
            
            // Draw a marker at the center representing the user's position
            drawCircle(
                color = Color.Black,
                radius = 15f,
                center = center
            )
        }
    }
}

/**
 * A composable that visualizes the triangulation result with uncertainty.
 * 
 * This component shows the estimated position based on triangulation from multiple beacons,
 * along with the uncertainty in the position estimation.
 * 
 * @param beacons List of beacons used for triangulation
 * @param estimatedX Estimated X coordinate
 * @param estimatedY Estimated Y coordinate
 * @param positionUncertainty Uncertainty radius for the position estimation
 * @param modifier Modifier for the component
 * @param distanceScale Scale factor for converting distance to pixels (pixels per meter)
 * @param beaconColors Map of beacon MAC addresses to colors
 * @param positionColor Color for the estimated position
 * @param uncertaintyColor Color for the position uncertainty area
 */
@Composable
fun TriangulationUncertaintyVisualizer(
    beacons: List<Beacon>,
    estimatedX: Float,
    estimatedY: Float,
    positionUncertainty: Float,
    modifier: Modifier = Modifier,
    distanceScale: Float = 10f,
    beaconColors: Map<String, Color> = emptyMap(),
    positionColor: Color = Color.Blue,
    uncertaintyColor: Color = Color.Red
) {
    Box(
        modifier = modifier.size(500.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            
            // Calculate the offset for the estimated position
            val positionOffset = Offset(
                center.x + (estimatedX * distanceScale),
                center.y + (estimatedY * distanceScale)
            )
            
            // Draw each beacon and its distance circle
            beacons.forEach { beacon ->
                // Get color for this beacon, or use default
                val beaconColor = beaconColors[beacon.macAddress] ?: Color.Blue
                
                // Calculate the beacon position
                val beaconOffset = Offset(
                    center.x + (beacon.x * distanceScale),
                    center.y + (beacon.y * distanceScale)
                )
                
                // Draw the beacon
                drawBeacon(beaconOffset, beaconColor)
                
                // Calculate the radius for the distance circle
                val distanceRadius = beacon.estimatedDistance * distanceScale
                
                // Draw the distance circle
                drawCircle(
                    color = Color.Green.copy(alpha = 0.5f),
                    radius = distanceRadius,
                    center = beaconOffset,
                    style = Stroke(width = 2f)
                )
            }
            
            // Draw the position uncertainty area
            drawCircle(
                color = uncertaintyColor.copy(alpha = 0.3f),
                radius = positionUncertainty * distanceScale,
                center = positionOffset
            )
            
            // Draw the estimated position
            drawCircle(
                color = positionColor,
                radius = 10f,
                center = positionOffset
            )
        }
    }
}