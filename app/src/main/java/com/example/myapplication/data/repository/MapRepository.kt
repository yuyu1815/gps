package com.example.myapplication.data.repository

import com.example.myapplication.domain.model.Beacon
import com.example.myapplication.domain.model.IndoorMap
import com.example.myapplication.domain.model.PointOfInterest
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Implementation of the IMapRepository interface.
 * Manages indoor maps in the positioning system.
 */
class MapRepository : IMapRepository {
    
    // Using ConcurrentHashMap for thread safety
    private val maps = ConcurrentHashMap<String, IndoorMap>()
    
    // StateFlow for observing map changes
    private val _mapsFlow = MutableStateFlow<List<IndoorMap>>(emptyList())
    
    // Currently active map ID
    private var activeMapId: String? = null
    
    // Moshi instance for JSON serialization/deserialization
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
    
    init {
        Timber.d("MapRepository initialized")
    }
    
    /**
     * Updates the maps flow with the current list of maps.
     */
    private fun updateMapsFlow() {
        _mapsFlow.value = maps.values.toList()
    }
    
    override fun getMapsFlow(): Flow<List<IndoorMap>> {
        return _mapsFlow.asStateFlow()
    }
    
    override suspend fun getAll(): List<IndoorMap> {
        return maps.values.toList()
    }
    
    override suspend fun getById(id: String): IndoorMap? {
        return maps[id]
    }
    
    override suspend fun save(item: IndoorMap): Boolean {
        try {
            maps[item.id] = item
            updateMapsFlow()
            Timber.d("Saved map: ${item.id}")
            return true
        } catch (e: Exception) {
            Timber.e(e, "Error saving map: ${item.id}")
            return false
        }
    }
    
    override suspend fun update(item: IndoorMap): Boolean {
        return if (maps.containsKey(item.id)) {
            maps[item.id] = item
            updateMapsFlow()
            Timber.d("Updated map: ${item.id}")
            true
        } else {
            Timber.w("Map not found for update: ${item.id}")
            false
        }
    }
    
    override suspend fun delete(id: String): Boolean {
        return if (maps.containsKey(id)) {
            maps.remove(id)
            if (activeMapId == id) {
                activeMapId = null
            }
            updateMapsFlow()
            Timber.d("Deleted map: $id")
            true
        } else {
            Timber.w("Map not found for deletion: $id")
            false
        }
    }
    
    override suspend fun getActiveMap(): IndoorMap? {
        return activeMapId?.let { maps[it] }
    }
    
    override suspend fun setActiveMap(mapId: String): Boolean {
        return if (maps.containsKey(mapId)) {
            activeMapId = mapId
            Timber.d("Set active map: $mapId")
            true
        } else {
            Timber.w("Map not found for setting active: $mapId")
            false
        }
    }
    
    override suspend fun addBeaconToMap(mapId: String, beacon: Beacon): Boolean {
        val map = maps[mapId]
        return if (map != null) {
            // Create a new list with the added beacon
            val updatedBeacons = map.beacons.toMutableList()
            // Remove any existing beacon with the same ID
            updatedBeacons.removeAll { it.id == beacon.id }
            updatedBeacons.add(beacon)
            
            // Create a new map with the updated beacons
            val updatedMap = map.copy(beacons = updatedBeacons)
            maps[mapId] = updatedMap
            updateMapsFlow()
            Timber.d("Added beacon ${beacon.id} to map $mapId")
            true
        } else {
            Timber.w("Map not found for adding beacon: $mapId")
            false
        }
    }
    
    override suspend fun removeBeaconFromMap(mapId: String, beaconId: String): Boolean {
        val map = maps[mapId]
        return if (map != null) {
            // Create a new list without the removed beacon
            val updatedBeacons = map.beacons.filter { it.id != beaconId }
            
            if (updatedBeacons.size < map.beacons.size) {
                // Create a new map with the updated beacons
                val updatedMap = map.copy(beacons = updatedBeacons)
                maps[mapId] = updatedMap
                updateMapsFlow()
                Timber.d("Removed beacon $beaconId from map $mapId")
                true
            } else {
                Timber.w("Beacon not found in map: $beaconId")
                false
            }
        } else {
            Timber.w("Map not found for removing beacon: $mapId")
            false
        }
    }
    
    override suspend fun getBeaconsForMap(mapId: String): List<Beacon> {
        return maps[mapId]?.beacons ?: emptyList()
    }
    
