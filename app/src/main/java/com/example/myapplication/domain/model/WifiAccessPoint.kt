package com.example.myapplication.domain.model

/**
 * Represents a Wi-Fi access point detected by the device.
 * Used for Wi-Fi fingerprinting and environment classification.
 *
 * @property bssid The BSSID (MAC address) of the access point
 * @property ssid The SSID (network name) of the access point
 * @property rssi The received signal strength indicator in dBm
 * @property frequency The frequency in MHz of the channel over which the client is communicating with the access point
 * @property timestamp The timestamp when this access point was detected (in milliseconds)
 * @property capabilities The authentication, key management, and encryption schemes supported by the access point
 * @property distance The estimated distance to the access point in meters (if available)
 * @property distanceConfidence Confidence level of the distance estimation (0.0-1.0)
 */
data class WifiAccessPoint(
    val bssid: String,
    val ssid: String,
    val rssi: Int,
    val frequency: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val capabilities: String = "",
    val distance: Float? = null,
    val distanceConfidence: Float = 0f
) {
    /**
     * Calculates the estimated distance to the access point based on RSSI.
     * Uses the log-distance path loss model: d = 10^((TxPower - RSSI)/(10 * n))
     * where:
     * - TxPower is the signal strength at 1 meter (typically around -40 dBm)
     * - n is the path loss exponent (typically 2.0-4.0 depending on the environment)
     *
     * @param txPower The signal strength at 1 meter distance (in dBm)
     * @param pathLossExponent The path loss exponent for the environment
     * @return Estimated distance in meters
     */
    fun calculateDistance(txPower: Int = -40, pathLossExponent: Double = 2.5): Float {
        return Math.pow(10.0, (txPower - rssi) / (10.0 * pathLossExponent)).toFloat()
    }

    /**
     * Determines if this access point is likely to be in the same room
     * based on signal strength.
     *
     * @return True if the access point is likely in the same room
     */
    fun isLikelyInSameRoom(): Boolean {
        // RSSI values stronger than -60 dBm typically indicate same room
        return rssi > -60
    }

    /**
     * Determines if this is a 5GHz access point.
     *
     * @return True if the access point operates on 5GHz band
     */
    fun is5GHz(): Boolean {
        return frequency > 5000
    }

    /**
     * Determines if this is a 2.4GHz access point.
     *
     * @return True if the access point operates on 2.4GHz band
     */
    fun is24GHz(): Boolean {
        return frequency in 2400..2500
    }

    /**
     * Determines if this access point supports WPA3 security.
     *
     * @return True if WPA3 is supported
     */
    fun supportsWpa3(): Boolean {
        return capabilities.contains("WPA3")
    }

    /**
     * Determines if this access point is an open network (no security).
     *
     * @return True if this is an open network
     */
    fun isOpenNetwork(): Boolean {
        return capabilities.isEmpty() || 
               (!capabilities.contains("WEP") && 
                !capabilities.contains("WPA") && 
                !capabilities.contains("PSK"))
    }

    /**
     * Returns a signal quality indicator from 0-4 based on RSSI.
     *
     * @return Signal quality level (0=worst, 4=best)
     */
    fun getSignalQuality(): Int {
        return when {
            rssi >= -55 -> 4  // Excellent
            rssi >= -66 -> 3  // Good
            rssi >= -77 -> 2  // Fair
            rssi >= -88 -> 1  // Poor
            else -> 0         // Very poor
        }
    }

    /**
     * Checks if the access point has a strong enough signal for reliable positioning.
     *
     * @return True if the signal is strong enough for positioning
     */
    fun isReliableForPositioning(): Boolean {
        return rssi > -80 // Signals weaker than -80 dBm are generally too weak for reliable positioning
    }

    companion object {
        /**
         * Filters a list of access points to only include those suitable for positioning.
         *
         * @param accessPoints List of detected access points
         * @return Filtered list containing only access points suitable for positioning
         */
        fun filterReliableAccessPoints(accessPoints: List<WifiAccessPoint>): List<WifiAccessPoint> {
            return accessPoints.filter { it.isReliableForPositioning() }
        }

        /**
         * Calculates the average RSSI for a specific access point over time.
         *
         * @param accessPoints List of readings for the same access point (same BSSID)
         * @return Average RSSI value or null if the list is empty
         */
        fun calculateAverageRssi(accessPoints: List<WifiAccessPoint>): Int? {
            if (accessPoints.isEmpty()) return null
            return accessPoints.map { it.rssi }.average().toInt()
        }
    }
}