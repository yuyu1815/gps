package com.example.myapplication.wifi

/**
 * Represents a 2D position with x and y coordinates and optional metadata.
 */
data class Position(
    val x: Double,
    val y: Double,
    val accuracy: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis(),
    val metadata: Map<String, String> = emptyMap()
)
