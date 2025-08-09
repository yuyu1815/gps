package com.example.myapplication.data.repository

import com.example.myapplication.domain.model.PositionSource
import com.example.myapplication.domain.model.UserPosition
import com.example.myapplication.service.PositionChangeDetector
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import java.io.File
import java.util.LinkedList

/**
 * Implementation of the IPositionRepository interface.
 * Manages user positions in the indoor positioning system.
 */
class PositionRepository(private val context: android.content.Context) : IPositionRepository {
    
    // Current user position
    private val _currentPosition = MutableStateFlow<UserPosition>(UserPosition.invalid())
    
    // Position history, using LinkedList for efficient add/remove at both ends
    private val positionHistory = LinkedList<UserPosition>()
    
    // Position change detector
    private val positionChangeDetector = PositionChangeDetector(this)
    
    // Maximum history size to prevent excessive memory usage
    private val maxHistorySize = 1000
    
    // Maximum time to keep position history (24 hours in milliseconds)
    private val maxHistoryAge = 24 * 60 * 60 * 1000L
    
    // Downsampling interval for older positions (5 minutes in milliseconds)
    private val downsamplingInterval = 5 * 60 * 1000L
    
    // StateFlow for observing position history changes
    private val _positionHistoryFlow = MutableStateFlow<List<UserPosition>>(emptyList())
    
    // Moshi instance for JSON serialization/deserialization
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
    
    init {
        Timber.d("PositionRepository initialized with context: ${context.packageName}")
    }
    
    /**
     * Updates the position history flow with the current history.
     */
    private fun updatePositionHistoryFlow() {
        _positionHistoryFlow.value = positionHistory.toList()
    }
    
    override fun getCurrentPositionFlow(): Flow<UserPosition> {
        return _currentPosition.asStateFlow()
    }
    
    override fun getPositionHistoryFlow(): Flow<List<UserPosition>> {
        return _positionHistoryFlow.asStateFlow()
    }
    
    override suspend fun getCurrentPosition(): UserPosition {
        return _currentPosition.value
    }
    
    override suspend fun getPositionHistory(limit: Int): List<UserPosition> {
        return if (limit <= 0 || limit >= positionHistory.size) {
            positionHistory.toList()
        } else {
            positionHistory.takeLast(limit)
        }
    }
    
    override suspend fun updatePosition(position: UserPosition) {
        // Update current position
        _currentPosition.value = position
        
        // Detect position changes
        positionChangeDetector.updatePosition(position)
        
        // Add to history
        positionHistory.add(position)
        
        // Optimize memory usage by cleaning up old entries and downsampling
        performPositionHistoryOptimization()
        
        // Update history flow
        updatePositionHistoryFlow()
        
        Timber.d("Updated position: (${position.x}, ${position.y}), source: ${position.source}, accuracy: ${position.accuracy}")
    }
    
    /**
     * Gets the position change detector.
     */
    override fun getPositionChangeDetector(): PositionChangeDetector {
        return positionChangeDetector
    }

    /**
     * Resets the current position to the last known good position from the history.
     * A 'good' position is defined as one with a confidence level above a certain threshold.
     *
     * @param confidenceThreshold The minimum confidence level for a position to be considered 'good'
     * @return True if a good position was found and the current position was reset, false otherwise
     */
    suspend fun resetToLastKnownGoodPosition(confidenceThreshold: Float = 0.75f): Boolean {
        val lastGoodPosition = positionHistory.lastOrNull { it.confidence >= confidenceThreshold }

        return if (lastGoodPosition != null) {
            _currentPosition.value = lastGoodPosition
            Timber.w("Positioning recovery: Reset to last known good position with confidence ${lastGoodPosition.confidence}")
            true
        } else {
            Timber.w("Positioning recovery: No good position found in history to reset to.")
            false
        }
    }
    
    /**
     * Optimizes the position history to reduce memory usage.
     * This method:
     * 1. Removes positions older than maxHistoryAge
     * 2. Downsamples older positions to reduce memory usage
     * 3. Ensures the history doesn't exceed maxHistorySize
     */
    private fun performPositionHistoryOptimization() {
        val currentTime = System.currentTimeMillis()
        
        // Remove positions older than maxHistoryAge
        positionHistory.removeIf { currentTime - it.timestamp > maxHistoryAge }
        
        // Downsample older positions (keep only one position per downsamplingInterval)
        if (positionHistory.size > maxHistorySize / 2) {
            val downsampledHistory = LinkedList<UserPosition>()
            
            // Always keep the most recent positions at full resolution (last 10 minutes)
            val recentCutoff = currentTime - 10 * 60 * 1000L
            val olderPositions = positionHistory.filter { it.timestamp < recentCutoff }
            val recentPositions = positionHistory.filter { it.timestamp >= recentCutoff }
            
            // Group older positions by time intervals and keep only one per interval
            val groupedPositions = olderPositions.groupBy { 
                (it.timestamp / downsamplingInterval) * downsamplingInterval 
            }
            
            // Add one position from each time interval
            groupedPositions.forEach { (_, positions) ->
                // Prefer positions with higher confidence
                val bestPosition = positions.maxByOrNull { it.confidence } ?: positions.first()
                downsampledHistory.add(bestPosition)
            }
            
            // Add all recent positions
            downsampledHistory.addAll(recentPositions)
            
            // Replace the history with the downsampled version
            positionHistory.clear()
            positionHistory.addAll(downsampledHistory)
        }
        
        // Ensure we don't exceed maxHistorySize
        while (positionHistory.size > maxHistorySize) {
            positionHistory.removeFirst()
        }
    }
    
