package com.afgalindob.assistantapp.data.local.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.afgalindob.assistantapp.data.local.room.entity.NoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes WHERE deleteAt = 0 ORDER BY createdAt ASC")
    fun getNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE deleteAt > 0 ORDER BY deleteAt ASC")
    fun getDeletedNotes(): Flow<List<NoteEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertNote(note: NoteEntity): Long

    @Query("UPDATE notes SET title = :title, content = :content, updatedAt = :now WHERE id = :id")
    suspend fun updateNoteFields(id: Long, title: String, content: String, now: Long)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNoteById(id: Long)

    @Query("UPDATE notes SET deleteAt = :timestamp WHERE id = :id")
    suspend fun setOnDeleteNote(id: Long, timestamp: Long)

    @Query("UPDATE notes SET deleteAt = 0 WHERE id = :id")
    suspend fun restoreNote(id: Long)

    @Query("DELETE FROM notes WHERE deleteAt != 0 AND deleteAt < :now")
    suspend fun deleteExpiredNotes(now: Long)
}