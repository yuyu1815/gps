package com.example.myapplication.service

import android.util.Log
import timber.log.Timber
import java.io.File

/**
 * A Timber tree that logs to a file.
 */
class FileLoggingTree(private val logFileManager: LogFileManager) : Timber.DebugTree() {

    private val logFile: File by lazy {
        logFileManager.createLogFile(LogFileManager.LogType.APP, "session_log")
    }

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        // Don't log verbose and debug messages to file to avoid excessive logging
        if (priority == Log.VERBOSE || priority == Log.DEBUG) {
            return
        }

        val logMessage = "${System.currentTimeMillis()},${priorityToString(priority)},$tag: $message\n"

        try {
            logFile.appendText(logMessage)
            if (t != null) {
                logFile.appendText(Log.getStackTraceString(t) + "\n")
            }
        } catch (e: Exception) {
            Timber.tag("FileLoggingTree").e(e, "Error writing to log file")
        }
    }

    private fun priorityToString(priority: Int): String {
        return when (priority) {
            Log.VERBOSE -> "V"
            Log.DEBUG -> "D"
            Log.INFO -> "I"
            Log.WARN -> "W"
            Log.ERROR -> "E"
            Log.ASSERT -> "A"
            else -> "?"
        }
    }
}
