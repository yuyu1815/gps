package com.example.myapplication.service

import timber.log.Timber

/**
 * Lightweight crash logging stub (Firebase removed).
 */
class CrashReporter private constructor() {
    companion object {
        fun initialize() { /* no-op */ }
        fun logException(throwable: Throwable, message: String? = null) {
            if (message != null) Timber.e(throwable, message) else Timber.e(throwable)
        }
        fun logError(message: String, customKeys: Map<String, Any>? = null) {
            Timber.e("Error: $message ${customKeys ?: emptyMap<String, Any>()}")
        }
        fun setUserId(userId: String) { /* no-op */ }
        fun setCustomKey(key: String, value: String) { /* no-op */ }
        fun setCustomKey(key: String, value: Int) { /* no-op */ }
        fun setCustomKey(key: String, value: Long) { /* no-op */ }
        fun setCustomKey(key: String, value: Float) { /* no-op */ }
        fun setCustomKey(key: String, value: Double) { /* no-op */ }
        fun setCustomKey(key: String, value: Boolean) { /* no-op */ }
        fun setCrashlyticsCollectionEnabled(enabled: Boolean) { /* no-op */ }
    }
}