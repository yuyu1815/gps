package com.example.myapplication.data.parser

import com.example.myapplication.domain.model.Beacon
import com.example.myapplication.domain.model.IndoorMap
import com.example.myapplication.domain.model.PointOfInterest
import com.example.myapplication.domain.model.Wall
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import timber.log.Timber
import java.io.File

/**
 * A dedicated JSON parser for map and beacon configurations.
 * Provides methods to parse JSON files into model objects and convert model objects to JSON.
 */
class ConfigurationJsonParser {
    
    // Moshi instance for JSON serialization/deserialization
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
    
    /**
     * Parses a JSON file into an IndoorMap object.
     *
     * @param filePath The path to the JSON file
     * @return The parsed IndoorMap object, or null if parsing failed
     */
    fun parseMapFromJson(filePath: String): IndoorMap? {
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
                Timber.d("Parsed map from JSON: ${map.id}")
            }
            
            return map
        } catch (e: Exception) {
            Timber.e(e, "Error parsing map from JSON: $filePath")
            return null
        }
    }
    
    /**
     * Converts an IndoorMap object to JSON and saves it to a file.
     *
     * @param map The IndoorMap object to convert
     * @param filePath The path where the JSON file will be saved
     * @return true if the operation was successful, false otherwise
     */
    fun saveMapToJson(map: IndoorMap, filePath: String): Boolean {
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
    
    /**
     * Parses a JSON file into a list of Beacon objects.
     *
     * @param filePath The path to the JSON file
     * @return The parsed list of Beacon objects, or an empty list if parsing failed
     */
    fun parseBeaconsFromJson(filePath: String): List<Beacon> {
        try {
            val file = File(filePath)
            if (!file.exists()) {
                Timber.e("Beacons file not found: $filePath")
                return emptyList()
            }
            
            val json = file.readText()
            val type = com.squareup.moshi.Types.newParameterizedType(List::class.java, Beacon::class.java)
            val adapter = moshi.adapter<List<Beacon>>(type)
            val beacons = adapter.fromJson(json) ?: emptyList()
            
            Timber.d("Parsed ${beacons.size} beacons from JSON: $filePath")
            return beacons
        } catch (e: Exception) {
            Timber.e(e, "Error parsing beacons from JSON: $filePath")
            return emptyList()
        }
    }
    
    /**
     * Converts a list of Beacon objects to JSON and saves it to a file.
     *
     * @param beacons The list of Beacon objects to convert
     * @param filePath The path where the JSON file will be saved
     * @return true if the operation was successful, false otherwise
     */
    fun saveBeaconsToJson(beacons: List<Beacon>, filePath: String): Boolean {
        try {
            val file = File(filePath)
            val type = com.squareup.moshi.Types.newParameterizedType(List::class.java, Beacon::class.java)
            val adapter = moshi.adapter<List<Beacon>>(type)
            val json = adapter.toJson(beacons)
            
            file.writeText(json)
            Timber.d("Saved ${beacons.size} beacons to JSON: $filePath")
            return true
        } catch (e: Exception) {
            Timber.e(e, "Error saving beacons to JSON: $filePath")
            return false
        }
    }
    
    /**
     * Parses a JSON file into a list of Wall objects.
     *
     * @param filePath The path to the JSON file
     * @return The parsed list of Wall objects, or an empty list if parsing failed
     */
    fun parseWallsFromJson(filePath: String): List<Wall> {
        try {
            val file = File(filePath)
            if (!file.exists()) {
                Timber.e("Walls file not found: $filePath")
                return emptyList()
            }
            
            val json = file.readText()
            val type = com.squareup.moshi.Types.newParameterizedType(List::class.java, Wall::class.java)
            val adapter = moshi.adapter<List<Wall>>(type)
            val walls = adapter.fromJson(json) ?: emptyList()
            
            Timber.d("Parsed ${walls.size} walls from JSON: $filePath")
            return walls
        } catch (e: Exception) {
            Timber.e(e, "Error parsing walls from JSON: $filePath")
            return emptyList()
        }
    }
    
    /**
     * Converts a list of Wall objects to JSON and saves it to a file.
     *
     * @param walls The list of Wall objects to convert
     * @param filePath The path where the JSON file will be saved
     * @return true if the operation was successful, false otherwise
     */
    fun saveWallsToJson(walls: List<Wall>, filePath: String): Boolean {
        try {
            val file = File(filePath)
            val type = com.squareup.moshi.Types.newParameterizedType(List::class.java, Wall::class.java)
            val adapter = moshi.adapter<List<Wall>>(type)
            val json = adapter.toJson(walls)
            
            file.writeText(json)
            Timber.d("Saved ${walls.size} walls to JSON: $filePath")
            return true
        } catch (e: Exception) {
            Timber.e(e, "Error saving walls to JSON: $filePath")
            return false
        }
    }
    
    /**
     * Parses a JSON file into a list of PointOfInterest objects.
     *
     * @param filePath The path to the JSON file
     * @return The parsed list of PointOfInterest objects, or an empty list if parsing failed
     */
    fun parsePointsOfInterestFromJson(filePath: String): List<PointOfInterest> {
        try {
            val file = File(filePath)
            if (!file.exists()) {
                Timber.e("POIs file not found: $filePath")
                return emptyList()
            }
            
            val json = file.readText()
            val type = com.squareup.moshi.Types.newParameterizedType(List::class.java, PointOfInterest::class.java)
            val adapter = moshi.adapter<List<PointOfInterest>>(type)
            val pois = adapter.fromJson(json) ?: emptyList()
            
            Timber.d("Parsed ${pois.size} POIs from JSON: $filePath")
            return pois
        } catch (e: Exception) {
            Timber.e(e, "Error parsing POIs from JSON: $filePath")
            return emptyList()
        }
    }
    
    /**
     * Converts a list of PointOfInterest objects to JSON and saves it to a file.
     *
     * @param pois The list of PointOfInterest objects to convert
     * @param filePath The path where the JSON file will be saved
     * @return true if the operation was successful, false otherwise
     */
    fun savePointsOfInterestToJson(pois: List<PointOfInterest>, filePath: String): Boolean {
        try {
            val file = File(filePath)
            val type = com.squareup.moshi.Types.newParameterizedType(List::class.java, PointOfInterest::class.java)
            val adapter = moshi.adapter<List<PointOfInterest>>(type)
            val json = adapter.toJson(pois)
            
            file.writeText(json)
            Timber.d("Saved ${pois.size} POIs to JSON: $filePath")
            return true
        } catch (e: Exception) {
            Timber.e(e, "Error saving POIs to JSON: $filePath")
            return false
        }
    }
    
    /**
     * Discovers JSON files in a directory.
     *
     * @param directoryPath The path to the directory
     * @param fileExtension The file extension to look for (default is ".json")
     * @return A list of file paths
     */
    fun discoverJsonFiles(directoryPath: String, fileExtension: String = ".json"): List<String> {
        try {
            val directory = File(directoryPath)
            if (!directory.exists() || !directory.isDirectory) {
                Timber.e("Directory does not exist or is not a directory: $directoryPath")
                return emptyList()
            }
            
            // Find all JSON files in the directory
            val jsonFiles = directory.listFiles { file ->
                file.isFile && file.name.lowercase().endsWith(fileExtension)
            }?.map { it.absolutePath } ?: emptyList()
            
            Timber.d("Discovered ${jsonFiles.size} JSON files in $directoryPath")
            return jsonFiles
        } catch (e: Exception) {
            Timber.e(e, "Error discovering JSON files in directory: $directoryPath")
            return emptyList()
        }
    }
}