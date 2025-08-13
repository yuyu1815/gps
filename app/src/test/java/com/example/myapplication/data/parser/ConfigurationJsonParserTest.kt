package com.example.myapplication.data.parser

import com.example.myapplication.domain.model.Beacon
import com.example.myapplication.domain.model.IndoorMap
import com.example.myapplication.domain.model.PointOfInterest
import com.example.myapplication.domain.model.PoiType
import com.example.myapplication.domain.model.Wall
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
class ConfigurationJsonParserTest {
    
    private lateinit var parser: ConfigurationJsonParser
    private lateinit var testDir: File
    
    @Before
    fun setUp() {
        parser = ConfigurationJsonParser()
        testDir = File("build/tmp/test_json")
        testDir.mkdirs()
    }
    
    @After
    fun tearDown() {
        testDir.deleteRecursively()
    }
    
    @Test
    fun `test parseMapFromJson and saveMapToJson`() {
        // Create a test map
        val map = IndoorMap(
            id = "test_map_1",
            name = "Test Map",
            widthMeters = 50f,
            heightMeters = 30f,
            pixelsPerMeter = 20f,
            imagePath = "maps/test_map.png",
            beacons = listOf(
                Beacon(
                    id = "beacon_1",
                    macAddress = "00:11:22:33:44:55",
                    x = 10f,
                    y = 15f,
                    txPower = -59,
                    name = "Test Beacon 1"
                )
            ),
            walls = listOf(
                Wall(
                    startX = 0f,
                    startY = 0f,
                    endX = 10f,
                    endY = 0f
                )
            ),
            pointsOfInterest = listOf(
                PointOfInterest(
                    id = "poi_1",
                    name = "Test POI",
                    x = 5f,
                    y = 5f,
                    type = PoiType.ROOM,
                    description = "Test description"
                )
            )
        )
        
        // Save the map to JSON
        val filePath = File(testDir, "test_map.json").absolutePath
        val saveResult = parser.saveMapToJson(map, filePath)
        
        // Verify save was successful
        assertTrue("Map should be saved successfully", saveResult)
        assertTrue("JSON file should exist", File(filePath).exists())
        
        // Parse the map from JSON
        val parsedMap = parser.parseMapFromJson(filePath)
        
        // Verify parsed map matches original
        assertNotNull("Parsed map should not be null", parsedMap)
        assertEquals("Map ID should match", map.id, parsedMap?.id)
        assertEquals("Map name should match", map.name, parsedMap?.name)
        assertEquals("Map width should match", map.widthMeters, parsedMap?.widthMeters)
        assertEquals("Map height should match", map.heightMeters, parsedMap?.heightMeters)
        assertEquals("Map pixels per meter should match", map.pixelsPerMeter, parsedMap?.pixelsPerMeter)
        assertEquals("Map image path should match", map.imagePath, parsedMap?.imagePath)
        
        // Verify beacons
        assertEquals("Map should have 1 beacon", 1, parsedMap?.beacons?.size)
        val parsedBeacon = parsedMap?.beacons?.get(0)
        assertEquals("Beacon ID should match", map.beacons[0].id, parsedBeacon?.id)
        assertEquals("Beacon MAC address should match", map.beacons[0].macAddress, parsedBeacon?.macAddress)
        assertEquals("Beacon X coordinate should match", map.beacons[0].x, parsedBeacon?.x)
        assertEquals("Beacon Y coordinate should match", map.beacons[0].y, parsedBeacon?.y)
        assertEquals("Beacon TX power should match", map.beacons[0].txPower, parsedBeacon?.txPower)
        assertEquals("Beacon name should match", map.beacons[0].name, parsedBeacon?.name)
        
        // Verify walls
        assertEquals("Map should have 1 wall", 1, parsedMap?.walls?.size)
        val parsedWall = parsedMap?.walls?.get(0)
        assertEquals("Wall start X should match", map.walls[0].startX, parsedWall?.startX)
        assertEquals("Wall start Y should match", map.walls[0].startY, parsedWall?.startY)
        assertEquals("Wall end X should match", map.walls[0].endX, parsedWall?.endX)
        assertEquals("Wall end Y should match", map.walls[0].endY, parsedWall?.endY)
        
        // Verify points of interest
        assertEquals("Map should have 1 POI", 1, parsedMap?.pointsOfInterest?.size)
        val parsedPoi = parsedMap?.pointsOfInterest?.get(0)
        assertEquals("POI ID should match", map.pointsOfInterest[0].id, parsedPoi?.id)
        assertEquals("POI name should match", map.pointsOfInterest[0].name, parsedPoi?.name)
        assertEquals("POI X coordinate should match", map.pointsOfInterest[0].x, parsedPoi?.x)
        assertEquals("POI Y coordinate should match", map.pointsOfInterest[0].y, parsedPoi?.y)
        assertEquals("POI type should match", map.pointsOfInterest[0].type, parsedPoi?.type)
        assertEquals("POI description should match", map.pointsOfInterest[0].description, parsedPoi?.description)
    }
    
