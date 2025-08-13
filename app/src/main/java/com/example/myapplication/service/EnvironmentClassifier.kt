package com.example.myapplication.service

import com.example.myapplication.domain.model.SensorData
import com.example.myapplication.domain.model.Vector3D
import com.example.myapplication.domain.model.WifiAccessPoint

/**
 * Service for classifying indoor environments to optimize positioning algorithms.
 * 
 * This classifier analyzes sensor data, Wi-Fi signals, and environmental characteristics
 * to determine the type of environment the user is in, allowing the positioning system
 * to adapt its parameters accordingly.
 */
class EnvironmentClassifier {

    /**
     * Possible environment types that can be detected.
     */
    enum class EnvironmentType {
        OFFICE,              // Office spaces with cubicles, meeting rooms
        RETAIL,              // Retail spaces like shopping malls, stores
        WAREHOUSE,           // Large open spaces with high ceilings, metal shelving
        CORRIDOR,            // Narrow hallways with limited features
        STAIRWELL,           // Staircases with potential magnetic interference
        OPEN_AREA,           // Large open areas with few obstacles
        HIGH_INTERFERENCE,   // Areas with significant magnetic or RF interference
        UNKNOWN              // Default when environment cannot be classified
    }

    /**
     * Data class containing environment classification results.
     */
    data class ClassificationResult(
        val environmentType: EnvironmentType,
        val confidence: Float,
        val characteristics: Map<String, Float>
    )

    /**
     * Classifies the current environment based on Wi-Fi signals.
     *
     * @param accessPoints List of detected Wi-Fi access points
     * @return Classification result with environment type and confidence
     */
    fun classifyFromWifi(accessPoints: List<WifiAccessPoint>): ClassificationResult {
        // Characteristics to analyze
        val signalDensity = accessPoints.size.toFloat()
        val signalStrengthVariation = calculateSignalVariation(accessPoints)
        val signalDistribution = analyzeSignalDistribution(accessPoints)
        
        // Analyze signal patterns to determine environment type
        val (type, confidence) = when {
            // High density of APs with moderate signal variation suggests office environment
            signalDensity > 10 && signalStrengthVariation in 5f..15f -> 
                EnvironmentType.OFFICE to 0.8f
                
            // Very high density with high variation suggests retail environment
            signalDensity > 15 && signalStrengthVariation > 15f -> 
                EnvironmentType.RETAIL to 0.75f
                
            // Low density with low variation suggests warehouse or open area
            signalDensity < 5 && signalStrengthVariation < 10f -> {
                if (signalDistribution < 0.3f) {
                    EnvironmentType.WAREHOUSE to 0.7f
                } else {
                    EnvironmentType.OPEN_AREA to 0.65f
                }
            }
            
            // Linear distribution of APs suggests corridor
            signalDistribution > 0.7f && accessPoints.size >= 2 -> 
                EnvironmentType.CORRIDOR to 0.85f
                
            // Default case when patterns don't match known environments
            else -> EnvironmentType.UNKNOWN to 0.5f
        }
        
        return ClassificationResult(
            environmentType = type,
            confidence = confidence,
            characteristics = mapOf(
                "signalDensity" to signalDensity,
                "signalStrengthVariation" to signalStrengthVariation,
                "signalDistribution" to signalDistribution
            )
        )
    }

    /**
     * Classifies the current environment based on sensor data.
     *
     * @param sensorData Recent sensor readings
     * @return Classification result with environment type and confidence
     */
    fun classifyFromSensors(sensorData: List<SensorData.Combined>): ClassificationResult {
        if (sensorData.isEmpty()) {
            return ClassificationResult(
                EnvironmentType.UNKNOWN,
                0.0f,
                emptyMap()
            )
        }
        
        // Extract relevant sensor characteristics
        val magneticVariation = calculateMagneticVariation(sensorData)
        val accelerationStability = calculateAccelerationStability(sensorData)
        val rotationPatterns = analyzeRotationPatterns(sensorData)
        
        // Analyze sensor patterns to determine environment type
        val (type, confidence) = when {
            // High magnetic variation suggests areas with metal structures or electrical equipment
            magneticVariation > 15f -> {
                if (rotationPatterns > 0.7f) {
                    EnvironmentType.STAIRWELL to 0.75f
                } else {
                    EnvironmentType.HIGH_INTERFERENCE to 0.8f
                }
            }
            
            // Stable acceleration with consistent rotation suggests open areas
            accelerationStability > 0.8f && rotationPatterns < 0.3f ->
                EnvironmentType.OPEN_AREA to 0.7f
                
            // Moderate magnetic variation with specific rotation patterns suggests corridors
            magneticVariation in 5f..15f && rotationPatterns > 0.6f ->
                EnvironmentType.CORRIDOR to 0.75f
                
            // Default case when patterns don't match known environments
            else -> EnvironmentType.UNKNOWN to 0.5f
        }
        
        return ClassificationResult(
            environmentType = type,
            confidence = confidence,
            characteristics = mapOf(
                "magneticVariation" to magneticVariation,
                "accelerationStability" to accelerationStability,
                "rotationPatterns" to rotationPatterns
            )
        )
    }

