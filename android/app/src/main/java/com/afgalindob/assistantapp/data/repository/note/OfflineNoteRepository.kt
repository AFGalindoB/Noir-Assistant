package com.afgalindob.assistantapp.data.repository.note

import com.afgalindob.assistantapp.data.domain.NoteDomain
import com.afgalindob.assistantapp.data.local.room.dao.NoteDao
import com.afgalindob.assistantapp.data.local.room.entity.NoteEntity
import com.afgalindob.assistantapp.data.mapper.toNoteDomainList
import com.afgalindob.assistantapp.utils.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class OfflineNoteRepository(
    private val noteDao: NoteDao
) : NoteRepository {

    override fun getNotes(): Flow<List<NoteDomain>> =
        noteDao.getNotes().map { it.toNoteDomainList() }

    override fun getDeletedNotes(): Flow<List<NoteDomain>> =
        noteDao.getDeletedNotes().map { it.toNoteDomainList() }

    override suspend fun insertNote(note: NoteDomain): Long = withContext(Dispatchers.IO) {
        val now = DateUtils.now()
        val entity = NoteEntity(
            title = note.title,
            content = note.content,
            createdAt = now,
            updatedAt = now,
            deleteAt = 0L
        )
        noteDao.insertNote(entity)
    }

    override suspend fun deleteNoteById(id: Long) =
        noteDao.deleteNoteById(id)

    override suspend fun updateNote(note: NoteDomain) = withContext(Dispatchers.IO) {
        noteDao.updateNoteFields(
            id = note.id,
            title = note.title,
            content = note.content,
            now = DateUtils.now()
        )
    }


    override suspend fun setOnDeleteNote(id: Long, days: Long) {
        val expirationTimestamp = DateUtils.getExpirationTimestamp(days)
        noteDao.setOnDeleteNote(id, expirationTimestamp)
    }

    override suspend fun restoreSetOnDeleteNote(id: Long, days: Long) =
        noteDao.setOnDeleteNote(id, days)

    override suspend fun restoreNote(id: Long) =
        noteDao.restoreNote(id)

    override suspend fun deleteExpiredNotes() = withContext(Dispatchers.IO) {
        val now = DateUtils.now()
        noteDao.deleteExpiredNotes(now)
    }
}