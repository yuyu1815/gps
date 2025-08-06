package com.example.myapplication.domain.usecase.position

import com.example.myapplication.domain.model.Beacon
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import kotlin.math.abs

class TriangulatePositionUseCaseTest {

    private lateinit var triangulatePositionUseCase: TriangulatePositionUseCase
    private lateinit var leastSquaresPositionUseCase: LeastSquaresPositionUseCase
    
    @Before
    fun setUp() {
        triangulatePositionUseCase = TriangulatePositionUseCase()
        leastSquaresPositionUseCase = LeastSquaresPositionUseCase()
    }
    
    @Test
    fun testTriangulationWithNoBeacons() {
        runBlocking {
            // Given
            val beacons = emptyList<Beacon>()
            
            // When
            val result = triangulatePositionUseCase(TriangulatePositionUseCase.Params(beacons))
            
            // Then
            assertEquals(0f, result.position.x)
            assertEquals(0f, result.position.y)
            assertEquals(0f, result.confidence)
            assertEquals(0, result.beaconsUsed)
        }
    }
    
    @Test
    fun testTriangulationWithInvalidBeacons() {
        runBlocking {
            // Given - beacons with zero distance
            val beacons = listOf(
                Beacon(
                    macAddress = "AA:BB:CC:DD:EE:FF",
                    x = 10f,
                    y = 10f,
                    txPower = -70,
                    name = "Beacon 1",
                    estimatedDistance = 0f
                ),
                Beacon(
                    macAddress = "11:22:33:44:55:66",
                    x = 20f,
                    y = 10f,
                    txPower = -70,
                    name = "Beacon 2",
                    estimatedDistance = -1f
                )
            )
            
            // When
            val result = triangulatePositionUseCase(TriangulatePositionUseCase.Params(beacons))
            
            // Then
            assertEquals(0f, result.position.x)
            assertEquals(0f, result.position.y)
            assertEquals(0f, result.confidence)
            assertEquals(0, result.beaconsUsed)
        }
    }
    
    @Test
    fun testTriangulationWithThreeBeacons() {
        runBlocking {
            // Given - three beacons in a triangle formation
            // Beacon 1: (0,0) with distance 5
            // Beacon 2: (10,0) with distance 5
            // Beacon 3: (5,10) with distance 5
            // Expected position: (5,5)
            val beacons = listOf(
                Beacon(
                    macAddress = "AA:BB:CC:DD:EE:FF",
                    x = 0f,
                    y = 0f,
                    txPower = -70,
                    name = "Beacon 1",
                    estimatedDistance = 5f,
                    distanceConfidence = 0.8f
                ),
                Beacon(
                    macAddress = "11:22:33:44:55:66",
                    x = 10f,
                    y = 0f,
                    txPower = -70,
                    name = "Beacon 2",
                    estimatedDistance = 5f,
                    distanceConfidence = 0.8f
                ),
                Beacon(
                    macAddress = "AA:22:CC:44:EE:66",
                    x = 5f,
                    y = 10f,
                    txPower = -70,
                    name = "Beacon 3",
                    estimatedDistance = 5f,
                    distanceConfidence = 0.8f
                )
            )
            
            // When
            val result = triangulatePositionUseCase(TriangulatePositionUseCase.Params(beacons))
            
            // Then
            // Since we're using weighted centroid, the result should be close to (5,5)
            assertTrue(abs(5f - result.position.x) < 0.5f)
            assertTrue(abs(5f - result.position.y) < 0.5f)
            assertTrue(result.confidence > 0f)
            assertEquals(3, result.beaconsUsed)
        }
    }
    
    @Test
    fun testTriangulationWithDifferentConfidences() {
        runBlocking {
            // Given - three beacons with different confidence levels
            val beacons = listOf(
                Beacon(
                    macAddress = "AA:BB:CC:DD:EE:FF",
                    x = 0f,
                    y = 0f,
                    txPower = -70,
                    name = "Beacon 1",
                    estimatedDistance = 5f,
                    distanceConfidence = 0.9f  // High confidence
                ),
                Beacon(
                    macAddress = "11:22:33:44:55:66",
                    x = 10f,
                    y = 0f,
                    txPower = -70,
                    name = "Beacon 2",
                    estimatedDistance = 5f,
                    distanceConfidence = 0.5f  // Medium confidence
                ),
                Beacon(
                    macAddress = "AA:22:CC:44:EE:66",
                    x = 5f,
                    y = 10f,
                    txPower = -70,
                    name = "Beacon 3",
                    estimatedDistance = 5f,
                    distanceConfidence = 0.2f  // Low confidence
                )
            )
            
            // When
            val result = triangulatePositionUseCase(TriangulatePositionUseCase.Params(beacons))
            
            // Then
            // Result should be biased towards the high confidence beacon (0,0)
            assertTrue(result.position.x < 5f)
            assertTrue(result.position.y < 5f)
            assertTrue(result.confidence > 0f)
            assertEquals(3, result.beaconsUsed)
        }
    }
    
