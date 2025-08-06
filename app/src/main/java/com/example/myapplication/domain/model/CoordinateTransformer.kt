package com.example.myapplication.domain.model

/**
 * Handles coordinate transformations between different coordinate systems:
 * - Physical coordinates (meters): Real-world coordinates in meters
 * - Map coordinates (pixels): Coordinates on the map image
 * - Screen coordinates (pixels): Coordinates on the device screen, considering viewport, zoom, and panning
 */
class CoordinateTransformer(
    private var map: IndoorMap? = null,
    private var viewportWidth: Float = 0f,
    private var viewportHeight: Float = 0f,
    private var zoomLevel: Float = 1f,
    private var panOffsetX: Float = 0f,
    private var panOffsetY: Float = 0f
) {
    /**
     * Sets the map for coordinate transformation.
     */
    fun setMap(map: IndoorMap) {
        this.map = map
    }
    
    /**
     * Sets the viewport dimensions.
     */
    fun setViewport(width: Float, height: Float) {
        this.viewportWidth = width
        this.viewportHeight = height
    }
    
    /**
     * Sets the zoom level.
     */
    fun setZoom(zoom: Float) {
        this.zoomLevel = zoom.coerceIn(0.1f, 10f)
    }
    
    /**
     * Sets the pan offset.
     */
    fun setPanOffset(offsetX: Float, offsetY: Float) {
        this.panOffsetX = offsetX
        this.panOffsetY = offsetY
    }
    
    /**
     * Converts physical coordinates (meters) to screen coordinates (pixels).
     */
    fun metersToScreen(x: Float, y: Float): Pair<Float, Float> {
        val map = this.map ?: return Pair(0f, 0f)
        
        // Convert meters to map pixels
        val (mapX, mapY) = map.metersToPixels(x, y)
        
        // Apply zoom and pan to get screen coordinates
        val screenX = mapX * zoomLevel + panOffsetX + (viewportWidth - map.widthMeters * map.pixelsPerMeter * zoomLevel) / 2
        val screenY = mapY * zoomLevel + panOffsetY + (viewportHeight - map.heightMeters * map.pixelsPerMeter * zoomLevel) / 2
        
        return Pair(screenX, screenY)
    }
    
    /**
     * Converts screen coordinates (pixels) to physical coordinates (meters).
     */
    fun screenToMeters(screenX: Float, screenY: Float): Pair<Float, Float> {
        val map = this.map ?: return Pair(0f, 0f)
        
        // Adjust for viewport centering, pan, and zoom
        val mapX = (screenX - panOffsetX - (viewportWidth - map.widthMeters * map.pixelsPerMeter * zoomLevel) / 2) / zoomLevel
        val mapY = (screenY - panOffsetY - (viewportHeight - map.heightMeters * map.pixelsPerMeter * zoomLevel) / 2) / zoomLevel
        
        // Convert map pixels to meters
        return map.pixelsToMeters(mapX, mapY)
    }
    
    /**
     * Calculates the scale factor for rendering objects at the current zoom level.
     */
    fun getScaleFactor(): Float {
        return zoomLevel
    }
    
    /**
     * Checks if a physical coordinate is visible on the screen.
     */
    fun isVisibleOnScreen(x: Float, y: Float): Boolean {
        val (screenX, screenY) = metersToScreen(x, y)
        return screenX >= 0 && screenX <= viewportWidth && screenY >= 0 && screenY <= viewportHeight
    }
    
    /**
     * Calculates the visible area in physical coordinates.
     * Returns a pair of pairs: ((minX, minY), (maxX, maxY))
     */
    fun getVisibleArea(): Pair<Pair<Float, Float>, Pair<Float, Float>> {
        val topLeft = screenToMeters(0f, 0f)
        val bottomRight = screenToMeters(viewportWidth, viewportHeight)
        
        return Pair(topLeft, bottomRight)
    }
    
    /**
     * Centers the view on a specific physical coordinate.
     */
    fun centerOn(x: Float, y: Float) {
        val map = this.map ?: return
        
        // Convert meters to map pixels
        val (mapX, mapY) = map.metersToPixels(x, y)
        
        // Calculate the pan offset to center on this point
        panOffsetX = viewportWidth / 2 - mapX * zoomLevel
        panOffsetY = viewportHeight / 2 - mapY * zoomLevel
    }
}