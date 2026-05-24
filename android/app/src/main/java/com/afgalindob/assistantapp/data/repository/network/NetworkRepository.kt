package com.afgalindob.assistantapp.data.repository.network

import com.afgalindob.assistantapp.data.local.network.NetworkResult
import java.io.File

interface NetworkRepository {
    suspend fun processQrAuthData(qrJson: String): Result<Triple<String, String, String>>
    suspend fun processQrTokenData(qrJson: String): Result<Pair<String, String>>
    suspend fun checkServerHealth(): Boolean
    suspend fun checkCredentials(): Pair<NetworkResult, String>
    suspend fun requestAuth(endpoint: String, sessionId: String, typeQr: String): Pair<NetworkResult, String>
    suspend fun requestToken(endpoint: String, typeQr: String): Pair<NetworkResult, String>
    suspend fun uploadVoiceAudio(file: File): Pair<NetworkResult, String>
    suspend fun getProcessedAudios(): Triple<NetworkResult, String, String>
}