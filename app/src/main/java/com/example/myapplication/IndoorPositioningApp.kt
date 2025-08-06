package com.example.myapplication

import android.app.Application
import android.os.Build
import com.example.myapplication.di.repositoryModule
import com.example.myapplication.di.serviceModule
import com.example.myapplication.di.viewModelModule
import com.example.myapplication.service.AnalyticsManager
import com.example.myapplication.service.BackgroundProcessingManager
import com.example.myapplication.service.CrashReporter
import com.example.myapplication.service.VersionChecker
import com.google.firebase.ktx.Firebase
import com.google.firebase.ktx.initialize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import timber.log.Timber

/**
 * Application class for the Indoor Positioning app.
 * Handles global initialization of components like logging and dependency injection.
 */
class IndoorPositioningApp : Application() {

    // Background processing manager for optimizing long-running tasks
    private lateinit var backgroundProcessingManager: BackgroundProcessingManager
    
    // Version checker for update notifications
    private lateinit var versionChecker: VersionChecker
    
    // Application coroutine scope
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        
        // Initialize Timber for logging
        // In a real app, we would use BuildConfig.DEBUG, but for simplicity
        // we'll always plant the debug tree in this development version
        Timber.plant(Timber.DebugTree())
        Timber.d("Timber initialized in debug mode")
        Timber.d("Device: ${Build.MANUFACTURER} ${Build.MODEL}, Android ${Build.VERSION.RELEASE}")
        
        // Initialize Firebase
        Firebase.initialize(this)
        
        // Initialize and configure CrashReporter
        CrashReporter.initialize()
        CrashReporter.setCrashlyticsCollectionEnabled(true)
        
        // Set user identifiers for better crash reports
        CrashReporter.setUserId("anonymous_user")
        CrashReporter.setCustomKey("device_manufacturer", Build.MANUFACTURER)
        CrashReporter.setCustomKey("device_model", Build.MODEL)
        CrashReporter.setCustomKey("android_version", Build.VERSION.RELEASE)
        
        Timber.d("Firebase Crashlytics initialized")
        
        // Initialize Analytics Manager
        AnalyticsManager.initialize(applicationContext)
        
        // Set user properties for analytics
        AnalyticsManager.getInstance().apply {
            setUserId("anonymous_user")
            setUserProperty("device_manufacturer", Build.MANUFACTURER)
            setUserProperty("device_model", Build.MODEL)
            setUserProperty("android_version", Build.VERSION.RELEASE)
            setUserProperty("app_version", "1.0.0") // Hardcoded for simplicity
            
            // Log app start event
            logEvent("app_start", mapOf(
                "timestamp" to System.currentTimeMillis(),
                "is_first_run" to false // In a real app, this would be determined dynamically
            ))
        }
        
        Timber.d("Analytics Manager initialized")
        
        // Initialize Koin for dependency injection
        startKoin {
            // Use Android logger for Koin (logs to Logcat)
            androidLogger(Level.ERROR) // Only log errors to avoid excessive logging
            
            // Provide Android context to Koin
            androidContext(this@IndoorPositioningApp)
            
            // Load Koin modules
            modules(
                repositoryModule,
                viewModelModule,
                serviceModule
            )
        }
        Timber.d("Koin initialized with repository and viewModel modules")
        
        // Initialize and schedule background processing tasks
        backgroundProcessingManager = BackgroundProcessingManager(applicationContext)
        backgroundProcessingManager.scheduleBackgroundTasks()
        Timber.d("Background processing manager initialized and tasks scheduled")
        
        // Initialize version checker and check for updates
        versionChecker = VersionChecker(applicationContext)
        checkForUpdates()
        Timber.d("Version checker initialized and update check scheduled")
    }
    
    override fun onTerminate() {
        super.onTerminate()
        
        // Cancel background tasks when application is terminated
        if (::backgroundProcessingManager.isInitialized) {
            backgroundProcessingManager.cancelAllTasks()
            Timber.d("Background processing tasks cancelled")
        }
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
                Timber.d("Checking for updates...")
                val result = versionChecker.checkForUpdates(updateServerUrl)
                
                when (result) {
                    is VersionChecker.VersionCheckResult.UpdateAvailable -> {
                        Timber.d("Update available: Current=${result.currentVersion.versionName}, Latest=${result.latestVersion.versionName}")
                        // Notify about the update using the callback
                        VersionChecker.notifyUpdateAvailable(result)
                    }
                    is VersionChecker.VersionCheckResult.UpToDate -> {
                        Timber.d("App is up to date")
                    }
                    is VersionChecker.VersionCheckResult.Error -> {
                        Timber.e("Error checking for updates: ${result.message}")
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Exception while checking for updates")
            }
        }
    }
}