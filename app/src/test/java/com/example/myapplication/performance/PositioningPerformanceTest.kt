package com.example.myapplication.performance

import com.example.myapplication.domain.model.Beacon
import com.example.myapplication.domain.usecase.distance.CalculateDistanceUseCase
import com.example.myapplication.domain.usecase.position.TriangulatePositionUseCase
import com.example.myapplication.domain.usecase.position.LeastSquaresPositionUseCase
import com.example.myapplication.util.MockBleScanner
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import kotlin.system.measureTimeMillis

/**
 * Performance tests for the positioning algorithms.
 * 
 * These tests measure the execution time of various positioning algorithms
 * to ensure they meet performance requirements for real-time usage.
 */
class PositioningPerformanceTest {

    private lateinit var mockBleScanner: MockBleScanner
    private lateinit var triangulatePositionUseCase: TriangulatePositionUseCase
    private lateinit var leastSquaresPositionUseCase: LeastSquaresPositionUseCase
    
    @Before
    fun setUp() {
        mockBleScanner = MockBleScanner()
        triangulatePositionUseCase = TriangulatePositionUseCase()
        leastSquaresPositionUseCase = LeastSquaresPositionUseCase()
        
        // Generate a grid of beacons for testing
        mockBleScanner.generateGridBeacons(
            rows = 5,
            cols = 5,
            spacingX = 5f,
            spacingY = 5f,
            rssiAtOneMeter = -70,
            environmentalFactor = 2.0f,
            rssiNoise = 0.1f
        )
        
        mockBleScanner.startScanning()
    }
    
    @Test
    fun testTriangulationPerformance() {
        // Simulate user at center of grid
        mockBleScanner.simulateScanCycle(10f, 10f)
        
        // Get discovered beacons
        val beacons = runBlocking { mockBleScanner.discoveredBeaconsFlow.first() }
        
        // Measure execution time for 100 triangulation operations
        val executionTime = measureTimeMillis {
            repeat(100) {
                runBlocking {
                    triangulatePositionUseCase(
                        TriangulatePositionUseCase.Params(beacons)
                    )
                }
            }
        }
        
        // Calculate average execution time per operation
        val avgExecutionTime = executionTime / 100.0
        
        println("Triangulation average execution time: $avgExecutionTime ms")
        
        // Verify that triangulation is fast enough for real-time usage
        // Typically, we want positioning updates at least 1-10 times per second
        assertTrue("Triangulation is too slow for real-time usage", avgExecutionTime < 100.0)
    }
    
    @Test
    fun testLeastSquaresPerformance() {
        // Simulate user at center of grid
        mockBleScanner.simulateScanCycle(10f, 10f)
        
        // Get discovered beacons
        val beacons = runBlocking { mockBleScanner.discoveredBeaconsFlow.first() }
        
        // Measure execution time for 100 least squares operations
        val executionTime = measureTimeMillis {
            repeat(100) {
                runBlocking {
                    leastSquaresPositionUseCase(
                        LeastSquaresPositionUseCase.Params(beacons)
                    )
                }
            }
        }
        
        // Calculate average execution time per operation
        val avgExecutionTime = executionTime / 100.0
        
        println("Least Squares average execution time: $avgExecutionTime ms")
        
        // Verify that least squares is fast enough for real-time usage
        // It's expected to be slower than triangulation but still usable
        assertTrue("Least Squares is too slow for real-time usage", avgExecutionTime < 200.0)
    }
    
    @Test
    fun testScalabilityWithBeaconCount() {
        // Test with different numbers of beacons
        val beaconCounts = listOf(3, 5, 10, 15, 20, 25)
        
        for (count in beaconCounts) {
            // Clear previous beacons
            mockBleScanner.clearMockBeacons()
            
            // Generate new beacons
            for (i in 0 until count) {
                val angle = 2 * Math.PI * i / count
                val x = 10f + 5f * Math.cos(angle).toFloat()
                val y = 10f + 5f * Math.sin(angle).toFloat()
                
                val beacon = Beacon(
                    macAddress = "AA:BB:CC:DD:EE:${i.toString().padStart(2, '0')}",
                    x = x,
                    y = y,
                    txPower = -70,
                    name = "Beacon $i"
                )
                
                mockBleScanner.addMockBeacon(beacon)
            }
            
            // Simulate user at center
            mockBleScanner.simulateScanCycle(10f, 10f)
            
            // Get discovered beacons
            val beacons = runBlocking { mockBleScanner.discoveredBeaconsFlow.first() }
            
            // Measure triangulation time
            val triangulationTime = measureTimeMillis {
                repeat(10) {
                    runBlocking {
                        triangulatePositionUseCase(
                            TriangulatePositionUseCase.Params(beacons)
                        )
                    }
                }
            } / 10.0
            
            // Measure least squares time
            val leastSquaresTime = measureTimeMillis {
                repeat(10) {
                    runBlocking {
                        leastSquaresPositionUseCase(
                            LeastSquaresPositionUseCase.Params(beacons)
                        )
                    }
                }
            } / 10.0
            
            println("Beacon count: $count")
            println("  Triangulation time: $triangulationTime ms")
            println("  Least Squares time: $leastSquaresTime ms")
            
            // Verify that performance scales reasonably with beacon count
            assertTrue("Triangulation scales poorly with beacon count",
                triangulationTime < 10.0 * count)
            assertTrue("Least Squares scales poorly with beacon count",
                leastSquaresTime < 20.0 * count)
        }
    }
    
