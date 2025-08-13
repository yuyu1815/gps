package com.example.myapplication.service

import android.content.Context
import timber.log.Timber

/**
 * Lightweight analytics stub (Firebase removed). Uses Timber for debug-only logs.
 */
class AnalyticsManager private constructor(@Suppress("UNUSED_PARAMETER") context: Context) {
    companion object {
        private var instance: AnalyticsManager? = null
        fun initialize(context: Context) {
            if (instance == null) instance = AnalyticsManager(context.applicationContext)
            Timber.d("AnalyticsManager (stub) initialized")
        }
        fun getInstance(): AnalyticsManager {
            return instance ?: throw IllegalStateException("AnalyticsManager not initialized. Call initialize() first.")
        }
    }

    fun logScreenView(screenName: String, screenClass: String? = null) {
        Timber.d("[AnalyticsStub] screen=%s class=%s", screenName, screenClass ?: "")
    }
    fun logEvent(eventName: String, params: Map<String, Any>? = null) {
        Timber.d("[AnalyticsStub] event=%s params=%s", eventName, params?.toString() ?: "{}")
    }
    fun logBeaconDiscovery(beaconId: String, rssi: Int) { logEvent("beacon_discovery", mapOf("beacon_id" to beaconId, "rssi" to rssi)) }
    fun logPositionUpdate(x: Float, y: Float, accuracy: Float, source: String) {
        logEvent("position_update", mapOf("x_coordinate" to x, "y_coordinate" to y, "accuracy" to accuracy, "source" to source))
    }
    fun logPositioningFailure(failureType: String, details: Map<String, Any>? = null) { logEvent("positioning_failure", (details ?: emptyMap()) + ("failure_type" to failureType)) }
    fun logNavigation(fromScreen: String, toScreen: String) { logEvent("navigation", mapOf("from_screen" to fromScreen, "to_screen" to toScreen)) }
    fun logUserAction(action: String, params: Map<String, Any>? = null) { logEvent("user_action", (params ?: emptyMap()) + ("action" to action)) }
    fun logError(errorType: String, errorMessage: String, params: Map<String, Any>? = null) { logEvent("app_error", (params ?: emptyMap()) + ("error_type" to errorType) + ("error_message" to errorMessage)) }
    fun setUserProperty(name: String, value: String) { Timber.d("[AnalyticsStub] userProperty %s=%s", name, value) }
    fun setUserId(userId: String) { Timber.d("[AnalyticsStub] userId=%s", userId) }
    fun setAnalyticsCollectionEnabled(enabled: Boolean) { Timber.d("[AnalyticsStub] collection=%s", enabled) }
}