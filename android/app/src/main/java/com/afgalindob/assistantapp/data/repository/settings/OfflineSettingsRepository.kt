package com.afgalindob.assistantapp.data.repository.settings

import com.afgalindob.assistantapp.data.local.preferences.UserPreferencesManager
import com.afgalindob.assistantapp.data.local.preferences.UserPreferences
import com.afgalindob.assistantapp.data.repository.network.NetworkRepository
import kotlinx.coroutines.flow.Flow
import com.afgalindob.assistantapp.data.local.network.NetworkResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class OfflineSettingsRepository(
    private val networkRepository: NetworkRepository,
    private val preferencesManager: UserPreferencesManager
) : SettingsRepository {

    override val userData: Flow<UserPreferences> = preferencesManager.userPreferencesFlow
    override val languageData: Flow<String> = preferencesManager.languageFlow
    override val serverUsernameData: Flow<String> = preferencesManager.serverUsernameFlow
    override val isUrlConfigured: Flow<Boolean> = preferencesManager.isUrlConfiguredFlow
    override val isTokenConfigured: Flow<Boolean> = preferencesManager.isTokenConfiguredFlow

    override suspend fun hasUrlToAPI(): Boolean {
        return !preferencesManager.getUrlSynchronous().isNullOrBlank()
    }

    override suspend fun hasToken(): Boolean {
        return !preferencesManager.getTokenSynchronous().isNullOrBlank()
    }

    override suspend fun getUrl(): String? {
        return preferencesManager.getUrlSynchronous()
    }

    override suspend fun getToken(): String? {
        return preferencesManager.getTokenSynchronous()
    }

    override suspend fun getServerUsername(): String {
        return preferencesManager.getServerUsernameSynchronous()
    }

    override suspend fun clearToken() {
        preferencesManager.clearToken()
    }

    override suspend fun saveToken(token: String) {
        preferencesManager.saveToken(token)
    }

    override suspend fun saveUrl(domain: String) {
        preferencesManager.saveUrl(domain)
    }

    override suspend fun saveUser(userPrefs: UserPreferences) {
        preferencesManager.saveUserPreferences(userPrefs)
    }

    override suspend fun updateLanguage(languageCode: String) {
        preferencesManager.updateLanguage(languageCode)
    }

    override suspend fun updateReminderTime(time: String) {
        preferencesManager.updateReminderTime(time)
    }

    override suspend fun updateServerUsername(username: String) {
        preferencesManager.updateServerUsername(username)
    }

    override suspend fun checkServerHealth(): Boolean = withContext(Dispatchers.IO) {
        networkRepository.checkServerHealth()
    }

    override suspend fun checkCredentials(): Pair<NetworkResult, String> = withContext(Dispatchers.IO) {
        networkRepository.checkCredentials()
    }

    override suspend fun requestAuth(qrData: String): Result<Pair<NetworkResult, String>> = withContext(Dispatchers.IO) {
        networkRepository.processQrAuthData(qrData).map { (endpoint, sid, qrType) ->
            networkRepository.requestAuth(endpoint, sid, qrType)
        }
    }

    override suspend fun requestToken(qrData: String): Result<Pair<NetworkResult, String>> = withContext(Dispatchers.IO) {
        networkRepository.processQrTokenData(qrData).map { (endpoint, qrType) ->
            networkRepository.requestToken(endpoint, qrType)
        }
    }
}