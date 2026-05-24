package com.afgalindob.assistantapp.data.manager

import com.afgalindob.assistantapp.data.local.network.ServerMessage
import com.afgalindob.assistantapp.data.local.network.ServerStatus
import kotlinx.coroutines.flow.StateFlow

interface GlobalStateManager {
    val hasNotificationPermission: StateFlow<Boolean>
    val serverStatus: StateFlow<ServerStatus>
    val serverMessage: StateFlow<ServerMessage?>
    val isUrlConfigured: StateFlow<Boolean>
    val isTokenConfigured: StateFlow<Boolean>

    fun refreshSystemStates()
    fun checkCommunicationWithServer(offlineAdvertise: Boolean = false)
    fun clearMessage()
    fun updateServerStatus(status: ServerStatus)
    fun setServerMessage(message: ServerMessage?)
}