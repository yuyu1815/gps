package com.example.myapplication.wifi

class PositionEstimator {

    // This is a placeholder for a pre-existing map of fingerprints and their locations.
    // In a real application, this would be loaded from a file or database.
    private val fingerprintDatabase = mapOf(
        Position(10.0, 20.0) to mapOf(
            "bssid-1" to -50,
            "bssid-2" to -60,
            "bssid-3" to -70
        ),
        Position(15.0, 25.0) to mapOf(
            "bssid-1" to -55,
            "bssid-2" to -65,
            "bssid-3" to -75
        )
    )

    fun estimatePosition(currentFingerprint: Map<String, Int>): Position? {
        // This is a placeholder for the position estimation logic.
        // A real implementation would use a more sophisticated algorithm to compare fingerprints.
        if (currentFingerprint.isEmpty()) {
            return null
        }

        var bestMatch: Position? = null
        var minDistance = Double.MAX_VALUE

        fingerprintDatabase.forEach { (position, fingerprint) ->
            val distance = calculateDistance(currentFingerprint, fingerprint)
            if (distance < minDistance) {
                minDistance = distance
                bestMatch = position
            }
        }

        return bestMatch
    }

    private fun calculateDistance(fingerprint1: Map<String, Int>, fingerprint2: Map<String, Int>): Double {
        // Simple Euclidean distance calculation
        var sum = 0.0
        val allBssids = fingerprint1.keys + fingerprint2.keys
        for (bssid in allBssids) {
            val rssi1 = fingerprint1[bssid] ?: -100
            val rssi2 = fingerprint2[bssid] ?: -100
            sum += (rssi1 - rssi2) * (rssi1 - rssi2)
        }
        return Math.sqrt(sum)
    }
}
