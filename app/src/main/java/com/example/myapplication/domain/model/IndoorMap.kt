package com.example.myapplication.domain.model

/**
 * Represents an indoor map in the positioning system.
 * Contains information about the map dimensions, scale, and associated beacons.
 */
data class IndoorMap(
    /**
     * Unique identifier for the map.
     */
    val id: String,
    
    /**
     * Name of the map.
     */
    val name: String,
    
    /**
     * Width of the map in meters.
     */
    val widthMeters: Float,
    
    /**
     * Height of the map in meters.
     */
    val heightMeters: Float,
    
    /**
     * Scale of the map in pixels per meter.
     */
    val pixelsPerMeter: Float,
    
    /**
     * Path to the map image file.
     */
    val imagePath: String,
    
    /**
     * List of beacons associated with this map.
     */
    val beacons: List<Beacon> = emptyList(),
    
    /**
     * List of walls or obstacles on the map.
     * Each wall is represented as a line segment with start and end points.
     */
    val walls: List<Wall> = emptyList(),
    
    /**
     * List of points of interest on the map.
     */
    val pointsOfInterest: List<PointOfInterest> = emptyList()
) {
    /**
     * Converts a physical coordinate (in meters) to a pixel coordinate on the map image.
     */
    fun metersToPixels(x: Float, y: Float): Pair<Float, Float> {
        return Pair(x * pixelsPerMeter, y * pixelsPerMeter)
    }
    
    /**
     * Converts a pixel coordinate on the map image to a physical coordinate (in meters).
     */
    fun pixelsToMeters(x: Float, y: Float): Pair<Float, Float> {
        return Pair(x / pixelsPerMeter, y / pixelsPerMeter)
    }
    
    /**
     * Checks if a physical coordinate (in meters) is within the map boundaries.
     */
    fun isWithinBounds(x: Float, y: Float): Boolean {
        return x >= 0 && x <= widthMeters && y >= 0 && y <= heightMeters
    }
    
    /**
     * Gets the nearest beacon to a given position.
     */
    fun getNearestBeacon(x: Float, y: Float): Beacon? {
        if (beacons.isEmpty()) return null
        
        return beacons.minByOrNull { beacon ->
            val dx = beacon.x - x
            val dy = beacon.y - y
            dx * dx + dy * dy  // Square of distance
        }
    }
    
    /**
     * Gets beacons within a specified radius from a position.
     */
    fun getBeaconsInRadius(x: Float, y: Float, radiusMeters: Float): List<Beacon> {
        val radiusSquared = radiusMeters * radiusMeters
        
        return beacons.filter { beacon ->
            val dx = beacon.x - x
            val dy = beacon.y - y
            (dx * dx + dy * dy) <= radiusSquared
        }
    }
    
    /**
     * Gets a point of interest by its ID.
     */
    fun getPointOfInterest(id: String): PointOfInterest? {
        return pointsOfInterest.find { it.id == id }
    }
}

/**
 * Represents a wall or obstacle on the indoor map.
 */
data class Wall(
    /**
     * X coordinate of the start point (in meters).
     */
    val startX: Float,
    
    /**
     * Y coordinate of the start point (in meters).
     */
    val startY: Float,
    
    /**
     * X coordinate of the end point (in meters).
     */
    val endX: Float,
    
    /**
     * Y coordinate of the end point (in meters).
     */
    val endY: Float
)

/**
 * Represents a point of interest on the indoor map.
 */
data class PointOfInterest(
    /**
     * Unique identifier for the point of interest.
     */
    val id: String,
    
    /**
     * Name of the point of interest.
     */
    val name: String,
    
    /**
     * X coordinate of the point (in meters).
     */
    val x: Float,
    
    /**
     * Y coordinate of the point (in meters).
     */
    val y: Float,
    
    /**
     * Type of the point of interest.
     */
    val type: PoiType,
    
    /**
     * Additional information about the point of interest.
     */
    val description: String = ""
)

/**
 * Types of points of interest.
 */
enum class PoiType {
    ROOM,
    ENTRANCE,
    EXIT,
    ELEVATOR,
    STAIRS,
    RESTROOM,
    CAFE,
    SHOP,
    OTHER
}