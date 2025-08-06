package com.example.myapplication.domain.usecase.position

import com.example.myapplication.domain.model.Beacon
import com.example.myapplication.domain.model.UserPosition
import com.example.myapplication.domain.usecase.UseCase
import timber.log.Timber
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Use case for triangulating the user's position using multiple beacons.
 * 
 * This implementation uses a weighted centroid approach, where each beacon's position
 * is weighted by the inverse of its estimated distance (closer beacons have more influence).
 * 
 * For more accurate positioning, at least 3 beacons should be used.
 */
class TriangulatePositionUseCase : UseCase<TriangulatePositionUseCase.Params, TriangulatePositionUseCase.Result> {
    
    /**
     * Efficient data structure for beacon weighting in triangulation.
     * Stores beacon and weight information to avoid redundant calculations.
     */
    private class WeightedBeacon(
        val beacon: Beacon,
        var weight: Float = 0f,
        var normalizedWeight: Float = 0f
    )
    
    override suspend fun invoke(params: Params): Result {
        Timber.d("Triangulating position using ${params.beacons.size} beacons")
        
        if (params.beacons.isEmpty()) {
            Timber.w("No beacons provided for triangulation")
            return Result(
                position = UserPosition(0f, 0f, 0f),
                confidence = 0f,
                beaconsUsed = 0
            )
        }
        
        // Use ArrayList instead of filter for better performance
        val validBeacons = ArrayList<Beacon>(params.beacons.size)
        for (beacon in params.beacons) {
            if (beacon.estimatedDistance > 0) {
                validBeacons.add(beacon)
            }
        }
        
        if (validBeacons.isEmpty()) {
            Timber.w("No valid beacons with positive distances")
            return Result(
                position = UserPosition(0f, 0f, 0f),
                confidence = 0f,
                beaconsUsed = 0
            )
        }
        
        // Sort beacons by confidence and take the top MAX_BEACONS
        // Use efficient in-place sorting
        validBeacons.sortWith(compareByDescending { it.distanceConfidence })
        
        // Take only the top MAX_BEACONS
        val beaconCount = minOf(validBeacons.size, MAX_BEACONS)
        
        Timber.d("Using $beaconCount beacons for triangulation")
        
        // Use a single array for all beacon calculations to reduce allocations
        val weightedBeacons = Array(beaconCount) { index ->
            WeightedBeacon(validBeacons[index])
        }
        
        // Calculate weights based on distance and confidence
        var totalWeight = 0f
        for (i in 0 until beaconCount) {
            val weightedBeacon = weightedBeacons[i]
            val beacon = weightedBeacon.beacon
            
            val distanceWeight = 1.0f / beacon.estimatedDistance
            val confidenceWeight = beacon.distanceConfidence
            weightedBeacon.weight = distanceWeight * confidenceWeight
            totalWeight += weightedBeacon.weight
        }
        
        // Normalize weights to sum to 1.0
        if (totalWeight > 0) {
            for (i in 0 until beaconCount) {
                weightedBeacons[i].normalizedWeight = weightedBeacons[i].weight / totalWeight
            }
        } else {
            // If total weight is 0, use equal weights
            val equalWeight = 1.0f / beaconCount
            for (i in 0 until beaconCount) {
                weightedBeacons[i].normalizedWeight = equalWeight
            }
        }
        
        // Calculate weighted centroid
        var x = 0f
        var y = 0f
        
        for (i in 0 until beaconCount) {
            val weightedBeacon = weightedBeacons[i]
            val beacon = weightedBeacon.beacon
            val weight = weightedBeacon.normalizedWeight
            
            x += beacon.x * weight
            y += beacon.y * weight
        }
        
        // Calculate position confidence based on:
        // 1. Number of beacons used (more is better)
        // 2. Average beacon confidence
        // 3. Geometric dilution of precision (GDOP)
        val beaconCountFactor = (beaconCount.toFloat() / MAX_BEACONS).coerceAtMost(1.0f)
        
        // Calculate average beacon confidence
        var totalConfidence = 0f
        for (i in 0 until beaconCount) {
            totalConfidence += weightedBeacons[i].beacon.distanceConfidence
        }
        val avgBeaconConfidence = if (beaconCount > 0) totalConfidence / beaconCount else 0f
        
        // Extract beacons for GDOP calculation
        val beaconsForGdop = Array(beaconCount) { weightedBeacons[it].beacon }
        val gdop = calculateGDOP(beaconsForGdop.toList(), x, y)
        val gdopFactor = (1.0f / (1.0f + gdop)).coerceIn(0.0f, 1.0f)
        
        // Combine factors with different weights
        val positionConfidence = (0.3f * beaconCountFactor) + 
                                (0.4f * avgBeaconConfidence) + 
                                (0.3f * gdopFactor)
        
        Timber.d("Triangulated position: ($x, $y) with confidence $positionConfidence")
        
        return Result(
            position = UserPosition(x, y, positionConfidence),
            confidence = positionConfidence,
            beaconsUsed = beaconCount
        )
    }
    
