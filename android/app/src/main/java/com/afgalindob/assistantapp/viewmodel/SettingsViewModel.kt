package com.afgalindob.assistantapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afgalindob.assistantapp.data.local.preferences.UserPreferences
import com.afgalindob.assistantapp.data.local.network.NetworkResult
import com.afgalindob.assistantapp.data.repository.settings.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.afgalindob.assistantapp.data.local.network.ServerStatus
import com.afgalindob.assistantapp.data.local.network.ServerMessage
import com.afgalindob.assistantapp.data.manager.GlobalStateManager

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val globalStateManager: GlobalStateManager
) : ViewModel() {

    val serverStatus: StateFlow<ServerStatus> = globalStateManager.serverStatus

    val userPreferences: StateFlow<UserPreferences> = settingsRepository.userData.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = UserPreferences()
    )

    fun checkCommunicationWithServer() {
        globalStateManager.checkCommunicationWithServer(true)
    }

    fun requestAuth(qrData: String) {
        viewModelScope.launch {
            settingsRepository.requestAuth(qrData)
                .onSuccess { (result, message) ->
                    when (result) {
                        NetworkResult.SUCCESS -> {
                            globalStateManager.updateServerStatus(ServerStatus.AWAITING_APPROVAL)
                            globalStateManager.setServerMessage(ServerMessage("Solicitud de Vinculación", message, false))
                        }
                        NetworkResult.LOGIC_ERROR -> {
                            globalStateManager.setServerMessage(ServerMessage("Error en la solicitud", message, true))
                            globalStateManager.checkCommunicationWithServer()
                        }
                        NetworkResult.CONNECTIVITY_ERROR -> {
                            globalStateManager.setServerMessage(ServerMessage("Fallo de Conexión", message, true))
                            globalStateManager.checkCommunicationWithServer()
                        }
                    }
                }
                .onFailure { error ->
                    globalStateManager.checkCommunicationWithServer()
                    globalStateManager.setServerMessage(ServerMessage("Error de Lectura", error.message ?: "QR Inválido", true))
                }
        }
    }

    fun requestToken(qrData: String) {
        viewModelScope.launch {
            settingsRepository.requestToken(qrData)
                .onSuccess { (result, message) ->
                    when (result) {
                        NetworkResult.SUCCESS -> {
                            globalStateManager.updateServerStatus(ServerStatus.ONLINE)
                            globalStateManager.setServerMessage(ServerMessage("Acceso Concedido", message, false))
                        }
                        NetworkResult.LOGIC_ERROR -> {
                            globalStateManager.checkCommunicationWithServer()
                            globalStateManager.setServerMessage(ServerMessage("Error en la solicitud", message, true))
                        }
                        NetworkResult.CONNECTIVITY_ERROR -> {
                            globalStateManager.setServerMessage(ServerMessage("Fallo de Conexión", message, true))
                        }
                    }
                }
                .onFailure { error ->
                    globalStateManager.checkCommunicationWithServer()
                    globalStateManager.setServerMessage(ServerMessage("Error de Lectura", error.message ?: "QR Inválido", true))
                }
        }
    }

    fun updateProfile(preferences: UserPreferences) {
        viewModelScope.launch {
            settingsRepository.saveUser(preferences)
        }
    }

    fun updateLanguage(languageCode: String) {
        viewModelScope.launch {
            settingsRepository.updateLanguage(languageCode)
        }
    }

    fun updateReminderTime(time: String) {
        viewModelScope.launch {
            settingsRepository.updateReminderTime(time)
        }
    }

    fun updateServerUsername(username: String) {
        viewModelScope.launch {
            settingsRepository.updateServerUsername(username)
        }
    }
}