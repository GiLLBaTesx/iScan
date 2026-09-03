package com.examscanner.premium.localization

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.examscanner.premium.utils.SecureLogger
import java.util.Locale

/**
 * LocalizationManager - Per-app language switching for Offline Assessment.
 *
 * Supports two languages: English (default) and Filipino (Tagalog) (Req 20.1).
 * Switching is applied via AppCompat per-app locales so all UI text updates
 * immediately without an app restart (Req 20.3), and the choice is persisted in
 * SharedPreferences (Req 20.6). MELC descriptions intentionally remain in English
 * per DepEd standards and are NOT translated (Req 20.5).
 *
 * Startup should call [applySavedLanguage] so the persisted preference (or the
 * system default, falling back to English - Req 20.7) is applied.
 */
class LocalizationManager(private val context: Context) {

    /** Supported UI languages. [code] is a BCP-47 language tag used by AppCompat. */
    enum class AppLanguage(val code: String, val displayName: String, val nativeName: String) {
        ENGLISH("en", "English", "English"),
        FILIPINO("fil", "Filipino", "Filipino")
    }

    /**
     * Persists [language] in SharedPreferences (Req 20.6) and applies it via
     * AppCompat per-app locales so the UI updates without a restart (Req 20.3).
     */
    fun setLanguage(language: AppLanguage) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_APP_LANGUAGE, language.code)
            .apply()

        applyLocale(language)
        SecureLogger.d(TAG, "Language changed to ${language.code}")
    }

    /**
     * Returns the currently selected language. Falls back to the system default
     * (if it is one of the supported languages) and otherwise English (Req 20.7).
     */
    fun getCurrentLanguage(): AppLanguage {
        val saved = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_APP_LANGUAGE, null)

        return AppLanguage.values().firstOrNull { it.code == saved }
            ?: systemDefaultOrEnglish()
    }

    /**
     * Applies the saved (or default) language. Call this on startup so the UI
     * reflects the persisted preference (Req 20.6, 20.7).
     */
    fun applySavedLanguage() {
        applyLocale(getCurrentLanguage())
    }

    private fun applyLocale(language: AppLanguage) {
        AppCompatDelegate.setApplicationLocales(
            LocaleListCompat.forLanguageTags(language.code)
        )
    }

    /**
     * Resolves the system default language to a supported [AppLanguage],
     * defaulting to English when the system language is unsupported (Req 20.7).
     */
    private fun systemDefaultOrEnglish(): AppLanguage {
        val systemTag = Locale.getDefault().language.lowercase(Locale.ROOT)
        // Filipino/Tagalog can surface as "fil" or the legacy "tl" tag.
        return when (systemTag) {
            "fil", "tl" -> AppLanguage.FILIPINO
            "en" -> AppLanguage.ENGLISH
            else -> AppLanguage.ENGLISH
        }
    }

    companion object {
        private const val TAG = "LocalizationManager"
        // Matches the design (Req 20.6): SharedPreferences file "settings".
        private const val PREFS_NAME = "settings"
        private const val KEY_APP_LANGUAGE = "app_language"
    }
}