    /**
     * Calculates the Geometric Dilution of Precision (GDOP) for the given beacons and position.
     * 
     * GDOP is a measure of the geometric quality of the beacon configuration.
     * Lower values indicate better geometric configurations.
     * 
     * @param beacons List of beacons
     * @param x X coordinate of the position
     * @param y Y coordinate of the position
     * @return GDOP value (higher means worse precision)
     */
    private fun calculateGDOP(beacons: List<Beacon>, x: Float, y: Float): Float {
        if (beacons.size < 2) return Float.MAX_VALUE
        
        // Calculate angles from the position to each beacon
        val angles = beacons.map { beacon ->
            val dx = beacon.x - x
            val dy = beacon.y - y
            kotlin.math.atan2(dy.toDouble(), dx.toDouble())
        }
        
        // Calculate the average angle separation
        var totalSeparation = 0.0
        var count = 0
        
        for (i in 0 until angles.size - 1) {
            for (j in i + 1 until angles.size) {
                val separation = kotlin.math.abs(angles[i] - angles[j]) % (2 * kotlin.math.PI)
                val normalizedSeparation = kotlin.math.min(separation, 2 * kotlin.math.PI - separation)
                totalSeparation += normalizedSeparation
                count++
            }
        }
        
        // Ideal separation is 120 degrees (2*PI/3) for 3 beacons
        // or evenly distributed for more beacons
        val avgSeparation = if (count > 0) totalSeparation / count else 0.0
        val idealSeparation = kotlin.math.PI / beacons.size.coerceAtLeast(2)
        
        // Calculate GDOP as the deviation from ideal separation
        // Normalize to 0-10 range where 0 is perfect and 10 is worst
        val gdop = 10.0 * kotlin.math.abs(avgSeparation - idealSeparation) / idealSeparation
        
        return gdop.toFloat()
    }
    
    /**
     * Parameters for the TriangulatePositionUseCase.
     *
     * @param beacons List of beacons with their positions and estimated distances
     */
    data class Params(
        val beacons: List<Beacon>
    )
    
    /**
     * Result of the triangulation.
     *
     * @param position The triangulated user position
     * @param confidence Confidence level of the position estimation (0.0 to 1.0)
     * @param beaconsUsed Number of beacons used in the triangulation
     */
    data class Result(
        val position: UserPosition,
        val confidence: Float,
        val beaconsUsed: Int
    )
    
    companion object {
        /**
         * Maximum number of beacons to use for triangulation.
         * Using too many beacons can introduce more noise.
         */
        const val MAX_BEACONS = 5
    }
}

/**
 * Use case for triangulating the user's position using the least squares method.
 * 
 * This implementation uses a more sophisticated approach than the weighted centroid method.
 * It minimizes the sum of squared errors between the measured distances and the calculated distances.
 * 
 * For accurate positioning, at least 3 beacons should be used.
 */
class LeastSquaresPositionUseCase : UseCase<LeastSquaresPositionUseCase.Params, LeastSquaresPositionUseCase.Result> {
    
