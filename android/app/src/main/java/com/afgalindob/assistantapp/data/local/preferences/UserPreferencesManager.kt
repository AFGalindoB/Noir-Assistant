package com.afgalindob.assistantapp.data.local.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.afgalindob.assistantapp.utils.LanguageUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "user_settings")

class UserPreferencesManager(private val context: Context) {
    companion object {
        private val NAME_KEY = stringPreferencesKey("user_name")
        private val BIO_KEY = stringPreferencesKey("user_bio")
        private val IMAGE_KEY = stringPreferencesKey("user_image_uri")
        private val OFFSET_X_KEY = floatPreferencesKey("user_image_offset_x")
        private val OFFSET_Y_KEY = floatPreferencesKey("user_image_offset_y")
        private val ZOOM_KEY = floatPreferencesKey("user_image_zoom")
        private val LANGUAGE_KEY = stringPreferencesKey("user_language")
        private val REMINDER_TIME_KEY = stringPreferencesKey("reminder_time")
        private val SERVER_USERNAME_KEY = stringPreferencesKey("server_username")
        private val SERVER_URL_KEY = stringPreferencesKey("server_url")
        private val AUTH_TOKEN_KEY = stringPreferencesKey("auth_token")
    }

    val userPreferencesFlow: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        UserPreferences(
            name = prefs[NAME_KEY] ?: "",
            bio = prefs[BIO_KEY] ?: "",
            imageUri = prefs[IMAGE_KEY],
            centerX = prefs[OFFSET_X_KEY] ?: 0.5f,
            centerY = prefs[OFFSET_Y_KEY] ?: 0.5f,
            zoom = prefs[ZOOM_KEY] ?: 1f,
            language = prefs[LANGUAGE_KEY] ?: LanguageUtils.getSystemLanguageCode(),
            reminderTime = prefs[REMINDER_TIME_KEY] ?: "08:00",
            serverUsername = prefs[SERVER_USERNAME_KEY] ?: "",
            url = prefs[SERVER_URL_KEY] ?: ""
        )
    }

    val languageFlow: Flow<String> = context.dataStore.data
        .map { prefs ->
            prefs[LANGUAGE_KEY] ?: LanguageUtils.getSystemLanguageCode()
        }.distinctUntilChanged()

    val serverUsernameFlow: Flow<String> = context.dataStore.data
        .map { config -> config[SERVER_USERNAME_KEY] ?: "" }.distinctUntilChanged()

    val isUrlConfiguredFlow: Flow<Boolean> = context.dataStore.data
        .map { config -> !config[SERVER_URL_KEY].isNullOrBlank() }
        .distinctUntilChanged()

    val isTokenConfiguredFlow: Flow<Boolean> = context.dataStore.data
        .map { config -> !config[AUTH_TOKEN_KEY].isNullOrBlank() }
        .distinctUntilChanged()

    suspend fun getServerUsernameSynchronous(): String {
        return context.dataStore.data.map { config ->
            config[SERVER_USERNAME_KEY] ?: ""
        }.first()
    }

    suspend fun getUrlSynchronous(): String? = context.dataStore.data.map { prefs ->
        prefs[SERVER_URL_KEY].takeIf { !it.isNullOrBlank() }
    }.first()

    suspend fun getTokenSynchronous(): String? = context.dataStore.data.map { prefs ->
        prefs[AUTH_TOKEN_KEY].takeIf { !it.isNullOrBlank() }
    }.first()

    suspend fun saveUserPreferences(userPrefs: UserPreferences) {
        context.dataStore.edit { prefs ->
            prefs[NAME_KEY] = userPrefs.name
            prefs[BIO_KEY] = userPrefs.bio
            prefs[OFFSET_X_KEY] = userPrefs.centerX
            prefs[OFFSET_Y_KEY] = userPrefs.centerY
            prefs[ZOOM_KEY] = userPrefs.zoom

            if (userPrefs.imageUri != null) {
                prefs[IMAGE_KEY] = userPrefs.imageUri
            } else {
                prefs.remove(IMAGE_KEY)
            }
        }
    }

    suspend fun updateLanguage(languageCode: String) {
        context.dataStore.edit { prefs -> prefs[LANGUAGE_KEY] = LanguageUtils.normalizeLanguageCode(languageCode) }
    }

    suspend fun updateReminderTime(time: String) {
        context.dataStore.edit { prefs -> prefs[REMINDER_TIME_KEY] = time }
    }

    suspend fun updateServerUsername(username: String) {
        context.dataStore.edit { config -> config[SERVER_USERNAME_KEY] = username }
    }

    suspend fun updateUserName(name: String) {
        context.dataStore.edit { config -> config[SERVER_USERNAME_KEY] = name }
    }

    suspend fun saveUrl(url: String) {
        context.dataStore.edit { config -> config[SERVER_URL_KEY] = url }
    }

    suspend fun saveToken(token: String) {
        context.dataStore.edit { config -> config[AUTH_TOKEN_KEY] = token }
    }

    suspend fun clearToken() {
        context.dataStore.edit { config -> config.remove(AUTH_TOKEN_KEY) }
    }

    suspend fun clearUrl() {
        context.dataStore.edit { config -> config.remove(SERVER_URL_KEY) }
    }
}