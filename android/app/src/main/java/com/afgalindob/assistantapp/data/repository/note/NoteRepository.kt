package com.afgalindob.assistantapp.data.repository.note

import com.afgalindob.assistantapp.data.domain.NoteDomain
import kotlinx.coroutines.flow.Flow

interface NoteRepository {

    fun getNotes(): Flow<List<NoteDomain>>

    fun getDeletedNotes(): Flow<List<NoteDomain>>

    suspend fun insertNote(note: NoteDomain): Long

    suspend fun deleteNoteById(id: Long)

    suspend fun updateNote(note: NoteDomain)

    suspend fun setOnDeleteNote(id: Long, days: Long)

    suspend fun restoreSetOnDeleteNote(id: Long, days: Long)

    suspend fun restoreNote(id: Long)

    suspend fun deleteExpiredNotes()
}