    override suspend fun invoke(params: Params): Result {
        Timber.d("Calculating position using least squares method with ${params.beacons.size} beacons")
        
        if (params.beacons.size < 3) {
            Timber.w("Not enough beacons for least squares method (minimum 3, got ${params.beacons.size})")
            
            // Fall back to triangulation for fewer beacons
            val triangulationResult = TriangulatePositionUseCase().invoke(
                TriangulatePositionUseCase.Params(params.beacons)
            )
            
            return Result(
                position = triangulationResult.position,
                confidence = triangulationResult.confidence,
                beaconsUsed = triangulationResult.beaconsUsed,
                averageError = Float.MAX_VALUE
            )
        }
        
        // Filter out beacons with zero or negative distances
        val validBeacons = params.beacons.filter { it.estimatedDistance > 0 }
        
        if (validBeacons.size < 3) {
            Timber.w("Not enough valid beacons with positive distances for least squares")
            
            // Fall back to triangulation
            val triangulationResult = TriangulatePositionUseCase().invoke(
                TriangulatePositionUseCase.Params(validBeacons)
            )
            
            return Result(
                position = triangulationResult.position,
                confidence = triangulationResult.confidence,
                beaconsUsed = triangulationResult.beaconsUsed,
                averageError = Float.MAX_VALUE
            )
        }
        
        // Sort beacons by confidence and take the top MAX_BEACONS
        val selectedBeacons = validBeacons
            .sortedByDescending { it.distanceConfidence }
            .take(MAX_BEACONS)
        
        Timber.d("Using ${selectedBeacons.size} beacons for least squares calculation")
        
        // Start with an initial position estimate using weighted centroid
        val triangulationResult = TriangulatePositionUseCase().invoke(
            TriangulatePositionUseCase.Params(selectedBeacons)
        )
        
        var currentX = triangulationResult.position.x
        var currentY = triangulationResult.position.y
        
        // Iteratively refine the position estimate
        var bestError = Float.MAX_VALUE
        var bestX = currentX
        var bestY = currentY
        
        for (iteration in 0 until MAX_ITERATIONS) {
            // Calculate the gradient of the error function
            val (gradX, gradY, error) = calculateErrorGradient(selectedBeacons, currentX, currentY)
            
            // If this is a better solution, save it
            if (error < bestError) {
                bestError = error
                bestX = currentX
                bestY = currentY
            }
            
            // Check if we've converged
            if (error < ERROR_THRESHOLD) {
                Timber.d("Least squares converged after $iteration iterations with error $error")
                break
            }
            
            // Update the position estimate using gradient descent
            currentX -= LEARNING_RATE * gradX
            currentY -= LEARNING_RATE * gradY
            
            Timber.d("Iteration $iteration: position=($currentX, $currentY), error=$error")
        }
        
        // Calculate position confidence based on:
        // 1. Number of beacons used (more is better)
        // 2. Average beacon confidence
        // 3. Average error (lower is better)
        // 4. Geometric dilution of precision (GDOP)
        val beaconCountFactor = (selectedBeacons.size.toFloat() / MAX_BEACONS).coerceAtMost(1.0f)
        val avgBeaconConfidence = selectedBeacons.map { it.distanceConfidence }.average().toFloat()
        val errorFactor = (1.0f / (1.0f + bestError)).coerceIn(0.0f, 1.0f)
        val gdop = calculateGDOP(selectedBeacons, bestX, bestY)
        val gdopFactor = (1.0f / (1.0f + gdop)).coerceIn(0.0f, 1.0f)
        
        // Combine factors with different weights
        val positionConfidence = (0.2f * beaconCountFactor) + 
                                (0.3f * avgBeaconConfidence) + 
                                (0.3f * errorFactor) +
                                (0.2f * gdopFactor)
        
        Timber.d("Least squares position: ($bestX, $bestY) with confidence $positionConfidence and error $bestError")
        
        return Result(
            position = UserPosition(bestX, bestY, positionConfidence),
            confidence = positionConfidence,
            beaconsUsed = selectedBeacons.size,
            averageError = bestError
        )
    }
    
    /**
     * Efficient data structure for beacon position calculation.
     * Stores precomputed values to avoid redundant calculations.
     */
    private class BeaconCalculationCache(
        val beacon: Beacon,
        var dx: Float = 0f,
        var dy: Float = 0f,
        var calculatedDistance: Float = 0f,
        var error: Float = 0f,
        var weight: Float = 0f
    )
    
