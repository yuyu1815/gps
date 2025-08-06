package com.example.myapplication.service

import android.content.Context
import android.os.Environment
import com.example.myapplication.data.repository.IBeaconRepository
import com.example.myapplication.data.repository.IPositionRepository
import com.example.myapplication.domain.model.Beacon
import com.example.myapplication.domain.model.UserPosition
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Tool for conducting field tests of the indoor positioning system.
 * 
 * This class provides functionality for:
 * - Recording test data (positions, beacons)
 * - Calculating positioning accuracy metrics
 * - Exporting test results to CSV files
 * - Managing test sessions and scenarios
 */
class FieldTestTool(
    private val context: Context,
    private val beaconRepository: IBeaconRepository,
    private val positionRepository: IPositionRepository
) {
    // Test state
    private var isTestRunning = false
    private var currentTestName = ""
    private var testStartTime = 0L
    private var groundTruthPosition: UserPosition? = null
    
    // Test data
    private val positionSamples = mutableListOf<PositionSample>()
    private val beaconSamples = mutableListOf<BeaconSample>()
    
    // Test metrics
    private val _testMetrics = MutableStateFlow(TestMetrics())
    val testMetrics: StateFlow<TestMetrics> = _testMetrics.asStateFlow()
    
    // Coroutine scope for background operations
    private val scope = CoroutineScope(Dispatchers.IO)
    
    /**
     * Starts a new test session.
     * 
     * @param testName Name of the test for identification
     * @param groundTruthX True X coordinate of the test position (if known)
     * @param groundTruthY True Y coordinate of the test position (if known)
     */
    fun startTest(testName: String, groundTruthX: Float? = null, groundTruthY: Float? = null) {
        if (isTestRunning) {
            Timber.w("Test already running. Stop the current test before starting a new one.")
            return
        }
        
        // Clear previous test data
        positionSamples.clear()
        beaconSamples.clear()
        
        // Set test parameters
        currentTestName = testName
        testStartTime = System.currentTimeMillis()
        groundTruthPosition = if (groundTruthX != null && groundTruthY != null) {
            UserPosition(groundTruthX, groundTruthY, 1.0f, testStartTime)
        } else {
            null
        }
        
        // Reset metrics
        _testMetrics.value = TestMetrics()
        
        // Start data collection
        isTestRunning = true
        startDataCollection()
        
        Timber.i("Field test started: $testName")
    }
    
    /**
     * Stops the current test session and calculates metrics.
     * 
     * @return True if the test was successfully stopped, false otherwise
     */
    fun stopTest(): Boolean {
        if (!isTestRunning) {
            Timber.w("No test is currently running.")
            return false
        }
        
        isTestRunning = false
        
        // Calculate test metrics
        calculateTestMetrics()
        
        Timber.i("Field test stopped: $currentTestName")
        return true
    }
    
    /**
     * Updates the ground truth position during a test.
     * 
     * @param x True X coordinate
     * @param y True Y coordinate
     */
    fun updateGroundTruthPosition(x: Float, y: Float) {
        if (!isTestRunning) {
            Timber.w("No test is currently running.")
            return
        }
        
        groundTruthPosition = UserPosition(x, y, 1.0f, System.currentTimeMillis())
        Timber.d("Ground truth position updated: ($x, $y)")
    }
    
    /**
     * Exports the test data to CSV files.
     * 
     * @return Path to the exported files directory, or null if export failed
     */
    fun exportTestData(): String? {
        if (positionSamples.isEmpty() && beaconSamples.isEmpty()) {
            Timber.w("No test data to export.")
            return null
        }
        
        try {
            // Create directory for test data
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val testDirName = "field_test_${currentTestName.replace(" ", "_")}_$timestamp"
            
            val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            val testDir = File(storageDir, testDirName)
            
            if (!testDir.exists()) {
                testDir.mkdirs()
            }
            
            // Export position data
            if (positionSamples.isNotEmpty()) {
                val positionFile = File(testDir, "positions.csv")
                FileWriter(positionFile).use { writer ->
                    writer.append("timestamp,estimatedX,estimatedY,confidence,groundTruthX,groundTruthY,error\n")
                    
                    positionSamples.forEach { sample ->
                        writer.append("${sample.timestamp},${sample.estimatedX},${sample.estimatedY},")
                        writer.append("${sample.confidence},${sample.groundTruthX ?: ""},${sample.groundTruthY ?: ""},")
                        writer.append("${sample.error ?: ""}\n")
                    }
                }
            }
            
            // Export beacon data
            if (beaconSamples.isNotEmpty()) {
                val beaconFile = File(testDir, "beacons.csv")
                FileWriter(beaconFile).use { writer ->
                    writer.append("timestamp,macAddress,rssi,filteredRssi,estimatedDistance,distanceConfidence\n")
                    
                    beaconSamples.forEach { sample ->
                        writer.append("${sample.timestamp},${sample.macAddress},${sample.rssi},")
                        writer.append("${sample.filteredRssi},${sample.estimatedDistance},${sample.distanceConfidence}\n")
                    }
                }
            }
            
            // Export test metrics
            val metricsFile = File(testDir, "metrics.csv")
            FileWriter(metricsFile).use { writer ->
                val metrics = testMetrics.value
                writer.append("metric,value\n")
                writer.append("testName,${currentTestName}\n")
                writer.append("testDuration,${metrics.testDuration}\n")
                writer.append("meanError,${metrics.meanError}\n")
                writer.append("rmseError,${metrics.rmseError}\n")
                writer.append("maxError,${metrics.maxError}\n")
                writer.append("percentile95Error,${metrics.percentile95Error}\n")
                writer.append("standardDeviation,${metrics.standardDeviation}\n")
                writer.append("meanConfidence,${metrics.meanConfidence}\n")
                writer.append("meanBeaconCount,${metrics.meanBeaconCount}\n")
                writer.append("updateRate,${metrics.updateRate}\n")
            }
            
            Timber.i("Test data exported to ${testDir.absolutePath}")
            return testDir.absolutePath
            
        } catch (e: Exception) {
            Timber.e(e, "Failed to export test data")
            return null
        }
    }
    
    /**
     * Starts collecting data from various sources.
     */
    private fun startDataCollection() {
        scope.launch {
            // Collect position data
            positionRepository.getCurrentPositionFlow().collect { position ->
                if (!isTestRunning || position == null) return@collect
                
                val currentTime = System.currentTimeMillis()
                val groundTruth = groundTruthPosition
                
                // Calculate error if ground truth is available
                val error = if (groundTruth != null) {
                    calculatePositionError(
                        position.x, position.y,
                        groundTruth.x, groundTruth.y
                    )
                } else {
                    null
                }
                
                // Record position sample
                val sample = PositionSample(
                    timestamp = currentTime,
                    estimatedX = position.x,
                    estimatedY = position.y,
                    confidence = position.confidence,
                    groundTruthX = groundTruth?.x,
                    groundTruthY = groundTruth?.y,
                    error = error
                )
                
                positionSamples.add(sample)
                
                // Update real-time metrics if ground truth is available
                if (groundTruth != null && error != null) {
                    updateRealTimeMetrics(error, position.confidence)
                }
            }
        }
        
        scope.launch {
            // Collect beacon data
            beaconRepository.getBeaconsFlow().collect { beacons ->
                if (!isTestRunning) return@collect
                
                val currentTime = System.currentTimeMillis()
                
                beacons.forEach { beacon ->
                    val sample = BeaconSample(
                        timestamp = currentTime,
                        macAddress = beacon.macAddress,
                        rssi = beacon.lastRssi,
                        filteredRssi = beacon.filteredRssi,
                        estimatedDistance = beacon.estimatedDistance,
                        distanceConfidence = beacon.distanceConfidence
                    )
                    
                    beaconSamples.add(sample)
                }
                
                // Update beacon count metric
                val currentMetrics = _testMetrics.value
                _testMetrics.value = currentMetrics.copy(
                    currentBeaconCount = beacons.size
                )
            }
        }
    }
    
    /**
     * Updates real-time metrics during a test.
     * 
     * @param error Current position error
     * @param confidence Current position confidence
     */
    private fun updateRealTimeMetrics(error: Float, confidence: Float) {
        val currentMetrics = _testMetrics.value
        
        // Update error metrics
        val errors = positionSamples.mapNotNull { it.error }
        val meanError = errors.average().toFloat()
        val maxError = errors.maxOrNull() ?: 0f
        
        // Calculate RMSE
        val rmse = sqrt(errors.map { it * it }.average()).toFloat()
        
        // Calculate 95th percentile error
        val sortedErrors = errors.sorted()
        val index95 = (sortedErrors.size * 0.95).toInt().coerceAtMost(sortedErrors.size - 1)
        val percentile95Error = sortedErrors.getOrNull(index95) ?: 0f
        
        // Calculate standard deviation
        val variance = errors.map { (it - meanError).pow(2) }.average().toFloat()
        val stdDev = sqrt(variance)
        
        // Calculate mean confidence
        val meanConfidence = positionSamples.map { it.confidence }.average().toFloat()
        
        // Calculate update rate (positions per second)
        val testDuration = (System.currentTimeMillis() - testStartTime) / 1000f
        val updateRate = if (testDuration > 0) positionSamples.size / testDuration else 0f
        
        // Calculate mean beacon count
        val beaconCounts = beaconSamples.groupBy { it.timestamp }
            .map { it.value.size }
        val meanBeaconCount = if (beaconCounts.isNotEmpty()) {
            beaconCounts.average().toFloat()
        } else {
            0f
        }
        
        // Update metrics
        _testMetrics.value = TestMetrics(
            testDuration = testDuration,
            meanError = meanError,
            rmseError = rmse,
            maxError = maxError,
            percentile95Error = percentile95Error,
            standardDeviation = stdDev,
            meanConfidence = meanConfidence,
            currentBeaconCount = currentMetrics.currentBeaconCount,
            meanBeaconCount = meanBeaconCount,
            updateRate = updateRate
        )
    }
    
    /**
     * Calculates all test metrics at the end of a test.
     */
    private fun calculateTestMetrics() {
        val errors = positionSamples.mapNotNull { it.error }
        
        if (errors.isEmpty()) {
            Timber.w("No position errors to calculate metrics from.")
            return
        }
        
        // Calculate final metrics
        updateRealTimeMetrics(errors.last(), positionSamples.last().confidence)
    }
    
    /**
     * Calculates the Euclidean distance between two points.
     */
    private fun calculatePositionError(
        estimatedX: Float, estimatedY: Float,
        actualX: Float, actualY: Float
    ): Float {
        val dx = estimatedX - actualX
        val dy = estimatedY - actualY
        return sqrt(dx * dx + dy * dy)
    }
    
    /**
     * Data class representing a position sample during testing.
     */
    data class PositionSample(
        val timestamp: Long,
        val estimatedX: Float,
        val estimatedY: Float,
        val confidence: Float,
        val groundTruthX: Float?,
        val groundTruthY: Float?,
        val error: Float?
    )
    
    /**
     * Data class representing a beacon sample during testing.
     */
    data class BeaconSample(
        val timestamp: Long,
        val macAddress: String,
        val rssi: Int,
        val filteredRssi: Int,
        val estimatedDistance: Float,
        val distanceConfidence: Float
    )
    
    /**
     * Data class representing test metrics.
     */
    data class TestMetrics(
        val testDuration: Float = 0f,
        val meanError: Float = 0f,
        val rmseError: Float = 0f,
        val maxError: Float = 0f,
        val percentile95Error: Float = 0f,
        val standardDeviation: Float = 0f,
        val meanConfidence: Float = 0f,
        val currentBeaconCount: Int = 0,
        val meanBeaconCount: Float = 0f,
        val updateRate: Float = 0f
    )
}