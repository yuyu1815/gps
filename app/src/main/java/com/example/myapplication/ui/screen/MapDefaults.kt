package com.example.myapplication.ui.screen

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

object MapDefaults {
    val padding = 16.dp
    val smallPadding = 8.dp
    val infoCardMaxWidthCompact = 0.9f
    val infoCardMaxWidthMedium = 0.5f
    val infoCardElevation = 4.dp
    val infoCardCornerRadius = 12.dp
    val zoomButtonSize = 48.dp
    val fabSize = 56.dp
    val debugOverlayMaxWidth = 0.9f
    val debugOverlayAlpha = 0.8f
    val debugOverlayElevation = 4.dp
    val debugOverlayCornerRadius = 12.dp

    val gridColor: Color = Color.LightGray
    const val GRID_SIZE = 50f
    const val GRID_STROKE_WIDTH = 1f

    val wallColor: Color = Color.Black
    const val WALL_STROKE_WIDTH = 5f
    val simulatedWalls = listOf(
        Offset(100f, 100f) to Offset(100f, 500f),
        Offset(100f, 100f) to Offset(500f, 100f),
        Offset(500f, 100f) to Offset(500f, 500f),
        Offset(100f, 500f) to Offset(500f, 500f)
    )

    val userPositionConfidenceColor: Color = Color.Green.copy(alpha = 0.2f)
    val userPositionDotColor: Color = Color.Green
    const val USER_POSITION_DOT_RADIUS = 10f
    const val USER_POSITION_X_OFFSET = 0f
    const val USER_POSITION_Y_OFFSET = 0f
    const val USER_POSITION_MULTIPLIER = 1f
    const val USER_CONFIDENCE_RADIUS_MULTIPLIER = 1f
    
    // Constants for confidence radius calculation
    const val MIN_CONFIDENCE_RADIUS = 10f
    const val MAX_CONFIDENCE_RADIUS = 100f

    val pathColor: Color = Color.Red
    val simulatedPathPoints = emptyList<Offset>()
    const val PATH_STROKE_WIDTH = 3f

    const val MIN_SCALE = 0.5f
    const val MAX_SCALE = 3f
    const val ZOOM_FACTOR = 1.2f
}
