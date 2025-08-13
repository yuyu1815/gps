package com.example.myapplication.slam

import android.graphics.Bitmap
import com.example.myapplication.domain.model.Motion
import timber.log.Timber
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Implements visual feature tracking algorithm for SLAM.
 * This class detects and tracks visual features across camera frames
 * to estimate motion and build a map of the environment.
 */
class VisualFeatureTracker {

    // Feature detection parameters
    private val maxFeatures = 100
    private val qualityLevel = 0.01
    private val minDistance = 10.0
    
    // Feature tracking parameters
    private val maxPyramidLevel = 3
    private val windowSize = 15
    private val maxIterations = 30
    private val epsilon = 0.01
    
    // Previous frame data
    private var previousFeatures = listOf<FeaturePoint>()
    private var previousGrayImage: ByteArray? = null
    private var previousTimestamp = 0L
    
    // Motion estimation
    private var cumulativeMotion = Motion(0.0, 0.0)
    
    /**
     * Processes a bitmap image to detect and track features.
     * 
     * @param bitmap The bitmap image to process
     * @return List of tracked feature points
     */
    fun processBitmap(bitmap: Bitmap?): List<FeaturePoint> {
        if (bitmap == null) {
            Timber.d("Null bitmap received, skipping frame")
            return emptyList()
        }
        
        // Convert bitmap to grayscale
        val grayImage = convertBitmapToGrayscale(bitmap)
        val currentTimestamp = System.currentTimeMillis()
        
        // If this is the first frame, just detect features
        if (previousGrayImage == null) {
            previousGrayImage = grayImage
            previousTimestamp = currentTimestamp
            previousFeatures = detectFeatures(grayImage, bitmap.width, bitmap.height)
            return previousFeatures
        }
        
        // Track features from previous frame to current frame
        val trackedFeatures = trackFeatures(
            previousGrayImage!!,
            grayImage,
            previousFeatures,
            bitmap.width,
            bitmap.height
        )
        
        // Update motion estimation
        updateMotionEstimation(previousFeatures, trackedFeatures, currentTimestamp - previousTimestamp)
        
        // Update previous frame data
        previousGrayImage = grayImage
        previousTimestamp = currentTimestamp
        
        // Detect new features if needed
        if (trackedFeatures.size < maxFeatures / 2) {
            val newFeatures = detectFeatures(grayImage, bitmap.width, bitmap.height)
            val combinedFeatures = combineFeatures(trackedFeatures, newFeatures)
            previousFeatures = combinedFeatures
            return combinedFeatures
        }
        
        previousFeatures = trackedFeatures
        return trackedFeatures
    }
    
    /**
     * Gets the current estimated motion from visual tracking.
     * 
     * @return Motion object with velocity and angular velocity
     */
    fun getMotion(): Motion {
        return cumulativeMotion
    }
    
    /**
     * Resets the tracker state.
     * Call this when tracking is lost or when starting a new session.
     */
    fun reset() {
        previousFeatures = emptyList()
        previousGrayImage = null
        previousTimestamp = 0L
        cumulativeMotion = Motion(0.0, 0.0)
        Timber.d("Visual feature tracker reset")
    }
    
    /**
     * Detects features in a grayscale image.
     * 
     * @param grayImage Grayscale image data
     * @param width Image width
     * @param height Image height
     * @return List of detected feature points
     */
    private fun detectFeatures(grayImage: ByteArray, width: Int, height: Int): List<FeaturePoint> {
        // This is a simplified mock implementation
        // In a real implementation, this would use OpenCV or a similar library
        
        val features = mutableListOf<FeaturePoint>()
        val timestamp = System.currentTimeMillis()
        
        // Detect features in a grid pattern with some randomness
        val gridSize = sqrt(maxFeatures.toDouble()).toInt()
        val stepX = width / gridSize
        val stepY = height / gridSize
        
        for (i in 0 until gridSize) {
            for (j in 0 until gridSize) {
                // Add some randomness to the grid positions
                val x = (i * stepX + stepX / 2 + (Math.random() * stepX / 2 - stepX / 4)).toFloat()
                val y = (j * stepY + stepY / 2 + (Math.random() * stepY / 2 - stepY / 4)).toFloat()
                
                // Calculate a confidence based on image gradient at this point
                val confidence = calculateConfidence(grayImage, x.toInt(), y.toInt(), width, height)
                
                if (confidence > 0.5f) {  // Only keep strong features
                    features.add(
                        FeaturePoint(
                            x = x / width,  // Normalize to 0-1 range
                            y = y / height,  // Normalize to 0-1 range
                            z = 0f,  // Unknown depth at this point
                            confidence = confidence,
                            id = (i * gridSize + j).toLong(),
                            timestamp = timestamp
                        )
                    )
                }
            }
        }
        
        Timber.d("Detected ${features.size} features")
        return features
    }
    
