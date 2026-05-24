package com.afgalindob.assistantapp.viewmodel.room

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afgalindob.assistantapp.data.repository.note.NoteRepository
import com.afgalindob.assistantapp.data.domain.NoteDomain
import com.afgalindob.assistantapp.data.domain.NoteFormState
import com.afgalindob.assistantapp.data.domain.validation.validateNoteForm
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NoteViewModel(private val repository: NoteRepository) : ViewModel() {

    // ─── FLUJO REACTIVO DIRECTO DE DOMINIO ───
    val notesDomain: StateFlow<List<NoteDomain>> =
        repository.getNotes()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    fun validate(form: NoteFormState) = validateNoteForm(form)

    // ─── OPERACIONES DE ESCRITURA USANDO EL MODELO DE DOMINIO ───

    fun createNote(form: NoteFormState) {
        viewModelScope.launch {
            val note = NoteDomain(
                id = 0L,
                title = form.title,
                content = form.content,
                deleteAt = 0L
            )
            repository.insertNote(note)
        }
    }

    fun updateNote(note: NoteDomain, form: NoteFormState) {
        viewModelScope.launch {
            val updated = NoteDomain(
                id = note.id,
                title = form.title,
                content = form.content,
                deleteAt = note.deleteAt
            )
            repository.updateNote(updated)
        }
    }

    fun softDeleteNote(note: NoteDomain) {
        viewModelScope.launch {
            repository.setOnDeleteNote(id = note.id, days = 30)
        }
    }

    fun restoreNote(note: NoteDomain) {
        viewModelScope.launch {
            repository.restoreNote(note.id)
        }
    }
}