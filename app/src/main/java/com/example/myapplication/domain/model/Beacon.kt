package com.example.myapplication.domain.model

import kotlin.math.pow

/**
 * Minimal Beacon model to support tests and instrumentation.
 * - Path loss distance estimation based on txPower and last RSSI
 * - Carries optional precomputed estimatedDistance and distanceConfidence used by tests
 */
 data class Beacon(
     val macAddress: String,
     val x: Float,
     val y: Float,
     val txPower: Int,
     val name: String? = null,
     var estimatedDistance: Float? = null,
     var distanceConfidence: Float = 0f
 ) {
     var lastRssi: Int = txPower
         private set

     var lastSeenTimestamp: Long = 0L
         private set

     /**
      * Update the latest RSSI reading and timestamp. Also refreshes estimatedDistance using default factor.
      */
     fun updateRssi(rssi: Int, timestamp: Long) {
         lastRssi = rssi
         lastSeenTimestamp = timestamp
         // Refresh quick estimate with default environmental factor 2.0
         estimatedDistance = calculateDistance(2.0f)
     }

     /**
      * Calculate distance using the log-distance path loss model:
      * d = 10 ^ ((txPower - rssi) / (10 * n))
      * where n is the environmental factor.
      */
     fun calculateDistance(environmentalFactor: Float = 2.0f): Float {
         val exponent = (txPower - lastRssi).toDouble() / (10.0 * environmentalFactor)
         return 10.0.pow(exponent).toFloat()
     }
 }