    /**
     * Tracks features from previous frame to current frame.
     * 
     * @param prevImage Previous grayscale image
     * @param currImage Current grayscale image
     * @param prevFeatures Features from previous frame
     * @param width Image width
     * @param height Image height
     * @return List of tracked feature points
     */
    private fun trackFeatures(
        prevImage: ByteArray,
        currImage: ByteArray,
        prevFeatures: List<FeaturePoint>,
        width: Int,
        height: Int
    ): List<FeaturePoint> {
        // This is a simplified mock implementation
        // In a real implementation, this would use optical flow tracking
        
        val trackedFeatures = mutableListOf<FeaturePoint>()
        val timestamp = System.currentTimeMillis()
        
        for (feature in prevFeatures) {
            // Convert normalized coordinates back to pixel coordinates
            val x = (feature.x * width).toInt()
            val y = (feature.y * height).toInt()
            
            // Simulate feature tracking with small random movement
            // In a real implementation, this would use optical flow
            val newX = (x + (Math.random() * 6 - 3)).toFloat()
            val newY = (y + (Math.random() * 6 - 3)).toFloat()
            
            // Check if the feature is still within the image bounds
            if (newX >= 0 && newX < width && newY >= 0 && newY < height) {
                // Calculate new confidence
                val confidence = calculateConfidence(currImage, newX.toInt(), newY.toInt(), width, height)
                
                // Only keep features with good confidence
                if (confidence > 0.3f) {
                    trackedFeatures.add(
                        FeaturePoint(
                            x = newX / width,  // Normalize to 0-1 range
                            y = newY / height,  // Normalize to 0-1 range
                            z = feature.z,  // Keep the same depth for now
                            confidence = confidence,
                            id = feature.id,  // Keep the same ID for tracking
                            timestamp = timestamp
                        )
                    )
                }
            }
        }
        
        Timber.d("Tracked ${trackedFeatures.size}/${prevFeatures.size} features")
        return trackedFeatures
    }
    
    /**
     * Updates motion estimation based on feature tracking.
     * 
     * @param prevFeatures Features from previous frame
     * @param currFeatures Features from current frame
     * @param deltaTimeMs Time difference in milliseconds
     */
    private fun updateMotionEstimation(
        prevFeatures: List<FeaturePoint>,
        currFeatures: List<FeaturePoint>,
        deltaTimeMs: Long
    ) {
        if (prevFeatures.isEmpty() || currFeatures.isEmpty() || deltaTimeMs <= 0) {
            return
        }
        
        // Match features by ID
        val matchedFeatures = mutableListOf<Pair<FeaturePoint, FeaturePoint>>()
        for (prevFeature in prevFeatures) {
            val matchingFeature = currFeatures.find { it.id == prevFeature.id }
            if (matchingFeature != null) {
                matchedFeatures.add(Pair(prevFeature, matchingFeature))
            }
        }
        
        if (matchedFeatures.isEmpty()) {
            return
        }
        
        // Calculate average motion
        var totalDx = 0.0
        var totalDy = 0.0
        var totalRotation = 0.0
        
        for ((prev, curr) in matchedFeatures) {
            totalDx += (curr.x - prev.x).toDouble()
            totalDy += (curr.y - prev.y).toDouble()
            
            // Estimate rotation from feature movement
            // This is a simplified approach - real implementation would use homography
            val dx = curr.x - prev.x
            val dy = curr.y - prev.y
            val rotation = Math.atan2(dy.toDouble(), dx.toDouble())
            totalRotation += rotation
        }
        
        val avgDx = totalDx / matchedFeatures.size
        val avgDy = totalDy / matchedFeatures.size
        val avgRotation = totalRotation / matchedFeatures.size
        
        // Calculate velocity in units per second
        val velocity = sqrt(avgDx * avgDx + avgDy * avgDy) * 1000 / deltaTimeMs
        val angularVelocity = avgRotation * 1000 / deltaTimeMs
        
        // Update cumulative motion with some smoothing
        cumulativeMotion = Motion(
            velocity = velocity * 0.3 + cumulativeMotion.velocity * 0.7,
            angularVelocity = angularVelocity * 0.3 + cumulativeMotion.angularVelocity * 0.7
        )
        
        Timber.d("Updated motion: v=${cumulativeMotion.velocity}, ω=${cumulativeMotion.angularVelocity}")
    }
    
