package com.example.myapplication.service

import android.content.Context
import androidx.annotation.StringRes

/**
 * Provides application-level access to string resources for non-UI layers
 * such as services, repositories, and logging.
 */
object StringProvider {
    private lateinit var applicationContext: Context

    fun init(context: Context) {
        applicationContext = context.applicationContext
    }

    fun getString(@StringRes resId: Int, vararg args: Any?): String {
        return if (::applicationContext.isInitialized) {
            applicationContext.getString(resId, *args)
        } else {
            // Fallback to empty string if not initialized yet to avoid crashes during early startup
            ""
        }
    }
}