    @Test
    fun `test parseBeaconsFromJson and saveBeaconsToJson`() {
        // Create test beacons
        val beacons = listOf(
            Beacon(
                id = "beacon_1",
                macAddress = "00:11:22:33:44:55",
                x = 10f,
                y = 15f,
                txPower = -59,
                name = "Test Beacon 1"
            ),
            Beacon(
                id = "beacon_2",
                macAddress = "AA:BB:CC:DD:EE:FF",
                x = 20f,
                y = 25f,
                txPower = -62,
                name = "Test Beacon 2"
            )
        )
        
        // Save beacons to JSON
        val filePath = File(testDir, "test_beacons.json").absolutePath
        val saveResult = parser.saveBeaconsToJson(beacons, filePath)
        
        // Verify save was successful
        assertTrue("Beacons should be saved successfully", saveResult)
        assertTrue("JSON file should exist", File(filePath).exists())
        
        // Parse beacons from JSON
        val parsedBeacons = parser.parseBeaconsFromJson(filePath)
        
        // Verify parsed beacons match original
        assertEquals("Should parse 2 beacons", 2, parsedBeacons.size)
        
        // Verify first beacon
        assertEquals("Beacon 1 ID should match", beacons[0].id, parsedBeacons[0].id)
        assertEquals("Beacon 1 MAC address should match", beacons[0].macAddress, parsedBeacons[0].macAddress)
        assertEquals("Beacon 1 X coordinate should match", beacons[0].x, parsedBeacons[0].x)
        assertEquals("Beacon 1 Y coordinate should match", beacons[0].y, parsedBeacons[0].y)
        assertEquals("Beacon 1 TX power should match", beacons[0].txPower, parsedBeacons[0].txPower)
        assertEquals("Beacon 1 name should match", beacons[0].name, parsedBeacons[0].name)
        
        // Verify second beacon
        assertEquals("Beacon 2 ID should match", beacons[1].id, parsedBeacons[1].id)
        assertEquals("Beacon 2 MAC address should match", beacons[1].macAddress, parsedBeacons[1].macAddress)
        assertEquals("Beacon 2 X coordinate should match", beacons[1].x, parsedBeacons[1].x)
        assertEquals("Beacon 2 Y coordinate should match", beacons[1].y, parsedBeacons[1].y)
        assertEquals("Beacon 2 TX power should match", beacons[1].txPower, parsedBeacons[1].txPower)
        assertEquals("Beacon 2 name should match", beacons[1].name, parsedBeacons[1].name)
    }
    
    @Test
    fun `test parseWallsFromJson and saveWallsToJson`() {
        // Create test walls
        val walls = listOf(
            Wall(
                startX = 0f,
                startY = 0f,
                endX = 10f,
                endY = 0f
            ),
            Wall(
                startX = 10f,
                startY = 0f,
                endX = 10f,
                endY = 10f
            )
        )
        
        // Save walls to JSON
        val filePath = File(testDir, "test_walls.json").absolutePath
        val saveResult = parser.saveWallsToJson(walls, filePath)
        
        // Verify save was successful
        assertTrue("Walls should be saved successfully", saveResult)
        assertTrue("JSON file should exist", File(filePath).exists())
        
        // Parse walls from JSON
        val parsedWalls = parser.parseWallsFromJson(filePath)
        
        // Verify parsed walls match original
        assertEquals("Should parse 2 walls", 2, parsedWalls.size)
        
        // Verify first wall
        assertEquals("Wall 1 start X should match", walls[0].startX, parsedWalls[0].startX)
        assertEquals("Wall 1 start Y should match", walls[0].startY, parsedWalls[0].startY)
        assertEquals("Wall 1 end X should match", walls[0].endX, parsedWalls[0].endX)
        assertEquals("Wall 1 end Y should match", walls[0].endY, parsedWalls[0].endY)
        
        // Verify second wall
        assertEquals("Wall 2 start X should match", walls[1].startX, parsedWalls[1].startX)
        assertEquals("Wall 2 start Y should match", walls[1].startY, parsedWalls[1].startY)
        assertEquals("Wall 2 end X should match", walls[1].endX, parsedWalls[1].endX)
        assertEquals("Wall 2 end Y should match", walls[1].endY, parsedWalls[1].endY)
    }
    
