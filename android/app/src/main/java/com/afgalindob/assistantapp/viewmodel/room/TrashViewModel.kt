package com.afgalindob.assistantapp.viewmodel.room

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afgalindob.assistantapp.data.domain.AudioDomain
import com.afgalindob.assistantapp.data.domain.NoteDomain
import com.afgalindob.assistantapp.data.domain.TaskDomain
import com.afgalindob.assistantapp.data.repository.trash.TrashRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TrashViewModel(private val repository: TrashRepository) : ViewModel() {

    // ─── FLUJOS DIRECTOS DE DOMINIO PARA LA PAPELERA ───

    val deletedNotes: StateFlow<List<NoteDomain>> = repository.getDeletedNotes()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val deletedTasks: StateFlow<List<TaskDomain>> = repository.getDeletedTasks()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val deletedAudioFiles: StateFlow<List<AudioDomain>> = repository.getDeletedAudioFiles()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // ─── ACCIONES PARA TAREAS ───
    fun restoreTask(task: TaskDomain) {
        viewModelScope.launch { repository.restoreTask(task.id) }
    }

    fun reDeleteTask(id: Long, days: Long) {
        viewModelScope.launch { repository.reDeleteTask(id, days) }
    }

    fun deletePermanentTask(task: TaskDomain) {
        viewModelScope.launch { repository.permanentlyDeleteTask(task.id) }
    }

    // ─── ACCIONES PARA NOTAS ───
    fun restoreNote(note: NoteDomain) {
        viewModelScope.launch { repository.restoreNote(note.id) }
    }

    fun reDeleteNote(id: Long, days: Long) {
        viewModelScope.launch { repository.reDeleteNote(id, days) }
    }

    fun deletePermanentNote(note: NoteDomain) {
        viewModelScope.launch { repository.permanentlyDeleteNote(note.id) }
    }

    // ─── ACCIONES PARA AUDIOS (Adaptados a los ID y firmas de dominio) ───
    fun restoreAudioFile(audio: AudioDomain) {
        viewModelScope.launch { repository.restoreAudioFile(audio.id) }
    }

    fun reDeleteAudio(id: Long, days: Long) {
        viewModelScope.launch { repository.reDeleteAudio(id, days) }
    }

    fun deletePermanentAudio(audio: AudioDomain) {
        viewModelScope.launch { repository.permanentlyDeleteAudio(audio.id) }
    }
}