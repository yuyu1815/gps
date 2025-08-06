# Android Indoor Positioning Application - API Documentation

## Table of Contents

1. [Introduction](#introduction)
2. [Core APIs](#core-apis)
   - [Beacon Management API](#beacon-management-api)
   - [Positioning API](#positioning-api)
   - [Map API](#map-api)
   - [Sensor API](#sensor-api)
3. [Extension Points](#extension-points)
   - [Custom Positioning Algorithms](#custom-positioning-algorithms)
   - [Custom Map Providers](#custom-map-providers)
   - [Custom Sensor Processing](#custom-sensor-processing)
   - [Custom Beacon Types](#custom-beacon-types)
4. [Integration Guidelines](#integration-guidelines)
5. [Example Implementations](#example-implementations)

## Introduction

This document provides comprehensive documentation for the Android Indoor Positioning Application's APIs, designed for developers who want to extend or integrate with the application. The APIs are organized into core functionality groups and extension points.

## Core APIs

### Beacon Management API

The Beacon Management API provides interfaces for discovering, monitoring, and managing BLE beacons.

#### IBeaconRepository

```kotlin
interface IBeaconRepository {
    /**
     * Gets all currently detected beacons.
     * @return Flow of beacon lists that updates when beacon data changes
     */
    fun getBeacons(): Flow<List<Beacon>>
    
    /**
     * Gets a specific beacon by ID.
     * @param id Beacon identifier
     * @return Flow containing the beacon if found, or null if not found
     */
    fun getBeaconById(id: String): Flow<Beacon?>
    
    /**
     * Updates beacon information.
     * @param beacon Updated beacon object
     */
    suspend fun updateBeacon(beacon: Beacon)
    
    /**
     * Adds a new beacon to the repository.
     * @param beacon Beacon to add
     */
    suspend fun addBeacon(beacon: Beacon)
    
    /**
     * Removes a beacon from the repository.
     * @param beaconId ID of the beacon to remove
     */
    suspend fun removeBeacon(beaconId: String)
    
    /**
     * Updates the RSSI value for a specific beacon.
     * @param beaconId ID of the beacon
     * @param rssi New RSSI value
     * @param timestamp Timestamp of the measurement
     */
    suspend fun updateBeaconRssi(beaconId: String, rssi: Int, timestamp: Long)
    
    /**
     * Gets beacons that have been detected within a specific time window.
     * @param timeWindowMs Maximum age of beacon data in milliseconds
     * @return List of recently detected beacons
     */
    fun getRecentBeacons(timeWindowMs: Long): Flow<List<Beacon>>
}
```

#### BleScanner

```kotlin
interface BleScanner {
    /**
     * Starts scanning for BLE beacons.
     * @param scanPeriodMs Duration of each scan in milliseconds
     * @param scanIntervalMs Time between scans in milliseconds
     */
    fun startScanning(scanPeriodMs: Long = 2000, scanIntervalMs: Long = 5000)
    
    /**
     * Stops scanning for BLE beacons.
     */
    fun stopScanning()
    
    /**
     * Checks if scanning is currently active.
     * @return True if scanning, false otherwise
     */
    fun isScanning(): Boolean
    
    /**
     * Sets a callback for scan results.
     * @param callback Function to call with scan results
     */
    fun setScanResultCallback(callback: (ScanResult) -> Unit)
    
    /**
     * Sets a filter for beacon scanning.
     * @param filter Filter configuration
     */
    fun setFilter(filter: BeaconFilter)
}
```

### Positioning API

The Positioning API provides interfaces for calculating and accessing position information.

#### IPositionRepository

```kotlin
interface IPositionRepository {
    /**
     * Gets the current user position.
     * @return Flow of user position that updates when position changes
     */
    fun getCurrentPosition(): Flow<UserPosition>
    
    /**
     * Gets the history of user positions.
     * @param count Maximum number of positions to retrieve
     * @return List of historical positions
     */
    fun getPositionHistory(count: Int): Flow<List<UserPosition>>
    
    /**
     * Updates the current user position.
     * @param position New user position
     */
    suspend fun updatePosition(position: UserPosition)
    
    /**
     * Clears the position history.
     */
    suspend fun clearPositionHistory()
    
    /**
     * Gets the position uncertainty.
     * @return Flow of position uncertainty in meters
     */
    fun getPositionUncertainty(): Flow<Float>
}
```

#### CalculatePositionUseCase

```kotlin
interface CalculatePositionUseCase : UseCase<CalculatePositionUseCase.Params, UserPosition> {
    /**
     * Parameters for position calculation.
     * @param beacons List of beacons to use for positioning
     * @param lastPosition Previous user position (optional)
     * @param sensorData Recent sensor data (optional)
     * @param useKalmanFilter Whether to apply Kalman filtering
     */
    data class Params(
        val beacons: List<Beacon>,
        val lastPosition: UserPosition? = null,
        val sensorData: SensorData.Combined? = null,
        val useKalmanFilter: Boolean = true
    )
}
```

### Map API

The Map API provides interfaces for loading, managing, and rendering indoor maps.

#### IMapRepository

```kotlin
interface IMapRepository {
    /**
     * Gets all available maps.
     * @return Flow of map lists that updates when maps are added or removed
     */
    fun getMaps(): Flow<List<IndoorMap>>
    
    /**
     * Gets a specific map by ID.
     * @param mapId Map identifier
     * @return Flow containing the map if found, or null if not found
     */
    fun getMapById(mapId: String): Flow<IndoorMap?>
    
    /**
     * Loads maps from a directory.
     * @param directoryPath Path to directory containing map files
     * @return List of loaded maps
     */
    suspend fun loadMapsFromDirectory(directoryPath: String): List<IndoorMap>
    
    /**
     * Adds a new map to the repository.
     * @param map Map to add
     */
    suspend fun addMap(map: IndoorMap)
    
    /**
     * Removes a map from the repository.
     * @param mapId ID of the map to remove
     */
    suspend fun removeMap(mapId: String)
    
    /**
     * Gets the currently active map.
     * @return Flow containing the active map, or null if no map is active
     */
    fun getActiveMap(): Flow<IndoorMap?>
    
    /**
     * Sets the active map.
     * @param mapId ID of the map to set as active
     */
    suspend fun setActiveMap(mapId: String)
}
```

#### CoordinateTransformer

```kotlin
interface CoordinateTransformer {
    /**
     * Converts physical coordinates (meters) to screen coordinates (pixels).
     * @param x X coordinate in meters
     * @param y Y coordinate in meters
     * @return Pair of screen coordinates (x, y) in pixels
     */
    fun metersToScreen(x: Float, y: Float): Pair<Float, Float>
    
    /**
     * Converts screen coordinates (pixels) to physical coordinates (meters).
     * @param screenX X coordinate in pixels
     * @param screenY Y coordinate in pixels
     * @return Pair of physical coordinates (x, y) in meters
     */
    fun screenToMeters(screenX: Float, screenY: Float): Pair<Float, Float>
    
    /**
     * Gets the scale factor for rendering objects at the current zoom level.
     * @return Scale factor
     */
    fun getScaleFactor(): Float
    
    /**
     * Checks if a physical coordinate is visible on the screen.
     * @param x X coordinate in meters
     * @param y Y coordinate in meters
     * @return True if visible, false otherwise
     */
    fun isVisibleOnScreen(x: Float, y: Float): Boolean
    
    /**
     * Gets the visible area in physical coordinates.
     * @return Pair of pairs: ((minX, minY), (maxX, maxY))
     */
    fun getVisibleArea(): Pair<Pair<Float, Float>, Pair<Float, Float>>
}
```

### Sensor API

The Sensor API provides interfaces for accessing and processing sensor data.

#### SensorMonitor

```kotlin
interface SensorMonitor {
    /**
     * Starts monitoring sensors.
     * @param sensorTypes Set of sensor types to monitor
     * @param samplingRateUs Sampling rate in microseconds
     */
    fun startMonitoring(sensorTypes: Set<SensorType>, samplingRateUs: Int = 20000)
    
    /**
     * Stops monitoring sensors.
     */
    fun stopMonitoring()
    
    /**
     * Checks if monitoring is currently active.
     * @return True if monitoring, false otherwise
     */
    fun isMonitoring(): Boolean
    
    /**
     * Gets the latest sensor data.
     * @return Flow of combined sensor data that updates when new readings are available
     */
    fun getSensorData(): Flow<SensorData.Combined>
    
    /**
     * Sets the sensor data listener.
     * @param listener Listener to receive sensor data updates
     */
    fun setSensorDataListener(listener: (SensorData.Combined) -> Unit)
    
    /**
     * Gets the supported sensor types.
     * @return Set of supported sensor types
     */
    fun getSupportedSensorTypes(): Set<SensorType>
}
```

#### StaticDetector

```kotlin
interface StaticDetector {
    /**
     * Detects if the device is in a static state based on sensor data.
     * @param sensorData The latest sensor data
     * @return True if the device is static, false otherwise
     */
    fun detectStaticState(sensorData: SensorData.Combined): Boolean
    
    /**
     * Checks if the device has been static for a long period.
     * @return True if the device has been static for a long period, false otherwise
     */
    fun isLongStaticState(): Boolean
    
    /**
     * Gets the confidence level in the current static state detection.
     * @return Value between 0 (no confidence) and 1 (high confidence)
     */
    fun getStaticConfidence(): Float
    
    /**
     * Gets the duration of the current static state in milliseconds.
     * @return Duration in milliseconds, 0 if not static
     */
    fun getStaticDuration(): Long
    
    /**
     * Resets the detector state.
     */
    fun reset()
}
```

## Extension Points

The application provides several extension points for customizing and enhancing functionality.

### Custom Positioning Algorithms

You can implement custom positioning algorithms by creating new implementations of the positioning interfaces.

#### Creating a Custom Positioning Algorithm

1. Create a new class that implements `CalculatePositionUseCase`:

```kotlin
class MyCustomPositioningAlgorithm : CalculatePositionUseCase {
    override suspend fun invoke(params: CalculatePositionUseCase.Params): UserPosition {
        // Implement your custom positioning algorithm
        // Process beacon data from params.beacons
        // Optionally use sensor data from params.sensorData
        // Return a UserPosition object with the calculated position
        
        return UserPosition(
            x = calculatedX,
            y = calculatedY,
            uncertainty = calculatedUncertainty,
            timestamp = System.currentTimeMillis()
        )
    }
}
```

2. Register your custom algorithm in the dependency injection module:

```kotlin
@Module
class CustomPositioningModule {
    @Provides
    fun provideCalculatePositionUseCase(): CalculatePositionUseCase {
        return MyCustomPositioningAlgorithm()
    }
}
```

### Custom Map Providers

You can implement custom map providers to support additional map formats or sources.

#### Creating a Custom Map Provider

1. Create a new class that implements map loading functionality:

```kotlin
class MyCustomMapProvider {
    /**
     * Loads a map from a custom format.
     * @param filePath Path to the map file
     * @return Loaded indoor map
     */
    fun loadMap(filePath: String): IndoorMap {
        // Implement custom map loading logic
        // Parse the file and create an IndoorMap object
        
        return IndoorMap(
            id = generateId(),
            name = extractName(),
            widthMeters = extractWidth(),
            heightMeters = extractHeight(),
            pixelsPerMeter = calculateScale(),
            // ... other properties
        )
    }
}
```

2. Extend the `MapRepository` to use your custom provider:

```kotlin
class CustomMapRepository(
    private val customMapProvider: MyCustomMapProvider,
    // ... other dependencies
) : MapRepository() {
    
    /**
     * Loads a map from a custom format.
     * @param filePath Path to the map file
     * @return Loaded indoor map
     */
    suspend fun loadCustomFormatMap(filePath: String): IndoorMap {
        val map = customMapProvider.loadMap(filePath)
        addMap(map)
        return map
    }
}
```

### Custom Sensor Processing

You can implement custom sensor processing algorithms to improve sensor data quality or extract additional information.

#### Creating a Custom Sensor Processor

1. Create a new class for processing sensor data:

```kotlin
class MyCustomSensorProcessor {
    /**
     * Processes raw sensor data to extract additional information.
     * @param sensorData Raw sensor data
     * @return Processed sensor data
     */
    fun processSensorData(sensorData: SensorData.Combined): ProcessedSensorData {
        // Implement custom sensor processing logic
        // Extract features, filter noise, etc.
        
        return ProcessedSensorData(
            // ... processed data fields
        )
    }
    
    data class ProcessedSensorData(
        // ... define your custom processed data structure
    )
}
```

2. Integrate your processor with the sensor monitoring system:

```kotlin
class EnhancedSensorMonitor(
    private val sensorMonitor: SensorMonitor,
    private val customProcessor: MyCustomSensorProcessor
) {
    /**
     * Gets processed sensor data.
     * @return Flow of processed sensor data
     */
    fun getProcessedSensorData(): Flow<MyCustomSensorProcessor.ProcessedSensorData> {
        return sensorMonitor.getSensorData()
            .map { customProcessor.processSensorData(it) }
    }
}
```

### Custom Beacon Types

You can extend the system to support custom beacon types or protocols.

#### Supporting a Custom Beacon Type

1. Create a new class to represent your custom beacon type:

```kotlin
class CustomBeacon(
    id: String,
    macAddress: String,
    x: Float,
    y: Float,
    txPower: Int,
    // ... standard beacon properties
    
    // Custom properties
    val customProperty1: String,
    val customProperty2: Int
) : Beacon(id, macAddress, x, y, txPower) {
    
    /**
     * Custom distance calculation method for this beacon type.
     */
    override fun calculateDistance(environmentalFactor: Float, useFilteredRssi: Boolean, currentTime: Long): Float {
        // Implement custom distance calculation algorithm
        // that may use the custom properties
        
        return customDistanceCalculation()
    }
}
```

2. Extend the `BleScanner` to detect and process your custom beacons:

```kotlin
class CustomBeaconScanner(
    // ... dependencies
) : BleScanner {
    
    override fun setScanResultCallback(callback: (ScanResult) -> Unit) {
        // Set up a custom callback that can identify and process your custom beacons
        super.setScanResultCallback { scanResult ->
            if (isCustomBeacon(scanResult)) {
                val customBeacon = parseCustomBeacon(scanResult)
                callback(customBeacon)
            } else {
                callback(scanResult)
            }
        }
    }
    
    private fun isCustomBeacon(scanResult: ScanResult): Boolean {
        // Implement logic to identify your custom beacon type
    }
    
    private fun parseCustomBeacon(scanResult: ScanResult): CustomBeacon {
        // Parse the scan result into your custom beacon type
    }
}
```

## Integration Guidelines

### Integrating with External Applications

To integrate the Indoor Positioning Application with external applications:

1. **Content Provider Integration**:
   - The application exposes a content provider for position data
   - External applications can query this provider with appropriate permissions

2. **Intent-based Integration**:
   - The application responds to specific intents for positioning requests
   - External applications can send intents to request position information

3. **Service Binding**:
   - External applications can bind to the positioning service
   - This provides real-time position updates

### Example Content Provider Query

```kotlin
// Query the position content provider
val uri = Uri.parse("content://com.example.myapplication.provider.position/current")
val projection = arrayOf("x", "y", "uncertainty", "timestamp")
val cursor = contentResolver.query(uri, projection, null, null, null)

cursor?.use {
    if (it.moveToFirst()) {
        val x = it.getFloat(it.getColumnIndexOrThrow("x"))
        val y = it.getFloat(it.getColumnIndexOrThrow("y"))
        val uncertainty = it.getFloat(it.getColumnIndexOrThrow("uncertainty"))
        val timestamp = it.getLong(it.getColumnIndexOrThrow("timestamp"))
        
        // Use the position data
    }
}
```

### Example Intent Request

```kotlin
// Send an intent to request position
val intent = Intent("com.example.myapplication.REQUEST_POSITION")
intent.putExtra("requestId", "uniqueRequestId")
sendBroadcast(intent)

// Register a receiver for the response
val receiver = object : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "com.example.myapplication.POSITION_RESULT") {
            val requestId = intent.getStringExtra("requestId")
            val x = intent.getFloatExtra("x", 0f)
            val y = intent.getFloatExtra("y", 0f)
            val uncertainty = intent.getFloatExtra("uncertainty", 0f)
            
            // Use the position data
        }
    }
}
registerReceiver(receiver, IntentFilter("com.example.myapplication.POSITION_RESULT"))
```

### Example Service Binding

```kotlin
// Bind to the positioning service
val serviceConnection = object : ServiceConnection {
    override fun onServiceConnected(name: ComponentName, service: IBinder) {
        val positioningService = (service as PositioningService.LocalBinder).getService()
        
        // Register for position updates
        positioningService.registerPositionListener { position ->
            // Handle position updates
        }
    }
    
    override fun onServiceDisconnected(name: ComponentName) {
        // Handle disconnection
    }
}

val intent = Intent(context, PositioningService::class.java)
context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
```

## Example Implementations

### Custom Triangulation Algorithm

```kotlin
class EnhancedTriangulationAlgorithm : TriangulatePositionUseCase {
    override suspend fun invoke(params: TriangulatePositionUseCase.Params): UserPosition {
        val beacons = params.beacons
        
        // Filter beacons with low confidence
        val reliableBeacons = beacons.filter { it.distanceConfidence > 0.6f }
        
        if (reliableBeacons.size < 3) {
            // Fall back to standard triangulation if not enough reliable beacons
            return standardTriangulation(beacons)
        }
        
        // Implement weighted least squares triangulation
        // that gives more weight to beacons with higher confidence
        
        // ... algorithm implementation
        
        return UserPosition(
            x = calculatedX,
            y = calculatedY,
            uncertainty = calculatedUncertainty,
            timestamp = System.currentTimeMillis()
        )
    }
    
    private fun standardTriangulation(beacons: List<Beacon>): UserPosition {
        // Standard triangulation implementation
        // ... algorithm implementation
    }
}
```

### Custom Map Renderer

```kotlin
@Composable
fun CustomMapRenderer(
    map: IndoorMap,
    transformer: CoordinateTransformer,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        // Draw the base map
        drawMap(map, transformer)
        
        // Draw custom overlays
        drawCustomOverlays(map, transformer)
        
        // Draw grid lines
        drawGrid(map, transformer)
    }
}

private fun DrawScope.drawMap(map: IndoorMap, transformer: CoordinateTransformer) {
    // Draw the map image
    // ... implementation
}

private fun DrawScope.drawCustomOverlays(map: IndoorMap, transformer: CoordinateTransformer) {
    // Draw custom overlays like zones, points of interest, etc.
    // ... implementation
}

private fun DrawScope.drawGrid(map: IndoorMap, transformer: CoordinateTransformer) {
    // Draw grid lines for reference
    // ... implementation
}
```

### Enhanced Step Detection

```kotlin
class EnhancedStepDetectionUseCase : DetectStepUseCase {
    override suspend fun invoke(params: Params): Result {
        val accelData = params.accelerometerData
        val gyroData = params.gyroscopeData
        
        // Combine accelerometer and gyroscope data for more accurate step detection
        val combinedMagnitude = combineAccelAndGyro(accelData, gyroData)
        
        // Apply adaptive thresholding based on recent movement patterns
        val (adaptivePeakThreshold, adaptiveValleyThreshold) = calculateAdaptiveThresholds()
        
        // Detect steps using the enhanced algorithm
        val stepDetected = detectStepWithCombinedData(
            combinedMagnitude,
            adaptivePeakThreshold,
            adaptiveValleyThreshold
        )
        
        // ... implementation
        
        return Result(
            stepDetected = stepDetected,
            stepCount = currentStepCount,
            // ... other result fields
        )
    }
    
    private fun combineAccelAndGyro(accelData: SensorData.Accelerometer, gyroData: SensorData.Gyroscope?): Float {
        // Combine accelerometer and gyroscope data
        // ... implementation
    }
    
    private fun calculateAdaptiveThresholds(): Pair<Float, Float> {
        // Calculate adaptive thresholds based on recent movement patterns
        // ... implementation
    }
    
    private fun detectStepWithCombinedData(
        combinedMagnitude: Float,
        peakThreshold: Float,
        valleyThreshold: Float
    ): Boolean {
        // Detect steps using the combined data and adaptive thresholds
        // ... implementation
    }
}
```