    @Test
    fun `test parsePointsOfInterestFromJson and savePointsOfInterestToJson`() {
        // Create test points of interest
        val pois = listOf(
            PointOfInterest(
                id = "poi_1",
                name = "Test POI 1",
                x = 5f,
                y = 5f,
                type = PoiType.ROOM,
                description = "Test description 1"
            ),
            PointOfInterest(
                id = "poi_2",
                name = "Test POI 2",
                x = 15f,
                y = 15f,
                type = PoiType.ENTRANCE,
                description = "Test description 2"
            )
        )
        
        // Save POIs to JSON
        val filePath = File(testDir, "test_pois.json").absolutePath
        val saveResult = parser.savePointsOfInterestToJson(pois, filePath)
        
        // Verify save was successful
        assertTrue("POIs should be saved successfully", saveResult)
        assertTrue("JSON file should exist", File(filePath).exists())
        
        // Parse POIs from JSON
        val parsedPois = parser.parsePointsOfInterestFromJson(filePath)
        
        // Verify parsed POIs match original
        assertEquals("Should parse 2 POIs", 2, parsedPois.size)
        
        // Verify first POI
        assertEquals("POI 1 ID should match", pois[0].id, parsedPois[0].id)
        assertEquals("POI 1 name should match", pois[0].name, parsedPois[0].name)
        assertEquals("POI 1 X coordinate should match", pois[0].x, parsedPois[0].x)
        assertEquals("POI 1 Y coordinate should match", pois[0].y, parsedPois[0].y)
        assertEquals("POI 1 type should match", pois[0].type, parsedPois[0].type)
        assertEquals("POI 1 description should match", pois[0].description, parsedPois[0].description)
        
        // Verify second POI
        assertEquals("POI 2 ID should match", pois[1].id, parsedPois[1].id)
        assertEquals("POI 2 name should match", pois[1].name, parsedPois[1].name)
        assertEquals("POI 2 X coordinate should match", pois[1].x, parsedPois[1].x)
        assertEquals("POI 2 Y coordinate should match", pois[1].y, parsedPois[1].y)
        assertEquals("POI 2 type should match", pois[1].type, parsedPois[1].type)
        assertEquals("POI 2 description should match", pois[1].description, parsedPois[1].description)
    }
    
    @Test
    fun `test discoverJsonFiles`() {
        // Create test JSON files
        File(testDir, "file1.json").createNewFile()
        File(testDir, "file2.json").createNewFile()
        File(testDir, "file3.txt").createNewFile()
        
        // Discover JSON files
        val jsonFiles = parser.discoverJsonFiles(testDir.absolutePath)
        
        // Verify discovered files
        assertEquals("Should discover 2 JSON files", 2, jsonFiles.size)
        assertTrue("Should discover file1.json", jsonFiles.any { it.endsWith("file1.json") })
        assertTrue("Should discover file2.json", jsonFiles.any { it.endsWith("file2.json") })
        assertFalse("Should not discover file3.txt", jsonFiles.any { it.endsWith("file3.txt") })
    }
    
    @Test
    fun `test parseMapFromJson with non-existent file`() {
        val filePath = File(testDir, "non_existent.json").absolutePath
        val parsedMap = parser.parseMapFromJson(filePath)
        assertNull("Parsed map should be null for non-existent file", parsedMap)
    }
    
    @Test
    fun `test parseBeaconsFromJson with non-existent file`() {
        val filePath = File(testDir, "non_existent.json").absolutePath
        val parsedBeacons = parser.parseBeaconsFromJson(filePath)
        assertTrue("Parsed beacons should be empty for non-existent file", parsedBeacons.isEmpty())
    }
}