package com.example.myapplication.service

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import timber.log.Timber

/**
 * Utility class for tracking analytics events and user properties.
 * This class provides a centralized way to log events and set user properties,
 * making it easier to maintain consistent analytics throughout the application.
 */
class AnalyticsManager private constructor(context: Context) {
    
    private val firebaseAnalytics: FirebaseAnalytics = FirebaseAnalytics.getInstance(context)
    
    companion object {
        private var instance: AnalyticsManager? = null
        
        /**
         * Initialize the AnalyticsManager with the application context.
         * This should be called once during application startup.
         *
         * @param context The application context
         */
        fun initialize(context: Context) {
            if (instance == null) {
                instance = AnalyticsManager(context.applicationContext)
                Timber.d("AnalyticsManager initialized")
            }
        }
        
        /**
         * Get the singleton instance of AnalyticsManager.
         * Make sure to call initialize() before calling this method.
         *
         * @return The AnalyticsManager instance
         * @throws IllegalStateException if initialize() has not been called
         */
        fun getInstance(): AnalyticsManager {
            return instance ?: throw IllegalStateException(
                "AnalyticsManager not initialized. Call initialize() first."
            )
        }
    }
    
    /**
     * Log a screen view event.
     *
     * @param screenName The name of the screen being viewed
     * @param screenClass The class name of the screen (optional)
     */
    fun logScreenView(screenName: String, screenClass: String? = null) {
        val bundle = Bundle().apply {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
            if (screenClass != null) {
                putString(FirebaseAnalytics.Param.SCREEN_CLASS, screenClass)
            }
        }
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, bundle)
        Timber.d("Screen view logged: $screenName")
    }
    
    /**
     * Log a custom event with parameters.
     *
     * @param eventName The name of the event
     * @param params Optional parameters for the event
     */
    fun logEvent(eventName: String, params: Map<String, Any>? = null) {
        val bundle = Bundle()
        params?.forEach { (key, value) ->
            when (value) {
                is String -> bundle.putString(key, value)
                is Int -> bundle.putInt(key, value)
                is Long -> bundle.putLong(key, value)
                is Float -> bundle.putFloat(key, value)
                is Double -> bundle.putDouble(key, value)
                is Boolean -> bundle.putBoolean(key, value)
                else -> bundle.putString(key, value.toString())
            }
        }
        firebaseAnalytics.logEvent(eventName, bundle)
        Timber.d("Event logged: $eventName, params: $params")
    }
    
    /**
     * Log a beacon discovery event.
     *
     * @param beaconId The ID of the discovered beacon
     * @param rssi The RSSI value of the beacon
     */
    fun logBeaconDiscovery(beaconId: String, rssi: Int) {
        val params = mapOf(
            "beacon_id" to beaconId,
            "rssi" to rssi
        )
        logEvent("beacon_discovery", params)
    }
    
    /**
     * Log a position update event.
     *
     * @param x The x coordinate
     * @param y The y coordinate
     * @param accuracy The accuracy of the position in meters
     * @param source The source of the position update (e.g., "ble", "pdr", "fusion")
     */
    fun logPositionUpdate(x: Float, y: Float, accuracy: Float, source: String) {
        val params = mapOf(
            "x_coordinate" to x,
            "y_coordinate" to y,
            "accuracy" to accuracy,
            "source" to source
        )
        logEvent("position_update", params)
    }
    
    /**
     * Log a navigation event.
     *
     * @param fromScreen The screen navigated from
     * @param toScreen The screen navigated to
     */
    fun logNavigation(fromScreen: String, toScreen: String) {
        val params = mapOf(
            "from_screen" to fromScreen,
            "to_screen" to toScreen
        )
        logEvent("navigation", params)
    }
    
    /**
     * Log a user action event.
     *
     * @param action The action performed by the user
     * @param params Additional parameters for the action
     */
    fun logUserAction(action: String, params: Map<String, Any>? = null) {
        val actionParams = params?.toMutableMap() ?: mutableMapOf()
        actionParams["action"] = action
        logEvent("user_action", actionParams)
    }
    
    /**
     * Log an error event.
     *
     * @param errorType The type of error
     * @param errorMessage The error message
     * @param params Additional parameters for the error
     */
    fun logError(errorType: String, errorMessage: String, params: Map<String, Any>? = null) {
        val errorParams = params?.toMutableMap() ?: mutableMapOf()
        errorParams["error_type"] = errorType
        errorParams["error_message"] = errorMessage
        logEvent("app_error", errorParams)
    }
    
    /**
     * Set a user property.
     *
     * @param name The name of the user property
     * @param value The value of the user property
     */
    fun setUserProperty(name: String, value: String) {
        firebaseAnalytics.setUserProperty(name, value)
        Timber.d("User property set: $name = $value")
    }
    
    /**
     * Set the user ID for analytics.
     * For privacy reasons, avoid using personally identifiable information.
     *
     * @param userId A unique identifier for the user
     */
    fun setUserId(userId: String) {
        firebaseAnalytics.setUserId(userId)
        Timber.d("User ID set: $userId")
    }
    
    /**
     * Enable or disable analytics collection.
     *
     * @param enabled Whether analytics collection should be enabled
     */
    fun setAnalyticsCollectionEnabled(enabled: Boolean) {
        firebaseAnalytics.setAnalyticsCollectionEnabled(enabled)
        Timber.d("Analytics collection ${if (enabled) "enabled" else "disabled"}")
    }
}