    override suspend fun addPointOfInterest(mapId: String, poi: PointOfInterest): Boolean {
        val map = maps[mapId]
        return if (map != null) {
            // Create a new list with the added POI
            val updatedPois = map.pointsOfInterest.toMutableList()
            // Remove any existing POI with the same ID
            updatedPois.removeAll { it.id == poi.id }
            updatedPois.add(poi)
            
            // Create a new map with the updated POIs
            val updatedMap = map.copy(pointsOfInterest = updatedPois)
            maps[mapId] = updatedMap
            updateMapsFlow()
            Timber.d("Added POI ${poi.id} to map $mapId")
            true
        } else {
            Timber.w("Map not found for adding POI: $mapId")
            false
        }
    }
    
    override suspend fun removePointOfInterest(mapId: String, poiId: String): Boolean {
        val map = maps[mapId]
        return if (map != null) {
            // Create a new list without the removed POI
            val updatedPois = map.pointsOfInterest.filter { it.id != poiId }
            
            if (updatedPois.size < map.pointsOfInterest.size) {
                // Create a new map with the updated POIs
                val updatedMap = map.copy(pointsOfInterest = updatedPois)
                maps[mapId] = updatedMap
                updateMapsFlow()
                Timber.d("Removed POI $poiId from map $mapId")
                true
            } else {
                Timber.w("POI not found in map: $poiId")
                false
            }
        } else {
            Timber.w("Map not found for removing POI: $mapId")
            false
        }
    }
    
    override suspend fun getPointsOfInterest(mapId: String): List<PointOfInterest> {
        return maps[mapId]?.pointsOfInterest ?: emptyList()
    }
    
    override suspend fun getPointOfInterest(mapId: String, poiId: String): PointOfInterest? {
        return maps[mapId]?.pointsOfInterest?.find { it.id == poiId }
    }
    
    override suspend fun loadMapFromJson(filePath: String): IndoorMap? {
        try {
            val file = File(filePath)
            if (!file.exists()) {
                Timber.e("Map file not found: $filePath")
                return null
            }
            
            val json = file.readText()
            val adapter = moshi.adapter(IndoorMap::class.java)
            val map = adapter.fromJson(json)
            
            if (map != null) {
                maps[map.id] = map
                updateMapsFlow()
                Timber.d("Loaded map from JSON: ${map.id}")
            }
            
            return map
        } catch (e: Exception) {
            Timber.e(e, "Error loading map from JSON: $filePath")
            return null
        }
    }
    
    override suspend fun saveMapToJson(map: IndoorMap, filePath: String): Boolean {
        try {
            val file = File(filePath)
            val adapter = moshi.adapter(IndoorMap::class.java)
            val json = adapter.toJson(map)
            
            file.writeText(json)
            Timber.d("Saved map to JSON: ${map.id}")
            return true
        } catch (e: Exception) {
            Timber.e(e, "Error saving map to JSON: $filePath")
            return false
        }
    }
    
    override suspend fun discoverMapFiles(directoryPath: String): List<String> {
        try {
            val directory = File(directoryPath)
            if (!directory.exists() || !directory.isDirectory) {
                Timber.e("Directory does not exist or is not a directory: $directoryPath")
                return emptyList()
            }
            
            // Find all JSON files in the directory
            val mapFiles = directory.listFiles { file ->
                file.isFile && file.name.lowercase().endsWith(".json")
            }?.map { it.absolutePath } ?: emptyList()
            
            Timber.d("Discovered ${mapFiles.size} map files in $directoryPath")
            return mapFiles
        } catch (e: Exception) {
            Timber.e(e, "Error discovering map files in directory: $directoryPath")
            return emptyList()
        }
    }
    
    override suspend fun loadAllMapsFromDirectory(directoryPath: String): List<IndoorMap> {
        try {
            val mapFiles = discoverMapFiles(directoryPath)
            val loadedMaps = mutableListOf<IndoorMap>()
            
            for (filePath in mapFiles) {
                val map = loadMapFromJson(filePath)
                if (map != null) {
                    loadedMaps.add(map)
                    Timber.d("Loaded map: ${map.id} from $filePath")
                } else {
                    Timber.w("Failed to load map from $filePath")
                }
            }
            
            Timber.d("Loaded ${loadedMaps.size} maps from directory: $directoryPath")
            return loadedMaps
        } catch (e: Exception) {
            Timber.e(e, "Error loading maps from directory: $directoryPath")
            return emptyList()
        }
    }
}