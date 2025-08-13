package com.example.myapplication.data.repository

import com.example.myapplication.domain.model.UserPosition
import com.example.myapplication.domain.model.UserPosition3D
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing user positions.
 */
interface IPositionRepository {
    /**
     * Gets a flow of the current user position, which emits updates when the position changes.
     * @return Flow of user position
     */
    fun getCurrentPositionFlow(): Flow<UserPosition>
    
    /**
     * Gets a flow of the position history, which emits updates when the history changes.
     * @return Flow of position history list
     */
    fun getPositionHistoryFlow(): Flow<List<UserPosition>>
    
    /**
     * Gets a flow of the current 3D user position, which emits updates when the position changes.
     * @return Flow of 3D user position
     */
    fun getCurrent3DPositionFlow(): Flow<UserPosition3D>
    
    /**
     * Gets a flow of the 3D position history, which emits updates when the history changes.
     * @return Flow of 3D position history list
     */
    fun get3DPositionHistoryFlow(): Flow<List<UserPosition3D>>
    
    /**
     * Gets the current user position.
     * @return The current user position
     */
    suspend fun getCurrentPosition(): UserPosition
    
    /**
     * Gets the current 3D user position.
     * @return The current 3D user position
     */
    suspend fun getCurrent3DPosition(): UserPosition3D
    
    /**
     * Gets the position history.
     * @param limit Maximum number of positions to return (0 for all)
     * @return List of historical positions
     */
    suspend fun getPositionHistory(limit: Int = 0): List<UserPosition>
    
    /**
     * Gets the 3D position history.
     * @param limit Maximum number of positions to return (0 for all)
     * @return List of historical 3D positions
     */
    suspend fun get3DPositionHistory(limit: Int = 0): List<UserPosition3D>
    
    /**
     * Updates the current user position.
     * @param position New user position
     */
    suspend fun updatePosition(position: UserPosition)
    
    /**
     * Updates the current 3D user position.
     * @param position New 3D user position
     */
    suspend fun update3DPosition(position: UserPosition3D)
    
    /**
     * Clears the position history.
     */
    suspend fun clearHistory()
    
    /**
     * Clears the 3D position history.
     */
    suspend fun clear3DHistory()
    
    /**
     * Gets the position history within a time range.
     * @param startTime Start time in milliseconds
     * @param endTime End time in milliseconds
     * @return List of positions within the time range
     */
    suspend fun getPositionHistoryInTimeRange(startTime: Long, endTime: Long): List<UserPosition>
    
    /**
     * Gets the average position over a time period.
     * @param timeMs Time period in milliseconds
     * @return Average position, or null if no positions are available
     */
    suspend fun getAveragePosition(timeMs: Long): UserPosition?
    
    /**
     * Gets the distance traveled over a time period.
     * @param timeMs Time period in milliseconds
     * @return Distance in meters
     */
    suspend fun getDistanceTraveled(timeMs: Long): Float
    
    /**
     * Gets the average speed over a time period.
     * @param timeMs Time period in milliseconds
     * @return Speed in meters per second
     */
    suspend fun getAverageSpeed(timeMs: Long): Float
    
    /**
     * Saves the position history to a file.
     * @param filePath Path to the file
     * @return True if the operation was successful, false otherwise
     */
    suspend fun saveHistoryToFile(filePath: String): Boolean
    
    /**
     * Loads the position history from a file.
     * @param filePath Path to the file
     * @return True if the operation was successful, false otherwise
     */
    suspend fun loadHistoryFromFile(filePath: String): Boolean
    
    /**
     * Optimizes the position history by removing old entries and downsampling data.
     * This helps reduce memory usage for long-running sessions.
     * @return The number of entries removed
     */
    suspend fun optimizePositionHistory(): Int
    
    /**
     * Gets the position change detector for monitoring position changes.
     * @return The position change detector
     */
    fun getPositionChangeDetector(): com.example.myapplication.service.PositionChangeDetector
}