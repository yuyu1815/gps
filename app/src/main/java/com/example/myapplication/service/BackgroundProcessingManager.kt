package com.example.myapplication.service

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.myapplication.data.repository.IBeaconRepository
import com.example.myapplication.data.repository.IPositionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Manager for optimizing background processing tasks.
 * 
 * This class uses WorkManager to schedule and execute background tasks efficiently,
 * reducing CPU and memory usage during long-running sessions.
 */
class BackgroundProcessingManager(private val context: Context) {

    private val workManager = WorkManager.getInstance(context)
    
    companion object {
        // Work request tags
        private const val DATA_CLEANUP_WORK = "data_cleanup_work"
        private const val BEACON_HEALTH_WORK = "beacon_health_work"
        private const val LOG_CLEANUP_WORK = "log_cleanup_work"
        
        // Work intervals
        private const val DATA_CLEANUP_INTERVAL_HOURS = 1L
        private const val BEACON_HEALTH_INTERVAL_HOURS = 6L
        private const val LOG_CLEANUP_INTERVAL_HOURS = 24L
    }
    
    /**
     * Schedules all background processing tasks.
     * This should be called during application startup.
     */
    fun scheduleBackgroundTasks() {
        scheduleDataCleanupTask()
        scheduleBeaconHealthCheckTask()
        scheduleLogCleanupTask()
        Timber.i("Scheduled all background processing tasks")
    }
    
    /**
     * Cancels all background processing tasks.
     * This should be called during application shutdown.
     */
    fun cancelAllTasks() {
        workManager.cancelAllWork()
        Timber.i("Cancelled all background processing tasks")
    }
    
    /**
     * Schedules a periodic task to clean up position history data.
     * This reduces memory usage for long-running sessions.
     */
    private fun scheduleDataCleanupTask() {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()
            
        val workRequest = PeriodicWorkRequestBuilder<DataCleanupWorker>(
            DATA_CLEANUP_INTERVAL_HOURS, TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .addTag(DATA_CLEANUP_WORK)
            .build()
            
        workManager.enqueueUniquePeriodicWork(
            DATA_CLEANUP_WORK,
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
        
        Timber.d("Scheduled data cleanup task to run every $DATA_CLEANUP_INTERVAL_HOURS hours")
    }
    
    /**
     * Schedules a periodic task to check beacon health.
     * This ensures beacons are monitored without constant polling.
     */
    private fun scheduleBeaconHealthCheckTask() {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()
            
        val workRequest = PeriodicWorkRequestBuilder<BeaconHealthWorker>(
            BEACON_HEALTH_INTERVAL_HOURS, TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .addTag(BEACON_HEALTH_WORK)
            .build()
            
        workManager.enqueueUniquePeriodicWork(
            BEACON_HEALTH_WORK,
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
        
        Timber.d("Scheduled beacon health check task to run every $BEACON_HEALTH_INTERVAL_HOURS hours")
    }
    
    /**
     * Schedules a periodic task to clean up log files.
     * This prevents excessive disk usage from accumulated logs.
     */
    private fun scheduleLogCleanupTask() {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .setRequiresStorageNotLow(true)
            .build()
            
        val workRequest = PeriodicWorkRequestBuilder<LogCleanupWorker>(
            LOG_CLEANUP_INTERVAL_HOURS, TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .addTag(LOG_CLEANUP_WORK)
            .build()
            
        workManager.enqueueUniquePeriodicWork(
            LOG_CLEANUP_WORK,
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
        
        Timber.d("Scheduled log cleanup task to run every $LOG_CLEANUP_INTERVAL_HOURS hours")
    }
    
    /**
     * Worker for cleaning up position history data.
     */
    class DataCleanupWorker(
        context: Context,
        params: WorkerParameters
    ) : CoroutineWorker(context, params), KoinComponent {
        
        private val positionRepository: IPositionRepository by inject()
        
        override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
            try {
                Timber.d("Starting position data cleanup task")
                
                // Clear old position history
                positionRepository.optimizePositionHistory()
                
                Timber.d("Position data cleanup completed successfully")
                Result.success()
            } catch (e: Exception) {
                Timber.e(e, "Error during position data cleanup")
                Result.retry()
            }
        }
    }
    
    /**
     * Worker for checking beacon health.
     */
    class BeaconHealthWorker(
        context: Context,
        params: WorkerParameters
    ) : CoroutineWorker(context, params), KoinComponent {
        
        private val beaconRepository: IBeaconRepository by inject()
        
        override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
            try {
                Timber.d("Starting beacon health check task")
                
                // Evaluate beacon health
                val criticalCount = beaconRepository.evaluateBeaconHealth()
                
                Timber.d("Beacon health check completed: $criticalCount beacons in critical state")
                Result.success()
            } catch (e: Exception) {
                Timber.e(e, "Error during beacon health check")
                Result.retry()
            }
        }
    }
    
    /**
     * Worker for cleaning up log files.
     */
    class LogCleanupWorker(
        context: Context,
        params: WorkerParameters
    ) : CoroutineWorker(context, params), KoinComponent {
        
        override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
            try {
                Timber.d("Starting log cleanup task")
                
                // Create LogFileManager instance
                val logFileManager = LogFileManager(applicationContext)
                
                // Perform automatic cleanup
                val deletedCount = logFileManager.performAutoCleanup()
                
                Timber.d("Log cleanup completed: deleted $deletedCount files")
                Result.success()
            } catch (e: Exception) {
                Timber.e(e, "Error during log cleanup")
                Result.retry()
            }
        }
    }
}