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
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import kotlin.math.pow
import com.example.myapplication.domain.model.CoordinateTransformer
import com.example.myapplication.domain.model.IndoorMap
import com.example.myapplication.domain.model.PointOfInterest
import com.example.myapplication.domain.model.UserPosition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File

/**
 * A composable function that renders an indoor map with points of interest and user position.
 *
 * @param map The indoor map to render
 * @param userPosition The current user position
 * @param selectedPoi The currently selected point of interest (if any)
 * @param showPois Whether to show points of interest on the map
 * @param showUserPosition Whether to show the user's position on the map
 * @param onMapClick Callback for when the map is clicked
 * @param onPoiClick Callback for when a point of interest is clicked
 */
@Composable
fun MapRenderer(
    map: IndoorMap,
    userPosition: UserPosition? = null,
    selectedPoi: PointOfInterest? = null,
    showPois: Boolean = true,
    showUserPosition: Boolean = true,
    onMapClick: ((Float, Float) -> Unit)? = null,
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
    
    // Draw uncertainty ellipse if sigmaX/Y available, otherwise fallback to circle by accuracy
    val scaleFactor = transformer.getScaleFactor()
    val sigmaX = userPosition.sigmaX?.times(scaleFactor)
    val sigmaY = userPosition.sigmaY?.times(scaleFactor)
    val sigmaThetaDeg = userPosition.sigmaTheta?.times(180f / Math.PI.toFloat()) ?: 0f
    if (sigmaX != null && sigmaY != null && sigmaX.isFinite() && sigmaY.isFinite()) {
        // 1-sigma ellipse; visually scale slightly
        val rx = (sigmaX * 2f).coerceAtMost(size.minDimension * 0.45f)
        val ry = (sigmaY * 2f).coerceAtMost(size.minDimension * 0.45f)
        rotate(degrees = sigmaThetaDeg, pivot = Offset(screenX, screenY)) {
            drawOval(
                color = Color(0xFF1565C0),
                topLeft = Offset(screenX - rx, screenY - ry),
                size = androidx.compose.ui.geometry.Size(rx * 2, ry * 2),
                alpha = 0.18f
            )
        }
    } else {
        val accuracyRadius = (userPosition.accuracy * scaleFactor).coerceAtMost(size.minDimension * 0.45f)
        drawCircle(
            color = Color(0xFF1565C0),
            radius = accuracyRadius,
            center = Offset(screenX, screenY),
            alpha = 0.18f
        )
    }
    
    // Draw user position
    val positionRadius = 25f * transformer.getScaleFactor()
    drawCircle(
        color = Color(0xFF1565C0),
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