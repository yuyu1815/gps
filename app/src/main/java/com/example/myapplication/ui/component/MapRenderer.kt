package com.example.myapplication.ui.component

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import kotlin.math.pow
import com.example.myapplication.domain.model.Beacon
import com.example.myapplication.domain.model.CoordinateTransformer
import com.example.myapplication.domain.model.IndoorMap
import com.example.myapplication.domain.model.PointOfInterest
import com.example.myapplication.domain.model.UserPosition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File

/**
 * A composable function that renders an indoor map with beacons, points of interest, and user position.
 *
 * @param map The indoor map to render
 * @param userPosition The current user position
 * @param selectedBeacon The currently selected beacon (if any)
 * @param selectedPoi The currently selected point of interest (if any)
 * @param showBeacons Whether to show beacons on the map
 * @param showPois Whether to show points of interest on the map
 * @param showUserPosition Whether to show the user's position on the map
 * @param onMapClick Callback for when the map is clicked
 * @param onBeaconClick Callback for when a beacon is clicked
 * @param onPoiClick Callback for when a point of interest is clicked
 */
@Composable
fun MapRenderer(
    map: IndoorMap,
    userPosition: UserPosition? = null,
    selectedBeacon: Beacon? = null,
    selectedPoi: PointOfInterest? = null,
    showBeacons: Boolean = true,
    showPois: Boolean = true,
    showUserPosition: Boolean = true,
    showBeaconDebugInfo: Boolean = false,
    onMapClick: ((Float, Float) -> Unit)? = null,
    onBeaconClick: ((Beacon) -> Unit)? = null,
    onPoiClick: ((PointOfInterest) -> Unit)? = null
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    
    // State for map image
    var mapBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    
    // State for zoom and pan
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    
    // State for canvas size
    var canvasSize by remember { mutableStateOf(IntSize(0, 0)) }
    
    // Coordinate transformer
    val transformer = remember { CoordinateTransformer(map) }
    
    // Load map image
    LaunchedEffect(map.imagePath) {
        try {
            withContext(Dispatchers.IO) {
                val file = File(map.imagePath)
                if (file.exists()) {
                    mapBitmap = BitmapFactory.decodeFile(file.absolutePath)
                    Timber.d("Loaded map image: ${map.imagePath}")
                } else {
                    Timber.e("Map image file not found: ${map.imagePath}")
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error loading map image: ${map.imagePath}")
        }
    }
    
    // Update transformer when canvas size changes
    LaunchedEffect(canvasSize, scale, offsetX, offsetY) {
        transformer.setViewport(canvasSize.width.toFloat(), canvasSize.height.toFloat())
        transformer.setZoom(scale)
        transformer.setPanOffset(offsetX, offsetY)
    }
    
    // Center map initially
    LaunchedEffect(Unit) {
        // Center the map in the viewport
        offsetX = 0f
        offsetY = 0f
        scale = 1f
    }
    
    // Center on user position when it changes
    LaunchedEffect(userPosition) {
        if (userPosition != null && showUserPosition) {
            transformer.centerOn(userPosition.x, userPosition.y)
            // Get the updated pan offset from the transformer
            val (visibleMinX, visibleMinY) = transformer.getVisibleArea().first
            val (visibleMaxX, visibleMaxY) = transformer.getVisibleArea().second
            
            // Calculate the center of the visible area
            val centerX = (visibleMinX + visibleMaxX) / 2
            val centerY = (visibleMinY + visibleMaxY) / 2
            
            // Calculate the offset needed to center on the user
            val (screenX, screenY) = transformer.metersToScreen(userPosition.x, userPosition.y)
            offsetX = canvasSize.width / 2f - screenX
            offsetY = canvasSize.height / 2f - screenY
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { centroid, pan, zoom, rotation ->
                    // Update scale with zoom gesture
                    scale = (scale * zoom).coerceIn(0.5f, 5f)
                    
                    // Update offset with pan gesture
                    offsetX += pan.x
                    offsetY += pan.y
                    
                    // Update the transformer
                    transformer.setZoom(scale)
                    transformer.setPanOffset(offsetX, offsetY)
                }
            }
    ) {
        Canvas(
            modifier = Modifier.fillMaxSize(),
            onDraw = {
                // Update canvas size
                if (canvasSize.width != size.width.toInt() || canvasSize.height != size.height.toInt()) {
                    canvasSize = IntSize(size.width.toInt(), size.height.toInt())
                }
                
                // Draw map background
                drawMapBackground(mapBitmap, scale, offsetX, offsetY)
                
                // Draw beacons
                if (showBeacons) {
                    drawBeacons(map.beacons, transformer, selectedBeacon, showBeaconDebugInfo)
                }
                
                // Draw points of interest
                if (showPois) {
                    drawPointsOfInterest(map.pointsOfInterest, transformer, selectedPoi)
                }
                
                // Draw user position
                if (showUserPosition && userPosition != null) {
                    drawUserPosition(userPosition, transformer)
                }
            }
        )
    }
}

/**
 * Draws the map background image.
 */
private fun DrawScope.drawMapBackground(
    mapBitmap: android.graphics.Bitmap?,
    scale: Float,
    offsetX: Float,
    offsetY: Float
) {
    mapBitmap?.let { bitmap ->
        // Calculate the scaled dimensions
        val scaledWidth = bitmap.width * scale
        val scaledHeight = bitmap.height * scale
        
        // Calculate the centered position
        val x = (size.width - scaledWidth) / 2 + offsetX
        val y = (size.height - scaledHeight) / 2 + offsetY
        
        // Draw the bitmap
        translate(x, y) {
            scale(scale) {
                drawImage(
                    image = bitmap.asImageBitmap(),
                    topLeft = Offset.Zero
                )
            }
        }
    }
}

/**
 * Draws beacons on the map with debugging information.
 */
private fun DrawScope.drawBeacons(
    beacons: List<Beacon>,
    transformer: CoordinateTransformer,
    selectedBeacon: Beacon?,
    showDebugInfo: Boolean = true
) {
    beacons.forEach { beacon ->
        val (screenX, screenY) = transformer.metersToScreen(beacon.x, beacon.y)
        
        // Draw beacon circle
        val radius = 20f * transformer.getScaleFactor()
        val color = if (beacon == selectedBeacon) Color.Red else Color.Blue
        
        // Draw signal strength indicator (outer ring) if debug info is enabled
        if (showDebugInfo && beacon.filteredRssi != 0) {
            // Convert RSSI to a visual indicator (larger circle for stronger signal)
            // RSSI typically ranges from -100 (weak) to -30 (strong)
            val signalStrength = ((beacon.filteredRssi + 100) / 70f).coerceIn(0f, 1f)
            val signalRadius = radius * (1.5f + signalStrength * 1.5f)
            
            drawCircle(
                color = color,
                radius = signalRadius,
                center = Offset(screenX, screenY),
                alpha = 0.2f
            )
        }
        
        // Draw beacon circle
        drawCircle(
            color = color,
            radius = radius,
            center = Offset(screenX, screenY),
            alpha = 0.7f
        )
        
        // Draw beacon ID circle
        drawCircle(
            color = Color.White,
            radius = radius * 0.7f,
            center = Offset(screenX, screenY)
        )
        
        // Enhanced visualization for debugging
        if (showDebugInfo) {
            // Draw connection lines between beacons for better visualization of placement
            beacons.forEach { otherBeacon ->
                if (otherBeacon != beacon) {
                    val (x2, y2) = transformer.metersToScreen(otherBeacon.x, otherBeacon.y)
                    
                    // Draw connection line
                    drawLine(
                        color = Color.Gray,
                        start = Offset(screenX, screenY),
                        end = Offset(x2, y2),
                        alpha = 0.3f,
                        strokeWidth = 1f * transformer.getScaleFactor()
                    )
                }
            }
            
            // Draw coverage area based on txPower
            // Calculate theoretical maximum range based on txPower
            val txPowerFactor = (beacon.txPower + 100) / 20.0
            val maxRangeMeters = Math.pow(10.0, txPowerFactor).toFloat().coerceIn(1f, 30f)
            val coverageRadius = maxRangeMeters * transformer.getScaleFactor()
            
            // Draw coverage boundary
            drawCircle(
                color = Color.Blue,
                radius = coverageRadius,
                center = Offset(screenX, screenY),
                alpha = 0.1f
            )
            
            // Draw confidence indicator if available
            if (beacon.distanceConfidence > 0f) {
                // Draw confidence ring with color based on confidence level
                val confidenceColor = when {
                    beacon.distanceConfidence > 0.7f -> Color.Green
                    beacon.distanceConfidence > 0.4f -> Color.Yellow
                    else -> Color.Red
                }
                
                drawCircle(
                    color = confidenceColor,
                    radius = radius * 2.5f,
                    center = Offset(screenX, screenY),
                    alpha = 0.15f,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = 2f * transformer.getScaleFactor()
                    )
                )
            }
        }
    }
}

/**
 * Draws points of interest on the map.
 */
private fun DrawScope.drawPointsOfInterest(
    pois: List<PointOfInterest>,
    transformer: CoordinateTransformer,
    selectedPoi: PointOfInterest?
) {
    pois.forEach { poi ->
        val (screenX, screenY) = transformer.metersToScreen(poi.x, poi.y)
        
        // Draw POI marker
        val radius = 15f * transformer.getScaleFactor()
        val color = when (poi.type) {
            com.example.myapplication.domain.model.PoiType.ROOM -> Color.Green
            com.example.myapplication.domain.model.PoiType.ENTRANCE -> Color.Cyan
            com.example.myapplication.domain.model.PoiType.EXIT -> Color.Magenta
            else -> Color.Yellow
        }
        
        // Draw highlighted border for selected POI
        if (poi == selectedPoi) {
            drawCircle(
                color = Color.Red,
                radius = radius * 1.3f,
                center = Offset(screenX, screenY),
                alpha = 0.5f
            )
        }
        
        drawCircle(
            color = color,
            radius = radius,
            center = Offset(screenX, screenY),
            alpha = 0.7f
        )
    }
}

/**
 * Draws the user's position on the map.
 */
private fun DrawScope.drawUserPosition(
    userPosition: UserPosition,
    transformer: CoordinateTransformer
) {
    val (screenX, screenY) = transformer.metersToScreen(userPosition.x, userPosition.y)
    
    // Draw accuracy circle
    val accuracyRadius = userPosition.accuracy * transformer.getScaleFactor() * transformer.getScaleFactor()
    drawCircle(
        color = Color.Blue,
        radius = accuracyRadius,
        center = Offset(screenX, screenY),
        alpha = 0.2f
    )
    
    // Draw user position
    val positionRadius = 25f * transformer.getScaleFactor()
    drawCircle(
        color = Color.Blue,
        radius = positionRadius,
        center = Offset(screenX, screenY),
        alpha = 0.8f
    )
    
    // Draw inner circle
    drawCircle(
        color = Color.White,
        radius = positionRadius * 0.6f,
        center = Offset(screenX, screenY)
    )
}