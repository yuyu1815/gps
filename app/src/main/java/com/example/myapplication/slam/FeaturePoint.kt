package com.example.myapplication.slam

/**
 * Represents a 3D feature point detected by the SLAM system.
 *
 * @property x The x-coordinate of the feature point.
 * @property y The y-coordinate of the feature point.
 * @property z The z-coordinate of the feature point (depth).
 * @property confidence The confidence value of the feature point detection (0.0-1.0).
 * @property id Unique identifier for tracking this feature point across frames.
 * @property timestamp The timestamp when this feature was detected.
 */
data class FeaturePoint(
    val x: Float,
    val y: Float,
    val z: Float = 0f,
    val confidence: Float = 1.0f,
    val id: Long = -1L,
    val timestamp: Long = System.currentTimeMillis()
)
