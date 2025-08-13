package com.example.myapplication.presentation.viewmodel

import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.repository.ISettingsRepository
import com.example.myapplication.wifi.WifiScanner
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class WifiViewModel(
    private val wifiScanner: WifiScanner,
    private val settingsRepository: ISettingsRepository
) : BaseViewModel<WifiViewModel.UiState>() {

    init {
        updateState(UiState())
        loadSelectedAsync()
    }

    private fun loadSelectedAsync() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val set = settingsRepository.getSelectedWifiBssids()
                withContext(Dispatchers.Main) {
                    uiState.value?.let { updateState(it.copy(selectedBssids = set)) }
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load selected Wi‑Fi BSSIDs")
            }
        }
    }

    fun toggleSelection(bssid: String) {
        val current = uiState.value ?: return
        val newSet = current.selectedBssids.toMutableSet().apply {
            if (contains(bssid)) remove(bssid) else add(bssid)
        }.toSet()
        
        // 即座にUIを更新
        updateState(current.copy(selectedBssids = newSet, isSaving = true))
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                settingsRepository.setSelectedWifiBssids(newSet)
            } catch (e: Exception) {
                Timber.e(e, "Failed to save selected Wi‑Fi BSSIDs")
            } finally {
                withContext(Dispatchers.Main) {
                    uiState.value?.let { updateState(it.copy(isSaving = false)) }
                }
            }
        }
    }

    data class UiState(
        val selectedBssids: Set<String> = emptySet(),
        val isSaving: Boolean = false
    )
}