    /**
     * Calculates the gradient of the error function for the given position.
     * 
     * The error function is the sum of squared differences between the measured distances
     * and the calculated distances from the position to each beacon.
     * 
     * Uses an efficient data structure to avoid redundant calculations and reduce memory allocations.
     * 
     * @param beacons List of beacons
     * @param x X coordinate of the position
     * @param y Y coordinate of the position
     * @return Triple of (gradient X, gradient Y, total error)
     */
    private fun calculateErrorGradient(beacons: List<Beacon>, x: Float, y: Float): Triple<Float, Float, Float> {
        var gradX = 0f
        var gradY = 0f
        var totalError = 0f
        
        // Reuse the same array for calculations to avoid allocations
        val calculationCache = Array(beacons.size) { index ->
            BeaconCalculationCache(beacons[index])
        }
        
        // Perform calculations in a single pass
        for (i in beacons.indices) {
            val cache = calculationCache[i]
            val beacon = cache.beacon
            
            cache.dx = x - beacon.x
            cache.dy = y - beacon.y
            cache.calculatedDistance = sqrt(cache.dx.pow(2) + cache.dy.pow(2))
            
            // Skip if calculated distance is too small to avoid division by zero
            if (cache.calculatedDistance < 0.1f) continue
            
            cache.error = cache.calculatedDistance - beacon.estimatedDistance
            cache.weight = beacon.distanceConfidence
            
            totalError += cache.error.pow(2)
            
            // Calculate gradient components
            gradX += 2 * cache.error * cache.dx / cache.calculatedDistance * cache.weight
            gradY += 2 * cache.error * cache.dy / cache.calculatedDistance * cache.weight
        }
        
        // Average error
        val avgError = if (beacons.isNotEmpty()) totalError / beacons.size else 0f
        
        return Triple(gradX, gradY, avgError)
    }
    
    /**
     * Efficient data structure for GDOP calculation.
     * Stores unit vectors to avoid redundant calculations.
     */
    private class GdopVector(
        var ux: Float = 0f,
        var uy: Float = 0f
    )
    
    /**
     * Calculates the Geometric Dilution of Precision (GDOP) for the given beacons and position.
     * 
     * Uses an efficient data structure to avoid redundant calculations and reduce memory allocations.
     * 
     * @param beacons List of beacons
     * @param x X coordinate of the position
     * @param y Y coordinate of the position
     * @return GDOP value (higher means worse precision)
     */
    private fun calculateGDOP(beacons: List<Beacon>, x: Float, y: Float): Float {
        if (beacons.size < 2) return Float.MAX_VALUE
        
        // Reuse the same array for calculations to avoid allocations
        val unitVectors = Array(beacons.size) { GdopVector() }
        
        // Calculate unit vectors from the position to each beacon
        for (i in beacons.indices) {
            val beacon = beacons[i]
            val vector = unitVectors[i]
            
            val dx = beacon.x - x
            val dy = beacon.y - y
            val distance = sqrt(dx.pow(2) + dy.pow(2))
            
            if (distance < 0.1f) {
                vector.ux = 0f
                vector.uy = 0f
            } else {
                vector.ux = dx / distance
                vector.uy = dy / distance
            }
        }
        
        // Calculate the geometry matrix G
        var gxx = 0f
        var gxy = 0f
        var gyy = 0f
        
        for (i in unitVectors.indices) {
            val vector = unitVectors[i]
            gxx += vector.ux * vector.ux
            gxy += vector.ux * vector.uy
            gyy += vector.uy * vector.uy
        }
        
        // Calculate the determinant of G^T * G
        val det = gxx * gyy - gxy * gxy
        
        // GDOP is proportional to the square root of the trace of (G^T * G)^-1
        val gdop = if (det > 0.001f) {
            sqrt((gxx + gyy) / det)
        } else {
            Float.MAX_VALUE // Singular matrix, very poor geometry
        }
        
        return gdop
    }
    
    /**
     * Parameters for the LeastSquaresPositionUseCase.
     *
     * @param beacons List of beacons with their positions and estimated distances
     */
    data class Params(
        val beacons: List<Beacon>
    )
    
    /**
     * Result of the least squares position calculation.
     *
     * @param position The calculated user position
     * @param confidence Confidence level of the position estimation (0.0 to 1.0)
     * @param beaconsUsed Number of beacons used in the calculation
     * @param averageError Average error between measured and calculated distances
     */
    data class Result(
        val position: UserPosition,
        val confidence: Float,
        val beaconsUsed: Int,
        val averageError: Float
    )
    
    companion object {
        /**
         * Maximum number of beacons to use for calculation.
         */
        const val MAX_BEACONS = 8
        
        /**
         * Maximum number of iterations for the least squares algorithm.
         */
        const val MAX_ITERATIONS = 20
        
        /**
         * Learning rate for gradient descent.
         */
        const val LEARNING_RATE = 0.5f
        
        /**
         * Error threshold for convergence.
         */
        const val ERROR_THRESHOLD = 0.1f
    }
}