package com.example.myapplication.service

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.LocaleList
import timber.log.Timber
import java.util.Locale

/**
 * アプリの言語管理クラス
 * 日本語、英語、システム設定の切り替えを管理します
 */
class LanguageManager(private val context: Context) {

    companion object {
        const val LANGUAGE_SYSTEM = "system"
        const val LANGUAGE_ENGLISH = "en"
        const val LANGUAGE_JAPANESE = "ja"
    }

    /**
     * 現在の言語設定を取得
     */
    fun getCurrentLanguage(): String {
        return try {
            val sharedPrefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            sharedPrefs.getString("app_language", LANGUAGE_SYSTEM) ?: LANGUAGE_SYSTEM
        } catch (e: Exception) {
            Timber.e(e, "Error getting current language")
            LANGUAGE_SYSTEM
        }
    }

    /**
     * 言語設定を保存
     */
    fun setLanguage(languageCode: String) {
        try {
            val sharedPrefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            sharedPrefs.edit().putString("app_language", languageCode).apply()
            Timber.d("Language set to: $languageCode")
        } catch (e: Exception) {
            Timber.e(e, "Error setting language")
        }
    }

    /**
     * 指定された言語でリソースを更新
     */
    fun updateResources(languageCode: String): Context {
        return try {
            val locale = when (languageCode) {
                LANGUAGE_ENGLISH -> Locale.ENGLISH
                LANGUAGE_JAPANESE -> Locale.JAPANESE
                else -> Locale.getDefault()
            }

            Locale.setDefault(locale)

            val config = Configuration(context.resources.configuration)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                config.setLocales(LocaleList(locale))
            } else {
                @Suppress("DEPRECATION")
                config.locale = locale
            }

            context.createConfigurationContext(config)
        } catch (e: Exception) {
            Timber.e(e, "Error updating resources")
            context
        }
    }

    /**
     * 利用可能な言語リストを取得
     */
    fun getAvailableLanguages(): List<LanguageOption> {
        return listOf(
            LanguageOption(LANGUAGE_SYSTEM, "System default", "システム設定に従う"),
            LanguageOption(LANGUAGE_ENGLISH, "English", "英語"),
            LanguageOption(LANGUAGE_JAPANESE, "日本語", "日本語")
        )
    }

    /**
     * 言語オプションのデータクラス
     */
    data class LanguageOption(
        val code: String,
        val displayNameEn: String,
        val displayNameJa: String
    ) {
        fun getDisplayName(currentLanguage: String): String {
            return when (currentLanguage) {
                LANGUAGE_JAPANESE -> displayNameJa
                else -> displayNameEn
            }
        }
    }

    /**
     * 現在の言語に基づいて文字列を取得
     */
    fun getString(resourceId: Int): String {
        return try {
            val currentLanguage = getCurrentLanguage()
            val updatedContext = updateResources(currentLanguage)
            updatedContext.getString(resourceId)
        } catch (e: Exception) {
            Timber.e(e, "Error getting string for resource: $resourceId")
            context.getString(resourceId)
        }
    }

    /**
     * 言語が変更されたかどうかを確認
     */
    fun hasLanguageChanged(oldLanguage: String): Boolean {
        val currentLanguage = getCurrentLanguage()
        return oldLanguage != currentLanguage
    }
}
