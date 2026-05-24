package com.afgalindob.assistantapp.data.manager

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.afgalindob.assistantapp.data.local.network.NetworkResult
import com.afgalindob.assistantapp.data.local.network.ServerMessage
import com.afgalindob.assistantapp.data.local.network.ServerStatus
import com.afgalindob.assistantapp.data.repository.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AndroidGlobalStateManager(
    private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val externalScope: CoroutineScope
) : GlobalStateManager {

    private val _hasNotificationPermission = MutableStateFlow(checkNotificationPermission())
    override val hasNotificationPermission: StateFlow<Boolean> = _hasNotificationPermission.asStateFlow()

    private val _serverStatus = MutableStateFlow(ServerStatus.DISCONNECTED)
    override val serverStatus: StateFlow<ServerStatus> = _serverStatus.asStateFlow()

    private val _serverMessage = MutableStateFlow<ServerMessage?>(null)
    override val serverMessage: StateFlow<ServerMessage?> = _serverMessage.asStateFlow()

    override val isUrlConfigured: StateFlow<Boolean> = settingsRepository.isUrlConfigured
        .stateIn(
            scope = externalScope,
            started = SharingStarted.Eagerly,
            initialValue = false
        )

    override val isTokenConfigured: StateFlow<Boolean> = settingsRepository.isTokenConfigured
        .stateIn(
            scope = externalScope,
            started = SharingStarted.Eagerly,
            initialValue = false
        )

    override fun refreshSystemStates() {
        _hasNotificationPermission.value = checkNotificationPermission()
    }

    private fun checkNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun observeUsername() {
        externalScope.launch {
            settingsRepository.serverUsernameData.collect { username ->
                validateUsernameStatus(username)
            }
        }
    }

    private fun validateUsernameStatus(username: String) {
        if (username.isBlank()) {
            _serverStatus.value = ServerStatus.NAME_REQUIRED
        } else {
            checkCommunicationWithServer()
        }
    }

    override fun checkCommunicationWithServer(offlineAdvertise: Boolean) {
        externalScope.launch {
            if (!settingsRepository.hasUrlToAPI()) {
                _serverStatus.value = ServerStatus.UNLINKED
                return@launch
            }

            if (!settingsRepository.checkServerHealth()) {
                if (offlineAdvertise) {
                    _serverMessage.value = ServerMessage(
                        "Error de Conexión",
                        "No se pudo establecer conexión con el servidor",
                        true
                    )
                }
                _serverStatus.value = ServerStatus.DISCONNECTED
                return@launch
            }

            if (!settingsRepository.hasToken()) {
                _serverStatus.value = ServerStatus.READY_TO_CONNECT
                if (offlineAdvertise) {
                    _serverMessage.value = ServerMessage(
                        "No autorizado",
                        "Insuficientes permisos de autenticación",
                        true
                    )
                }
                return@launch
            } else {
                val (result, message) = settingsRepository.checkCredentials()

                when (result) {
                    NetworkResult.SUCCESS -> {
                        _serverStatus.value = ServerStatus.ONLINE
                    }
                    NetworkResult.LOGIC_ERROR -> {
                        _serverStatus.value = ServerStatus.READY_TO_CONNECT
                        _serverMessage.value = ServerMessage("Error en la solicitud", message, true)
                    }
                    NetworkResult.CONNECTIVITY_ERROR -> {
                        _serverMessage.value = ServerMessage("Fallo de Conexión", message, true)
                        checkCommunicationWithServer(offlineAdvertise = false)
                    }
                }
            }
        }
    }

    override fun clearMessage() {
        _serverMessage.value = null
    }

    override fun updateServerStatus(status: ServerStatus) {
        _serverStatus.value = status
    }

    override fun setServerMessage(message: ServerMessage?) {
        _serverMessage.value = message
    }
}