package com.afgalindob.assistantapp.utils

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

object LanguageUtils {

    fun getSystemLanguageCode(): String {
        val currentLang = Locale.getDefault().language.lowercase()
        return when {
            currentLang.startsWith("es") -> "es"
            currentLang.startsWith("en") -> "en"
            else -> "en"
        }
    }

    fun normalizeLanguageCode(code: String): String {
        return when (code.lowercase().take(2)) {
            "es" -> "es"
            "en" -> "en"
            else -> "en"
        }
    }

    fun applyAppLanguage(languageCode: String) {
        val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags(
            normalizeLanguageCode(languageCode)
        )
        AppCompatDelegate.setApplicationLocales(appLocale)
    }
}