    /**
     * Combines multiple classification results to get a more accurate environment type.
     *
     * @param wifiResult Classification result from Wi-Fi analysis
     * @param sensorResult Classification result from sensor analysis
     * @return Combined classification result
     */
    fun combineClassifications(
        wifiResult: ClassificationResult,
        sensorResult: ClassificationResult
    ): ClassificationResult {
        // If both classifications agree, increase confidence
        if (wifiResult.environmentType == sensorResult.environmentType) {
            return ClassificationResult(
                environmentType = wifiResult.environmentType,
                confidence = (wifiResult.confidence + sensorResult.confidence) / 2f + 0.1f,
                characteristics = wifiResult.characteristics + sensorResult.characteristics
            )
        }
        
        // Otherwise, choose the classification with higher confidence
        return if (wifiResult.confidence > sensorResult.confidence) {
            wifiResult
        } else {
            sensorResult
        }
    }

    /**
     * Calculates the variation in signal strengths among access points.
     */
    private fun calculateSignalVariation(accessPoints: List<WifiAccessPoint>): Float {
        if (accessPoints.isEmpty()) return 0f
        
        val rssiValues = accessPoints.map { it.rssi.toFloat() }
        val mean = rssiValues.average().toFloat()
        val variance = rssiValues.map { (it - mean) * (it - mean) }.average().toFloat()
        
        return kotlin.math.sqrt(variance)
    }

    /**
     * Analyzes the spatial distribution of access points based on signal strengths.
     * Returns a value between 0 (evenly distributed) and 1 (linearly distributed).
     */
    private fun analyzeSignalDistribution(accessPoints: List<WifiAccessPoint>): Float {
        if (accessPoints.size < 3) return 0.5f
        
        // This is a simplified analysis that could be enhanced with actual positioning data
        // For now, we use signal strength patterns to estimate distribution
        
        // Sort by signal strength
        val sortedAPs = accessPoints.sortedBy { it.rssi }
        
        // Calculate differences between consecutive signal strengths
        val differences = sortedAPs.zipWithNext { a, b -> kotlin.math.abs(a.rssi - b.rssi).toFloat() }
        
        // Calculate standard deviation of differences
        val mean = differences.average().toFloat()
        val variance = differences.map { (it - mean) * (it - mean) }.average().toFloat()
        val stdDev = kotlin.math.sqrt(variance.toDouble()).toFloat()
        
        // Low standard deviation suggests linear distribution (like in a corridor)
        return 1f - (stdDev / 20f).coerceIn(0f, 1f)
    }

    /**
     * Calculates variation in magnetic field readings.
     */
    private fun calculateMagneticVariation(sensorData: List<SensorData.Combined>): Float {
        val magneticReadings = sensorData.map { it.magnetometer }
        if (magneticReadings.isEmpty()) return 0f
        
        // Calculate magnitude of each reading
        val magnitudes = magneticReadings.map { it.magnitude() }
        
        // Calculate standard deviation
        val mean = magnitudes.average().toFloat()
        val variance = magnitudes.map { (it - mean) * (it - mean) }.average().toFloat()
        
        return kotlin.math.sqrt(variance.toDouble()).toFloat()
    }

    /**
     * Calculates stability of acceleration readings.
     * Returns a value between 0 (unstable) and 1 (stable).
     */
    private fun calculateAccelerationStability(sensorData: List<SensorData.Combined>): Float {
        val accelerationReadings = sensorData.map { it.accelerometer }
        if (accelerationReadings.isEmpty()) return 0.5f
        
        // Calculate magnitude of each reading
        val magnitudes = accelerationReadings.map { it.magnitude() }
        
        // Calculate standard deviation
        val mean = magnitudes.average().toFloat()
        val variance = magnitudes.map { (it - mean) * (it - mean) }.average().toFloat()
        val stdDev = kotlin.math.sqrt(variance.toDouble()).toFloat()
        
        // Lower standard deviation means more stable readings
        return (1f - (stdDev / 10f)).coerceIn(0f, 1f)
    }

    /**
     * Analyzes rotation patterns to detect specific movement types.
     * Returns a value between 0 (random rotation) and 1 (consistent pattern).
     */
    private fun analyzeRotationPatterns(sensorData: List<SensorData.Combined>): Float {
        val rotationReadings = sensorData.mapNotNull { it.gyroscope }
        if (rotationReadings.size < 5) return 0.5f
        
        // Analyze consecutive rotation changes
        val changes = rotationReadings.zipWithNext { a, b ->
            val dx = b.x - a.x
            val dy = b.y - a.y
            val dz = b.z - a.z
            Triple(dx, dy, dz)
        }
        
        // Check if one axis dominates (suggesting corridor or stairwell movement)
        val sumX = changes.sumOf { kotlin.math.abs(it.first.toDouble()) }
        val sumY = changes.sumOf { kotlin.math.abs(it.second.toDouble()) }
        val sumZ = changes.sumOf { kotlin.math.abs(it.third.toDouble()) }
        val total = sumX + sumY + sumZ
        
        if (total == 0.0) return 0.5f
        
        val dominance = kotlin.math.max(kotlin.math.max(sumX, sumY), sumZ) / total
        
        return dominance.toFloat()
    }
}