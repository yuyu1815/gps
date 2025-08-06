package com.example.myapplication.service

import android.content.Context
import android.content.pm.PackageManager
import android.os.Parcelable
import androidx.core.content.pm.PackageInfoCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import org.json.JSONObject
import timber.log.Timber
import java.net.HttpURLConnection
import java.net.URL

/**
 * Service responsible for checking application version and notifying about updates.
 * Compares the current installed version with the latest version available on the server.
 */
class VersionChecker(private val context: Context) {
    
    companion object {
        // Singleton instance
        private var instance: VersionChecker? = null
        
        // Callback for update notifications
        private var updateCallback: ((VersionCheckResult.UpdateAvailable) -> Unit)? = null
        
        fun getInstance(context: Context): VersionChecker {
            if (instance == null) {
                instance = VersionChecker(context.applicationContext)
            }
            return instance!!
        }
        
        fun setUpdateCallback(callback: (VersionCheckResult.UpdateAvailable) -> Unit) {
            updateCallback = callback
        }
        
        fun notifyUpdateAvailable(updateInfo: VersionCheckResult.UpdateAvailable) {
            updateCallback?.invoke(updateInfo)
        }
    }

    /**
     * Data class representing version information
     *
     * @property versionCode The version code (used for comparison)
     * @property versionName The human-readable version name (e.g., "1.0.1")
     * @property updateUrl The URL where the update can be downloaded
     * @property releaseNotes Optional release notes for the update
     * @property forceUpdate Whether the update is mandatory
     */
    @Parcelize
    data class VersionInfo(
        val versionCode: Long,
        val versionName: String,
        val updateUrl: String,
        val releaseNotes: String? = null,
        val forceUpdate: Boolean = false
    ) : Parcelable

    /**
     * Result of the version check operation
     */
    sealed class VersionCheckResult : Parcelable {
        /**
         * Current version is up to date
         */
        @Parcelize
        object UpToDate : VersionCheckResult()

        /**
         * Update is available
         *
         * @property currentVersion Current installed version
         * @property latestVersion Latest available version
         */
        @Parcelize
        data class UpdateAvailable(
            val currentVersion: VersionInfo,
            val latestVersion: VersionInfo
        ) : VersionCheckResult()

        /**
         * Error occurred during version check
         *
         * @property message Error message
         */
        @Parcelize
        data class Error(val message: String) : VersionCheckResult()
    }

    /**
     * Gets the current installed version information
     *
     * @return VersionInfo object with current version details
     */
    fun getCurrentVersion(): VersionInfo {
        try {
            val packageInfo = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }

            val versionCode = PackageInfoCompat.getLongVersionCode(packageInfo)
            val versionName = packageInfo.versionName ?: "Unknown"

            return VersionInfo(
                versionCode = versionCode,
                versionName = versionName,
                updateUrl = "" // No update URL for current version
            )
        } catch (e: Exception) {
            Timber.e(e, "Error getting current version")
            return VersionInfo(
                versionCode = 0,
                versionName = "Unknown",
                updateUrl = ""
            )
        }
    }

    /**
     * Checks if an update is available by comparing with the server version
     *
     * @param serverUrl URL to check for updates (should return JSON with version information)
     * @return VersionCheckResult indicating if an update is available
     */
    suspend fun checkForUpdates(serverUrl: String): VersionCheckResult {
        return withContext(Dispatchers.IO) {
            try {
                val currentVersion = getCurrentVersion()
                val latestVersion = fetchLatestVersionFromServer(serverUrl)

                if (latestVersion.versionCode > currentVersion.versionCode) {
                    Timber.d("Update available: Current=${currentVersion.versionName}, Latest=${latestVersion.versionName}")
                    VersionCheckResult.UpdateAvailable(currentVersion, latestVersion)
                } else {
                    Timber.d("App is up to date (${currentVersion.versionName})")
                    VersionCheckResult.UpToDate
                }
            } catch (e: Exception) {
                Timber.e(e, "Error checking for updates")
                VersionCheckResult.Error("Failed to check for updates: ${e.message}")
            }
        }
    }

    /**
     * Fetches the latest version information from the server
     *
     * @param serverUrl URL to fetch version information from
     * @return VersionInfo object with latest version details
     */
    private suspend fun fetchLatestVersionFromServer(serverUrl: String): VersionInfo {
        return withContext(Dispatchers.IO) {
            var connection: HttpURLConnection? = null
            try {
                val url = URL(serverUrl)
                connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                connection.requestMethod = "GET"

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    parseVersionResponse(response)
                } else {
                    Timber.e("Server returned error code: $responseCode")
                    throw Exception("Server returned error code: $responseCode")
                }
            } finally {
                connection?.disconnect()
            }
        }
    }

    /**
     * Parses the JSON response from the server
     *
     * @param jsonResponse JSON string from server
     * @return VersionInfo object with parsed version details
     */
    private fun parseVersionResponse(jsonResponse: String): VersionInfo {
        val json = JSONObject(jsonResponse)
        return VersionInfo(
            versionCode = json.getLong("versionCode"),
            versionName = json.getString("versionName"),
            updateUrl = json.getString("updateUrl"),
            releaseNotes = if (json.has("releaseNotes")) json.getString("releaseNotes") else null,
            forceUpdate = if (json.has("forceUpdate")) json.getBoolean("forceUpdate") else false
        )
    }
}