    @Test
    fun testDistanceCalculationPerformance() {
        // Create a test beacon
        val beacon = Beacon(
            macAddress = "AA:BB:CC:DD:EE:FF",
            x = 10f,
            y = 20f,
            txPower = -70,
            name = "Test Beacon"
        )
        
        // Update RSSI
        beacon.updateRssi(-80, System.currentTimeMillis())
        
        // Create distance calculation parameters
        val params = CalculateDistanceUseCase.Params(
            beacon = beacon,
            environmentalFactor = 2.0f,
            useFilteredRssi = true
        )
        
        // Measure execution time for 1000 distance calculations
        val executionTime = measureTimeMillis {
            repeat(1000) {
                beacon.calculateDistance(params.environmentalFactor, params.useFilteredRssi)
            }
        }
        
        // Calculate average execution time per calculation
        val avgExecutionTime = executionTime / 1000.0
        
        println("Distance calculation average execution time: $avgExecutionTime ms")
        
        // Verify that distance calculation is very fast
        // This should be a very lightweight operation
        assertTrue("Distance calculation is too slow", avgExecutionTime < 0.1)
    }
    
    @Test
    fun testPositioningAccuracyVsPerformance() {
        // Generate a grid of beacons
        mockBleScanner.clearMockBeacons()
        mockBleScanner.generateGridBeacons(
            rows = 4,
            cols = 4,
            spacingX = 10f,
            spacingY = 10f,
            rssiAtOneMeter = -70,
            environmentalFactor = 2.0f,
            rssiNoise = 0.2f  // Add some noise for realism
        )
        
        // Test positions
        val testPositions = listOf(
            Pair(5f, 5f),      // Near corner
            Pair(15f, 15f),    // Center
            Pair(25f, 5f),     // Near edge
            Pair(12.5f, 17.5f) // Between beacons
        )
        
        for ((userX, userY) in testPositions) {
            // Simulate user at test position
            mockBleScanner.simulateScanCycle(userX, userY)
            
            // Get discovered beacons
            val beacons = runBlocking { mockBleScanner.discoveredBeaconsFlow.first() }
            
            // Measure triangulation time and accuracy
            var triangulationPosition = runBlocking {
                val startTime = System.nanoTime()
                val result = triangulatePositionUseCase(
                    TriangulatePositionUseCase.Params(beacons)
                )
                val endTime = System.nanoTime()
                val executionTime = (endTime - startTime) / 1_000_000.0 // Convert to ms
                
                println("Triangulation at ($userX, $userY):")
                println("  Time: $executionTime ms")
                println("  Position: (${result.position.x}, ${result.position.y})")
                println("  Error: ${calculatePositionError(userX, userY, result.position.x, result.position.y)} m")
                println("  Confidence: ${result.confidence}")
                
                result.position
            }
            
            // Measure least squares time and accuracy
            var leastSquaresPosition = runBlocking {
                val startTime = System.nanoTime()
                val result = leastSquaresPositionUseCase(
                    LeastSquaresPositionUseCase.Params(beacons)
                )
                val endTime = System.nanoTime()
                val executionTime = (endTime - startTime) / 1_000_000.0 // Convert to ms
                
                println("Least Squares at ($userX, $userY):")
                println("  Time: $executionTime ms")
                println("  Position: (${result.position.x}, ${result.position.y})")
                println("  Error: ${calculatePositionError(userX, userY, result.position.x, result.position.y)} m")
                println("  Confidence: ${result.confidence}")
                
                result.position
            }
            
            // Verify that both algorithms produce reasonable results
            val triangulationError = calculatePositionError(
                userX, userY, triangulationPosition.x, triangulationPosition.y
            )
            
            val leastSquaresError = calculatePositionError(
                userX, userY, leastSquaresPosition.x, leastSquaresPosition.y
            )
            
            // With noise, we expect some error, but it should be reasonable
            assertTrue("Triangulation error too large: $triangulationError m",
                triangulationError < 5.0)
            assertTrue("Least Squares error too large: $leastSquaresError m",
                leastSquaresError < 5.0)
        }
    }
    
    /**
     * Calculates the Euclidean distance between two points.
     */
    private fun calculatePositionError(
        actualX: Float, actualY: Float,
        estimatedX: Float, estimatedY: Float
    ): Double {
        val dx = actualX - estimatedX
        val dy = actualY - estimatedY
        return Math.sqrt((dx * dx + dy * dy).toDouble())
    }
}