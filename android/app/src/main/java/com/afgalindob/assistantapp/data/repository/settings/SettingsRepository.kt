package com.afgalindob.assistantapp.data.repository.settings

import com.afgalindob.assistantapp.data.local.network.NetworkResult
import com.afgalindob.assistantapp.data.local.preferences.UserPreferences
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val userData: Flow<UserPreferences>
    val languageData: Flow<String>
    val serverUsernameData: Flow<String>
    val isUrlConfigured: Flow<Boolean>
    val isTokenConfigured: Flow<Boolean>

    suspend fun getUrl(): String?
    suspend fun getToken(): String?
    suspend fun hasUrlToAPI(): Boolean
    suspend fun hasToken(): Boolean
    suspend fun getServerUsername(): String
    suspend fun clearToken()
    suspend fun saveToken(token: String)
    suspend fun saveUrl(domain: String)
    suspend fun saveUser(userPrefs: UserPreferences)
    suspend fun updateLanguage(languageCode: String)
    suspend fun updateReminderTime(time: String)
    suspend fun updateServerUsername(username: String)

    suspend fun checkServerHealth(): Boolean
    suspend fun checkCredentials(): Pair<NetworkResult, String>
    suspend fun requestAuth(qrData: String): Result<Pair<NetworkResult, String>>
    suspend fun requestToken(qrData: String): Result<Pair<NetworkResult, String>>
}