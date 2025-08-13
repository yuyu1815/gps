package com.example.myapplication.wifi

import kotlinx.coroutines.flow.StateFlow

class FingerprintGenerator(private val wifiScanner: WifiScanner) {

    fun generateFingerprintMap(scanResults: List<ScanResult>): Map<String, Int> {
        val fingerprintMap = mutableMapOf<String, Int>()
        scanResults.forEach {
            fingerprintMap[it.bssid] = it.rssi
        }
        return fingerprintMap
    }
}
