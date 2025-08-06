package com.example.myapplication.service

import android.content.Context
import com.google.firebase.crashlytics.FirebaseCrashlytics
import timber.log.Timber

/**
 * Utility class for reporting crashes and non-fatal exceptions to Firebase Crashlytics.
 * This class provides a centralized way to log errors and exceptions, making it easier
 * to maintain consistent crash reporting throughout the application.
 */
class CrashReporter private constructor() {
    companion object {
        private var crashlytics: FirebaseCrashlytics? = null
        private var isInitialized = false
        
        /**
         * Initialize the CrashReporter.
         * This should be called once during application startup.
         */
        fun initialize() {
            if (!isInitialized) {
                try {
                    crashlytics = FirebaseCrashlytics.getInstance()
                    isInitialized = true
                    Timber.d("CrashReporter initialized")
                } catch (e: Exception) {
                    Timber.e(e, "Failed to initialize CrashReporter")
                    isInitialized = false
                }
            }
        }
        
        /**
         * Check if CrashReporter is initialized
         */
        private fun checkInitialized() {
            if (!isInitialized || crashlytics == null) {
                Timber.w("CrashReporter not initialized. Call initialize() first.")
            }
        }

        /**
         * Logs a non-fatal exception to Crashlytics.
         * This should be used for exceptions that don't crash the app but are still important to track.
         *
         * @param throwable The exception to log
         * @param message Optional message to provide context for the exception
         */
        fun logException(throwable: Throwable, message: String? = null) {
            // Log to Timber first for local debugging
            if (message != null) {
                Timber.e(throwable, "Non-fatal exception: $message")
            } else {
                Timber.e(throwable, "Non-fatal exception")
            }

            checkInitialized()
            
            // Add custom log message if provided
            if (message != null) {
                crashlytics?.log("Non-fatal exception: $message")
            }

            // Record the exception in Crashlytics
            crashlytics?.recordException(throwable)
        }

        /**
         * Logs a custom error message to Crashlytics.
         * This should be used for errors that don't have an associated exception.
         *
         * @param message The error message to log
         * @param customKeys Optional map of custom key-value pairs to add to the log
         */
        fun logError(message: String, customKeys: Map<String, Any>? = null) {
            // Log to Timber first for local debugging
            Timber.e("Error: $message")

            checkInitialized()
            
            // Log the message to Crashlytics
            crashlytics?.log("Error: $message")

            // Add any custom keys
            customKeys?.forEach { (key, value) ->
                when (value) {
                    is String -> crashlytics?.setCustomKey(key, value)
                    is Int -> crashlytics?.setCustomKey(key, value)
                    is Long -> crashlytics?.setCustomKey(key, value)
                    is Float -> crashlytics?.setCustomKey(key, value)
                    is Double -> crashlytics?.setCustomKey(key, value)
                    is Boolean -> crashlytics?.setCustomKey(key, value)
                    else -> crashlytics?.setCustomKey(key, value.toString())
                }
            }
        }

        /**
         * Sets a user identifier for crash reports.
         * This helps identify which users are experiencing crashes.
         * For privacy reasons, avoid using personally identifiable information.
         *
         * @param userId A unique identifier for the user
         */
        fun setUserId(userId: String) {
            checkInitialized()
            crashlytics?.setUserId(userId)
        }

        /**
         * Sets a custom key-value pair that will be included in crash reports.
         * This can be used to provide additional context for crashes.
         *
         * @param key The key for the custom data
         * @param value The value for the custom data
         */
        fun setCustomKey(key: String, value: String) {
            checkInitialized()
            crashlytics?.setCustomKey(key, value)
        }

        /**
         * Sets a custom key-value pair that will be included in crash reports.
         *
         * @param key The key for the custom data
         * @param value The value for the custom data
         */
        fun setCustomKey(key: String, value: Int) {
            checkInitialized()
            crashlytics?.setCustomKey(key, value)
        }

        /**
         * Sets a custom key-value pair that will be included in crash reports.
         *
         * @param key The key for the custom data
         * @param value The value for the custom data
         */
        fun setCustomKey(key: String, value: Long) {
            checkInitialized()
            crashlytics?.setCustomKey(key, value)
        }

        /**
         * Sets a custom key-value pair that will be included in crash reports.
         *
         * @param key The key for the custom data
         * @param value The value for the custom data
         */
        fun setCustomKey(key: String, value: Float) {
            checkInitialized()
            crashlytics?.setCustomKey(key, value)
        }

        /**
         * Sets a custom key-value pair that will be included in crash reports.
         *
         * @param key The key for the custom data
         * @param value The value for the custom data
         */
        fun setCustomKey(key: String, value: Double) {
            checkInitialized()
            crashlytics?.setCustomKey(key, value)
        }

        /**
         * Sets a custom key-value pair that will be included in crash reports.
         *
         * @param key The key for the custom data
         * @param value The value for the custom data
         */
        fun setCustomKey(key: String, value: Boolean) {
            checkInitialized()
            crashlytics?.setCustomKey(key, value)
        }

        /**
         * Enables or disables Crashlytics data collection.
         * This can be used to respect user preferences for crash reporting.
         *
         * @param enabled Whether crash reporting should be enabled
         */
        fun setCrashlyticsCollectionEnabled(enabled: Boolean) {
            checkInitialized()
            crashlytics?.setCrashlyticsCollectionEnabled(enabled)
        }
    }
}