    override suspend fun clearHistory() {
        positionHistory.clear()
        updatePositionHistoryFlow()
        Timber.d("Position history cleared")
    }
    
    override suspend fun getPositionHistoryInTimeRange(startTime: Long, endTime: Long): List<UserPosition> {
        return positionHistory.filter { it.timestamp in startTime..endTime }
    }
    
    override suspend fun getAveragePosition(timeMs: Long): UserPosition? {
        val currentTime = System.currentTimeMillis()
        val startTime = currentTime - timeMs
        
        val positions = getPositionHistoryInTimeRange(startTime, currentTime)
        if (positions.isEmpty()) {
            return null
        }
        
        var sumX = 0f
        var sumY = 0f
        var sumAccuracy = 0f
        var sumConfidence = 0f
        
        positions.forEach { position ->
            sumX += position.x
            sumY += position.y
            sumAccuracy += position.accuracy
            sumConfidence += position.confidence
        }
        
        val count = positions.size
        
        return UserPosition(
            x = sumX / count,
            y = sumY / count,
            accuracy = sumAccuracy / count,
            timestamp = currentTime,
            source = PositionSource.FUSION,
            confidence = sumConfidence / count
        )
    }
    
    override suspend fun getDistanceTraveled(timeMs: Long): Float {
        val currentTime = System.currentTimeMillis()
        val startTime = currentTime - timeMs
        
        val positions = getPositionHistoryInTimeRange(startTime, currentTime)
        if (positions.size < 2) {
            return 0f
        }
        
        var distance = 0f
        for (i in 1 until positions.size) {
            distance += positions[i].distanceTo(positions[i - 1])
        }
        
        return distance
    }
    
    override suspend fun getAverageSpeed(timeMs: Long): Float {
        val distance = getDistanceTraveled(timeMs)
        return if (timeMs > 0) {
            // Convert to meters per second
            distance / (timeMs / 1000f)
        } else {
            0f
        }
    }
    
    override suspend fun saveHistoryToFile(filePath: String): Boolean {
        try {
            val file = File(filePath)
            val type = com.squareup.moshi.Types.newParameterizedType(List::class.java, UserPosition::class.java)
            val adapter = moshi.adapter<List<UserPosition>>(type)
            val json = adapter.toJson(positionHistory.toList())
            
            file.writeText(json)
            Timber.d("Saved position history to file: $filePath")
            return true
        } catch (e: Exception) {
            Timber.e(e, "Error saving position history to file: $filePath")
            return false
        }
    }
    
    override suspend fun loadHistoryFromFile(filePath: String): Boolean {
        try {
            val file = File(filePath)
            if (!file.exists()) {
                Timber.e("Position history file not found: $filePath")
                return false
            }
            
            val json = file.readText()
            val type = com.squareup.moshi.Types.newParameterizedType(List::class.java, UserPosition::class.java)
            val adapter = moshi.adapter<List<UserPosition>>(type)
            val loadedHistory = adapter.fromJson(json)
            
            if (loadedHistory != null) {
                positionHistory.clear()
                positionHistory.addAll(loadedHistory)
                updatePositionHistoryFlow()
                Timber.d("Loaded position history from file: $filePath")
                return true
            } else {
                Timber.e("Failed to parse position history from file: $filePath")
                return false
            }
        } catch (e: Exception) {
            Timber.e(e, "Error loading position history from file: $filePath")
            return false
        }
    }
    
    /**
     * Optimizes the position history by removing old entries and downsampling data.
     * This helps reduce memory usage for long-running sessions.
     * 
     * @return The number of entries removed
     */
    override suspend fun optimizePositionHistory(): Int {
        val initialSize = positionHistory.size
        val currentTime = System.currentTimeMillis()
        
        // Step 1: Remove positions older than maxHistoryAge
        positionHistory.removeIf { currentTime - it.timestamp > maxHistoryAge }
        
        // Step 2: Downsample older positions if we still have too many
        if (positionHistory.size > maxHistorySize / 2) {
            val downsampledHistory = LinkedList<UserPosition>()
            
            // Always keep the most recent positions at full resolution (last 10 minutes)
            val recentCutoff = currentTime - 10 * 60 * 1000L
            val olderPositions = positionHistory.filter { it.timestamp < recentCutoff }
            val recentPositions = positionHistory.filter { it.timestamp >= recentCutoff }
            
            // Group older positions by time intervals and keep only one per interval
            val groupedPositions = olderPositions.groupBy { 
                (it.timestamp / downsamplingInterval) * downsamplingInterval 
            }
            
            // Add one position from each time interval
            groupedPositions.forEach { (_, positions) ->
                // Prefer positions with higher confidence
                val bestPosition = positions.maxByOrNull { it.confidence } ?: positions.first()
                downsampledHistory.add(bestPosition)
            }
            
            // Add all recent positions
            downsampledHistory.addAll(recentPositions)
            
            // Replace the history with the downsampled version
            positionHistory.clear()
            positionHistory.addAll(downsampledHistory)
        }
        
        // Step 3: Ensure we don't exceed maxHistorySize
        while (positionHistory.size > maxHistorySize) {
            positionHistory.removeFirst()
        }
        
        // Update the history flow
        updatePositionHistoryFlow()
        
        // Return the number of entries removed
        val entriesRemoved = initialSize - positionHistory.size
        Timber.d("Optimized position history: removed $entriesRemoved entries, new size: ${positionHistory.size}")
        
        return entriesRemoved
    }
}