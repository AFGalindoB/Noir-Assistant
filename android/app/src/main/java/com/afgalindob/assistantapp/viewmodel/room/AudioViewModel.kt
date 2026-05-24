package com.afgalindob.assistantapp.viewmodel.room

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afgalindob.assistantapp.data.domain.AudioDomain
import com.afgalindob.assistantapp.data.domain.AudioStatus
import com.afgalindob.assistantapp.data.local.network.NetworkResult
import com.afgalindob.assistantapp.data.local.network.ServerMessage
import com.afgalindob.assistantapp.data.local.network.ServerStatus
import com.afgalindob.assistantapp.data.manager.GlobalStateManager
import com.afgalindob.assistantapp.data.repository.audio.AudioRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.afgalindob.assistantapp.utils.sections.audioSectionOrder
import com.afgalindob.assistantapp.utils.sections.getAudioSection
import kotlinx.coroutines.flow.MutableStateFlow

class AudioViewModel(
    private val audioRepository: AudioRepository,
    private val globalStateManager: GlobalStateManager
) : ViewModel() {

    val serverStatus: StateFlow<ServerStatus> = globalStateManager.serverStatus
    val isTokenConfigured: StateFlow<Boolean> = globalStateManager.isTokenConfigured

    private val isUploadingAudios = MutableStateFlow(false)
    val isUploadingAudiosState: StateFlow<Boolean> = isUploadingAudios

    val activeAudios: StateFlow<List<AudioDomain>> = audioRepository.getAllActiveRequestsStream()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val audiosBySection: StateFlow<Map<Int, List<AudioDomain>>> = activeAudios
        .map { list ->
            list.groupBy { it.getAudioSection() }
                .toSortedMap(compareBy { audioSectionOrder(it) })
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )

    // ─── ESTADOS Y CONTROLES DEL REPRODUCTOR (EXOPLAYER) ───
    val playbackProgressMs = audioRepository.playbackProgress
    val playbackDuration = audioRepository.playbackDuration
    val currentlyPlayingName = audioRepository.currentlyPlayingName

    fun togglePlayStop(fileName: String) {
        audioRepository.togglePlayStop(fileName)
    }

    fun seekRelative(milliseconds: Long) {
        audioRepository.seekRelative(milliseconds)
    }

    fun onSliderSeek(progress: Float) {
        val newPosition = (progress * playbackDuration.value).toLong()
        audioRepository.seekTo(newPosition)
    }

    // ─── ESTADOS Y CONTROLES DE GRABACIÓN (MICRÓFONO) ───
    val isRecording = audioRepository.isRecording

    fun toggleRecording() {
        if (isRecording.value) {
            viewModelScope.launch {
                audioRepository.stopRecording()
            }
        } else {
            audioRepository.startRecording()
        }
    }

    // ─── OPERACIONES DE PERSISTENCIA Y CICLO DE VIDA DE AUDIOS ───
    fun sendToTrash(audio: AudioDomain, daysToLive: Long = 30): Boolean {
        if (audio.status == AudioStatus.PROCESSING)
            return false

        viewModelScope.launch {
            val currentPlaying = currentlyPlayingName.value
            val audioToTrashName = activeAudios.value.find { it.id == audio.id }?.fileName

            if (currentPlaying != null && currentPlaying == audioToTrashName) {
                audioRepository.togglePlayStop(currentPlaying)
            }

            audioRepository.sendToTrash(audio.id, daysToLive)
        }

        return true
    }

    fun processPendingUploads(){
        viewModelScope.launch {
            if (!isUploadingAudios.value){
                isUploadingAudios.value = true

                val (result, message) = audioRepository.processPendingUploadsQueue()

                when (result) {
                    NetworkResult.SUCCESS -> {
                        globalStateManager.setServerMessage(ServerMessage("Audios procesados", message, false))
                    }
                    NetworkResult.LOGIC_ERROR -> {
                        globalStateManager.setServerMessage(ServerMessage("Error en la solicitud", message, true))
                    }
                    NetworkResult.CONNECTIVITY_ERROR -> {
                        globalStateManager.setServerMessage(ServerMessage("Fallo de Conexión", message, true))
                        globalStateManager.checkCommunicationWithServer()
                    }
                }
            }
        }
    }

    fun restoreAudio(id: Long) {
        viewModelScope.launch {
            audioRepository.restoreRequest(id)
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioRepository.release()
    }

    fun checkCommunicationWithServer() {
        globalStateManager.checkCommunicationWithServer(true)
    }
}