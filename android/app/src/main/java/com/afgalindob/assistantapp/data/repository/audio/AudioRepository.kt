package com.afgalindob.assistantapp.data.repository.audio

import com.afgalindob.assistantapp.data.domain.AudioDomain
import com.afgalindob.assistantapp.data.domain.AudioStatus
import com.afgalindob.assistantapp.data.local.network.NetworkResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface AudioRepository {

    // ─── MÉTODOS PARA EL REPRODUCTOR DE AUDIO ───
    val playbackDuration: StateFlow<Long>
    val playbackProgress: StateFlow<Long>
    val currentlyPlayingName: StateFlow<String?>

    fun togglePlayStop(fileName: String)
    fun seekTo(position: Long)
    fun seekRelative(milliseconds: Long)
    fun release()

    // ─── MÉTODOS DE BASE DE DATOS Y FLUJOS REACTIVOS (UI) ───
    fun getAllActiveRequestsStream(): Flow<List<AudioDomain>>
    fun getAllTrashedRequestsStream(): Flow<List<AudioDomain>>
    suspend fun getNextPendingRequestInQueue(): AudioDomain?

    // ─── CONTROL DE HARDWARE DE GRABACIÓN ───
    val isRecording: StateFlow<Boolean>
    fun startRecording()
    suspend fun stopRecording(): String?

    // ─── ACCIONES DE CONTROL Y SETTERS SEMÁNTICOS (Retornan Unit) ───
    suspend fun updateRequestStatusByName(fileName: String, status: AudioStatus): Boolean
    suspend fun setRequestAsCompletedWithExpiration(fileName: String): Boolean
    suspend fun sendToTrash(id: Long, days: Long)
    suspend fun restoreSetOnDeleteAudio(id: Long, days: Long)
    suspend fun restoreRequest(id: Long)
    suspend fun deletePermanently(id: Long): Boolean
    suspend fun purgeExpiredRequests()
    suspend fun processPendingUploadsQueue(): Pair<NetworkResult, String>
}