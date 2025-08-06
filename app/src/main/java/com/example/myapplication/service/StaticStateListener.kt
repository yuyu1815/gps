package com.example.myapplication.service

/**
 * Interface for components that need to be notified of device static state changes.
 * This is used to optimize power consumption by reducing scanning frequency
 * when the device is not moving.
 */
interface StaticStateListener {
    /**
     * Called when the device's static state changes.
     * 
     * @param isStatic True if the device is now static, false otherwise
     * @param isLongStatic True if the device has been static for a long period, false otherwise
     * @param confidence The confidence level in the static state detection (0.0 to 1.0)
     * @param durationMs The duration of the current static state in milliseconds (0 if not static)
     */
    fun onStaticStateChanged(isStatic: Boolean, isLongStatic: Boolean, confidence: Float, durationMs: Long)
}