    @Test
    fun testTriangulationWithDifferentDistances() {
        runBlocking {
            // Given - three beacons with different distances
            val beacons = listOf(
                Beacon(
                    macAddress = "AA:BB:CC:DD:EE:FF",
                    x = 0f,
                    y = 0f,
                    txPower = -70,
                    name = "Beacon 1",
                    estimatedDistance = 2f,  // Closer
                    distanceConfidence = 0.8f
                ),
                Beacon(
                    macAddress = "11:22:33:44:55:66",
                    x = 10f,
                    y = 0f,
                    txPower = -70,
                    name = "Beacon 2",
                    estimatedDistance = 5f,  // Medium
                    distanceConfidence = 0.8f
                ),
                Beacon(
                    macAddress = "AA:22:CC:44:EE:66",
                    x = 5f,
                    y = 10f,
                    txPower = -70,
                    name = "Beacon 3",
                    estimatedDistance = 8f,  // Farther
                    distanceConfidence = 0.8f
                )
            )
            
            // When
            val result = triangulatePositionUseCase(TriangulatePositionUseCase.Params(beacons))
            
            // Then
            // Result should be biased towards the closer beacon (0,0)
            assertTrue(result.position.x < 5f)
            assertTrue(result.position.y < 5f)
            assertTrue(result.confidence > 0f)
            assertEquals(3, result.beaconsUsed)
        }
    }
    
    @Test
    fun testLeastSquaresWithThreeBeacons() {
        runBlocking {
            // Given - three beacons in a triangle formation with perfect distances
            // Beacon 1: (0,0) with distance 7.07 to point (5,5)
            // Beacon 2: (10,0) with distance 7.07 to point (5,5)
            // Beacon 3: (5,10) with distance 5 to point (5,5)
            val beacons = listOf(
                Beacon(
                    macAddress = "AA:BB:CC:DD:EE:FF",
                    x = 0f,
                    y = 0f,
                    txPower = -70,
                    name = "Beacon 1",
                    estimatedDistance = 7.07f,
                    distanceConfidence = 0.8f
                ),
                Beacon(
                    macAddress = "11:22:33:44:55:66",
                    x = 10f,
                    y = 0f,
                    txPower = -70,
                    name = "Beacon 2",
                    estimatedDistance = 7.07f,
                    distanceConfidence = 0.8f
                ),
                Beacon(
                    macAddress = "AA:22:CC:44:EE:66",
                    x = 5f,
                    y = 10f,
                    txPower = -70,
                    name = "Beacon 3",
                    estimatedDistance = 5f,
                    distanceConfidence = 0.8f
                )
            )
            
            // When
            val result = leastSquaresPositionUseCase(LeastSquaresPositionUseCase.Params(beacons))
            
            // Then
            // The result should be close to (5,5)
            assertTrue(abs(5f - result.position.x) < 0.5f)
            assertTrue(abs(5f - result.position.y) < 0.5f)
            assertTrue(result.confidence > 0f)
            assertEquals(3, result.beaconsUsed)
            assertTrue(result.averageError < 1.0f)  // Should have low error
        }
    }
    
    @Test
    fun testLeastSquaresWithNoiseInDistances() {
        runBlocking {
            // Given - three beacons with some noise in the distances
            val beacons = listOf(
                Beacon(
                    macAddress = "AA:BB:CC:DD:EE:FF",
                    x = 0f,
                    y = 0f,
                    txPower = -70,
                    name = "Beacon 1",
                    estimatedDistance = 8f,  // True distance to (5,5) is 7.07
                    distanceConfidence = 0.8f
                ),
                Beacon(
                    macAddress = "11:22:33:44:55:66",
                    x = 10f,
                    y = 0f,
                    txPower = -70,
                    name = "Beacon 2",
                    estimatedDistance = 6f,  // True distance to (5,5) is 7.07
                    distanceConfidence = 0.8f
                ),
                Beacon(
                    macAddress = "AA:22:CC:44:EE:66",
                    x = 5f,
                    y = 10f,
                    txPower = -70,
                    name = "Beacon 3",
                    estimatedDistance = 5.5f,  // True distance to (5,5) is 5
                    distanceConfidence = 0.8f
                )
            )
            
            // When
            val result = leastSquaresPositionUseCase(LeastSquaresPositionUseCase.Params(beacons))
            
            // Then
            // The result should still be somewhat close to (5,5)
            assertTrue(abs(5f - result.position.x) < 2.0f)
            assertTrue(abs(5f - result.position.y) < 2.0f)
            assertTrue(result.confidence > 0f)
            assertEquals(3, result.beaconsUsed)
        }
    }
    
    @Test
    fun testLeastSquaresWithInsufficientBeacons() {
        runBlocking {
            // Given - only two beacons (insufficient for least squares)
            val beacons = listOf(
                Beacon(
                    macAddress = "AA:BB:CC:DD:EE:FF",
                    x = 0f,
                    y = 0f,
                    txPower = -70,
                    name = "Beacon 1",
                    estimatedDistance = 7.07f,
                    distanceConfidence = 0.8f
                ),
                Beacon(
                    macAddress = "11:22:33:44:55:66",
                    x = 10f,
                    y = 0f,
                    txPower = -70,
                    name = "Beacon 2",
                    estimatedDistance = 7.07f,
                    distanceConfidence = 0.8f
                )
            )
            
            // When
            val result = leastSquaresPositionUseCase(LeastSquaresPositionUseCase.Params(beacons))
            
            // Then
            // Should fall back to triangulation
            assertTrue(result.confidence > 0f)
            assertEquals(2, result.beaconsUsed)
            assertEquals(Float.MAX_VALUE, result.averageError)
        }
    }
}