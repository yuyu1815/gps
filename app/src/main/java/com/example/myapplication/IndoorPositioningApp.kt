package com.example.myapplication

import android.app.Application
import android.os.Build
import com.example.myapplication.di.repositoryModule
import com.example.myapplication.di.serviceModule
import com.example.myapplication.di.viewModelModule
import com.example.myapplication.di.useCaseModule
import com.example.myapplication.service.AnalyticsManager
import com.example.myapplication.service.FileLoggingTree
import com.example.myapplication.service.LogFileManager
import com.example.myapplication.service.VersionChecker
import com.example.myapplication.service.StringProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import timber.log.Timber
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.example.myapplication.data.repository.SettingsRepository

/**
 * Application class for the Indoor Positioning app.
 * Handles global initialization of components like logging and dependency injection.
 */
class IndoorPositioningApp : Application() {

    // Version checker for update notifications
    private lateinit var versionChecker: VersionChecker
    
    // Application coroutine scope
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()

        // Initialize Koin for dependency injection first
        startKoin {
            // Use Android logger for Koin (logs to Logcat)
            androidLogger(Level.ERROR) // Only log errors to avoid excessive logging
            
            // Provide Android context to Koin
            androidContext(this@IndoorPositioningApp)
            
            // Load Koin modules
            modules(
                repositoryModule,
                viewModelModule,
                serviceModule,
                useCaseModule
            )
        }
        
        // Initialize Timber for logging
        // In a real app, we would use BuildConfig.DEBUG, but for simplicity
        // we'll always plant the debug tree in this development version
        Timber.plant(Timber.DebugTree())

        // Initialize StringProvider for non-UI localized strings (e.g., logs)
        StringProvider.init(this)

        // Plant the file logging tree for persistent logs - 非同期化
        applicationScope.launch(Dispatchers.IO) {
            try {
                val logFileManager: LogFileManager = get()
                Timber.plant(FileLoggingTree(logFileManager))
                Timber.d(com.example.myapplication.service.StringProvider.getString(com.example.myapplication.R.string.log_timber_initialized))
                Timber.d(com.example.myapplication.service.StringProvider.getString(
                    com.example.myapplication.R.string.log_device_info,
                    Build.MANUFACTURER,
                    Build.MODEL,
                    Build.VERSION.RELEASE
                ))
            } catch (e: Exception) {
                Timber.e(e, com.example.myapplication.service.StringProvider.getString(com.example.myapplication.R.string.log_error_init_file_logging))
            }
        }
        
        // Initialize lightweight analytics stub (no network)
        AnalyticsManager.initialize(applicationContext)
        
        // Optional background tasks disabled for minimal core
        
        // Initialize version checker and check for updates
        versionChecker = VersionChecker(applicationContext)
        checkForUpdates()
        Timber.d(com.example.myapplication.service.StringProvider.getString(com.example.myapplication.R.string.log_version_checker_ready))

        // Apply persisted app language at startup (non-blocking)
        applicationScope.launch {
            try {
                val settingsRepo = SettingsRepository(this@IndoorPositioningApp)
                val lang = settingsRepo.getAppLanguage()
                val locales = when (lang) {
                    "system" -> LocaleListCompat.getEmptyLocaleList()
                    else -> LocaleListCompat.forLanguageTags(lang)
                }
                AppCompatDelegate.setApplicationLocales(locales)
                Timber.d(com.example.myapplication.service.StringProvider.getString(com.example.myapplication.R.string.log_applied_app_language, lang))
            } catch (e: Exception) {
                Timber.e(e, com.example.myapplication.service.StringProvider.getString(com.example.myapplication.R.string.log_failed_apply_language))
            }
        }
    }
    
    override fun onTerminate() {
        super.onTerminate()
        
        // Cancel background tasks when application is terminated
        // if (::backgroundProcessingManager.isInitialized) {
        //     backgroundProcessingManager.cancelAllTasks()
        //     Timber.d("Background processing tasks cancelled")
        // }
    }
    
    /**
     * Checks for app updates using the VersionChecker service.
     * This is called during app initialization and can be called
     * manually when the user requests a check for updates.
     */
    private fun checkForUpdates() {
        // In a real app, this URL would point to your update server
        // For this example, we're using a placeholder URL
        val updateServerUrl = "https://example.com/api/updates/indoor-positioning-app"
        
        applicationScope.launch {
            try {
                Timber.d(com.example.myapplication.service.StringProvider.getString(com.example.myapplication.R.string.log_checking_updates))
                val result = versionChecker.checkForUpdates(updateServerUrl)
                
                when (result) {
                    is VersionChecker.VersionCheckResult.UpdateAvailable -> {
                        Timber.d(com.example.myapplication.service.StringProvider.getString(
                            com.example.myapplication.R.string.log_update_available,
                            result.currentVersion.versionName,
                            result.latestVersion.versionName
                        ))
                        // Notify about the update using the callback
                        VersionChecker.notifyUpdateAvailable(result)
                    }
                    is VersionChecker.VersionCheckResult.UpToDate -> {
                        Timber.d(com.example.myapplication.service.StringProvider.getString(com.example.myapplication.R.string.log_app_up_to_date))
                    }
                    is VersionChecker.VersionCheckResult.Error -> {
                        Timber.e(com.example.myapplication.service.StringProvider.getString(com.example.myapplication.R.string.log_error_checking_updates, result.message))
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, com.example.myapplication.service.StringProvider.getString(com.example.myapplication.R.string.log_exception_checking_updates))
            }
        }
    }
}