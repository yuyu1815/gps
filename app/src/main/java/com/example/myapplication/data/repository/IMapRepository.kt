package com.example.myapplication.data.repository

import com.example.myapplication.domain.model.Beacon
import com.example.myapplication.domain.model.IndoorMap
import com.example.myapplication.domain.model.PointOfInterest
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing indoor maps.
 */
interface IMapRepository : IRepository<IndoorMap> {
    /**
     * Gets a flow of all maps, which emits updates when the map data changes.
     * @return Flow of map list
     */
    fun getMapsFlow(): Flow<List<IndoorMap>>
    
    /**
     * Gets the currently active map.
     * @return The active map, or null if no map is active
     */
    suspend fun getActiveMap(): IndoorMap?
    
    /**
     * Sets the active map.
     * @param mapId ID of the map to set as active
     * @return True if the operation was successful, false otherwise
     */
    suspend fun setActiveMap(mapId: String): Boolean
    
    /**
     * Adds a beacon to a map.
     * @param mapId ID of the map
     * @param beacon Beacon to add
     * @return True if the operation was successful, false otherwise
     */
    suspend fun addBeaconToMap(mapId: String, beacon: Beacon): Boolean
    
    /**
     * Removes a beacon from a map.
     * @param mapId ID of the map
     * @param beaconId ID of the beacon to remove
     * @return True if the operation was successful, false otherwise
     */
    suspend fun removeBeaconFromMap(mapId: String, beaconId: String): Boolean
    
    /**
     * Gets all beacons for a map.
     * @param mapId ID of the map
     * @return List of beacons for the map
     */
    suspend fun getBeaconsForMap(mapId: String): List<Beacon>
    
    /**
     * Adds a point of interest to a map.
     * @param mapId ID of the map
     * @param poi Point of interest to add
     * @return True if the operation was successful, false otherwise
     */
    suspend fun addPointOfInterest(mapId: String, poi: PointOfInterest): Boolean
    
    /**
     * Removes a point of interest from a map.
     * @param mapId ID of the map
     * @param poiId ID of the point of interest to remove
     * @return True if the operation was successful, false otherwise
     */
    suspend fun removePointOfInterest(mapId: String, poiId: String): Boolean
    
    /**
     * Gets all points of interest for a map.
     * @param mapId ID of the map
     * @return List of points of interest for the map
     */
    suspend fun getPointsOfInterest(mapId: String): List<PointOfInterest>
    
    /**
     * Gets a point of interest by its ID.
     * @param mapId ID of the map
     * @param poiId ID of the point of interest
     * @return The point of interest, or null if not found
     */
    suspend fun getPointOfInterest(mapId: String, poiId: String): PointOfInterest?
    
    /**
     * Loads a map from a JSON file.
     * @param filePath Path to the JSON file
     * @return The loaded map, or null if loading failed
     */
    suspend fun loadMapFromJson(filePath: String): IndoorMap?
    
    /**
     * Saves a map to a JSON file.
     * @param map Map to save
     * @param filePath Path to the JSON file
     * @return True if the operation was successful, false otherwise
     */
    suspend fun saveMapToJson(map: IndoorMap, filePath: String): Boolean
    
    /**
     * Discovers map files in the specified directory.
     * @param directoryPath Path to the directory to search for map files
     * @return List of file paths to discovered map files
     */
    suspend fun discoverMapFiles(directoryPath: String): List<String>
    
    /**
     * Loads all maps from the specified directory.
     * @param directoryPath Path to the directory containing map files
     * @return List of loaded maps
     */
    suspend fun loadAllMapsFromDirectory(directoryPath: String): List<IndoorMap>
}