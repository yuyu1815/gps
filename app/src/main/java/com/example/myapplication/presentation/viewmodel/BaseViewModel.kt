package com.example.myapplication.presentation.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

/**
 * Base ViewModel class that provides common functionality for all ViewModels.
 * Implements error handling and loading state management.
 */
abstract class BaseViewModel<UiState> : ViewModel() {
    
    private val _uiState = MutableStateFlow<UiState?>(null)
    val uiState: StateFlow<UiState?> = _uiState.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    /**
     * Updates the UI state.
     */
    protected fun updateState(state: UiState) {
        _uiState.value = state
    }
    
    /**
     * Sets the loading state.
     */
    protected fun setLoading(loading: Boolean) {
        _isLoading.value = loading
    }
    
    /**
     * Sets an error message.
     */
    protected fun setError(message: String?) {
        _error.value = message
        if (message != null) {
            Timber.e("Error: $message")
        }
    }
    
    /**
     * Clears the current error message.
     */
    fun clearError() {
        _error.value = null
    }
    
    /**
     * Executes a suspending operation with loading state and error handling.
     */
    protected suspend fun <T> executeWithLoadingAndErrorHandling(
        block: suspend () -> T,
        onSuccess: (T) -> Unit = {},
        onError: (Throwable) -> Unit = {}
    ) {
        try {
            setLoading(true)
            val result = block()
            onSuccess(result)
        } catch (e: Exception) {
            Timber.e(e, "Error in ViewModel operation")
            setError(e.message ?: "Unknown error occurred")
            onError(e)
        } finally {
            setLoading(false)
        }
    }
}