    /**
     * Combines tracked features with newly detected features.
     * 
     * @param trackedFeatures Features tracked from previous frame
     * @param newFeatures Newly detected features
     * @return Combined list of features
     */
    private fun combineFeatures(
        trackedFeatures: List<FeaturePoint>,
        newFeatures: List<FeaturePoint>
    ): List<FeaturePoint> {
        val trackedIds = trackedFeatures.map { it.id }.toSet()
        val filteredNewFeatures = newFeatures.filter { it.id !in trackedIds }
        
        // Take only as many new features as needed to reach maxFeatures
        val neededFeatures = maxFeatures - trackedFeatures.size
        val selectedNewFeatures = if (filteredNewFeatures.size > neededFeatures) {
            filteredNewFeatures.sortedByDescending { it.confidence }.take(neededFeatures)
        } else {
            filteredNewFeatures
        }
        
        return trackedFeatures + selectedNewFeatures
    }
    
    /**
     * Converts a bitmap to grayscale.
     * 
     * @param bitmap The input bitmap
     * @return Grayscale image as byte array
     */
    private fun convertBitmapToGrayscale(bitmap: Bitmap): ByteArray {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        val grayImage = ByteArray(width * height)
        
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        
        for (i in 0 until width * height) {
            val pixel = pixels[i]
            val r = (pixel shr 16) and 0xff
            val g = (pixel shr 8) and 0xff
            val b = pixel and 0xff
            
            // Convert RGB to grayscale using standard weights
            val gray = (0.299 * r + 0.587 * g + 0.114 * b).toInt()
            grayImage[i] = gray.toByte()
        }
        
        return grayImage
    }
    
    /**
     * Calculates confidence for a feature point based on image gradients.
     * 
     * @param grayImage Grayscale image data
     * @param x X coordinate
     * @param y Y coordinate
     * @param width Image width
     * @param height Image height
     * @return Confidence value (0.0-1.0)
     */
    private fun calculateConfidence(
        grayImage: ByteArray,
        x: Int,
        y: Int,
        width: Int,
        height: Int
    ): Float {
        // Check bounds
        if (x < 1 || x >= width - 1 || y < 1 || y >= height - 1) {
            return 0.0f
        }
        
        // Calculate image gradients
        val center = grayImage[y * width + x].toInt() and 0xFF
        val left = grayImage[y * width + (x - 1)].toInt() and 0xFF
        val right = grayImage[y * width + (x + 1)].toInt() and 0xFF
        val top = grayImage[(y - 1) * width + x].toInt() and 0xFF
        val bottom = grayImage[(y + 1) * width + x].toInt() and 0xFF
        
        // Calculate gradient magnitudes
        val dx = abs(right - left)
        val dy = abs(bottom - top)
        
        // Calculate gradient magnitude
        val gradientMagnitude = sqrt((dx * dx + dy * dy).toDouble()).toFloat()
        
        // Normalize to 0-1 range (assuming max gradient is 255*sqrt(2))
        return (gradientMagnitude / (255.0f * sqrt(2.0f))).coerceIn(0.0f, 1.0f)
    }
}