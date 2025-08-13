package com.example.myapplication.service

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import timber.log.Timber

/**
 * アプリ全体のイベントバス
 * 言語変更などのイベントを通知するために使用
 */
object EventBus {
    
    sealed class AppEvent {
        data class LanguageChanged(val languageCode: String) : AppEvent()
        data class SettingsUpdated(val settingKey: String, val value: Any) : AppEvent()
        data class NavigationRequested(val destination: String) : AppEvent()
    }

    private val _events = MutableSharedFlow<AppEvent>()
    val events: SharedFlow<AppEvent> = _events.asSharedFlow()

    /**
     * イベントを発行
     */
    suspend fun emit(event: AppEvent) {
        try {
            _events.emit(event)
            Timber.d("Event emitted: $event")
        } catch (e: Exception) {
            Timber.e(e, "Error emitting event: $event")
        }
    }

    /**
     * 言語変更イベントを発行
     */
    suspend fun emitLanguageChanged(languageCode: String) {
        emit(AppEvent.LanguageChanged(languageCode))
    }

    /**
     * 設定更新イベントを発行
     */
    suspend fun emitSettingsUpdated(settingKey: String, value: Any) {
        emit(AppEvent.SettingsUpdated(settingKey, value))
    }

    /**
     * ナビゲーション要求イベントを発行
     */
    suspend fun emitNavigationRequested(destination: String) {
        emit(AppEvent.NavigationRequested(destination))
    }
}
