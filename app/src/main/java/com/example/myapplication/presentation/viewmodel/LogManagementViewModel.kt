package com.example.myapplication.presentation.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.service.LogFileManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File

/**
 * ViewModel for managing log files.
 */
class LogManagementViewModel(application: Application) : AndroidViewModel(application) {
    
    private val logFileManager = LogFileManager(application)
    
    // Observable state
    private val _logFiles = mutableStateListOf<LogFileManager.LogFileInfo>()
    val logFiles: List<LogFileManager.LogFileInfo> = _logFiles
    
    var totalSize by mutableStateOf("")
        private set
    
    var isLoading by mutableStateOf(false)
        private set
    
    var errorMessage by mutableStateOf<String?>(null)
        private set
    
    init {
        loadLogFiles()
    }
    
    /**
     * Loads log files of the specified type.
     *
     * @param type The type of log files to load
     */
    fun loadLogFiles(type: LogFileManager.LogType = LogFileManager.LogType.ALL) {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            
            try {
                withContext(Dispatchers.IO) {
                    val files = logFileManager.getLogFiles(type)
                    
                    withContext(Dispatchers.Main) {
                        _logFiles.clear()
                        _logFiles.addAll(files)
                        totalSize = logFileManager.getFormattedTotalLogSize(type)
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading log files")
                errorMessage = "Failed to load log files: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }
    
    /**
     * Deletes a log file.
     *
     * @param file The file to delete
     */
    fun deleteLogFile(file: File) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val success = logFileManager.deleteLogFile(file)
                    
                    if (success) {
                        withContext(Dispatchers.Main) {
                            // Find and remove the deleted file from the list
                            val index = _logFiles.indexOfFirst { it.file == file }
                            if (index >= 0) {
                                _logFiles.removeAt(index)
                            }
                            
                            // Update total size
                            totalSize = logFileManager.getFormattedTotalLogSize(LogFileManager.LogType.ALL)
                        }
                    } else {
                        errorMessage = "Failed to delete log file"
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error deleting log file")
                errorMessage = "Failed to delete log file: ${e.message}"
            }
        }
    }
    
    /**
     * Deletes all log files of the specified type.
     *
     * @param type The type of log files to delete
     */
    fun deleteAllLogFiles(type: LogFileManager.LogType = LogFileManager.LogType.ALL) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val count = logFileManager.deleteAllLogFiles(type)
                    
                    withContext(Dispatchers.Main) {
                        if (count > 0) {
                            // Reload log files after deletion
                            loadLogFiles(type)
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error deleting all log files")
                errorMessage = "Failed to delete log files: ${e.message}"
            }
        }
    }
    
    /**
     * Clears the error message.
     */
    fun clearErrorMessage() {
        errorMessage = null
    }
}